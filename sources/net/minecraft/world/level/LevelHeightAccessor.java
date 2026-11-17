package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public interface LevelHeightAccessor {
   int getHeight();

   int getMinBuildHeight();

   default int getMaxBuildHeight() {
      return this.getMinBuildHeight() + this.getHeight();
   }

   default int getSectionsCount() {
      return this.getMaxSection() - this.getMinSection();
   }

   default int getMinSection() {
      return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
   }

   default int getMaxSection() {
      return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
   }

   default boolean isOutsideBuildHeight(BlockPos $$0) {
      return this.isOutsideBuildHeight($$0.getY());
   }

   default boolean isOutsideBuildHeight(int $$0) {
      return $$0 < this.getMinBuildHeight() || $$0 >= this.getMaxBuildHeight();
   }

   default int getSectionIndex(int $$0) {
      return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord($$0));
   }

   default int getSectionIndexFromSectionY(int $$0) {
      return $$0 - this.getMinSection();
   }

   default int getSectionYFromSectionIndex(int $$0) {
      return $$0 + this.getMinSection();
   }

   static LevelHeightAccessor create(int $$0, int $$1) {
      return new LevelHeightAccessor() {
         @Override
         public int getHeight() {
            return $$1;
         }

         @Override
         public int getMinBuildHeight() {
            return $$0;
         }
      };
   }
}
