package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class PhantomSpawner implements CustomSpawner {
   private int nextTick;

   @Override
   public int tick(ServerLevel $$0, boolean $$1, boolean $$2) {
      if (!$$1) {
         return 0;
      } else if (!$$0.getGameRules().getBoolean(GameRules.RULE_DOINSOMNIA)) {
         return 0;
      } else {
         RandomSource $$3 = $$0.random;
         this.nextTick--;
         if (this.nextTick > 0) {
            return 0;
         } else {
            this.nextTick = this.nextTick + (60 + $$3.nextInt(60)) * 20;
            if ($$0.getSkyDarken() < 5 && $$0.dimensionType().hasSkyLight()) {
               return 0;
            } else {
               int $$4 = 0;

               for (ServerPlayer $$5 : $$0.players()) {
                  if (!$$5.isSpectator()) {
                     BlockPos $$6 = $$5.blockPosition();
                     if (!$$0.dimensionType().hasSkyLight() || $$6.getY() >= $$0.getSeaLevel() && $$0.canSeeSky($$6)) {
                        DifficultyInstance $$7 = $$0.getCurrentDifficultyAt($$6);
                        if ($$7.isHarderThan($$3.nextFloat() * 3.0F)) {
                           ServerStatsCounter $$8 = $$5.getStats();
                           int $$9 = Mth.clamp($$8.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                           int $$10 = 24000;
                           if ($$3.nextInt($$9) >= 72000) {
                              BlockPos $$11 = $$6.above(20 + $$3.nextInt(15)).east(-10 + $$3.nextInt(21)).south(-10 + $$3.nextInt(21));
                              BlockState $$12 = $$0.getBlockState($$11);
                              FluidState $$13 = $$0.getFluidState($$11);
                              if (NaturalSpawner.isValidEmptySpawnBlock($$0, $$11, $$12, $$13, EntityType.PHANTOM)) {
                                 SpawnGroupData $$14 = null;
                                 int $$15 = 1 + $$3.nextInt($$7.getDifficulty().getId() + 1);

                                 for (int $$16 = 0; $$16 < $$15; $$16++) {
                                    Phantom $$17 = EntityType.PHANTOM.create($$0);
                                    if ($$17 != null) {
                                       $$17.moveTo($$11, 0.0F, 0.0F);
                                       $$14 = $$17.finalizeSpawn($$0, $$7, MobSpawnType.NATURAL, $$14, null);
                                       $$0.addFreshEntityWithPassengers($$17);
                                       $$4++;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               return $$4;
            }
         }
      }
   }
}
