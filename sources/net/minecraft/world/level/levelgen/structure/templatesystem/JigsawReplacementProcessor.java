package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class JigsawReplacementProcessor extends StructureProcessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<JigsawReplacementProcessor> CODEC = Codec.unit(() -> JigsawReplacementProcessor.INSTANCE);
   public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

   private JigsawReplacementProcessor() {
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader $$0,
      BlockPos $$1,
      BlockPos $$2,
      StructureTemplate.StructureBlockInfo $$3,
      StructureTemplate.StructureBlockInfo $$4,
      StructurePlaceSettings $$5
   ) {
      BlockState $$6 = $$4.state();
      if ($$6.is(Blocks.JIGSAW)) {
         if ($$4.nbt() == null) {
            LOGGER.warn("Jigsaw block at {} is missing nbt, will not replace", $$1);
            return $$4;
         } else {
            String $$7 = $$4.nbt().getString("final_state");

            BlockState $$9;
            try {
               BlockResult $$8 = BlockStateParser.parseForBlock($$0.holderLookup(Registries.BLOCK), $$7, true);
               $$9 = $$8.blockState();
            } catch (CommandSyntaxException var11) {
               throw new RuntimeException(var11);
            }

            return $$9.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo($$4.pos(), $$9, null);
         }
      } else {
         return $$4;
      }
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.JIGSAW_REPLACEMENT;
   }
}
