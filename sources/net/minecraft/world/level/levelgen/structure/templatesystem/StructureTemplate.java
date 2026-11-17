package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class StructureTemplate {
   public static final String PALETTE_TAG = "palette";
   public static final String PALETTE_LIST_TAG = "palettes";
   public static final String ENTITIES_TAG = "entities";
   public static final String BLOCKS_TAG = "blocks";
   public static final String BLOCK_TAG_POS = "pos";
   public static final String BLOCK_TAG_STATE = "state";
   public static final String BLOCK_TAG_NBT = "nbt";
   public static final String ENTITY_TAG_POS = "pos";
   public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
   public static final String ENTITY_TAG_NBT = "nbt";
   public static final String SIZE_TAG = "size";
   private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
   private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
   private Vec3i size = Vec3i.ZERO;
   private String author = "?";

   public Vec3i getSize() {
      return this.size;
   }

   public void setAuthor(String $$0) {
      this.author = $$0;
   }

   public String getAuthor() {
      return this.author;
   }

   public void fillFromWorld(Level $$0, BlockPos $$1, Vec3i $$2, boolean $$3, @Nullable Block $$4) {
      if ($$2.getX() >= 1 && $$2.getY() >= 1 && $$2.getZ() >= 1) {
         BlockPos $$5 = $$1.offset($$2).offset(-1, -1, -1);
         List<StructureTemplate.StructureBlockInfo> $$6 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> $$7 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> $$8 = Lists.newArrayList();
         BlockPos $$9 = new BlockPos(Math.min($$1.getX(), $$5.getX()), Math.min($$1.getY(), $$5.getY()), Math.min($$1.getZ(), $$5.getZ()));
         BlockPos $$10 = new BlockPos(Math.max($$1.getX(), $$5.getX()), Math.max($$1.getY(), $$5.getY()), Math.max($$1.getZ(), $$5.getZ()));
         this.size = $$2;

         for (BlockPos $$11 : BlockPos.betweenClosed($$9, $$10)) {
            BlockPos $$12 = $$11.subtract($$9);
            BlockState $$13 = $$0.getBlockState($$11);
            if ($$4 == null || !$$13.is($$4)) {
               BlockEntity $$14 = $$0.getBlockEntity($$11);
               StructureTemplate.StructureBlockInfo $$15;
               if ($$14 != null) {
                  $$15 = new StructureTemplate.StructureBlockInfo($$12, $$13, $$14.saveWithId());
               } else {
                  $$15 = new StructureTemplate.StructureBlockInfo($$12, $$13, null);
               }

               addToLists($$15, $$6, $$7, $$8);
            }
         }

         List<StructureTemplate.StructureBlockInfo> $$17 = buildInfoList($$6, $$7, $$8);
         this.palettes.clear();
         this.palettes.add(new StructureTemplate.Palette($$17));
         if ($$3) {
            this.fillEntityList($$0, $$9, $$10.offset(1, 1, 1));
         } else {
            this.entityInfoList.clear();
         }
      }
   }

   private static void addToLists(
      StructureTemplate.StructureBlockInfo $$0,
      List<StructureTemplate.StructureBlockInfo> $$1,
      List<StructureTemplate.StructureBlockInfo> $$2,
      List<StructureTemplate.StructureBlockInfo> $$3
   ) {
      if ($$0.nbt != null) {
         $$2.add($$0);
      } else if (!$$0.state.getBlock().hasDynamicShape() && $$0.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
         $$1.add($$0);
      } else {
         $$3.add($$0);
      }
   }

   private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
      List<StructureTemplate.StructureBlockInfo> $$0, List<StructureTemplate.StructureBlockInfo> $$1, List<StructureTemplate.StructureBlockInfo> $$2
   ) {
      Comparator<StructureTemplate.StructureBlockInfo> $$3 = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt($$0x -> $$0x.pos.getY())
         .thenComparingInt($$0x -> $$0x.pos.getX())
         .thenComparingInt($$0x -> $$0x.pos.getZ());
      $$0.sort($$3);
      $$2.sort($$3);
      $$1.sort($$3);
      List<StructureTemplate.StructureBlockInfo> $$4 = Lists.newArrayList();
      $$4.addAll($$0);
      $$4.addAll($$2);
      $$4.addAll($$1);
      return $$4;
   }

   private void fillEntityList(Level $$0, BlockPos $$1, BlockPos $$2) {
      List<Entity> $$3 = $$0.getEntitiesOfClass(Entity.class, new AABB($$1, $$2), $$0x -> !($$0x instanceof Player));
      this.entityInfoList.clear();

      for (Entity $$4 : $$3) {
         Vec3 $$5 = new Vec3($$4.getX() - (double)$$1.getX(), $$4.getY() - (double)$$1.getY(), $$4.getZ() - (double)$$1.getZ());
         CompoundTag $$6 = new CompoundTag();
         $$4.save($$6);
         BlockPos $$7;
         if ($$4 instanceof Painting) {
            $$7 = ((Painting)$$4).getPos().subtract($$1);
         } else {
            $$7 = BlockPos.containing($$5);
         }

         this.entityInfoList.add(new StructureTemplate.StructureEntityInfo($$5, $$7, $$6.copy()));
      }
   }

   public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos $$0, StructurePlaceSettings $$1, Block $$2) {
      return this.filterBlocks($$0, $$1, $$2, true);
   }

   public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos $$0, StructurePlaceSettings $$1, Block $$2, boolean $$3) {
      ObjectArrayList<StructureTemplate.StructureBlockInfo> $$4 = new ObjectArrayList();
      BoundingBox $$5 = $$1.getBoundingBox();
      if (this.palettes.isEmpty()) {
         return $$4;
      } else {
         for (StructureTemplate.StructureBlockInfo $$6 : $$1.getRandomPalette(this.palettes, $$0).blocks($$2)) {
            BlockPos $$7 = $$3 ? calculateRelativePosition($$1, $$6.pos).offset($$0) : $$6.pos;
            if ($$5 == null || $$5.isInside($$7)) {
               $$4.add(new StructureTemplate.StructureBlockInfo($$7, $$6.state.rotate($$1.getRotation()), $$6.nbt));
            }
         }

         return $$4;
      }
   }

   public BlockPos calculateConnectedPosition(StructurePlaceSettings $$0, BlockPos $$1, StructurePlaceSettings $$2, BlockPos $$3) {
      BlockPos $$4 = calculateRelativePosition($$0, $$1);
      BlockPos $$5 = calculateRelativePosition($$2, $$3);
      return $$4.subtract($$5);
   }

   public static BlockPos calculateRelativePosition(StructurePlaceSettings $$0, BlockPos $$1) {
      return transform($$1, $$0.getMirror(), $$0.getRotation(), $$0.getRotationPivot());
   }

   public boolean placeInWorld(ServerLevelAccessor $$0, BlockPos $$1, BlockPos $$2, StructurePlaceSettings $$3, RandomSource $$4, int $$5) {
      if (this.palettes.isEmpty()) {
         return false;
      } else {
         List<StructureTemplate.StructureBlockInfo> $$6 = $$3.getRandomPalette(this.palettes, $$1).blocks();
         if ((!$$6.isEmpty() || !$$3.isIgnoreEntities() && !this.entityInfoList.isEmpty())
            && this.size.getX() >= 1
            && this.size.getY() >= 1
            && this.size.getZ() >= 1) {
            BoundingBox $$7 = $$3.getBoundingBox();
            List<BlockPos> $$8 = Lists.newArrayListWithCapacity($$3.shouldKeepLiquids() ? $$6.size() : 0);
            List<BlockPos> $$9 = Lists.newArrayListWithCapacity($$3.shouldKeepLiquids() ? $$6.size() : 0);
            List<Pair<BlockPos, CompoundTag>> $$10 = Lists.newArrayListWithCapacity($$6.size());
            int $$11 = Integer.MAX_VALUE;
            int $$12 = Integer.MAX_VALUE;
            int $$13 = Integer.MAX_VALUE;
            int $$14 = Integer.MIN_VALUE;
            int $$15 = Integer.MIN_VALUE;
            int $$16 = Integer.MIN_VALUE;

            for (StructureTemplate.StructureBlockInfo $$18 : processBlockInfos($$0, $$1, $$2, $$3, $$6)) {
               BlockPos $$19 = $$18.pos;
               if ($$7 == null || $$7.isInside($$19)) {
                  FluidState $$20 = $$3.shouldKeepLiquids() ? $$0.getFluidState($$19) : null;
                  BlockState $$21 = $$18.state.mirror($$3.getMirror()).rotate($$3.getRotation());
                  if ($$18.nbt != null) {
                     BlockEntity $$22 = $$0.getBlockEntity($$19);
                     Clearable.tryClear($$22);
                     $$0.setBlock($$19, Blocks.BARRIER.defaultBlockState(), 20);
                  }

                  if ($$0.setBlock($$19, $$21, $$5)) {
                     $$11 = Math.min($$11, $$19.getX());
                     $$12 = Math.min($$12, $$19.getY());
                     $$13 = Math.min($$13, $$19.getZ());
                     $$14 = Math.max($$14, $$19.getX());
                     $$15 = Math.max($$15, $$19.getY());
                     $$16 = Math.max($$16, $$19.getZ());
                     $$10.add(Pair.of($$19, $$18.nbt));
                     if ($$18.nbt != null) {
                        BlockEntity $$23 = $$0.getBlockEntity($$19);
                        if ($$23 != null) {
                           if ($$23 instanceof RandomizableContainerBlockEntity) {
                              $$18.nbt.putLong("LootTableSeed", $$4.nextLong());
                           }

                           $$23.load($$18.nbt);
                        }
                     }

                     if ($$20 != null) {
                        if ($$21.getFluidState().isSource()) {
                           $$9.add($$19);
                        } else if ($$21.getBlock() instanceof LiquidBlockContainer) {
                           ((LiquidBlockContainer)$$21.getBlock()).placeLiquid($$0, $$19, $$21, $$20);
                           if (!$$20.isSource()) {
                              $$8.add($$19);
                           }
                        }
                     }
                  }
               }
            }

            boolean $$24 = true;
            Direction[] $$25 = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

            while ($$24 && !$$8.isEmpty()) {
               $$24 = false;
               Iterator<BlockPos> $$26 = $$8.iterator();

               while ($$26.hasNext()) {
                  BlockPos $$27 = $$26.next();
                  FluidState $$28 = $$0.getFluidState($$27);

                  for (int $$29 = 0; $$29 < $$25.length && !$$28.isSource(); $$29++) {
                     BlockPos $$30 = $$27.relative($$25[$$29]);
                     FluidState $$31 = $$0.getFluidState($$30);
                     if ($$31.isSource() && !$$9.contains($$30)) {
                        $$28 = $$31;
                     }
                  }

                  if ($$28.isSource()) {
                     BlockState $$32 = $$0.getBlockState($$27);
                     Block $$33 = $$32.getBlock();
                     if ($$33 instanceof LiquidBlockContainer) {
                        ((LiquidBlockContainer)$$33).placeLiquid($$0, $$27, $$32, $$28);
                        $$24 = true;
                        $$26.remove();
                     }
                  }
               }
            }

            if ($$11 <= $$14) {
               if (!$$3.getKnownShape()) {
                  DiscreteVoxelShape $$34 = new BitSetDiscreteVoxelShape($$14 - $$11 + 1, $$15 - $$12 + 1, $$16 - $$13 + 1);
                  int $$35 = $$11;
                  int $$36 = $$12;
                  int $$37 = $$13;

                  for (Pair<BlockPos, CompoundTag> $$38 : $$10) {
                     BlockPos $$39 = (BlockPos)$$38.getFirst();
                     $$34.fill($$39.getX() - $$35, $$39.getY() - $$36, $$39.getZ() - $$37);
                  }

                  updateShapeAtEdge($$0, $$5, $$34, $$35, $$36, $$37);
               }

               for (Pair<BlockPos, CompoundTag> $$40 : $$10) {
                  BlockPos $$41 = (BlockPos)$$40.getFirst();
                  if (!$$3.getKnownShape()) {
                     BlockState $$42 = $$0.getBlockState($$41);
                     BlockState $$43 = Block.updateFromNeighbourShapes($$42, $$0, $$41);
                     if ($$42 != $$43) {
                        $$0.setBlock($$41, $$43, $$5 & -2 | 16);
                     }

                     $$0.blockUpdated($$41, $$43.getBlock());
                  }

                  if ($$40.getSecond() != null) {
                     BlockEntity $$44 = $$0.getBlockEntity($$41);
                     if ($$44 != null) {
                        $$44.setChanged();
                     }
                  }
               }
            }

            if (!$$3.isIgnoreEntities()) {
               this.placeEntities($$0, $$1, $$3.getMirror(), $$3.getRotation(), $$3.getRotationPivot(), $$7, $$3.shouldFinalizeEntities());
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public static void updateShapeAtEdge(LevelAccessor $$0, int $$1, DiscreteVoxelShape $$2, int $$3, int $$4, int $$5) {
      $$2.forAllFaces(($$5x, $$6, $$7, $$8) -> {
         BlockPos $$9 = new BlockPos($$3 + $$6, $$4 + $$7, $$5 + $$8);
         BlockPos $$10 = $$9.relative($$5x);
         BlockState $$11 = $$0.getBlockState($$9);
         BlockState $$12 = $$0.getBlockState($$10);
         BlockState $$13 = $$11.updateShape($$5x, $$12, $$0, $$9, $$10);
         if ($$11 != $$13) {
            $$0.setBlock($$9, $$13, $$1 & -2);
         }

         BlockState $$14 = $$12.updateShape($$5x.getOpposite(), $$13, $$0, $$10, $$9);
         if ($$12 != $$14) {
            $$0.setBlock($$10, $$14, $$1 & -2);
         }
      });
   }

   public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
      ServerLevelAccessor $$0, BlockPos $$1, BlockPos $$2, StructurePlaceSettings $$3, List<StructureTemplate.StructureBlockInfo> $$4
   ) {
      List<StructureTemplate.StructureBlockInfo> $$5 = new ArrayList<>();
      List<StructureTemplate.StructureBlockInfo> $$6 = new ArrayList<>();

      for (StructureTemplate.StructureBlockInfo $$7 : $$4) {
         BlockPos $$8 = calculateRelativePosition($$3, $$7.pos).offset($$1);
         StructureTemplate.StructureBlockInfo $$9 = new StructureTemplate.StructureBlockInfo($$8, $$7.state, $$7.nbt != null ? $$7.nbt.copy() : null);
         Iterator<StructureProcessor> $$10 = $$3.getProcessors().iterator();

         while ($$9 != null && $$10.hasNext()) {
            $$9 = $$10.next().processBlock($$0, $$1, $$2, $$7, $$9, $$3);
         }

         if ($$9 != null) {
            $$6.add($$9);
            $$5.add($$7);
         }
      }

      for (StructureProcessor $$11 : $$3.getProcessors()) {
         $$6 = $$11.finalizeProcessing($$0, $$1, $$2, $$5, $$6, $$3);
      }

      return $$6;
   }

   private void placeEntities(ServerLevelAccessor $$0, BlockPos $$1, Mirror $$2, Rotation $$3, BlockPos $$4, @Nullable BoundingBox $$5, boolean $$6) {
      for (StructureTemplate.StructureEntityInfo $$7 : this.entityInfoList) {
         BlockPos $$8 = transform($$7.blockPos, $$2, $$3, $$4).offset($$1);
         if ($$5 == null || $$5.isInside($$8)) {
            CompoundTag $$9 = $$7.nbt.copy();
            Vec3 $$10 = transform($$7.pos, $$2, $$3, $$4);
            Vec3 $$11 = $$10.add((double)$$1.getX(), (double)$$1.getY(), (double)$$1.getZ());
            ListTag $$12 = new ListTag();
            $$12.add(DoubleTag.valueOf($$11.x));
            $$12.add(DoubleTag.valueOf($$11.y));
            $$12.add(DoubleTag.valueOf($$11.z));
            $$9.put("Pos", $$12);
            $$9.remove("UUID");
            createEntityIgnoreException($$0, $$9).ifPresent($$6x -> {
               float $$7x = $$6x.rotate($$3);
               $$7x += $$6x.mirror($$2) - $$6x.getYRot();
               $$6x.moveTo($$11.x, $$11.y, $$11.z, $$7x, $$6x.getXRot());
               if ($$6 && $$6x instanceof Mob) {
                  ((Mob)$$6x).finalizeSpawn($$0, $$0.getCurrentDifficultyAt(BlockPos.containing($$11)), MobSpawnType.STRUCTURE, null, $$9);
               }

               $$0.addFreshEntityWithPassengers($$6x);
            });
         }
      }
   }

   private static Optional<Entity> createEntityIgnoreException(ServerLevelAccessor $$0, CompoundTag $$1) {
      try {
         return EntityType.create($$1, $$0.getLevel());
      } catch (Exception var3) {
         return Optional.empty();
      }
   }

   public Vec3i getSize(Rotation $$0) {
      switch ($$0) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
         default:
            return this.size;
      }
   }

   public static BlockPos transform(BlockPos $$0, Mirror $$1, Rotation $$2, BlockPos $$3) {
      int $$4 = $$0.getX();
      int $$5 = $$0.getY();
      int $$6 = $$0.getZ();
      boolean $$7 = true;
      switch ($$1) {
         case LEFT_RIGHT:
            $$6 = -$$6;
            break;
         case FRONT_BACK:
            $$4 = -$$4;
            break;
         default:
            $$7 = false;
      }

      int $$8 = $$3.getX();
      int $$9 = $$3.getZ();
      switch ($$2) {
         case COUNTERCLOCKWISE_90:
            return new BlockPos($$8 - $$9 + $$6, $$5, $$8 + $$9 - $$4);
         case CLOCKWISE_90:
            return new BlockPos($$8 + $$9 - $$6, $$5, $$9 - $$8 + $$4);
         case CLOCKWISE_180:
            return new BlockPos($$8 + $$8 - $$4, $$5, $$9 + $$9 - $$6);
         default:
            return $$7 ? new BlockPos($$4, $$5, $$6) : $$0;
      }
   }

   public static Vec3 transform(Vec3 $$0, Mirror $$1, Rotation $$2, BlockPos $$3) {
      double $$4 = $$0.x;
      double $$5 = $$0.y;
      double $$6 = $$0.z;
      boolean $$7 = true;
      switch ($$1) {
         case LEFT_RIGHT:
            $$6 = 1.0 - $$6;
            break;
         case FRONT_BACK:
            $$4 = 1.0 - $$4;
            break;
         default:
            $$7 = false;
      }

      int $$8 = $$3.getX();
      int $$9 = $$3.getZ();
      switch ($$2) {
         case COUNTERCLOCKWISE_90:
            return new Vec3((double)($$8 - $$9) + $$6, $$5, (double)($$8 + $$9 + 1) - $$4);
         case CLOCKWISE_90:
            return new Vec3((double)($$8 + $$9 + 1) - $$6, $$5, (double)($$9 - $$8) + $$4);
         case CLOCKWISE_180:
            return new Vec3((double)($$8 + $$8 + 1) - $$4, $$5, (double)($$9 + $$9 + 1) - $$6);
         default:
            return $$7 ? new Vec3($$4, $$5, $$6) : $$0;
      }
   }

   public BlockPos getZeroPositionWithTransform(BlockPos $$0, Mirror $$1, Rotation $$2) {
      return getZeroPositionWithTransform($$0, $$1, $$2, this.getSize().getX(), this.getSize().getZ());
   }

   public static BlockPos getZeroPositionWithTransform(BlockPos $$0, Mirror $$1, Rotation $$2, int $$3, int $$4) {
      $$3--;
      $$4--;
      int $$5 = $$1 == Mirror.FRONT_BACK ? $$3 : 0;
      int $$6 = $$1 == Mirror.LEFT_RIGHT ? $$4 : 0;
      BlockPos $$7 = $$0;
      switch ($$2) {
         case COUNTERCLOCKWISE_90:
            $$7 = $$0.offset($$6, 0, $$3 - $$5);
            break;
         case CLOCKWISE_90:
            $$7 = $$0.offset($$4 - $$6, 0, $$5);
            break;
         case CLOCKWISE_180:
            $$7 = $$0.offset($$3 - $$5, 0, $$4 - $$6);
            break;
         case NONE:
            $$7 = $$0.offset($$5, 0, $$6);
      }

      return $$7;
   }

   public BoundingBox getBoundingBox(StructurePlaceSettings $$0, BlockPos $$1) {
      return this.getBoundingBox($$1, $$0.getRotation(), $$0.getRotationPivot(), $$0.getMirror());
   }

   public BoundingBox getBoundingBox(BlockPos $$0, Rotation $$1, BlockPos $$2, Mirror $$3) {
      return getBoundingBox($$0, $$1, $$2, $$3, this.size);
   }

   @VisibleForTesting
   protected static BoundingBox getBoundingBox(BlockPos $$0, Rotation $$1, BlockPos $$2, Mirror $$3, Vec3i $$4) {
      Vec3i $$5 = $$4.offset(-1, -1, -1);
      BlockPos $$6 = transform(BlockPos.ZERO, $$3, $$1, $$2);
      BlockPos $$7 = transform(BlockPos.ZERO.offset($$5), $$3, $$1, $$2);
      return BoundingBox.fromCorners($$6, $$7).move($$0);
   }

   public CompoundTag save(CompoundTag $$0) {
      if (this.palettes.isEmpty()) {
         $$0.put("blocks", new ListTag());
         $$0.put("palette", new ListTag());
      } else {
         List<StructureTemplate.SimplePalette> $$1 = Lists.newArrayList();
         StructureTemplate.SimplePalette $$2 = new StructureTemplate.SimplePalette();
         $$1.add($$2);

         for (int $$3 = 1; $$3 < this.palettes.size(); $$3++) {
            $$1.add(new StructureTemplate.SimplePalette());
         }

         ListTag $$4 = new ListTag();
         List<StructureTemplate.StructureBlockInfo> $$5 = this.palettes.get(0).blocks();

         for (int $$6 = 0; $$6 < $$5.size(); $$6++) {
            StructureTemplate.StructureBlockInfo $$7 = $$5.get($$6);
            CompoundTag $$8 = new CompoundTag();
            $$8.put("pos", this.newIntegerList($$7.pos.getX(), $$7.pos.getY(), $$7.pos.getZ()));
            int $$9 = $$2.idFor($$7.state);
            $$8.putInt("state", $$9);
            if ($$7.nbt != null) {
               $$8.put("nbt", $$7.nbt);
            }

            $$4.add($$8);

            for (int $$10 = 1; $$10 < this.palettes.size(); $$10++) {
               StructureTemplate.SimplePalette $$11 = $$1.get($$10);
               $$11.addMapping(this.palettes.get($$10).blocks().get($$6).state, $$9);
            }
         }

         $$0.put("blocks", $$4);
         if ($$1.size() == 1) {
            ListTag $$12 = new ListTag();

            for (BlockState $$13 : $$2) {
               $$12.add(NbtUtils.writeBlockState($$13));
            }

            $$0.put("palette", $$12);
         } else {
            ListTag $$14 = new ListTag();

            for (StructureTemplate.SimplePalette $$15 : $$1) {
               ListTag $$16 = new ListTag();

               for (BlockState $$17 : $$15) {
                  $$16.add(NbtUtils.writeBlockState($$17));
               }

               $$14.add($$16);
            }

            $$0.put("palettes", $$14);
         }
      }

      ListTag $$18 = new ListTag();

      for (StructureTemplate.StructureEntityInfo $$19 : this.entityInfoList) {
         CompoundTag $$20 = new CompoundTag();
         $$20.put("pos", this.newDoubleList($$19.pos.x, $$19.pos.y, $$19.pos.z));
         $$20.put("blockPos", this.newIntegerList($$19.blockPos.getX(), $$19.blockPos.getY(), $$19.blockPos.getZ()));
         if ($$19.nbt != null) {
            $$20.put("nbt", $$19.nbt);
         }

         $$18.add($$20);
      }

      $$0.put("entities", $$18);
      $$0.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
      return NbtUtils.addCurrentDataVersion($$0);
   }

   public void load(HolderGetter<Block> $$0, CompoundTag $$1) {
      this.palettes.clear();
      this.entityInfoList.clear();
      ListTag $$2 = $$1.getList("size", 3);
      this.size = new Vec3i($$2.getInt(0), $$2.getInt(1), $$2.getInt(2));
      ListTag $$3 = $$1.getList("blocks", 10);
      if ($$1.contains("palettes", 9)) {
         ListTag $$4 = $$1.getList("palettes", 9);

         for (int $$5 = 0; $$5 < $$4.size(); $$5++) {
            this.loadPalette($$0, $$4.getList($$5), $$3);
         }
      } else {
         this.loadPalette($$0, $$1.getList("palette", 10), $$3);
      }

      ListTag $$6 = $$1.getList("entities", 10);

      for (int $$7 = 0; $$7 < $$6.size(); $$7++) {
         CompoundTag $$8 = $$6.getCompound($$7);
         ListTag $$9 = $$8.getList("pos", 6);
         Vec3 $$10 = new Vec3($$9.getDouble(0), $$9.getDouble(1), $$9.getDouble(2));
         ListTag $$11 = $$8.getList("blockPos", 3);
         BlockPos $$12 = new BlockPos($$11.getInt(0), $$11.getInt(1), $$11.getInt(2));
         if ($$8.contains("nbt")) {
            CompoundTag $$13 = $$8.getCompound("nbt");
            this.entityInfoList.add(new StructureTemplate.StructureEntityInfo($$10, $$12, $$13));
         }
      }
   }

   private void loadPalette(HolderGetter<Block> $$0, ListTag $$1, ListTag $$2) {
      StructureTemplate.SimplePalette $$3 = new StructureTemplate.SimplePalette();

      for (int $$4 = 0; $$4 < $$1.size(); $$4++) {
         $$3.addMapping(NbtUtils.readBlockState($$0, $$1.getCompound($$4)), $$4);
      }

      List<StructureTemplate.StructureBlockInfo> $$5 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> $$6 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> $$7 = Lists.newArrayList();

      for (int $$8 = 0; $$8 < $$2.size(); $$8++) {
         CompoundTag $$9 = $$2.getCompound($$8);
         ListTag $$10 = $$9.getList("pos", 3);
         BlockPos $$11 = new BlockPos($$10.getInt(0), $$10.getInt(1), $$10.getInt(2));
         BlockState $$12 = $$3.stateFor($$9.getInt("state"));
         CompoundTag $$13;
         if ($$9.contains("nbt")) {
            $$13 = $$9.getCompound("nbt");
         } else {
            $$13 = null;
         }

         StructureTemplate.StructureBlockInfo $$15 = new StructureTemplate.StructureBlockInfo($$11, $$12, $$13);
         addToLists($$15, $$5, $$6, $$7);
      }

      List<StructureTemplate.StructureBlockInfo> $$16 = buildInfoList($$5, $$6, $$7);
      this.palettes.add(new StructureTemplate.Palette($$16));
   }

   private ListTag newIntegerList(int... $$0) {
      ListTag $$1 = new ListTag();

      for (int $$2 : $$0) {
         $$1.add(IntTag.valueOf($$2));
      }

      return $$1;
   }

   private ListTag newDoubleList(double... $$0) {
      ListTag $$1 = new ListTag();

      for (double $$2 : $$0) {
         $$1.add(DoubleTag.valueOf($$2));
      }

      return $$1;
   }

   public static final class Palette {
      private final List<StructureTemplate.StructureBlockInfo> blocks;
      private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();

      Palette(List<StructureTemplate.StructureBlockInfo> $$0) {
         this.blocks = $$0;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks() {
         return this.blocks;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks(Block $$0) {
         return this.cache.computeIfAbsent($$0, $$0x -> this.blocks.stream().filter($$1 -> $$1.state.is($$0x)).collect(Collectors.toList()));
      }
   }

   static class SimplePalette implements Iterable<BlockState> {
      public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
      private final IdMapper<BlockState> ids = new IdMapper<>(16);
      private int lastId;

      public int idFor(BlockState $$0) {
         int $$1 = this.ids.getId($$0);
         if ($$1 == -1) {
            $$1 = this.lastId++;
            this.ids.addMapping($$0, $$1);
         }

         return $$1;
      }

      @Nullable
      public BlockState stateFor(int $$0) {
         BlockState $$1 = this.ids.byId($$0);
         return $$1 == null ? DEFAULT_BLOCK_STATE : $$1;
      }

      @Override
      public Iterator<BlockState> iterator() {
         return this.ids.iterator();
      }

      public void addMapping(BlockState $$0, int $$1) {
         this.ids.addMapping($$0, $$1);
      }
   }

   public static record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {

      @Override
      public String toString() {
         return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
      }
   }

   public static class StructureEntityInfo {
      public final Vec3 pos;
      public final BlockPos blockPos;
      public final CompoundTag nbt;

      public StructureEntityInfo(Vec3 $$0, BlockPos $$1, CompoundTag $$2) {
         this.pos = $$0;
         this.blockPos = $$1;
         this.nbt = $$2;
      }
   }
}
