package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

public class BlockIgnoreProcessor extends StructureProcessor {
   public static final Codec<BlockIgnoreProcessor> CODEC = BlockState.CODEC
      .xmap(BlockStateBase::getBlock, Block::defaultBlockState)
      .listOf()
      .fieldOf("blocks")
      .xmap(BlockIgnoreProcessor::new, $$0 -> $$0.toIgnore)
      .codec();
   public static final BlockIgnoreProcessor STRUCTURE_BLOCK = new BlockIgnoreProcessor(ImmutableList.of(Blocks.STRUCTURE_BLOCK));
   public static final BlockIgnoreProcessor AIR = new BlockIgnoreProcessor(ImmutableList.of(Blocks.AIR));
   public static final BlockIgnoreProcessor STRUCTURE_AND_AIR = new BlockIgnoreProcessor(ImmutableList.of(Blocks.AIR, Blocks.STRUCTURE_BLOCK));
   private final ImmutableList<Block> toIgnore;

   public BlockIgnoreProcessor(List<Block> $$0) {
      this.toIgnore = ImmutableList.copyOf($$0);
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader $$0,
      BlockPos $$1,
      BlockPos $$2,
      StructureTemplate.StructureBlockInfo $$3,
      StructureTemplate.StructureBlockInfo $$4,
      StructurePlaceSettings $$5
   ) {
      return this.toIgnore.contains($$4.state().getBlock()) ? null : $$4;
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.BLOCK_IGNORE;
   }
}
