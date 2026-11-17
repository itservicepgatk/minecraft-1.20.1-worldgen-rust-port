package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Beardifier implements DensityFunctions.BeardifierOrMarker {
   public static final int BEARD_KERNEL_RADIUS = 12;
   private static final int BEARD_KERNEL_SIZE = 24;
   private static final float[] BEARD_KERNEL = Util.make(new float[13824], $$0 -> {
      for (int $$1 = 0; $$1 < 24; $$1++) {
         for (int $$2 = 0; $$2 < 24; $$2++) {
            for (int $$3 = 0; $$3 < 24; $$3++) {
               $$0[$$1 * 24 * 24 + $$2 * 24 + $$3] = (float)computeBeardContribution($$2 - 12, $$3 - 12, $$1 - 12);
            }
         }
      }
   });
   private final ObjectListIterator<Beardifier.Rigid> pieceIterator;
   private final ObjectListIterator<JigsawJunction> junctionIterator;

   public static Beardifier forStructuresInChunk(StructureManager $$0, ChunkPos $$1) {
      int $$2 = $$1.getMinBlockX();
      int $$3 = $$1.getMinBlockZ();
      ObjectList<Beardifier.Rigid> $$4 = new ObjectArrayList(10);
      ObjectList<JigsawJunction> $$5 = new ObjectArrayList(32);
      $$0.startsForStructure($$1, $$0x -> $$0x.terrainAdaptation() != TerrainAdjustment.NONE).forEach($$5x -> {
         TerrainAdjustment $$6 = $$5x.getStructure().terrainAdaptation();

         for (StructurePiece $$7 : $$5x.getPieces()) {
            if ($$7.isCloseToChunk($$1, 12)) {
               if ($$7 instanceof PoolElementStructurePiece) {
                  PoolElementStructurePiece $$8 = (PoolElementStructurePiece)$$7;
                  StructureTemplatePool.Projection $$9 = $$8.getElement().getProjection();
                  if ($$9 == StructureTemplatePool.Projection.RIGID) {
                     $$4.add(new Beardifier.Rigid($$8.getBoundingBox(), $$6, $$8.getGroundLevelDelta()));
                  }

                  for (JigsawJunction $$10 : $$8.getJunctions()) {
                     int $$11 = $$10.getSourceX();
                     int $$12 = $$10.getSourceZ();
                     if ($$11 > $$2 - 12 && $$12 > $$3 - 12 && $$11 < $$2 + 15 + 12 && $$12 < $$3 + 15 + 12) {
                        $$5.add($$10);
                     }
                  }
               } else {
                  $$4.add(new Beardifier.Rigid($$7.getBoundingBox(), $$6, 0));
               }
            }
         }
      });
      return new Beardifier($$4.iterator(), $$5.iterator());
   }

   @VisibleForTesting
   public Beardifier(ObjectListIterator<Beardifier.Rigid> $$0, ObjectListIterator<JigsawJunction> $$1) {
      this.pieceIterator = $$0;
      this.junctionIterator = $$1;
   }

   @Override
   public double compute(DensityFunction.FunctionContext $$0) {
      int $$1 = $$0.blockX();
      int $$2 = $$0.blockY();
      int $$3 = $$0.blockZ();
      double $$4 = 0.0;

      while (this.pieceIterator.hasNext()) {
         Beardifier.Rigid $$5 = (Beardifier.Rigid)this.pieceIterator.next();
         BoundingBox $$6 = $$5.box();
         int $$7 = $$5.groundLevelDelta();
         int $$8 = Math.max(0, Math.max($$6.minX() - $$1, $$1 - $$6.maxX()));
         int $$9 = Math.max(0, Math.max($$6.minZ() - $$3, $$3 - $$6.maxZ()));
         int $$10 = $$6.minY() + $$7;
         int $$11 = $$2 - $$10;

         int $$12 = switch ($$5.terrainAdjustment()) {
            case NONE -> 0;
            case BURY, BEARD_THIN -> $$11;
            case BEARD_BOX -> Math.max(0, Math.max($$10 - $$2, $$2 - $$6.maxY()));
         };

         $$4 += switch ($$5.terrainAdjustment()) {
            case NONE -> 0.0;
            case BURY -> getBuryContribution($$8, $$12, $$9);
            case BEARD_THIN, BEARD_BOX -> getBeardContribution($$8, $$12, $$9, $$11) * 0.8;
         };
      }

      this.pieceIterator.back(Integer.MAX_VALUE);

      while (this.junctionIterator.hasNext()) {
         JigsawJunction $$13 = (JigsawJunction)this.junctionIterator.next();
         int $$14 = $$1 - $$13.getSourceX();
         int $$15 = $$2 - $$13.getSourceGroundY();
         int $$16 = $$3 - $$13.getSourceZ();
         $$4 += getBeardContribution($$14, $$15, $$16, $$15) * 0.4;
      }

      this.junctionIterator.back(Integer.MAX_VALUE);
      return $$4;
   }

   @Override
   public double minValue() {
      return Double.NEGATIVE_INFINITY;
   }

   @Override
   public double maxValue() {
      return Double.POSITIVE_INFINITY;
   }

   private static double getBuryContribution(int $$0, int $$1, int $$2) {
      double $$3 = Mth.length((double)$$0, (double)$$1 / 2.0, (double)$$2);
      return Mth.clampedMap($$3, 0.0, 6.0, 1.0, 0.0);
   }

   private static double getBeardContribution(int $$0, int $$1, int $$2, int $$3) {
      int $$4 = $$0 + 12;
      int $$5 = $$1 + 12;
      int $$6 = $$2 + 12;
      if (isInKernelRange($$4) && isInKernelRange($$5) && isInKernelRange($$6)) {
         double $$7 = (double)$$3 + 0.5;
         double $$8 = Mth.lengthSquared((double)$$0, $$7, (double)$$2);
         double $$9 = -$$7 * Mth.fastInvSqrt($$8 / 2.0) / 2.0;
         return $$9 * (double)BEARD_KERNEL[$$6 * 24 * 24 + $$4 * 24 + $$5];
      } else {
         return 0.0;
      }
   }

   private static boolean isInKernelRange(int $$0) {
      return $$0 >= 0 && $$0 < 24;
   }

   private static double computeBeardContribution(int $$0, int $$1, int $$2) {
      return computeBeardContribution($$0, (double)$$1 + 0.5, $$2);
   }

   private static double computeBeardContribution(int $$0, double $$1, int $$2) {
      double $$3 = Mth.lengthSquared((double)$$0, $$1, (double)$$2);
      return Math.pow(Math.E, -$$3 / 16.0);
   }

   @VisibleForTesting
   public static record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
   }
}
