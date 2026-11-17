package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class StructureCheck {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_STRUCTURE = -1;
   private final ChunkScanAccess storageAccess;
   private final RegistryAccess registryAccess;
   private final Registry<Biome> biomes;
   private final Registry<Structure> structureConfigs;
   private final StructureTemplateManager structureTemplateManager;
   private final ResourceKey<Level> dimension;
   private final ChunkGenerator chunkGenerator;
   private final RandomState randomState;
   private final LevelHeightAccessor heightAccessor;
   private final BiomeSource biomeSource;
   private final long seed;
   private final DataFixer fixerUpper;
   private final Long2ObjectMap<Object2IntMap<Structure>> loadedChunks = new Long2ObjectOpenHashMap();
   private final Map<Structure, Long2BooleanMap> featureChecks = new HashMap<>();

   public StructureCheck(
      ChunkScanAccess $$0,
      RegistryAccess $$1,
      StructureTemplateManager $$2,
      ResourceKey<Level> $$3,
      ChunkGenerator $$4,
      RandomState $$5,
      LevelHeightAccessor $$6,
      BiomeSource $$7,
      long $$8,
      DataFixer $$9
   ) {
      this.storageAccess = $$0;
      this.registryAccess = $$1;
      this.structureTemplateManager = $$2;
      this.dimension = $$3;
      this.chunkGenerator = $$4;
      this.randomState = $$5;
      this.heightAccessor = $$6;
      this.biomeSource = $$7;
      this.seed = $$8;
      this.fixerUpper = $$9;
      this.biomes = $$1.registryOrThrow(Registries.BIOME);
      this.structureConfigs = $$1.registryOrThrow(Registries.STRUCTURE);
   }

   public StructureCheckResult checkStart(ChunkPos $$0, Structure $$1, boolean $$2) {
      long $$3 = $$0.toLong();
      Object2IntMap<Structure> $$4 = (Object2IntMap<Structure>)this.loadedChunks.get($$3);
      if ($$4 != null) {
         return this.checkStructureInfo($$4, $$1, $$2);
      } else {
         StructureCheckResult $$5 = this.tryLoadFromStorage($$0, $$1, $$2, $$3);
         if ($$5 != null) {
            return $$5;
         } else {
            boolean $$6 = this.featureChecks
               .computeIfAbsent($$1, $$0x -> new Long2BooleanOpenHashMap())
               .computeIfAbsent($$3, $$2x -> this.canCreateStructure($$0, $$1));
            return !$$6 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
         }
      }
   }

   private boolean canCreateStructure(ChunkPos $$0, Structure $$1) {
      return $$1.findValidGenerationPoint(
            new Structure.GenerationContext(
               this.registryAccess,
               this.chunkGenerator,
               this.biomeSource,
               this.randomState,
               this.structureTemplateManager,
               this.seed,
               $$0,
               this.heightAccessor,
               $$1.biomes()::contains
            )
         )
         .isPresent();
   }

   @Nullable
   private StructureCheckResult tryLoadFromStorage(ChunkPos $$0, Structure $$1, boolean $$2, long $$3) {
      CollectFields $$4 = new CollectFields(
         new FieldSelector(IntTag.TYPE, "DataVersion"),
         new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"),
         new FieldSelector("structures", CompoundTag.TYPE, "starts")
      );

      try {
         this.storageAccess.scanChunk($$0, $$4).join();
      } catch (Exception var13) {
         LOGGER.warn("Failed to read chunk {}", $$0, var13);
         return StructureCheckResult.CHUNK_LOAD_NEEDED;
      }

      if (!($$4.getResult() instanceof CompoundTag $$7)) {
         return null;
      } else {
         int $$8 = ChunkStorage.getVersion($$7);
         if ($$8 <= 1493) {
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         } else {
            ChunkStorage.injectDatafixingContext($$7, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

            CompoundTag $$9;
            try {
               $$9 = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, $$7, $$8);
            } catch (Exception var12) {
               LOGGER.warn("Failed to partially datafix chunk {}", $$0, var12);
               return StructureCheckResult.CHUNK_LOAD_NEEDED;
            }

            Object2IntMap<Structure> $$12 = this.loadStructures($$9);
            if ($$12 == null) {
               return null;
            } else {
               this.storeFullResults($$3, $$12);
               return this.checkStructureInfo($$12, $$1, $$2);
            }
         }
      }
   }

   @Nullable
   private Object2IntMap<Structure> loadStructures(CompoundTag $$0) {
      if (!$$0.contains("structures", 10)) {
         return null;
      } else {
         CompoundTag $$1 = $$0.getCompound("structures");
         if (!$$1.contains("starts", 10)) {
            return null;
         } else {
            CompoundTag $$2 = $$1.getCompound("starts");
            if ($$2.isEmpty()) {
               return Object2IntMaps.emptyMap();
            } else {
               Object2IntMap<Structure> $$3 = new Object2IntOpenHashMap();
               Registry<Structure> $$4 = this.registryAccess.registryOrThrow(Registries.STRUCTURE);

               for (String $$5 : $$2.getAllKeys()) {
                  ResourceLocation $$6 = ResourceLocation.tryParse($$5);
                  if ($$6 != null) {
                     Structure $$7 = $$4.get($$6);
                     if ($$7 != null) {
                        CompoundTag $$8 = $$2.getCompound($$5);
                        if (!$$8.isEmpty()) {
                           String $$9 = $$8.getString("id");
                           if (!"INVALID".equals($$9)) {
                              int $$10 = $$8.getInt("references");
                              $$3.put($$7, $$10);
                           }
                        }
                     }
                  }
               }

               return $$3;
            }
         }
      }
   }

   private static Object2IntMap<Structure> deduplicateEmptyMap(Object2IntMap<Structure> $$0) {
      return $$0.isEmpty() ? Object2IntMaps.emptyMap() : $$0;
   }

   private StructureCheckResult checkStructureInfo(Object2IntMap<Structure> $$0, Structure $$1, boolean $$2) {
      int $$3 = $$0.getOrDefault($$1, -1);
      return $$3 == -1 || $$2 && $$3 != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
   }

   public void onStructureLoad(ChunkPos $$0, Map<Structure, StructureStart> $$1) {
      long $$2 = $$0.toLong();
      Object2IntMap<Structure> $$3 = new Object2IntOpenHashMap();
      $$1.forEach(($$1x, $$2x) -> {
         if ($$2x.isValid()) {
            $$3.put($$1x, $$2x.getReferences());
         }
      });
      this.storeFullResults($$2, $$3);
   }

   private void storeFullResults(long $$0, Object2IntMap<Structure> $$1) {
      this.loadedChunks.put($$0, deduplicateEmptyMap($$1));
      this.featureChecks.values().forEach($$1x -> $$1x.remove($$0));
   }

   public void incrementReference(ChunkPos $$0, Structure $$1) {
      this.loadedChunks.compute($$0.toLong(), ($$1x, $$2) -> {
         if ($$2 == null || $$2.isEmpty()) {
            $$2 = new Object2IntOpenHashMap();
         }

         $$2.computeInt($$1, ($$0xx, $$1xx) -> $$1xx == null ? 1 : $$1xx + 1);
         return $$2;
      });
   }
}
