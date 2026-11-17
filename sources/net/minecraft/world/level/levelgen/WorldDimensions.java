package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.RegistryAccess.ImmutableRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData.SpecialWorldProperty;

public record WorldDimensions(Registry<LevelStem> dimensions) {
   public static final MapCodec<WorldDimensions> CODEC = RecordCodecBuilder.mapCodec(
      $$0 -> $$0.group(
               RegistryCodecs.fullCodec(Registries.LEVEL_STEM, Lifecycle.stable(), LevelStem.CODEC)
                  .fieldOf("dimensions")
                  .forGetter(WorldDimensions::dimensions)
            )
            .apply($$0, $$0.stable(WorldDimensions::new))
   );
   private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);
   private static final int VANILLA_DIMENSION_COUNT = BUILTIN_ORDER.size();

   public WorldDimensions(Registry<LevelStem> dimensions) {
      LevelStem $$1 = dimensions.get(LevelStem.OVERWORLD);
      if ($$1 == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         this.dimensions = dimensions;
      }
   }

   public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> $$0) {
      return Stream.concat(BUILTIN_ORDER.stream(), $$0.filter($$0x -> !BUILTIN_ORDER.contains($$0x)));
   }

   public WorldDimensions replaceOverworldGenerator(RegistryAccess $$0, ChunkGenerator $$1) {
      Registry<DimensionType> $$2 = $$0.registryOrThrow(Registries.DIMENSION_TYPE);
      Registry<LevelStem> $$3 = withOverworld($$2, this.dimensions, $$1);
      return new WorldDimensions($$3);
   }

   public static Registry<LevelStem> withOverworld(Registry<DimensionType> $$0, Registry<LevelStem> $$1, ChunkGenerator $$2) {
      LevelStem $$3 = $$1.get(LevelStem.OVERWORLD);
      Holder<DimensionType> $$4 = (Holder<DimensionType>)($$3 == null ? $$0.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD) : $$3.type());
      return withOverworld($$1, $$4, $$2);
   }

   public static Registry<LevelStem> withOverworld(Registry<LevelStem> $$0, Holder<DimensionType> $$1, ChunkGenerator $$2) {
      WritableRegistry<LevelStem> $$3 = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.experimental());
      $$3.register(LevelStem.OVERWORLD, new LevelStem($$1, $$2), Lifecycle.stable());

      for (java.util.Map.Entry<ResourceKey<LevelStem>, LevelStem> $$4 : $$0.entrySet()) {
         ResourceKey<LevelStem> $$5 = $$4.getKey();
         if ($$5 != LevelStem.OVERWORLD) {
            $$3.register($$5, $$4.getValue(), $$0.lifecycle($$4.getValue()));
         }
      }

      return $$3.freeze();
   }

   public ChunkGenerator overworld() {
      LevelStem $$0 = this.dimensions.get(LevelStem.OVERWORLD);
      if ($$0 == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return $$0.generator();
      }
   }

   public Optional<LevelStem> get(ResourceKey<LevelStem> $$0) {
      return this.dimensions.getOptional($$0);
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return this.dimensions().entrySet().stream().map(java.util.Map.Entry::getKey).map(Registries::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   private static SpecialWorldProperty specialWorldProperty(Registry<LevelStem> $$0) {
      return $$0.getOptional(LevelStem.OVERWORLD).map($$0x -> {
         ChunkGenerator $$1 = $$0x.generator();
         if ($$1 instanceof DebugLevelSource) {
            return SpecialWorldProperty.DEBUG;
         } else {
            return $$1 instanceof FlatLevelSource ? SpecialWorldProperty.FLAT : SpecialWorldProperty.NONE;
         }
      }).orElse(SpecialWorldProperty.NONE);
   }

   static Lifecycle checkStability(ResourceKey<LevelStem> $$0, LevelStem $$1) {
      return isVanillaLike($$0, $$1) ? Lifecycle.stable() : Lifecycle.experimental();
   }

   private static boolean isVanillaLike(ResourceKey<LevelStem> $$0, LevelStem $$1) {
      if ($$0 == LevelStem.OVERWORLD) {
         return isStableOverworld($$1);
      } else if ($$0 == LevelStem.NETHER) {
         return isStableNether($$1);
      } else {
         return $$0 == LevelStem.END ? isStableEnd($$1) : false;
      }
   }

   private static boolean isStableOverworld(LevelStem $$0) {
      Holder<DimensionType> $$1 = $$0.type();
      if (!$$1.is(BuiltinDimensionTypes.OVERWORLD) && !$$1.is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
         return false;
      } else {
         if ($$0.generator().getBiomeSource() instanceof MultiNoiseBiomeSource $$2 && !$$2.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) {
            return false;
         }

         return true;
      }
   }

   private static boolean isStableNether(LevelStem $$0) {
      return $$0.type().is(BuiltinDimensionTypes.NETHER)
         && $$0.generator() instanceof NoiseBasedChunkGenerator $$1
         && $$1.stable(NoiseGeneratorSettings.NETHER)
         && $$1.getBiomeSource() instanceof MultiNoiseBiomeSource $$2
         && $$2.stable(MultiNoiseBiomeSourceParameterLists.NETHER);
   }

   private static boolean isStableEnd(LevelStem $$0) {
      return $$0.type().is(BuiltinDimensionTypes.END)
         && $$0.generator() instanceof NoiseBasedChunkGenerator $$1
         && $$1.stable(NoiseGeneratorSettings.END)
         && $$1.getBiomeSource() instanceof TheEndBiomeSource;
   }

   public WorldDimensions.Complete bake(Registry<LevelStem> $$0) {
      Stream<ResourceKey<LevelStem>> $$1 = Stream.concat($$0.registryKeySet().stream(), this.dimensions.registryKeySet().stream()).distinct();

      record Entry(ResourceKey<LevelStem> key, LevelStem value) {

         Lifecycle lifecycle() {
            return WorldDimensions.checkStability(this.key, this.value);
         }
      }

      List<Entry> $$2 = new ArrayList<>();
      keysInOrder($$1).forEach($$2x -> $$0.getOptional($$2x).or(() -> this.dimensions.getOptional($$2x)).ifPresent($$2xx -> $$2.add(new Entry($$2x, $$2xx))));
      Lifecycle $$3 = $$2.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
      WritableRegistry<LevelStem> $$4 = new MappedRegistry<>(Registries.LEVEL_STEM, $$3);
      $$2.forEach($$1x -> $$4.register($$1x.key, $$1x.value, $$1x.lifecycle()));
      Registry<LevelStem> $$5 = $$4.freeze();
      SpecialWorldProperty $$6 = specialWorldProperty($$5);
      return new WorldDimensions.Complete($$5.freeze(), $$6);
   }

   public static record Complete(Registry<LevelStem> dimensions, SpecialWorldProperty specialWorldProperty) {
      public Lifecycle lifecycle() {
         return this.dimensions.registryLifecycle();
      }

      public Frozen dimensionsRegistryAccess() {
         return new ImmutableRegistryAccess(List.of(this.dimensions)).freeze();
      }
   }
}
