package net.spindle.createwarehouse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.spindle.createwarehouse.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiblockDecorationBlock extends StaticShapeBlock {
    private final List<PartPlacement> parts;

    public MultiblockDecorationBlock(Properties properties, VoxelShape shape, PartPlacement... parts) {
        super(properties, shape);
        this.parts = List.of(parts);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;

        Level level = context.getLevel();
        BlockPos masterPos = context.getClickedPos();
        for (PartPlacement part : parts) {
            BlockPos partPos = masterPos.offset(part.offset());
            if (!level.isInWorldBounds(partPos) || !level.getBlockState(partPos).canBeReplaced(context))
                return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide())
            return;

        for (PartPlacement part : parts) {
            BlockPos offset = part.offset();
            BlockState partState = ModBlocks.MULTIBLOCK_PART.getDefaultState()
                    .setValue(MultiblockPartBlock.MASTER_X, 1 - offset.getX())
                    .setValue(MultiblockPartBlock.MASTER_Y, 3 - offset.getY())
                    .setValue(MultiblockPartBlock.MASTER_Z, 1 - offset.getZ())
                    .setValue(MultiblockPartBlock.PART_SHAPE, part.shape());
            level.setBlock(pos.offset(offset), partState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()))
            removeParts(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void removeParts(LevelAccessor level, BlockPos masterPos) {
        for (PartPlacement part : parts) {
            BlockPos partPos = masterPos.offset(part.offset());
            BlockState state = level.getBlockState(partPos);
            if (state.is(ModBlocks.MULTIBLOCK_PART)
                    && MultiblockPartBlock.getMasterPos(state, partPos).equals(masterPos))
                level.destroyBlock(partPos, false);
        }
    }

    public record PartPlacement(BlockPos offset, MultiblockPartBlock.PartShape shape) {
        public static PartPlacement at(int x, int y, int z, MultiblockPartBlock.PartShape shape) {
            return new PartPlacement(new BlockPos(x, y, z), shape);
        }
    }
}
