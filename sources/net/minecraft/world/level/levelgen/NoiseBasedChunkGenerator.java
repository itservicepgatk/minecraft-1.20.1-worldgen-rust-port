package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final Codec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(
               BiomeSource.CODEC.fieldOf("biome_source").forGetter($$0x -> $$0x.biomeSource),
               NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter($$0x -> $$0x.settings)
            )
            .apply($$0, $$0.stable(NoiseBasedChunkGenerator::new))
   );
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final Holder<NoiseGeneratorSettings> settings;
   private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

   public NoiseBasedChunkGenerator(BiomeSource $$0, Holder<NoiseGeneratorSettings> $$1) {
      super($$0);
      this.settings = $$1;
      this.globalFluidPicker = Suppliers.memoize(() -> createFluidPicker($$1.value()));
   }

   private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings $$0) {
      Aquifer.FluidStatus $$1 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      int $$2 = $$0.seaLevel();
      Aquifer.FluidStatus $$3 = new Aquifer.FluidStatus($$2, $$0.defaultFluid());
      Aquifer.FluidStatus $$4 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
      return ($$4x, $$5, $$6) -> $$5 < Math.min(-54, $$2) ? $$1 : $$3;
   }

   @Override
   public CompletableFuture<ChunkAccess> createBiomes(Executor $$0, RandomState $$1, Blender $$2, StructureManager $$3, ChunkAccess $$4) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         this.doCreateBiomes($$2, $$1, $$3, $$4);
         return $$4;
      }), Util.backgroundExecutor());
   }

   private void doCreateBiomes(Blender $$0, RandomState $$1, StructureManager $$2, ChunkAccess $$3) {
      NoiseChunk $$4 = $$3.getOrCreateNoiseChunk($$3x -> this.createNoiseChunk($$3x, $$2, $$0, $$1));
      BiomeResolver $$5 = BelowZeroRetrogen.getBiomeResolver($$0.getBiomeResolver(this.biomeSource), $$3);
      $$3.fillBiomesFromNoise($$5, $$4.cachedClimateSampler($$1.router(), this.settings.value().spawnTarget()));
   }

   private NoiseChunk createNoiseChunk(ChunkAccess $$0, StructureManager $$1, Blender $$2, RandomState $$3) {
      return NoiseChunk.forChunk($$0, $$3, Beardifier.forStructuresInChunk($$1, $$0.getPos()), this.settings.value(), this.globalFluidPicker.get(), $$2);
   }

   @Override
   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public Holder<NoiseGeneratorSettings> generatorSettings() {
      return this.settings;
   }

   public boolean stable(ResourceKey<NoiseGeneratorSettings> $$0) {
      return this.settings.is($$0);
   }

   @Override
   public int getBaseHeight(int $$0, int $$1, Heightmap.Types $$2, LevelHeightAccessor $$3, RandomState $$4) {
      return this.iterateNoiseColumn($$3, $$4, $$0, $$1, null, $$2.isOpaque()).orElse($$3.getMinBuildHeight());
   }

   @Override
   public NoiseColumn getBaseColumn(int $$0, int $$1, LevelHeightAccessor $$2, RandomState $$3) {
      MutableObject<NoiseColumn> $$4 = new MutableObject();
      this.iterateNoiseColumn($$2, $$3, $$0, $$1, $$4, null);
      return (NoiseColumn)$$4.getValue();
   }

   @Override
   public void addDebugScreenInfo(List<String> $$0, RandomState $$1, BlockPos $$2) {
      DecimalFormat $$3 = new DecimalFormat("0.000");
      NoiseRouter $$4 = $$1.router();
      DensityFunction.SinglePointContext $$5 = new DensityFunction.SinglePointContext($$2.getX(), $$2.getY(), $$2.getZ());
      double $$6 = $$4.ridges().compute($$5);
      $$0.add(
         "NoiseRouter T: "
            + $$3.format($$4.temperature().compute($$5))
            + " V: "
            + $$3.format($$4.vegetation().compute($$5))
            + " C: "
            + $$3.format($$4.continents().compute($$5))
            + " E: "
            + $$3.format($$4.erosion().compute($$5))
            + " D: "
            + $$3.format($$4.depth().compute($$5))
            + " W: "
            + $$3.format($$6)
            + " PV: "
            + $$3.format((double)NoiseRouterData.peaksAndValleys((float)$$6))
            + " AS: "
            + $$3.format($$4.initialDensityWithoutJaggedness().compute($$5))
            + " N: "
            + $$3.format($$4.finalDensity().compute($$5))
      );
   }

   private OptionalInt iterateNoiseColumn(
      LevelHeightAccessor $$0, RandomState $$1, int $$2, int $$3, @Nullable MutableObject<NoiseColumn> $$4, @Nullable Predicate<BlockState> $$5
   ) {
      NoiseSettings $$6 = this.settings.value().noiseSettings().clampToHeightAccessor($$0);
      int $$7 = $$6.getCellHeight();
      int $$8 = $$6.minY();
      int $$9 = Mth.floorDiv($$8, $$7);
      int $$10 = Mth.floorDiv($$6.height(), $$7);
      if ($$10 <= 0) {
         return OptionalInt.empty();
      } else {
         BlockState[] $$11;
         if ($$4 == null) {
            $$11 = null;
         } else {
            $$11 = new BlockState[$$6.height()];
            $$4.setValue(new NoiseColumn($$8, $$11));
         }

         int $$13 = $$6.getCellWidth();
         int $$14 = Math.floorDiv($$2, $$13);
         int $$15 = Math.floorDiv($$3, $$13);
         int $$16 = Math.floorMod($$2, $$13);
         int $$17 = Math.floorMod($$3, $$13);
         int $$18 = $$14 * $$13;
         int $$19 = $$15 * $$13;
         double $$20 = (double)$$16 / (double)$$13;
         double $$21 = (double)$$17 / (double)$$13;
         NoiseChunk $$22 = new NoiseChunk(
            1, $$1, $$18, $$19, $$6, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker.get(), Blender.empty()
         );
         $$22.initializeForFirstCellX();
         $$22.advanceCellX(0);

         for (int $$23 = $$10 - 1; $$23 >= 0; $$23--) {
            $$22.selectCellYZ($$23, 0);

            for (int $$24 = $$7 - 1; $$24 >= 0; $$24--) {
               int $$25 = ($$9 + $$23) * $$7 + $$24;
               double $$26 = (double)$$24 / (double)$$7;
               $$22.updateForY($$25, $$26);
               $$22.updateForX($$2, $$20);
               $$22.updateForZ($$3, $$21);
               BlockState $$27 = $$22.getInterpolatedState();
               BlockState $$28 = $$27 == null ? this.settings.value().defaultBlock() : $$27;
               if ($$11 != null) {
                  int $$29 = $$23 * $$7 + $$24;
                  $$11[$$29] = $$28;
               }

               if ($$5 != null && $$5.test($$28)) {
                  $$22.stopInterpolation();
                  return OptionalInt.of($$25 + 1);
               }
            }
         }

         $$22.stopInterpolation();
         return OptionalInt.empty();
      }
   }

   @Override
   public void buildSurface(WorldGenRegion $$0, StructureManager $$1, RandomState $$2, ChunkAccess $$3) {
      if (!SharedConstants.debugVoidTerrain($$3.getPos())) {
         WorldGenerationContext $$4 = new WorldGenerationContext(this, $$0);
         this.buildSurface($$3, $$4, $$2, $$1, $$0.getBiomeManager(), $$0.registryAccess().registryOrThrow(Registries.BIOME), Blender.of($$0));
      }
   }

   @VisibleForTesting
   public void buildSurface(
      ChunkAccess $$0, WorldGenerationContext $$1, RandomState $$2, StructureManager $$3, BiomeManager $$4, Registry<Biome> $$5, Blender $$6
   ) {
      NoiseChunk $$7 = $$0.getOrCreateNoiseChunk($$3x -> this.createNoiseChunk($$3x, $$3, $$6, $$2));
      NoiseGeneratorSettings $$8 = this.settings.value();
      $$2.surfaceSystem().buildSurface($$2, $$4, $$5, $$8.useLegacyRandomSource(), $$1, $$0, $$7, $$8.surfaceRule());
   }

   @Override
   public void applyCarvers(WorldGenRegion $$0, long $$1, RandomState $$2, BiomeManager $$3, StructureManager $$4, ChunkAccess $$5, GenerationStep.Carving $$6) {
      BiomeManager $$7 = $$3.withDifferentSource(($$1x, $$2x, $$3x) -> this.biomeSource.getNoiseBiome($$1x, $$2x, $$3x, $$2.sampler()));
      WorldgenRandom $$8 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      int $$9 = 8;
      ChunkPos $$10 = $$5.getPos();
      NoiseChunk $$11 = $$5.getOrCreateNoiseChunk($$3x -> this.createNoiseChunk($$3x, $$4, Blender.of($$0), $$2));
      Aquifer $$12 = $$11.aquifer();
      CarvingContext $$13 = new CarvingContext(this, $$0.registryAccess(), $$5.getHeightAccessorForGeneration(), $$11, $$2, this.settings.value().surfaceRule());
      CarvingMask $$14 = ((ProtoChunk)$$5).getOrCreateCarvingMask($$6);

      for (int $$15 = -8; $$15 <= 8; $$15++) {
         for (int $$16 = -8; $$16 <= 8; $$16++) {
            ChunkPos $$17 = new ChunkPos($$10.x + $$15, $$10.z + $$16);
            ChunkAccess $$18 = $$0.getChunk($$17.x, $$17.z);
            BiomeGenerationSettings $$19 = $$18.carverBiome(
               () -> this.getBiomeGenerationSettings(
                     this.biomeSource.getNoiseBiome(QuartPos.fromBlock($$17.getMinBlockX()), 0, QuartPos.fromBlock($$17.getMinBlockZ()), $$2.sampler())
                  )
            );
            Iterable<Holder<ConfiguredWorldCarver<?>>> $$20 = $$19.getCarvers($$6);
            int $$21 = 0;

            for (Holder<ConfiguredWorldCarver<?>> $$22 : $$20) {
               ConfiguredWorldCarver<?> $$23 = $$22.value();
               $$8.setLargeFeatureSeed($$1 + (long)$$21, $$17.x, $$17.z);
               if ($$23.isStartChunk($$8)) {
                  $$23.carve($$13, $$5, $$7::getBiome, $$8, $$12, $$17, $$14);
               }

               $$21++;
            }
         }
      }
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Executor $$0, Blender $$1, RandomState $$2, StructureManager $$3, ChunkAccess $$4) {
      NoiseSettings $$5 = this.settings.value().noiseSettings().clampToHeightAccessor($$4.getHeightAccessorForGeneration());
      int $$6 = $$5.minY();
      int $$7 = Mth.floorDiv($$6, $$5.getCellHeight());
      int $$8 = Mth.floorDiv($$5.height(), $$5.getCellHeight());
      if ($$8 <= 0) {
         return CompletableFuture.completedFuture($$4);
      } else {
         int $$9 = $$4.getSectionIndex($$8 * $$5.getCellHeight() - 1 + $$6);
         int $$10 = $$4.getSectionIndex($$6);
         Set<LevelChunkSection> $$11 = Sets.newHashSet();

         for (int $$12 = $$9; $$12 >= $$10; $$12--) {
            LevelChunkSection $$13 = $$4.getSection($$12);
            $$13.acquire();
            $$11.add($$13);
         }

         return CompletableFuture.supplyAsync(
               Util.wrapThreadWithTaskName("wgen_fill_noise", () -> this.doFill($$1, $$3, $$2, $$4, $$7, $$8)), Util.backgroundExecutor()
            )
            .whenCompleteAsync(($$1x, $$2x) -> {
               for (LevelChunkSection $$3x : $$11) {
                  $$3x.release();
               }
            }, $$0);
      }
   }

   private ChunkAccess doFill(Blender $$0, StructureManager $$1, RandomState $$2, ChunkAccess $$3, int $$4, int $$5) {
      NoiseChunk $$6 = $$3.getOrCreateNoiseChunk($$3x -> this.createNoiseChunk($$3x, $$1, $$0, $$2));
      Heightmap $$7 = $$3.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap $$8 = $$3.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos $$9 = $$3.getPos();
      int $$10 = $$9.getMinBlockX();
      int $$11 = $$9.getMinBlockZ();
      Aquifer $$12 = $$6.aquifer();
      $$6.initializeForFirstCellX();
      MutableBlockPos $$13 = new MutableBlockPos();
      int $$14 = $$6.cellWidth();
      int $$15 = $$6.cellHeight();
      int $$16 = 16 / $$14;
      int $$17 = 16 / $$14;

      for (int $$18 = 0; $$18 < $$16; $$18++) {
         $$6.advanceCellX($$18);

         for (int $$19 = 0; $$19 < $$17; $$19++) {
            int $$20 = $$3.getSectionsCount() - 1;
            LevelChunkSection $$21 = $$3.getSection($$20);

            for (int $$22 = $$5 - 1; $$22 >= 0; $$22--) {
               $$6.selectCellYZ($$22, $$19);

               for (int $$23 = $$15 - 1; $$23 >= 0; $$23--) {
                  int $$24 = ($$4 + $$22) * $$15 + $$23;
                  int $$25 = $$24 & 15;
                  int $$26 = $$3.getSectionIndex($$24);
                  if ($$20 != $$26) {
                     $$20 = $$26;
                     $$21 = $$3.getSection($$26);
                  }

                  double $$27 = (double)$$23 / (double)$$15;
                  $$6.updateForY($$24, $$27);

                  for (int $$28 = 0; $$28 < $$14; $$28++) {
                     int $$29 = $$10 + $$18 * $$14 + $$28;
                     int $$30 = $$29 & 15;
                     double $$31 = (double)$$28 / (double)$$14;
                     $$6.updateForX($$29, $$31);

                     for (int $$32 = 0; $$32 < $$14; $$32++) {
                        int $$33 = $$11 + $$19 * $$14 + $$32;
                        int $$34 = $$33 & 15;
                        double $$35 = (double)$$32 / (double)$$14;
                        $$6.updateForZ($$33, $$35);
                        BlockState $$36 = $$6.getInterpolatedState();
                        if ($$36 == null) {
                           $$36 = this.settings.value().defaultBlock();
                        }

                        $$36 = this.debugPreliminarySurfaceLevel($$6, $$29, $$24, $$33, $$36);
                        if ($$36 != AIR && !SharedConstants.debugVoidTerrain($$3.getPos())) {
                           $$21.setBlockState($$30, $$25, $$34, $$36, false);
                           $$7.update($$30, $$24, $$34, $$36);
                           $$8.update($$30, $$24, $$34, $$36);
                           if ($$12.shouldScheduleFluidUpdate() && !$$36.getFluidState().isEmpty()) {
                              $$13.set($$29, $$24, $$33);
                              $$3.markPosForPostprocessing($$13);
                           }
                        }
                     }
                  }
               }
            }
         }

         $$6.swapSlices();
      }

      $$6.stopInterpolation();
      return $$3;
   }

   private BlockState debugPreliminarySurfaceLevel(NoiseChunk $$0, int $$1, int $$2, int $$3, BlockState $$4) {
      return $$4;
   }

   @Override
   public int getGenDepth() {
      return this.settings.value().noiseSettings().height();
   }

   @Override
   public int getSeaLevel() {
      return this.settings.value().seaLevel();
   }

   @Override
   public int getMinY() {
      return this.settings.value().noiseSettings().minY();
   }

   @Override
   public void spawnOriginalMobs(WorldGenRegion $$0) {
      if (!this.settings.value().disableMobGeneration()) {
         ChunkPos $$1 = $$0.getCenter();
         Holder<Biome> $$2 = $$0.getBiome($$1.getWorldPosition().atY($$0.getMaxBuildHeight() - 1));
         WorldgenRandom $$3 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         $$3.setDecorationSeed($$0.getSeed(), $$1.getMinBlockX(), $$1.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration($$0, $$2, $$1, $$3);
      }
   }
}
