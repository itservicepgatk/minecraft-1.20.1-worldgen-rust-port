package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class RootPlacerType<P extends RootPlacer> {
   public static final RootPlacerType<MangroveRootPlacer> MANGROVE_ROOT_PLACER = register("mangrove_root_placer", MangroveRootPlacer.CODEC);
   private final Codec<P> codec;

   private static <P extends RootPlacer> RootPlacerType<P> register(String $$0, Codec<P> $$1) {
      return Registry.register(BuiltInRegistries.ROOT_PLACER_TYPE, $$0, new RootPlacerType<>($$1));
   }

   private RootPlacerType(Codec<P> $$0) {
      this.codec = $$0;
   }

   public Codec<P> codec() {
      return this.codec;
   }
}
