package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

public class AppendLoot implements RuleBlockEntityModifier {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<AppendLoot> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(ResourceLocation.CODEC.fieldOf("loot_table").forGetter($$0x -> $$0x.lootTable)).apply($$0, AppendLoot::new)
   );
   private final ResourceLocation lootTable;

   public AppendLoot(ResourceLocation $$0) {
      this.lootTable = $$0;
   }

   @Override
   public CompoundTag apply(RandomSource $$0, @Nullable CompoundTag $$1) {
      CompoundTag $$2 = $$1 == null ? new CompoundTag() : $$1.copy();
      ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, this.lootTable).resultOrPartial(LOGGER::error).ifPresent($$1x -> $$2.put("LootTable", $$1x));
      $$2.putLong("LootTableSeed", $$0.nextLong());
      return $$2;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.APPEND_LOOT;
   }
}
