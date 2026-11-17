package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.IntrusiveBase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;

public class MobSpawnSettings {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;
   public static final WeightedRandomList<MobSpawnSettings.SpawnerData> EMPTY_MOB_LIST = WeightedRandomList.create();
   public static final MobSpawnSettings EMPTY = new MobSpawnSettings.Builder().build();
   public static final MapCodec<MobSpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
      $$0 -> $$0.group(
               Codec.floatRange(0.0F, 0.9999999F).optionalFieldOf("creature_spawn_probability", 0.1F).forGetter($$0x -> $$0x.creatureGenerationProbability),
               Codec.simpleMap(
                     MobCategory.CODEC,
                     WeightedRandomList.codec(MobSpawnSettings.SpawnerData.CODEC).promotePartial(Util.prefix("Spawn data: ", LOGGER::error)),
                     StringRepresentable.keys(MobCategory.values())
                  )
                  .fieldOf("spawners")
                  .forGetter($$0x -> $$0x.spawners),
               Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnSettings.MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE)
                  .fieldOf("spawn_costs")
                  .forGetter($$0x -> $$0x.mobSpawnCosts)
            )
            .apply($$0, MobSpawnSettings::new)
   );
   private final float creatureGenerationProbability;
   private final Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> spawners;
   private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts;

   MobSpawnSettings(float $$0, Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> $$1, Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> $$2) {
      this.creatureGenerationProbability = $$0;
      this.spawners = ImmutableMap.copyOf($$1);
      this.mobSpawnCosts = ImmutableMap.copyOf($$2);
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobs(MobCategory $$0) {
      return this.spawners.getOrDefault($$0, EMPTY_MOB_LIST);
   }

   @Nullable
   public MobSpawnSettings.MobSpawnCost getMobSpawnCost(EntityType<?> $$0) {
      return this.mobSpawnCosts.get($$0);
   }

   public float getCreatureProbability() {
      return this.creatureGenerationProbability;
   }

   public static class Builder {
      private final Map<MobCategory, List<MobSpawnSettings.SpawnerData>> spawners = Stream.of(MobCategory.values())
         .collect(ImmutableMap.toImmutableMap($$0 -> $$0, $$0 -> Lists.newArrayList()));
      private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts = Maps.newLinkedHashMap();
      private float creatureGenerationProbability = 0.1F;

      public MobSpawnSettings.Builder addSpawn(MobCategory $$0, MobSpawnSettings.SpawnerData $$1) {
         this.spawners.get($$0).add($$1);
         return this;
      }

      public MobSpawnSettings.Builder addMobCharge(EntityType<?> $$0, double $$1, double $$2) {
         this.mobSpawnCosts.put($$0, new MobSpawnSettings.MobSpawnCost($$2, $$1));
         return this;
      }

      public MobSpawnSettings.Builder creatureGenerationProbability(float $$0) {
         this.creatureGenerationProbability = $$0;
         return this;
      }

      public MobSpawnSettings build() {
         return new MobSpawnSettings(
            this.creatureGenerationProbability,
            this.spawners.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, $$0 -> WeightedRandomList.create((List)$$0.getValue()))),
            ImmutableMap.copyOf(this.mobSpawnCosts)
         );
      }
   }

   public static record MobSpawnCost(double energyBudget, double charge) {
      public static final Codec<MobSpawnSettings.MobSpawnCost> CODEC = RecordCodecBuilder.create(
         $$0 -> $$0.group(
                  Codec.DOUBLE.fieldOf("energy_budget").forGetter($$0x -> $$0x.energyBudget), Codec.DOUBLE.fieldOf("charge").forGetter($$0x -> $$0x.charge)
               )
               .apply($$0, MobSpawnSettings.MobSpawnCost::new)
      );
   }

   public static class SpawnerData extends IntrusiveBase {
      public static final Codec<MobSpawnSettings.SpawnerData> CODEC = ExtraCodecs.validate(
         RecordCodecBuilder.create(
            $$0 -> $$0.group(
                     BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter($$0x -> $$0x.type),
                     Weight.CODEC.fieldOf("weight").forGetter(IntrusiveBase::getWeight),
                     ExtraCodecs.POSITIVE_INT.fieldOf("minCount").forGetter($$0x -> $$0x.minCount),
                     ExtraCodecs.POSITIVE_INT.fieldOf("maxCount").forGetter($$0x -> $$0x.maxCount)
                  )
                  .apply($$0, MobSpawnSettings.SpawnerData::new)
         ),
         $$0 -> $$0.minCount > $$0.maxCount ? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount") : DataResult.success($$0)
      );
      public final EntityType<?> type;
      public final int minCount;
      public final int maxCount;

      public SpawnerData(EntityType<?> $$0, int $$1, int $$2, int $$3) {
         this($$0, Weight.of($$1), $$2, $$3);
      }

      public SpawnerData(EntityType<?> $$0, Weight $$1, int $$2, int $$3) {
         super($$1);
         this.type = $$0.getCategory() == MobCategory.MISC ? EntityType.PIG : $$0;
         this.minCount = $$2;
         this.maxCount = $$3;
      }

      @Override
      public String toString() {
         return EntityType.getKey(this.type) + "*(" + this.minCount + "-" + this.maxCount + "):" + this.getWeight();
      }
   }
}
