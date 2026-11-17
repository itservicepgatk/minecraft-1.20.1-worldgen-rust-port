package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class PoolElementStructurePiece extends StructurePiece {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final StructurePoolElement element;
   protected BlockPos position;
   private final int groundLevelDelta;
   protected final Rotation rotation;
   private final List<JigsawJunction> junctions = Lists.newArrayList();
   private final StructureTemplateManager structureTemplateManager;

   public PoolElementStructurePiece(StructureTemplateManager $$0, StructurePoolElement $$1, BlockPos $$2, int $$3, Rotation $$4, BoundingBox $$5) {
      super(StructurePieceType.JIGSAW, 0, $$5);
      this.structureTemplateManager = $$0;
      this.element = $$1;
      this.position = $$2;
      this.groundLevelDelta = $$3;
      this.rotation = $$4;
   }

   public PoolElementStructurePiece(StructurePieceSerializationContext $$0, CompoundTag $$1) {
      super(StructurePieceType.JIGSAW, $$1);
      this.structureTemplateManager = $$0.structureTemplateManager();
      this.position = new BlockPos($$1.getInt("PosX"), $$1.getInt("PosY"), $$1.getInt("PosZ"));
      this.groundLevelDelta = $$1.getInt("ground_level_delta");
      DynamicOps<Tag> $$2 = RegistryOps.create(NbtOps.INSTANCE, $$0.registryAccess());
      this.element = (StructurePoolElement)StructurePoolElement.CODEC
         .parse($$2, $$1.getCompound("pool_element"))
         .resultOrPartial(LOGGER::error)
         .orElseThrow(() -> new IllegalStateException("Invalid pool element found"));
      this.rotation = Rotation.valueOf($$1.getString("rotation"));
      this.boundingBox = this.element.getBoundingBox(this.structureTemplateManager, this.position, this.rotation);
      ListTag $$3 = $$1.getList("junctions", 10);
      this.junctions.clear();
      $$3.forEach($$1x -> this.junctions.add(JigsawJunction.deserialize(new Dynamic($$2, $$1x))));
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext $$0, CompoundTag $$1) {
      $$1.putInt("PosX", this.position.getX());
      $$1.putInt("PosY", this.position.getY());
      $$1.putInt("PosZ", this.position.getZ());
      $$1.putInt("ground_level_delta", this.groundLevelDelta);
      DynamicOps<Tag> $$2 = RegistryOps.create(NbtOps.INSTANCE, $$0.registryAccess());
      StructurePoolElement.CODEC.encodeStart($$2, this.element).resultOrPartial(LOGGER::error).ifPresent($$1x -> $$1.put("pool_element", $$1x));
      $$1.putString("rotation", this.rotation.name());
      ListTag $$3 = new ListTag();

      for (JigsawJunction $$4 : this.junctions) {
         $$3.add((Tag)$$4.serialize($$2).getValue());
      }

      $$1.put("junctions", $$3);
   }

   @Override
   public void postProcess(WorldGenLevel $$0, StructureManager $$1, ChunkGenerator $$2, RandomSource $$3, BoundingBox $$4, ChunkPos $$5, BlockPos $$6) {
      this.place($$0, $$1, $$2, $$3, $$4, $$6, false);
   }

   public void place(WorldGenLevel $$0, StructureManager $$1, ChunkGenerator $$2, RandomSource $$3, BoundingBox $$4, BlockPos $$5, boolean $$6) {
      this.element.place(this.structureTemplateManager, $$0, $$1, $$2, this.position, $$5, this.rotation, $$4, $$3, $$6);
   }

   @Override
   public void move(int $$0, int $$1, int $$2) {
      super.move($$0, $$1, $$2);
      this.position = this.position.offset($$0, $$1, $$2);
   }

   @Override
   public Rotation getRotation() {
      return this.rotation;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.position, this.rotation, this.element);
   }

   public StructurePoolElement getElement() {
      return this.element;
   }

   public BlockPos getPosition() {
      return this.position;
   }

   public int getGroundLevelDelta() {
      return this.groundLevelDelta;
   }

   public void addJunction(JigsawJunction $$0) {
      this.junctions.add($$0);
   }

   public List<JigsawJunction> getJunctions() {
      return this.junctions;
   }
}
