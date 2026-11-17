package net.minecraft.world.level.levelgen.presets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public class WorldPreset {
   public static final Codec<WorldPreset> DIRECT_CODEC = ExtraCodecs.validate(
      RecordCodecBuilder.create(
         $$0 -> $$0.group(
                  Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter($$0x -> $$0x.dimensions)
               )
               .apply($$0, WorldPreset::new)
      ),
      WorldPreset::requireOverworld
   );
   public static final Codec<Holder<WorldPreset>> CODEC = RegistryFileCodec.create(Registries.WORLD_PRESET, DIRECT_CODEC);
   private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

   public WorldPreset(Map<ResourceKey<LevelStem>, LevelStem> $$0) {
      this.dimensions = $$0;
   }

   private Registry<LevelStem> createRegistry() {
      WritableRegistry<LevelStem> $$0 = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.experimental());
      WorldDimensions.keysInOrder(this.dimensions.keySet().stream()).forEach($$1 -> {
         LevelStem $$2 = this.dimensions.get($$1);
         if ($$2 != null) {
            $$0.register((ResourceKey<LevelStem>)$$1, $$2, Lifecycle.stable());
         }
      });
      return $$0.freeze();
   }

   public WorldDimensions createWorldDimensions() {
      return new WorldDimensions(this.createRegistry());
   }

   public Optional<LevelStem> overworld() {
      return Optional.ofNullable(this.dimensions.get(LevelStem.OVERWORLD));
   }

   private static DataResult<WorldPreset> requireOverworld(WorldPreset $$0) {
      return $$0.overworld().isEmpty() ? DataResult.error(() -> "Missing overworld dimension") : DataResult.success($$0, Lifecycle.stable());
   }
}
