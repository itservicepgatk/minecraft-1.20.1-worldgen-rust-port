package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class FeatureSizeType<P extends FeatureSize> {
   public static final FeatureSizeType<TwoLayersFeatureSize> TWO_LAYERS_FEATURE_SIZE = register("two_layers_feature_size", TwoLayersFeatureSize.CODEC);
   public static final FeatureSizeType<ThreeLayersFeatureSize> THREE_LAYERS_FEATURE_SIZE = register("three_layers_feature_size", ThreeLayersFeatureSize.CODEC);
   private final Codec<P> codec;

   private static <P extends FeatureSize> FeatureSizeType<P> register(String $$0, Codec<P> $$1) {
      return Registry.register(BuiltInRegistries.FEATURE_SIZE_TYPE, $$0, new FeatureSizeType<>($$1));
   }

   private FeatureSizeType(Codec<P> $$0) {
      this.codec = $$0;
   }

   public Codec<P> codec() {
      return this.codec;
   }
}
