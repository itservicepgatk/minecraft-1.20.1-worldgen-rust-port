package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public class RarityFilter extends PlacementFilter {
   public static final Codec<RarityFilter> CODEC = ExtraCodecs.POSITIVE_INT.fieldOf("chance").xmap(RarityFilter::new, $$0 -> $$0.chance).codec();
   private final int chance;

   private RarityFilter(int $$0) {
      this.chance = $$0;
   }

   public static RarityFilter onAverageOnceEvery(int $$0) {
      return new RarityFilter($$0);
   }

   @Override
   protected boolean shouldPlace(PlacementContext $$0, RandomSource $$1, BlockPos $$2) {
      return $$1.nextFloat() < 1.0F / (float)this.chance;
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.RARITY_FILTER;
   }
}
