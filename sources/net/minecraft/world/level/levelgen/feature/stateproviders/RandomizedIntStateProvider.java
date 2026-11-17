package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RandomizedIntStateProvider extends BlockStateProvider {
   public static final Codec<RandomizedIntStateProvider> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(
               BlockStateProvider.CODEC.fieldOf("source").forGetter($$0x -> $$0x.source),
               Codec.STRING.fieldOf("property").forGetter($$0x -> $$0x.propertyName),
               IntProvider.CODEC.fieldOf("values").forGetter($$0x -> $$0x.values)
            )
            .apply($$0, RandomizedIntStateProvider::new)
   );
   private final BlockStateProvider source;
   private final String propertyName;
   @Nullable
   private IntegerProperty property;
   private final IntProvider values;

   public RandomizedIntStateProvider(BlockStateProvider $$0, IntegerProperty $$1, IntProvider $$2) {
      this.source = $$0;
      this.property = $$1;
      this.propertyName = $$1.getName();
      this.values = $$2;
      Collection<Integer> $$3 = $$1.getPossibleValues();

      for (int $$4 = $$2.getMinValue(); $$4 <= $$2.getMaxValue(); $$4++) {
         if (!$$3.contains($$4)) {
            throw new IllegalArgumentException("Property value out of range: " + $$1.getName() + ": " + $$4);
         }
      }
   }

   public RandomizedIntStateProvider(BlockStateProvider $$0, String $$1, IntProvider $$2) {
      this.source = $$0;
      this.propertyName = $$1;
      this.values = $$2;
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource $$0, BlockPos $$1) {
      BlockState $$2 = this.source.getState($$0, $$1);
      if (this.property == null || !$$2.hasProperty(this.property)) {
         this.property = findProperty($$2, this.propertyName);
      }

      return $$2.setValue(this.property, Integer.valueOf(this.values.sample($$0)));
   }

   private static IntegerProperty findProperty(BlockState $$0, String $$1) {
      Collection<Property<?>> $$2 = $$0.getProperties();
      Optional<IntegerProperty> $$3 = $$2.stream()
         .filter($$1x -> $$1x.getName().equals($$1))
         .filter($$0x -> $$0x instanceof IntegerProperty)
         .map($$0x -> (IntegerProperty)$$0x)
         .findAny();
      return $$3.orElseThrow(() -> new IllegalArgumentException("Illegal property: " + $$1));
   }
}
