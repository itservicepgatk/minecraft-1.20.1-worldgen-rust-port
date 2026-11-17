package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;

public class BiomeSpecialEffects {
   public static final Codec<BiomeSpecialEffects> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(
               Codec.INT.fieldOf("fog_color").forGetter($$0x -> $$0x.fogColor),
               Codec.INT.fieldOf("water_color").forGetter($$0x -> $$0x.waterColor),
               Codec.INT.fieldOf("water_fog_color").forGetter($$0x -> $$0x.waterFogColor),
               Codec.INT.fieldOf("sky_color").forGetter($$0x -> $$0x.skyColor),
               Codec.INT.optionalFieldOf("foliage_color").forGetter($$0x -> $$0x.foliageColorOverride),
               Codec.INT.optionalFieldOf("grass_color").forGetter($$0x -> $$0x.grassColorOverride),
               BiomeSpecialEffects.GrassColorModifier.CODEC
                  .optionalFieldOf("grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.NONE)
                  .forGetter($$0x -> $$0x.grassColorModifier),
               AmbientParticleSettings.CODEC.optionalFieldOf("particle").forGetter($$0x -> $$0x.ambientParticleSettings),
               SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter($$0x -> $$0x.ambientLoopSoundEvent),
               AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter($$0x -> $$0x.ambientMoodSettings),
               AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter($$0x -> $$0x.ambientAdditionsSettings),
               Music.CODEC.optionalFieldOf("music").forGetter($$0x -> $$0x.backgroundMusic)
            )
            .apply($$0, BiomeSpecialEffects::new)
   );
   private final int fogColor;
   private final int waterColor;
   private final int waterFogColor;
   private final int skyColor;
   private final Optional<Integer> foliageColorOverride;
   private final Optional<Integer> grassColorOverride;
   private final BiomeSpecialEffects.GrassColorModifier grassColorModifier;
   private final Optional<AmbientParticleSettings> ambientParticleSettings;
   private final Optional<Holder<SoundEvent>> ambientLoopSoundEvent;
   private final Optional<AmbientMoodSettings> ambientMoodSettings;
   private final Optional<AmbientAdditionsSettings> ambientAdditionsSettings;
   private final Optional<Music> backgroundMusic;

   BiomeSpecialEffects(
      int $$0,
      int $$1,
      int $$2,
      int $$3,
      Optional<Integer> $$4,
      Optional<Integer> $$5,
      BiomeSpecialEffects.GrassColorModifier $$6,
      Optional<AmbientParticleSettings> $$7,
      Optional<Holder<SoundEvent>> $$8,
      Optional<AmbientMoodSettings> $$9,
      Optional<AmbientAdditionsSettings> $$10,
      Optional<Music> $$11
   ) {
      this.fogColor = $$0;
      this.waterColor = $$1;
      this.waterFogColor = $$2;
      this.skyColor = $$3;
      this.foliageColorOverride = $$4;
      this.grassColorOverride = $$5;
      this.grassColorModifier = $$6;
      this.ambientParticleSettings = $$7;
      this.ambientLoopSoundEvent = $$8;
      this.ambientMoodSettings = $$9;
      this.ambientAdditionsSettings = $$10;
      this.backgroundMusic = $$11;
   }

   public int getFogColor() {
      return this.fogColor;
   }

   public int getWaterColor() {
      return this.waterColor;
   }

   public int getWaterFogColor() {
      return this.waterFogColor;
   }

   public int getSkyColor() {
      return this.skyColor;
   }

   public Optional<Integer> getFoliageColorOverride() {
      return this.foliageColorOverride;
   }

   public Optional<Integer> getGrassColorOverride() {
      return this.grassColorOverride;
   }

   public BiomeSpecialEffects.GrassColorModifier getGrassColorModifier() {
      return this.grassColorModifier;
   }

   public Optional<AmbientParticleSettings> getAmbientParticleSettings() {
      return this.ambientParticleSettings;
   }

   public Optional<Holder<SoundEvent>> getAmbientLoopSoundEvent() {
      return this.ambientLoopSoundEvent;
   }

   public Optional<AmbientMoodSettings> getAmbientMoodSettings() {
      return this.ambientMoodSettings;
   }

   public Optional<AmbientAdditionsSettings> getAmbientAdditionsSettings() {
      return this.ambientAdditionsSettings;
   }

   public Optional<Music> getBackgroundMusic() {
      return this.backgroundMusic;
   }

   public static class Builder {
      private OptionalInt fogColor = OptionalInt.empty();
      private OptionalInt waterColor = OptionalInt.empty();
      private OptionalInt waterFogColor = OptionalInt.empty();
      private OptionalInt skyColor = OptionalInt.empty();
      private Optional<Integer> foliageColorOverride = Optional.empty();
      private Optional<Integer> grassColorOverride = Optional.empty();
      private BiomeSpecialEffects.GrassColorModifier grassColorModifier = BiomeSpecialEffects.GrassColorModifier.NONE;
      private Optional<AmbientParticleSettings> ambientParticle = Optional.empty();
      private Optional<Holder<SoundEvent>> ambientLoopSoundEvent = Optional.empty();
      private Optional<AmbientMoodSettings> ambientMoodSettings = Optional.empty();
      private Optional<AmbientAdditionsSettings> ambientAdditionsSettings = Optional.empty();
      private Optional<Music> backgroundMusic = Optional.empty();

      public BiomeSpecialEffects.Builder fogColor(int $$0) {
         this.fogColor = OptionalInt.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder waterColor(int $$0) {
         this.waterColor = OptionalInt.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder waterFogColor(int $$0) {
         this.waterFogColor = OptionalInt.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder skyColor(int $$0) {
         this.skyColor = OptionalInt.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder foliageColorOverride(int $$0) {
         this.foliageColorOverride = Optional.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder grassColorOverride(int $$0) {
         this.grassColorOverride = Optional.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder grassColorModifier(BiomeSpecialEffects.GrassColorModifier $$0) {
         this.grassColorModifier = $$0;
         return this;
      }

      public BiomeSpecialEffects.Builder ambientParticle(AmbientParticleSettings $$0) {
         this.ambientParticle = Optional.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder ambientLoopSound(Holder<SoundEvent> $$0) {
         this.ambientLoopSoundEvent = Optional.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder ambientMoodSound(AmbientMoodSettings $$0) {
         this.ambientMoodSettings = Optional.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder ambientAdditionsSound(AmbientAdditionsSettings $$0) {
         this.ambientAdditionsSettings = Optional.of($$0);
         return this;
      }

      public BiomeSpecialEffects.Builder backgroundMusic(@Nullable Music $$0) {
         this.backgroundMusic = Optional.ofNullable($$0);
         return this;
      }

      public BiomeSpecialEffects build() {
         return new BiomeSpecialEffects(
            this.fogColor.orElseThrow(() -> new IllegalStateException("Missing 'fog' color.")),
            this.waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")),
            this.waterFogColor.orElseThrow(() -> new IllegalStateException("Missing 'water fog' color.")),
            this.skyColor.orElseThrow(() -> new IllegalStateException("Missing 'sky' color.")),
            this.foliageColorOverride,
            this.grassColorOverride,
            this.grassColorModifier,
            this.ambientParticle,
            this.ambientLoopSoundEvent,
            this.ambientMoodSettings,
            this.ambientAdditionsSettings,
            this.backgroundMusic
         );
      }
   }

   public static enum GrassColorModifier implements StringRepresentable {
      NONE("none") {
         @Override
         public int modifyColor(double $$0, double $$1, int $$2) {
            return $$2;
         }
      },
      DARK_FOREST("dark_forest") {
         @Override
         public int modifyColor(double $$0, double $$1, int $$2) {
            return ($$2 & 16711422) + 2634762 >> 1;
         }
      },
      SWAMP("swamp") {
         @Override
         public int modifyColor(double $$0, double $$1, int $$2) {
            double $$3 = Biome.BIOME_INFO_NOISE.getValue($$0 * 0.0225, $$1 * 0.0225, false);
            return $$3 < -0.1 ? 5011004 : 6975545;
         }
      };

      private final String name;
      public static final Codec<BiomeSpecialEffects.GrassColorModifier> CODEC = StringRepresentable.fromEnum(BiomeSpecialEffects.GrassColorModifier::values);

      public abstract int modifyColor(double var1, double var3, int var5);

      GrassColorModifier(String $$0) {
         this.name = $$0;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
