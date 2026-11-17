package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public final class JigsawStructure extends Structure {
   public static final int MAX_TOTAL_STRUCTURE_RANGE = 128;
   public static final Codec<JigsawStructure> CODEC = ExtraCodecs.validate(
         RecordCodecBuilder.mapCodec(
            $$0 -> $$0.group(
                     settingsCodec($$0),
                     StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter($$0x -> $$0x.startPool),
                     ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter($$0x -> $$0x.startJigsawName),
                     Codec.intRange(0, 7).fieldOf("size").forGetter($$0x -> $$0x.maxDepth),
                     HeightProvider.CODEC.fieldOf("start_height").forGetter($$0x -> $$0x.startHeight),
                     Codec.BOOL.fieldOf("use_expansion_hack").forGetter($$0x -> $$0x.useExpansionHack),
                     Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter($$0x -> $$0x.projectStartToHeightmap),
                     Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter($$0x -> $$0x.maxDistanceFromCenter)
                  )
                  .apply($$0, JigsawStructure::new)
         ),
         JigsawStructure::verifyRange
      )
      .codec();
   private final Holder<StructureTemplatePool> startPool;
   private final Optional<ResourceLocation> startJigsawName;
   private final int maxDepth;
   private final HeightProvider startHeight;
   private final boolean useExpansionHack;
   private final Optional<Heightmap.Types> projectStartToHeightmap;
   private final int maxDistanceFromCenter;

   private static DataResult<JigsawStructure> verifyRange(JigsawStructure $$0) {
      int $$1 = switch ($$0.terrainAdaptation()) {
         case NONE -> 0;
         case BURY, BEARD_THIN, BEARD_BOX -> 12;
      };
      return $$0.maxDistanceFromCenter + $$1 > 128
         ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128")
         : DataResult.success($$0);
   }

   public JigsawStructure(
      Structure.StructureSettings $$0,
      Holder<StructureTemplatePool> $$1,
      Optional<ResourceLocation> $$2,
      int $$3,
      HeightProvider $$4,
      boolean $$5,
      Optional<Heightmap.Types> $$6,
      int $$7
   ) {
      super($$0);
      this.startPool = $$1;
      this.startJigsawName = $$2;
      this.maxDepth = $$3;
      this.startHeight = $$4;
      this.useExpansionHack = $$5;
      this.projectStartToHeightmap = $$6;
      this.maxDistanceFromCenter = $$7;
   }

   public JigsawStructure(Structure.StructureSettings $$0, Holder<StructureTemplatePool> $$1, int $$2, HeightProvider $$3, boolean $$4, Heightmap.Types $$5) {
      this($$0, $$1, Optional.empty(), $$2, $$3, $$4, Optional.of($$5), 80);
   }

   public JigsawStructure(Structure.StructureSettings $$0, Holder<StructureTemplatePool> $$1, int $$2, HeightProvider $$3, boolean $$4) {
      this($$0, $$1, Optional.empty(), $$2, $$3, $$4, Optional.empty(), 80);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext $$0) {
      ChunkPos $$1 = $$0.chunkPos();
      int $$2 = this.startHeight.sample($$0.random(), new WorldGenerationContext($$0.chunkGenerator(), $$0.heightAccessor()));
      BlockPos $$3 = new BlockPos($$1.getMinBlockX(), $$2, $$1.getMinBlockZ());
      return JigsawPlacement.addPieces(
         $$0, this.startPool, this.startJigsawName, this.maxDepth, $$3, this.useExpansionHack, this.projectStartToHeightmap, this.maxDistanceFromCenter
      );
   }

   @Override
   public StructureType<?> type() {
      return StructureType.JIGSAW;
   }
}
