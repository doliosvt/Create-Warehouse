package net.spindle.createwarehouse.block.custom;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A horizontal station contact that calls the controller attached to a nearby gantry-shaft line. */
public class GantryContactBlock extends WrenchableDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty CALLING = BooleanProperty.create("calling");
    public static final BooleanProperty POWERING = BrassDiodeBlock.POWERING;

    public GantryContactBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(POWERED, false)
                .setValue(CALLING, false)
                .setValue(POWERING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED, CALLING, POWERING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean moving) {
        if (level.isClientSide())
            return;

        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) == powered)
            return;

        state = state.setValue(POWERED, powered);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        if (!powered || state.getValue(CALLING))
            return;

        if (GantryControllerBlockEntity.callFromContact(level, pos))
            setCalling(level, pos, true);
    }

    public static void setCalling(LevelAccessor level, BlockPos pos, boolean calling) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GantryContactBlock))
            return;
        if (state.getValue(CALLING) == calling)
            return;
        level.setBlock(pos, state.setValue(CALLING, calling), Block.UPDATE_CLIENTS);
    }

    public static void pulse(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GantryContactBlock))
            return;

        level.setBlock(pos, state.setValue(POWERING, true), Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(pos, state.getBlock());
        level.scheduleTick(pos, state.getBlock(), 4);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERING))
            return;
        level.setBlock(pos, state.setValue(POWERING, false), Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
        return false;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWERING);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return side == null || state.getValue(FACING) != side.getOpposite();
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (side == null || side == state.getValue(FACING).getOpposite())
            return 0;
        return state.getValue(POWERING) ? 15 : 0;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    public static int getLight(BlockState state) {
        return state.getValue(POWERING) ? 10 : 0;
    }
}
