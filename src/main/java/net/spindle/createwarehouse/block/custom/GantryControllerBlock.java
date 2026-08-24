package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.spindle.createwarehouse.block.ModBlockEntities;

/**
 * A split shaft whose front half is controlled by a horizontal gantry target.
 * The rear half accepts ordinary Create rotation and the facing half drives the
 * gantry shaft.
 */
public class GantryControllerBlock extends DirectionalKineticBlock
        implements IBE<GantryControllerBlockEntity> {
    public static final BooleanProperty POWERING = BlockStateProperties.POWERED;

    public GantryControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if (preferred != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()))
            return defaultBlockState().setValue(FACING, preferred);

        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWERING);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWERING)
                && side == state.getValue(FACING).getOpposite() ? 15 : 0;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GantryControllerBlockEntity controller)
            RotationPropagator.handleAdded(level, pos, controller);
    }

    @Override
    public Class<GantryControllerBlockEntity> getBlockEntityClass() {
        return GantryControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GantryControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.GANTRY_CONTROLLER.get();
    }
}
