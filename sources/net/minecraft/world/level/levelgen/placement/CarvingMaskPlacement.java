package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;

public class CarvingMaskPlacement extends PlacementModifier {
   public static final Codec<CarvingMaskPlacement> CODEC = GenerationStep.Carving.CODEC
      .fieldOf("step")
      .xmap(CarvingMaskPlacement::new, $$0 -> $$0.step)
      .codec();
   private final GenerationStep.Carving step;

   private CarvingMaskPlacement(GenerationStep.Carving $$0) {
      this.step = $$0;
   }

   public static CarvingMaskPlacement forStep(GenerationStep.Carving $$0) {
      return new CarvingMaskPlacement($$0);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext $$0, RandomSource $$1, BlockPos $$2) {
      ChunkPos $$3 = new ChunkPos($$2);
      return $$0.getCarvingMask($$3, this.step).stream($$3);
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.CARVING_MASK_PLACEMENT;
   }
}
