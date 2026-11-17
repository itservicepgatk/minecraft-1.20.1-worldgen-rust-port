package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LegacySinglePoolElement extends SinglePoolElement {
   public static final Codec<LegacySinglePoolElement> CODEC = RecordCodecBuilder.create(
      $$0 -> $$0.group(templateCodec(), processorsCodec(), projectionCodec()).apply($$0, LegacySinglePoolElement::new)
   );

   protected LegacySinglePoolElement(Either<ResourceLocation, StructureTemplate> $$0, Holder<StructureProcessorList> $$1, StructureTemplatePool.Projection $$2) {
      super($$0, $$1, $$2);
   }

   @Override
   protected StructurePlaceSettings getSettings(Rotation $$0, BoundingBox $$1, boolean $$2) {
      StructurePlaceSettings $$3 = super.getSettings($$0, $$1, $$2);
      $$3.popProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      $$3.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      return $$3;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.LEGACY;
   }

   @Override
   public String toString() {
      return "LegacySingle[" + this.template + "]";
   }
}
