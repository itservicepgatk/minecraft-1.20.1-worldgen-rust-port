package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BeehiveDecorator extends TreeDecorator {
   public static final Codec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(BeehiveDecorator::new, $$0 -> $$0.probability)
      .codec();
   private static final Direction WORLDGEN_FACING = Direction.SOUTH;
   private static final Direction[] SPAWN_DIRECTIONS = Plane.HORIZONTAL.stream().filter($$0 -> $$0 != WORLDGEN_FACING.getOpposite()).toArray(Direction[]::new);
   private final float probability;

   public BeehiveDecorator(float $$0) {
      this.probability = $$0;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.BEEHIVE;
   }

   @Override
   public void place(TreeDecorator.Context $$0) {
      RandomSource $$1 = $$0.random();
      if (!($$1.nextFloat() >= this.probability)) {
         List<BlockPos> $$2 = $$0.leaves();
         List<BlockPos> $$3 = $$0.logs();
         int $$4 = !$$2.isEmpty()
            ? Math.max($$2.get(0).getY() - 1, $$3.get(0).getY() + 1)
            : Math.min($$3.get(0).getY() + 1 + $$1.nextInt(3), $$3.get($$3.size() - 1).getY());
         List<BlockPos> $$5 = $$3.stream()
            .filter($$1x -> $$1x.getY() == $$4)
            .flatMap($$0x -> Stream.of(SPAWN_DIRECTIONS).map($$0x::relative))
            .collect(Collectors.toList());
         if (!$$5.isEmpty()) {
            Collections.shuffle($$5);
            Optional<BlockPos> $$6 = $$5.stream().filter($$1x -> $$0.isAir($$1x) && $$0.isAir($$1x.relative(WORLDGEN_FACING))).findFirst();
            if (!$$6.isEmpty()) {
               $$0.setBlock($$6.get(), Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
               $$0.level().getBlockEntity($$6.get(), BlockEntityType.BEEHIVE).ifPresent($$1x -> {
                  int $$2x = 2 + $$1.nextInt(2);

                  for (int $$3x = 0; $$3x < $$2x; $$3x++) {
                     CompoundTag $$4x = new CompoundTag();
                     $$4x.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BEE).toString());
                     $$1x.storeBee($$4x, $$1.nextInt(599), false);
                  }
               });
            }
         }
      }
   }
}
