package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SimpleBlockConfiguration(BlockStateProvider toPlace) implements FeatureConfiguration {
   public static final Codec<SimpleBlockConfiguration> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(BlockStateProvider.CODEC.fieldOf("to_place").forGetter($$0x -> $$0x.toPlace)).apply($$0, SimpleBlockConfiguration::new)
   );
}
