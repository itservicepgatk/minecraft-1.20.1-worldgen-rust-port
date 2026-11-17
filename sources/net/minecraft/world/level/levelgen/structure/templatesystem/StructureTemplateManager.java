package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureTemplateManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String STRUCTURE_DIRECTORY_NAME = "structures";
   private static final String TEST_STRUCTURES_DIR = "gameteststructures";
   private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
   private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
   private final Map<ResourceLocation, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
   private final DataFixer fixerUpper;
   private ResourceManager resourceManager;
   private final Path generatedDir;
   private final List<StructureTemplateManager.Source> sources;
   private final HolderGetter<Block> blockLookup;
   private static final FileToIdConverter LISTER = new FileToIdConverter("structures", ".nbt");

   public StructureTemplateManager(ResourceManager $$0, LevelStorageAccess $$1, DataFixer $$2, HolderGetter<Block> $$3) {
      this.resourceManager = $$0;
      this.fixerUpper = $$2;
      this.generatedDir = $$1.getLevelPath(LevelResource.GENERATED_DIR).normalize();
      this.blockLookup = $$3;
      Builder<StructureTemplateManager.Source> $$4 = ImmutableList.builder();
      $$4.add(new StructureTemplateManager.Source(this::loadFromGenerated, this::listGenerated));
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         $$4.add(new StructureTemplateManager.Source(this::loadFromTestStructures, this::listTestStructures));
      }

      $$4.add(new StructureTemplateManager.Source(this::loadFromResource, this::listResources));
      this.sources = $$4.build();
   }

   public StructureTemplate getOrCreate(ResourceLocation $$0) {
      Optional<StructureTemplate> $$1 = this.get($$0);
      if ($$1.isPresent()) {
         return $$1.get();
      } else {
         StructureTemplate $$2 = new StructureTemplate();
         this.structureRepository.put($$0, Optional.of($$2));
         return $$2;
      }
   }

   public Optional<StructureTemplate> get(ResourceLocation $$0) {
      return this.structureRepository.computeIfAbsent($$0, this::tryLoad);
   }

   public Stream<ResourceLocation> listTemplates() {
      return this.sources.stream().flatMap($$0 -> $$0.lister().get()).distinct();
   }

   private Optional<StructureTemplate> tryLoad(ResourceLocation $$0) {
      for (StructureTemplateManager.Source $$1 : this.sources) {
         try {
            Optional<StructureTemplate> $$2 = $$1.loader().apply($$0);
            if ($$2.isPresent()) {
               return $$2;
            }
         } catch (Exception var5) {
         }
      }

      return Optional.empty();
   }

   public void onResourceManagerReload(ResourceManager $$0) {
      this.resourceManager = $$0;
      this.structureRepository.clear();
   }

   private Optional<StructureTemplate> loadFromResource(ResourceLocation $$0) {
      ResourceLocation $$1 = LISTER.idToFile($$0);
      return this.load(() -> this.resourceManager.open($$1), $$1x -> LOGGER.error("Couldn't load structure {}", $$0, $$1x));
   }

   private Stream<ResourceLocation> listResources() {
      return LISTER.listMatchingResources(this.resourceManager).keySet().stream().map(LISTER::fileToId);
   }

   private Optional<StructureTemplate> loadFromTestStructures(ResourceLocation $$0) {
      return this.loadFromSnbt($$0, Paths.get("gameteststructures"));
   }

   private Stream<ResourceLocation> listTestStructures() {
      return this.listFolderContents(Paths.get("gameteststructures"), "minecraft", ".snbt");
   }

   private Optional<StructureTemplate> loadFromGenerated(ResourceLocation $$0) {
      if (!Files.isDirectory(this.generatedDir)) {
         return Optional.empty();
      } else {
         Path $$1 = createAndValidatePathToStructure(this.generatedDir, $$0, ".nbt");
         return this.load(() -> new FileInputStream($$1.toFile()), $$1x -> LOGGER.error("Couldn't load structure from {}", $$1, $$1x));
      }
   }

   private Stream<ResourceLocation> listGenerated() {
      if (!Files.isDirectory(this.generatedDir)) {
         return Stream.empty();
      } else {
         try {
            return Files.list(this.generatedDir).filter($$0 -> Files.isDirectory($$0)).flatMap($$0 -> this.listGeneratedInNamespace($$0));
         } catch (IOException var2) {
            return Stream.empty();
         }
      }
   }

   private Stream<ResourceLocation> listGeneratedInNamespace(Path $$0) {
      Path $$1 = $$0.resolve("structures");
      return this.listFolderContents($$1, $$0.getFileName().toString(), ".nbt");
   }

   private Stream<ResourceLocation> listFolderContents(Path $$0, String $$1, String $$2) {
      if (!Files.isDirectory($$0)) {
         return Stream.empty();
      } else {
         int $$3 = $$2.length();
         Function<String, String> $$4 = $$1x -> $$1x.substring(0, $$1x.length() - $$3);

         try {
            return Files.walk($$0).filter($$1x -> $$1x.toString().endsWith($$2)).mapMulti(($$3x, $$4x) -> {
               try {
                  $$4x.accept(new ResourceLocation($$1, $$4.apply(this.relativize($$0, $$3x))));
               } catch (ResourceLocationException var7x) {
                  LOGGER.error("Invalid location while listing pack contents", var7x);
               }
            });
         } catch (IOException var7) {
            LOGGER.error("Failed to list folder contents", var7);
            return Stream.empty();
         }
      }
   }

   private String relativize(Path $$0, Path $$1) {
      return $$0.relativize($$1).toString().replace(File.separator, "/");
   }

   private Optional<StructureTemplate> loadFromSnbt(ResourceLocation $$0, Path $$1) {
      if (!Files.isDirectory($$1)) {
         return Optional.empty();
      } else {
         Path $$2 = FileUtil.createPathToResource($$1, $$0.getPath(), ".snbt");

         try {
            Optional var6;
            try (BufferedReader $$3 = Files.newBufferedReader($$2)) {
               String $$4 = IOUtils.toString($$3);
               var6 = Optional.of(this.readStructure(NbtUtils.snbtToStructure($$4)));
            }

            return var6;
         } catch (NoSuchFileException var9) {
            return Optional.empty();
         } catch (CommandSyntaxException | IOException var10) {
            LOGGER.error("Couldn't load structure from {}", $$2, var10);
            return Optional.empty();
         }
      }
   }

   private Optional<StructureTemplate> load(StructureTemplateManager.InputStreamOpener $$0, Consumer<Throwable> $$1) {
      try {
         Optional var4;
         try (InputStream $$2 = $$0.open()) {
            var4 = Optional.of(this.readStructure($$2));
         }

         return var4;
      } catch (FileNotFoundException var8) {
         return Optional.empty();
      } catch (Throwable var9) {
         $$1.accept(var9);
         return Optional.empty();
      }
   }

   private StructureTemplate readStructure(InputStream $$0) throws IOException {
      CompoundTag $$1 = NbtIo.readCompressed($$0);
      return this.readStructure($$1);
   }

   public StructureTemplate readStructure(CompoundTag $$0) {
      StructureTemplate $$1 = new StructureTemplate();
      int $$2 = NbtUtils.getDataVersion($$0, 500);
      $$1.load(this.blockLookup, DataFixTypes.STRUCTURE.updateToCurrentVersion(this.fixerUpper, $$0, $$2));
      return $$1;
   }

   public boolean save(ResourceLocation $$0) {
      Optional<StructureTemplate> $$1 = this.structureRepository.get($$0);
      if (!$$1.isPresent()) {
         return false;
      } else {
         StructureTemplate $$2 = $$1.get();
         Path $$3 = createAndValidatePathToStructure(this.generatedDir, $$0, ".nbt");
         Path $$4 = $$3.getParent();
         if ($$4 == null) {
            return false;
         } else {
            try {
               Files.createDirectories(Files.exists($$4) ? $$4.toRealPath() : $$4);
            } catch (IOException var13) {
               LOGGER.error("Failed to create parent directory: {}", $$4);
               return false;
            }

            CompoundTag $$6 = $$2.save(new CompoundTag());

            try {
               try (OutputStream $$7 = new FileOutputStream($$3.toFile())) {
                  NbtIo.writeCompressed($$6, $$7);
               }

               return true;
            } catch (Throwable var12) {
               return false;
            }
         }
      }
   }

   public Path getPathToGeneratedStructure(ResourceLocation $$0, String $$1) {
      return createPathToStructure(this.generatedDir, $$0, $$1);
   }

   public static Path createPathToStructure(Path $$0, ResourceLocation $$1, String $$2) {
      try {
         Path $$3 = $$0.resolve($$1.getNamespace());
         Path $$4 = $$3.resolve("structures");
         return FileUtil.createPathToResource($$4, $$1.getPath(), $$2);
      } catch (InvalidPathException var5) {
         throw new ResourceLocationException("Invalid resource path: " + $$1, var5);
      }
   }

   private static Path createAndValidatePathToStructure(Path $$0, ResourceLocation $$1, String $$2) {
      if ($$1.getPath().contains("//")) {
         throw new ResourceLocationException("Invalid resource path: " + $$1);
      } else {
         Path $$3 = createPathToStructure($$0, $$1, $$2);
         if ($$3.startsWith($$0) && FileUtil.isPathNormalized($$3) && FileUtil.isPathPortable($$3)) {
            return $$3;
         } else {
            throw new ResourceLocationException("Invalid resource path: " + $$3);
         }
      }
   }

   public void remove(ResourceLocation $$0) {
      this.structureRepository.remove($$0);
   }

   @FunctionalInterface
   interface InputStreamOpener {
      InputStream open() throws IOException;
   }

   static record Source(Function<ResourceLocation, Optional<StructureTemplate>> loader, Supplier<Stream<ResourceLocation>> lister) {
   }
}
