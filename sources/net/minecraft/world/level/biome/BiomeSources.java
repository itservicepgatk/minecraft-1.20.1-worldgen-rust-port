package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;

public class BiomeSources {
   public static Codec<? extends BiomeSource> bootstrap(Registry<Codec<? extends BiomeSource>> $$0) {
      Registry.register($$0, "fixed", FixedBiomeSource.CODEC);
      Registry.register($$0, "multi_noise", MultiNoiseBiomeSource.CODEC);
      Registry.register($$0, "checkerboard", CheckerboardColumnBiomeSource.CODEC);
      return Registry.register($$0, "the_end", TheEndBiomeSource.CODEC);
   }
}
