package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
   static final Logger LOGGER = LogUtils.getLogger();

   public static Optional<Structure.GenerationStub> addPieces(
      Structure.GenerationContext $$0,
      Holder<StructureTemplatePool> $$1,
      Optional<ResourceLocation> $$2,
      int $$3,
      BlockPos $$4,
      boolean $$5,
      Optional<Heightmap.Types> $$6,
      int $$7
   ) {
      RegistryAccess $$8 = $$0.registryAccess();
      ChunkGenerator $$9 = $$0.chunkGenerator();
      StructureTemplateManager $$10 = $$0.structureTemplateManager();
      LevelHeightAccessor $$11 = $$0.heightAccessor();
      WorldgenRandom $$12 = $$0.random();
      Registry<StructureTemplatePool> $$13 = $$8.registryOrThrow(Registries.TEMPLATE_POOL);
      Rotation $$14 = Rotation.getRandom($$12);
      StructureTemplatePool $$15 = $$1.value();
      StructurePoolElement $$16 = $$15.getRandomTemplate($$12);
      if ($$16 == EmptyPoolElement.INSTANCE) {
         return Optional.empty();
      } else {
         BlockPos $$19;
         if ($$2.isPresent()) {
            ResourceLocation $$17 = $$2.get();
            Optional<BlockPos> $$18 = getRandomNamedJigsaw($$16, $$17, $$4, $$14, $$10, $$12);
            if ($$18.isEmpty()) {
               LOGGER.error(
                  "No starting jigsaw {} found in start pool {}", $$17, $$1.unwrapKey().map($$0x -> $$0x.location().toString()).orElse("<unregistered>")
               );
               return Optional.empty();
            }

            $$19 = $$18.get();
         } else {
            $$19 = $$4;
         }

         Vec3i $$21 = $$19.subtract($$4);
         BlockPos $$22 = $$4.subtract($$21);
         PoolElementStructurePiece $$23 = new PoolElementStructurePiece(
            $$10, $$16, $$22, $$16.getGroundLevelDelta(), $$14, $$16.getBoundingBox($$10, $$22, $$14)
         );
         BoundingBox $$24 = $$23.getBoundingBox();
         int $$25 = ($$24.maxX() + $$24.minX()) / 2;
         int $$26 = ($$24.maxZ() + $$24.minZ()) / 2;
         int $$27;
         if ($$6.isPresent()) {
            $$27 = $$4.getY() + $$9.getFirstFreeHeight($$25, $$26, $$6.get(), $$11, $$0.randomState());
         } else {
            $$27 = $$22.getY();
         }

         int $$29 = $$24.minY() + $$23.getGroundLevelDelta();
         $$23.move(0, $$27 - $$29, 0);
         int $$30 = $$27 + $$21.getY();
         return Optional.of(
            new Structure.GenerationStub(
               new BlockPos($$25, $$30, $$26),
               (Consumer<StructurePiecesBuilder>)($$14x -> {
                  List<PoolElementStructurePiece> $$15x = Lists.newArrayList();
                  $$15x.add($$23);
                  if ($$3 > 0) {
                     AABB $$16x = new AABB(
                        (double)($$25 - $$7),
                        (double)($$30 - $$7),
                        (double)($$26 - $$7),
                        (double)($$25 + $$7 + 1),
                        (double)($$30 + $$7 + 1),
                        (double)($$26 + $$7 + 1)
                     );
                     VoxelShape $$17 = Shapes.join(Shapes.create($$16x), Shapes.create(AABB.of($$24)), BooleanOp.ONLY_FIRST);
                     addPieces($$0.randomState(), $$3, $$5, $$9, $$10, $$11, $$12, $$13, $$23, $$15x, $$17);
                     $$15x.forEach($$14x::addPiece);
                  }
               })
            )
         );
      }
   }

   private static Optional<BlockPos> getRandomNamedJigsaw(
      StructurePoolElement $$0, ResourceLocation $$1, BlockPos $$2, Rotation $$3, StructureTemplateManager $$4, WorldgenRandom $$5
   ) {
      List<StructureTemplate.StructureBlockInfo> $$6 = $$0.getShuffledJigsawBlocks($$4, $$2, $$3, $$5);
      Optional<BlockPos> $$7 = Optional.empty();

      for (StructureTemplate.StructureBlockInfo $$8 : $$6) {
         ResourceLocation $$9 = ResourceLocation.tryParse($$8.nbt().getString("name"));
         if ($$1.equals($$9)) {
            $$7 = Optional.of($$8.pos());
            break;
         }
      }

      return $$7;
   }

   private static void addPieces(
      RandomState $$0,
      int $$1,
      boolean $$2,
      ChunkGenerator $$3,
      StructureTemplateManager $$4,
      LevelHeightAccessor $$5,
      RandomSource $$6,
      Registry<StructureTemplatePool> $$7,
      PoolElementStructurePiece $$8,
      List<PoolElementStructurePiece> $$9,
      VoxelShape $$10
   ) {
      JigsawPlacement.Placer $$11 = new JigsawPlacement.Placer($$7, $$1, $$3, $$4, $$9, $$6);
      $$11.placing.addLast(new JigsawPlacement.PieceState($$8, new MutableObject($$10), 0));

      while (!$$11.placing.isEmpty()) {
         JigsawPlacement.PieceState $$12 = $$11.placing.removeFirst();
         $$11.tryPlacingChildren($$12.piece, $$12.free, $$12.depth, $$2, $$5, $$0);
      }
   }

   public static boolean generateJigsaw(ServerLevel $$0, Holder<StructureTemplatePool> $$1, ResourceLocation $$2, int $$3, BlockPos $$4, boolean $$5) {
      ChunkGenerator $$6 = $$0.getChunkSource().getGenerator();
      StructureTemplateManager $$7 = $$0.getStructureManager();
      StructureManager $$8 = $$0.structureManager();
      RandomSource $$9 = $$0.getRandom();
      Structure.GenerationContext $$10 = new Structure.GenerationContext(
         $$0.registryAccess(), $$6, $$6.getBiomeSource(), $$0.getChunkSource().randomState(), $$7, $$0.getSeed(), new ChunkPos($$4), $$0, $$0x -> true
      );
      Optional<Structure.GenerationStub> $$11 = addPieces($$10, $$1, Optional.of($$2), $$3, $$4, false, Optional.empty(), 128);
      if ($$11.isPresent()) {
         StructurePiecesBuilder $$12 = $$11.get().getPiecesBuilder();

         for (StructurePiece $$13 : $$12.build().pieces()) {
            if ($$13 instanceof PoolElementStructurePiece $$14) {
               $$14.place($$0, $$8, $$6, $$9, BoundingBox.infinite(), $$4, $$5);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static final class PieceState {
      final PoolElementStructurePiece piece;
      final MutableObject<VoxelShape> free;
      final int depth;

      PieceState(PoolElementStructurePiece $$0, MutableObject<VoxelShape> $$1, int $$2) {
         this.piece = $$0;
         this.free = $$1;
         this.depth = $$2;
      }
   }

   static final class Placer {
      private final Registry<StructureTemplatePool> pools;
      private final int maxDepth;
      private final ChunkGenerator chunkGenerator;
      private final StructureTemplateManager structureTemplateManager;
      private final List<? super PoolElementStructurePiece> pieces;
      private final RandomSource random;
      final Deque<JigsawPlacement.PieceState> placing = Queues.newArrayDeque();

      Placer(
         Registry<StructureTemplatePool> $$0,
         int $$1,
         ChunkGenerator $$2,
         StructureTemplateManager $$3,
         List<? super PoolElementStructurePiece> $$4,
         RandomSource $$5
      ) {
         this.pools = $$0;
         this.maxDepth = $$1;
         this.chunkGenerator = $$2;
         this.structureTemplateManager = $$3;
         this.pieces = $$4;
         this.random = $$5;
      }

      void tryPlacingChildren(PoolElementStructurePiece $$0, MutableObject<VoxelShape> $$1, int $$2, boolean $$3, LevelHeightAccessor $$4, RandomState $$5) {
         StructurePoolElement $$6 = $$0.getElement();
         BlockPos $$7 = $$0.getPosition();
         Rotation $$8 = $$0.getRotation();
         StructureTemplatePool.Projection $$9 = $$6.getProjection();
         boolean $$10 = $$9 == StructureTemplatePool.Projection.RIGID;
         MutableObject<VoxelShape> $$11 = new MutableObject();
         BoundingBox $$12 = $$0.getBoundingBox();
         int $$13 = $$12.minY();

         label129:
         for (StructureTemplate.StructureBlockInfo $$14 : $$6.getShuffledJigsawBlocks(this.structureTemplateManager, $$7, $$8, this.random)) {
            Direction $$15 = JigsawBlock.getFrontFacing($$14.state());
            BlockPos $$16 = $$14.pos();
            BlockPos $$17 = $$16.relative($$15);
            int $$18 = $$16.getY() - $$13;
            int $$19 = -1;
            ResourceKey<StructureTemplatePool> $$20 = readPoolName($$14);
            Optional<? extends Holder<StructureTemplatePool>> $$21 = this.pools.getHolder($$20);
            if ($$21.isEmpty()) {
               JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", $$20.location());
            } else {
               Holder<StructureTemplatePool> $$22 = (Holder<StructureTemplatePool>)$$21.get();
               if ($$22.value().size() == 0 && !$$22.is(Pools.EMPTY)) {
                  JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", $$20.location());
               } else {
                  Holder<StructureTemplatePool> $$23 = $$22.value().getFallback();
                  if ($$23.value().size() == 0 && !$$23.is(Pools.EMPTY)) {
                     JigsawPlacement.LOGGER
                        .warn("Empty or non-existent fallback pool: {}", $$23.unwrapKey().map($$0x -> $$0x.location().toString()).orElse("<unregistered>"));
                  } else {
                     boolean $$24 = $$12.isInside($$17);
                     MutableObject<VoxelShape> $$25;
                     if ($$24) {
                        $$25 = $$11;
                        if ($$11.getValue() == null) {
                           $$11.setValue(Shapes.create(AABB.of($$12)));
                        }
                     } else {
                        $$25 = $$1;
                     }

                     List<StructurePoolElement> $$27 = Lists.newArrayList();
                     if ($$2 != this.maxDepth) {
                        $$27.addAll($$22.value().getShuffledTemplates(this.random));
                     }

                     $$27.addAll($$23.value().getShuffledTemplates(this.random));

                     for (StructurePoolElement $$28 : $$27) {
                        if ($$28 == EmptyPoolElement.INSTANCE) {
                           break;
                        }

                        for (Rotation $$29 : Rotation.getShuffled(this.random)) {
                           List<StructureTemplate.StructureBlockInfo> $$30 = $$28.getShuffledJigsawBlocks(
                              this.structureTemplateManager, BlockPos.ZERO, $$29, this.random
                           );
                           BoundingBox $$31 = $$28.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, $$29);
                           int $$33;
                           if ($$3 && $$31.getYSpan() <= 16) {
                              $$33 = $$30.stream()
                                 .mapToInt(
                                    $$1x -> {
                                       if (!$$31.isInside($$1x.pos().relative(JigsawBlock.getFrontFacing($$1x.state())))) {
                                          return 0;
                                       } else {
                                          ResourceKey<StructureTemplatePool> $$2x = readPoolName($$1x);
                                          Optional<? extends Holder<StructureTemplatePool>> $$3x = this.pools.getHolder($$2x);
                                          Optional<Holder<StructureTemplatePool>> $$4x = $$3x.map($$0xx -> ((StructureTemplatePool)$$0xx.value()).getFallback());
                                          int $$5x = $$3x.<Integer>map(
                                                $$0xx -> ((StructureTemplatePool)$$0xx.value()).getMaxSize(this.structureTemplateManager)
                                             )
                                             .orElse(0);
                                          int $$6x = $$4x.<Integer>map(
                                                $$0xx -> ((StructureTemplatePool)$$0xx.value()).getMaxSize(this.structureTemplateManager)
                                             )
                                             .orElse(0);
                                          return Math.max($$5x, $$6x);
                                       }
                                    }
                                 )
                                 .max()
                                 .orElse(0);
                           } else {
                              $$33 = 0;
                           }

                           for (StructureTemplate.StructureBlockInfo $$34 : $$30) {
                              if (JigsawBlock.canAttach($$14, $$34)) {
                                 BlockPos $$35 = $$34.pos();
                                 BlockPos $$36 = $$17.subtract($$35);
                                 BoundingBox $$37 = $$28.getBoundingBox(this.structureTemplateManager, $$36, $$29);
                                 int $$38 = $$37.minY();
                                 StructureTemplatePool.Projection $$39 = $$28.getProjection();
                                 boolean $$40 = $$39 == StructureTemplatePool.Projection.RIGID;
                                 int $$41 = $$35.getY();
                                 int $$42 = $$18 - $$41 + JigsawBlock.getFrontFacing($$14.state()).getStepY();
                                 int $$43;
                                 if ($$10 && $$40) {
                                    $$43 = $$13 + $$42;
                                 } else {
                                    if ($$19 == -1) {
                                       $$19 = this.chunkGenerator.getFirstFreeHeight($$16.getX(), $$16.getZ(), Heightmap.Types.WORLD_SURFACE_WG, $$4, $$5);
                                    }

                                    $$43 = $$19 - $$41;
                                 }

                                 int $$45 = $$43 - $$38;
                                 BoundingBox $$46 = $$37.moved(0, $$45, 0);
                                 BlockPos $$47 = $$36.offset(0, $$45, 0);
                                 if ($$33 > 0) {
                                    int $$48 = Math.max($$33 + 1, $$46.maxY() - $$46.minY());
                                    $$46.encapsulate(new BlockPos($$46.minX(), $$46.minY() + $$48, $$46.minZ()));
                                 }

                                 if (!Shapes.joinIsNotEmpty((VoxelShape)$$25.getValue(), Shapes.create(AABB.of($$46).deflate(0.25)), BooleanOp.ONLY_SECOND)) {
                                    $$25.setValue(Shapes.joinUnoptimized((VoxelShape)$$25.getValue(), Shapes.create(AABB.of($$46)), BooleanOp.ONLY_FIRST));
                                    int $$49 = $$0.getGroundLevelDelta();
                                    int $$50;
                                    if ($$40) {
                                       $$50 = $$49 - $$42;
                                    } else {
                                       $$50 = $$28.getGroundLevelDelta();
                                    }

                                    PoolElementStructurePiece $$52 = new PoolElementStructurePiece(this.structureTemplateManager, $$28, $$47, $$50, $$29, $$46);
                                    int $$53;
                                    if ($$10) {
                                       $$53 = $$13 + $$18;
                                    } else if ($$40) {
                                       $$53 = $$43 + $$41;
                                    } else {
                                       if ($$19 == -1) {
                                          $$19 = this.chunkGenerator.getFirstFreeHeight($$16.getX(), $$16.getZ(), Heightmap.Types.WORLD_SURFACE_WG, $$4, $$5);
                                       }

                                       $$53 = $$19 + $$42 / 2;
                                    }

                                    $$0.addJunction(new JigsawJunction($$17.getX(), $$53 - $$18 + $$49, $$17.getZ(), $$42, $$39));
                                    $$52.addJunction(new JigsawJunction($$16.getX(), $$53 - $$41 + $$50, $$16.getZ(), -$$42, $$9));
                                    this.pieces.add($$52);
                                    if ($$2 + 1 <= this.maxDepth) {
                                       this.placing.addLast(new JigsawPlacement.PieceState($$52, $$25, $$2 + 1));
                                    }
                                    continue label129;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private static ResourceKey<StructureTemplatePool> readPoolName(StructureTemplate.StructureBlockInfo $$0) {
         return ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation($$0.nbt().getString("pool")));
      }
   }
}
