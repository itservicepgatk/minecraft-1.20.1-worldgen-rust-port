package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

class AllOfPredicate extends CombiningPredicate {
   public static final Codec<AllOfPredicate> CODEC = codec(AllOfPredicate::new);

   public AllOfPredicate(List<BlockPredicate> $$0) {
      super($$0);
   }

   public boolean test(WorldGenLevel $$0, BlockPos $$1) {
      for (BlockPredicate $$2 : this.predicates) {
         if (!$$2.test($$0, $$1)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.ALL_OF;
   }
}
