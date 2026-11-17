package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class WeightedListHeight extends HeightProvider {
   public static final Codec<WeightedListHeight> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(SimpleWeightedRandomList.wrappedCodec(HeightProvider.CODEC).fieldOf("distribution").forGetter($$0x -> $$0x.distribution))
            .apply($$0, WeightedListHeight::new)
   );
   private final SimpleWeightedRandomList<HeightProvider> distribution;

   public WeightedListHeight(SimpleWeightedRandomList<HeightProvider> $$0) {
      this.distribution = $$0;
   }

   @Override
   public int sample(RandomSource $$0, WorldGenerationContext $$1) {
      return this.distribution.getRandomValue($$0).orElseThrow(IllegalStateException::new).sample($$0, $$1);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.WEIGHTED_LIST;
   }
}
