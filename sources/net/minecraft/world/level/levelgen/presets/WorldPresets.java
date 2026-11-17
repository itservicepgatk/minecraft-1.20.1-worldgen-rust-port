package net.minecraft.world.level.levelgen.presets;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class WorldPresets {
   public static final ResourceKey<WorldPreset> NORMAL = register("normal");
   public static final ResourceKey<WorldPreset> FLAT = register("flat");
   public static final ResourceKey<WorldPreset> LARGE_BIOMES = register("large_biomes");
   public static final ResourceKey<WorldPreset> AMPLIFIED = register("amplified");
   public static final ResourceKey<WorldPreset> SINGLE_BIOME_SURFACE = register("single_biome_surface");
   public static final ResourceKey<WorldPreset> DEBUG = register("debug_all_block_states");

   public static void bootstrap(BootstapContext<WorldPreset> $$0) {
      new WorldPresets.Bootstrap($$0).bootstrap();
   }

   private static ResourceKey<WorldPreset> register(String $$0) {
      return ResourceKey.create(Registries.WORLD_PRESET, new ResourceLocation($$0));
   }

   public static Optional<ResourceKey<WorldPreset>> fromSettings(Registry<LevelStem> $$0) {
      return $$0.getOptional(LevelStem.OVERWORLD).flatMap($$0x -> {
         ChunkGenerator $$1 = $$0x.generator();
         if ($$1 instanceof FlatLevelSource) {
            return Optional.of(FLAT);
         } else {
            return $$1 instanceof DebugLevelSource ? Optional.of(DEBUG) : Optional.empty();
         }
      });
   }

   public static WorldDimensions createNormalWorldDimensions(RegistryAccess $$0) {
      return $$0.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(NORMAL).value().createWorldDimensions();
   }

   public static LevelStem getNormalOverworld(RegistryAccess $$0) {
      return $$0.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(NORMAL).value().overworld().orElseThrow();
   }

   static class Bootstrap {
      private final BootstapContext<WorldPreset> context;
      private final HolderGetter<NoiseGeneratorSettings> noiseSettings;
      private final HolderGetter<Biome> biomes;
      private final HolderGetter<PlacedFeature> placedFeatures;
      private final HolderGetter<StructureSet> structureSets;
      private final HolderGetter<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterLists;
      private final Holder<DimensionType> overworldDimensionType;
      private final LevelStem netherStem;
      private final LevelStem endStem;

      Bootstrap(BootstapContext<WorldPreset> $$0) {
         this.context = $$0;
         HolderGetter<DimensionType> $$1 = $$0.lookup(Registries.DIMENSION_TYPE);
         this.noiseSettings = $$0.lookup(Registries.NOISE_SETTINGS);
         this.biomes = $$0.lookup(Registries.BIOME);
         this.placedFeatures = $$0.lookup(Registries.PLACED_FEATURE);
         this.structureSets = $$0.lookup(Registries.STRUCTURE_SET);
         this.multiNoiseBiomeSourceParameterLists = $$0.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
         this.overworldDimensionType = $$1.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
         Holder<DimensionType> $$2 = $$1.getOrThrow(BuiltinDimensionTypes.NETHER);
         Holder<NoiseGeneratorSettings> $$3 = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.NETHER);
         Reference<MultiNoiseBiomeSourceParameterList> $$4 = this.multiNoiseBiomeSourceParameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER);
         this.netherStem = new LevelStem($$2, new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset($$4), $$3));
         Holder<DimensionType> $$5 = $$1.getOrThrow(BuiltinDimensionTypes.END);
         Holder<NoiseGeneratorSettings> $$6 = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.END);
         this.endStem = new LevelStem($$5, new NoiseBasedChunkGenerator(TheEndBiomeSource.create(this.biomes), $$6));
      }

      private LevelStem makeOverworld(ChunkGenerator $$0) {
         return new LevelStem(this.overworldDimensionType, $$0);
      }

      private LevelStem makeNoiseBasedOverworld(BiomeSource $$0, Holder<NoiseGeneratorSettings> $$1) {
         return this.makeOverworld(new NoiseBasedChunkGenerator($$0, $$1));
      }

      private WorldPreset createPresetWithCustomOverworld(LevelStem $$0) {
         return new WorldPreset(Map.of(LevelStem.OVERWORLD, $$0, LevelStem.NETHER, this.netherStem, LevelStem.END, this.endStem));
      }

      private void registerCustomOverworldPreset(ResourceKey<WorldPreset> $$0, LevelStem $$1) {
         this.context.register($$0, this.createPresetWithCustomOverworld($$1));
      }

      private void registerOverworlds(BiomeSource $$0) {
         Holder<NoiseGeneratorSettings> $$1 = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
         this.registerCustomOverworldPreset(WorldPresets.NORMAL, this.makeNoiseBasedOverworld($$0, $$1));
         Holder<NoiseGeneratorSettings> $$2 = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
         this.registerCustomOverworldPreset(WorldPresets.LARGE_BIOMES, this.makeNoiseBasedOverworld($$0, $$2));
         Holder<NoiseGeneratorSettings> $$3 = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.AMPLIFIED);
         this.registerCustomOverworldPreset(WorldPresets.AMPLIFIED, this.makeNoiseBasedOverworld($$0, $$3));
      }

      public void bootstrap() {
         Reference<MultiNoiseBiomeSourceParameterList> $$0 = this.multiNoiseBiomeSourceParameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
         this.registerOverworlds(MultiNoiseBiomeSource.createFromPreset($$0));
         Holder<NoiseGeneratorSettings> $$1 = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
         Reference<Biome> $$2 = this.biomes.getOrThrow(Biomes.PLAINS);
         this.registerCustomOverworldPreset(WorldPresets.SINGLE_BIOME_SURFACE, this.makeNoiseBasedOverworld(new FixedBiomeSource($$2), $$1));
         this.registerCustomOverworldPreset(
            WorldPresets.FLAT,
            this.makeOverworld(new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(this.biomes, this.structureSets, this.placedFeatures)))
         );
         this.registerCustomOverworldPreset(WorldPresets.DEBUG, this.makeOverworld(new DebugLevelSource($$2)));
      }
   }
}
