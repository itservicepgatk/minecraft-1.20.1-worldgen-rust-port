package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class ConstantHeight extends HeightProvider {
   public static final ConstantHeight ZERO = new ConstantHeight(VerticalAnchor.absolute(0));
   public static final Codec<ConstantHeight> CODEC = Codec.either(
         VerticalAnchor.CODEC,
         RecordCodecBuilder.create($$0 -> $$0.group(VerticalAnchor.CODEC.fieldOf("value").forGetter($$0x -> $$0x.value)).apply($$0, ConstantHeight::new))
      )
      .xmap($$0 -> (ConstantHeight)$$0.map(ConstantHeight::of, $$0x -> $$0x), $$0 -> Either.left($$0.value));
   private final VerticalAnchor value;

   public static ConstantHeight of(VerticalAnchor $$0) {
      return new ConstantHeight($$0);
   }

   private ConstantHeight(VerticalAnchor $$0) {
      this.value = $$0;
   }

   public VerticalAnchor getValue() {
      return this.value;
   }

   @Override
   public int sample(RandomSource $$0, WorldGenerationContext $$1) {
      return this.value.resolveY($$1);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.CONSTANT;
   }

   @Override
   public String toString() {
      return this.value.toString();
   }
}
