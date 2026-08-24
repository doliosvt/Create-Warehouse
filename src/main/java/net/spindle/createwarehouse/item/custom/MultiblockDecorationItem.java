package net.spindle.createwarehouse.item.custom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.spindle.createwarehouse.block.custom.MultiblockDecorationBlock;
import org.jetbrains.annotations.Nullable;

public class MultiblockDecorationItem extends BlockItem {
    private final MultiblockDecorationBlock multiblock;

    public MultiblockDecorationItem(Block block, Properties properties) {
        super(block, properties);
        this.multiblock = (MultiblockDecorationBlock) block;
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        return multiblock.findPlacementContext(context);
    }
}
