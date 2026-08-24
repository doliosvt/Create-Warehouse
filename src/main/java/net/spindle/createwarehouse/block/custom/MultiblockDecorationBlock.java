package net.spindle.createwarehouse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    public MultiblockDecorationBlock(Properties properties, VoxelShape shape, PartPlacement... parts) {
        super(properties, shape);
        this.parts = List.of(parts);
        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;
        for (PartPlacement part : parts) {
            BlockPos offset = part.offset();
            minX = Math.min(minX, offset.getX());
            maxX = Math.max(maxX, offset.getX());
            minZ = Math.min(minZ, offset.getZ());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    @Nullable
    public BlockPlaceContext findPlacementContext(BlockPlaceContext context) {
        Direction preferredDirection = context.getHorizontalDirection();
        Direction[] directions = {
                preferredDirection,
                preferredDirection.getCounterClockWise(),
                preferredDirection.getOpposite(),
                preferredDirection.getClockWise()
        };

        BlockPos clickedPos = context.getClickedPos();
        for (Direction direction : directions) {
            BlockPos masterPos = getMasterPosForCorner(clickedPos, direction);
            BlockPlaceContext candidateContext = BlockPlaceContext.at(
                    context, masterPos, context.getClickedFace());
            if (hasRoom(candidateContext, masterPos))
                return candidateContext;
        }
        return null;
    }

    private BlockPos getMasterPosForCorner(BlockPos clickedPos, Direction direction) {
        int anchorX;
        int anchorZ;
        switch (direction) {
            case NORTH -> {
                anchorX = minX;
                anchorZ = maxZ;
            }
            case EAST -> {
                anchorX = minX;
                anchorZ = minZ;
            }
            case SOUTH -> {
                anchorX = maxX;
                anchorZ = minZ;
            }
            case WEST -> {
                anchorX = maxX;
                anchorZ = maxZ;
            }
            default -> throw new IllegalArgumentException("Expected a horizontal direction");
        }
        return clickedPos.offset(-anchorX, 0, -anchorZ);
    }

    private boolean hasRoom(BlockPlaceContext context, BlockPos masterPos) {
        Level level = context.getLevel();
        if (!level.isInWorldBounds(masterPos)
                || !level.getBlockState(masterPos).canBeReplaced(context))
            return false;

        for (PartPlacement part : parts) {
            BlockPos partPos = masterPos.offset(part.offset());
            if (!level.isInWorldBounds(partPos)
                    || !level.getBlockState(partPos).canBeReplaced(context))
                return false;
        }
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;

        BlockPos masterPos = context.getClickedPos();
        return hasRoom(context, masterPos) ? state : null;
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
