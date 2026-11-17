package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class FeaturePoolElement extends StructurePoolElement {
   public static final Codec<FeaturePoolElement> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(PlacedFeature.CODEC.fieldOf("feature").forGetter($$0x -> $$0x.feature), projectionCodec()).apply($$0, FeaturePoolElement::new)
   );
   private final Holder<PlacedFeature> feature;
   private final CompoundTag defaultJigsawNBT;

   protected FeaturePoolElement(Holder<PlacedFeature> $$0, StructureTemplatePool.Projection $$1) {
      super($$1);
      this.feature = $$0;
      this.defaultJigsawNBT = this.fillDefaultJigsawNBT();
   }

   private CompoundTag fillDefaultJigsawNBT() {
      CompoundTag $$0 = new CompoundTag();
      $$0.putString("name", "minecraft:bottom");
      $$0.putString("final_state", "minecraft:air");
      $$0.putString("pool", "minecraft:empty");
      $$0.putString("target", "minecraft:empty");
      $$0.putString("joint", JointType.ROLLABLE.getSerializedName());
      return $$0;
   }

   @Override
   public Vec3i getSize(StructureTemplateManager $$0, Rotation $$1) {
      return Vec3i.ZERO;
   }

   @Override
   public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager $$0, BlockPos $$1, Rotation $$2, RandomSource $$3) {
      List<StructureTemplate.StructureBlockInfo> $$4 = Lists.newArrayList();
      $$4.add(
         new StructureTemplate.StructureBlockInfo(
            $$1,
            Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, FrontAndTop.fromFrontAndTop(Direction.DOWN, Direction.SOUTH)),
            this.defaultJigsawNBT
         )
      );
      return $$4;
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager $$0, BlockPos $$1, Rotation $$2) {
      Vec3i $$3 = this.getSize($$0, $$2);
      return new BoundingBox($$1.getX(), $$1.getY(), $$1.getZ(), $$1.getX() + $$3.getX(), $$1.getY() + $$3.getY(), $$1.getZ() + $$3.getZ());
   }

   @Override
   public boolean place(
      StructureTemplateManager $$0,
      WorldGenLevel $$1,
      StructureManager $$2,
      ChunkGenerator $$3,
      BlockPos $$4,
      BlockPos $$5,
      Rotation $$6,
      BoundingBox $$7,
      RandomSource $$8,
      boolean $$9
   ) {
      return this.feature.value().place($$1, $$3, $$8, $$4);
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.FEATURE;
   }

   @Override
   public String toString() {
      return "Feature[" + this.feature + "]";
   }
}
