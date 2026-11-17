package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AlterGroundDecorator extends TreeDecorator {
   public static final Codec<AlterGroundDecorator> CODEC = BlockStateProvider.CODEC
      .fieldOf("provider")
      .xmap(AlterGroundDecorator::new, $$0 -> $$0.provider)
      .codec();
   private final BlockStateProvider provider;

   public AlterGroundDecorator(BlockStateProvider $$0) {
      this.provider = $$0;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ALTER_GROUND;
   }

   @Override
   public void place(TreeDecorator.Context $$0) {
      List<BlockPos> $$1 = Lists.newArrayList();
      List<BlockPos> $$2 = $$0.roots();
      List<BlockPos> $$3 = $$0.logs();
      if ($$2.isEmpty()) {
         $$1.addAll($$3);
      } else if (!$$3.isEmpty() && $$2.get(0).getY() == $$3.get(0).getY()) {
         $$1.addAll($$3);
         $$1.addAll($$2);
      } else {
         $$1.addAll($$2);
      }

      if (!$$1.isEmpty()) {
         int $$4 = $$1.get(0).getY();
         $$1.stream().filter($$1x -> $$1x.getY() == $$4).forEach($$1x -> {
            this.placeCircle($$0, $$1x.west().north());
            this.placeCircle($$0, $$1x.east(2).north());
            this.placeCircle($$0, $$1x.west().south(2));
            this.placeCircle($$0, $$1x.east(2).south(2));

            for (int $$2x = 0; $$2x < 5; $$2x++) {
               int $$3x = $$0.random().nextInt(64);
               int $$4x = $$3x % 8;
               int $$5 = $$3x / 8;
               if ($$4x == 0 || $$4x == 7 || $$5 == 0 || $$5 == 7) {
                  this.placeCircle($$0, $$1x.offset(-3 + $$4x, 0, -3 + $$5));
               }
            }
         });
      }
   }

   private void placeCircle(TreeDecorator.Context $$0, BlockPos $$1) {
      for (int $$2 = -2; $$2 <= 2; $$2++) {
         for (int $$3 = -2; $$3 <= 2; $$3++) {
            if (Math.abs($$2) != 2 || Math.abs($$3) != 2) {
               this.placeBlockAt($$0, $$1.offset($$2, 0, $$3));
            }
         }
      }
   }

   private void placeBlockAt(TreeDecorator.Context $$0, BlockPos $$1) {
      for (int $$2 = 2; $$2 >= -3; $$2--) {
         BlockPos $$3 = $$1.above($$2);
         if (Feature.isGrassOrDirt($$0.level(), $$3)) {
            $$0.setBlock($$3, this.provider.getState($$0.random(), $$1));
            break;
         }

         if (!$$0.isAir($$3) && $$2 < 0) {
            break;
         }
      }
   }
}
