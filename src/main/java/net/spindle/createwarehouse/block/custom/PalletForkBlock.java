package net.spindle.createwarehouse.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PalletForkBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PalletForkBlock> CODEC = simpleCodec(PalletForkBlock::new);

    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(-16, 0, 12.9, 16, 12, 15.9),
            Block.box(-10, 1, -8.2, -6, 3, 15.8),
            Block.box(6, 1, -8.2, 10, 3, 15.8));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(0.1, 0, -16, 3.1, 12, 16),
            Block.box(0.2, 1, -10, 24.2, 3, -6),
            Block.box(0.2, 1, 6, 24.2, 3, 10));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(0, 0, 0.1, 32, 12, 3.1),
            Block.box(6, 1, 0.2, 10, 3, 24.2),
            Block.box(22, 1, 0.2, 26, 3, 24.2));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(12.9, 0, 0, 15.9, 12, 32),
            Block.box(-8.2, 1, 6, 15.8, 3, 10),
            Block.box(-8.2, 1, 22, 15.8, 3, 26));
    public PalletForkBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return getForkShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return getForkShape(state);
    }

    private static VoxelShape getForkShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> Shapes.empty();
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
