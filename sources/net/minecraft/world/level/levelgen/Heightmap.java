package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Predicate<BlockState> NOT_AIR = $$0 -> !$$0.isAir();
   static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = BlockStateBase::blocksMotion;
   private final BitStorage data;
   private final Predicate<BlockState> isOpaque;
   private final ChunkAccess chunk;

   public Heightmap(ChunkAccess $$0, Heightmap.Types $$1) {
      this.isOpaque = $$1.isOpaque();
      this.chunk = $$0;
      int $$2 = Mth.ceillog2($$0.getHeight() + 1);
      this.data = new SimpleBitStorage($$2, 256);
   }

   public static void primeHeightmaps(ChunkAccess $$0, Set<Heightmap.Types> $$1) {
      int $$2 = $$1.size();
      ObjectList<Heightmap> $$3 = new ObjectArrayList($$2);
      ObjectListIterator<Heightmap> $$4 = $$3.iterator();
      int $$5 = $$0.getHighestSectionPosition() + 16;
      MutableBlockPos $$6 = new MutableBlockPos();

      for (int $$7 = 0; $$7 < 16; $$7++) {
         for (int $$8 = 0; $$8 < 16; $$8++) {
            for (Heightmap.Types $$9 : $$1) {
               $$3.add($$0.getOrCreateHeightmapUnprimed($$9));
            }

            for (int $$10 = $$5 - 1; $$10 >= $$0.getMinBuildHeight(); $$10--) {
               $$6.set($$7, $$10, $$8);
               BlockState $$11 = $$0.getBlockState($$6);
               if (!$$11.is(Blocks.AIR)) {
                  while ($$4.hasNext()) {
                     Heightmap $$12 = (Heightmap)$$4.next();
                     if ($$12.isOpaque.test($$11)) {
                        $$12.setHeight($$7, $$8, $$10 + 1);
                        $$4.remove();
                     }
                  }

                  if ($$3.isEmpty()) {
                     break;
                  }

                  $$4.back($$2);
               }
            }
         }
      }
   }

   public boolean update(int $$0, int $$1, int $$2, BlockState $$3) {
      int $$4 = this.getFirstAvailable($$0, $$2);
      if ($$1 <= $$4 - 2) {
         return false;
      } else {
         if (this.isOpaque.test($$3)) {
            if ($$1 >= $$4) {
               this.setHeight($$0, $$2, $$1 + 1);
               return true;
            }
         } else if ($$4 - 1 == $$1) {
            MutableBlockPos $$5 = new MutableBlockPos();

            for (int $$6 = $$1 - 1; $$6 >= this.chunk.getMinBuildHeight(); $$6--) {
               $$5.set($$0, $$6, $$2);
               if (this.isOpaque.test(this.chunk.getBlockState($$5))) {
                  this.setHeight($$0, $$2, $$6 + 1);
                  return true;
               }
            }

            this.setHeight($$0, $$2, this.chunk.getMinBuildHeight());
            return true;
         }

         return false;
      }
   }

   public int getFirstAvailable(int $$0, int $$1) {
      return this.getFirstAvailable(getIndex($$0, $$1));
   }

   public int getHighestTaken(int $$0, int $$1) {
      return this.getFirstAvailable(getIndex($$0, $$1)) - 1;
   }

   private int getFirstAvailable(int $$0) {
      return this.data.get($$0) + this.chunk.getMinBuildHeight();
   }

   private void setHeight(int $$0, int $$1, int $$2) {
      this.data.set(getIndex($$0, $$1), $$2 - this.chunk.getMinBuildHeight());
   }

   public void setRawData(ChunkAccess $$0, Heightmap.Types $$1, long[] $$2) {
      long[] $$3 = this.data.getRaw();
      if ($$3.length == $$2.length) {
         System.arraycopy($$2, 0, $$3, 0, $$2.length);
      } else {
         LOGGER.warn("Ignoring heightmap data for chunk " + $$0.getPos() + ", size does not match; expected: " + $$3.length + ", got: " + $$2.length);
         primeHeightmaps($$0, EnumSet.of($$1));
      }
   }

   public long[] getRawData() {
      return this.data.getRaw();
   }

   private static int getIndex(int $$0, int $$1) {
      return $$0 + $$1 * 16;
   }

   public static enum Types implements StringRepresentable {
      WORLD_SURFACE_WG("WORLD_SURFACE_WG", Heightmap.Usage.WORLDGEN, Heightmap.NOT_AIR),
      WORLD_SURFACE("WORLD_SURFACE", Heightmap.Usage.CLIENT, Heightmap.NOT_AIR),
      OCEAN_FLOOR_WG("OCEAN_FLOOR_WG", Heightmap.Usage.WORLDGEN, Heightmap.MATERIAL_MOTION_BLOCKING),
      OCEAN_FLOOR("OCEAN_FLOOR", Heightmap.Usage.LIVE_WORLD, Heightmap.MATERIAL_MOTION_BLOCKING),
      MOTION_BLOCKING("MOTION_BLOCKING", Heightmap.Usage.CLIENT, $$0 -> $$0.blocksMotion() || !$$0.getFluidState().isEmpty()),
      MOTION_BLOCKING_NO_LEAVES(
         "MOTION_BLOCKING_NO_LEAVES",
         Heightmap.Usage.LIVE_WORLD,
         $$0 -> ($$0.blocksMotion() || !$$0.getFluidState().isEmpty()) && !($$0.getBlock() instanceof LeavesBlock)
      );

      public static final Codec<Heightmap.Types> CODEC = StringRepresentable.fromEnum(Heightmap.Types::values);
      private final String serializationKey;
      private final Heightmap.Usage usage;
      private final Predicate<BlockState> isOpaque;

      private Types(String $$0, Heightmap.Usage $$1, Predicate<BlockState> $$2) {
         this.serializationKey = $$0;
         this.usage = $$1;
         this.isOpaque = $$2;
      }

      public String getSerializationKey() {
         return this.serializationKey;
      }

      public boolean sendToClient() {
         return this.usage == Heightmap.Usage.CLIENT;
      }

      public boolean keepAfterWorldgen() {
         return this.usage != Heightmap.Usage.WORLDGEN;
      }

      public Predicate<BlockState> isOpaque() {
         return this.isOpaque;
      }

      @Override
      public String getSerializedName() {
         return this.serializationKey;
      }
   }

   public static enum Usage {
      WORLDGEN,
      LIVE_WORLD,
      CLIENT;
   }
}
