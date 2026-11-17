package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.SimpleWeightedRandomList.Builder;
import net.minecraft.world.level.block.state.BlockState;

public class WeightedStateProvider extends BlockStateProvider {
   public static final Codec<WeightedStateProvider> CODEC = SimpleWeightedRandomList.wrappedCodec(BlockState.CODEC)
      .comapFlatMap(WeightedStateProvider::create, $$0 -> $$0.weightedList)
      .fieldOf("entries")
      .codec();
   private final SimpleWeightedRandomList<BlockState> weightedList;

   private static DataResult<WeightedStateProvider> create(SimpleWeightedRandomList<BlockState> $$0) {
      return $$0.isEmpty() ? DataResult.error(() -> "WeightedStateProvider with no states") : DataResult.success(new WeightedStateProvider($$0));
   }

   public WeightedStateProvider(SimpleWeightedRandomList<BlockState> $$0) {
      this.weightedList = $$0;
   }

   public WeightedStateProvider(Builder<BlockState> $$0) {
      this($$0.build());
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.WEIGHTED_STATE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource $$0, BlockPos $$1) {
      return this.weightedList.getRandomValue($$0).orElseThrow(IllegalStateException::new);
   }
}
