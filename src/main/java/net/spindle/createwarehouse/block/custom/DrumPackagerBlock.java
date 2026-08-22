package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.spindle.createwarehouse.block.ModBlockEntities;

public class DrumPackagerBlock extends MultiblockDecorationBlock implements IBE<DrumPackagerBlockEntity> {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public DrumPackagerBlock(Properties properties, VoxelShape shape, PartPlacement... parts) {
        super(properties, shape, parts);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public Class<DrumPackagerBlockEntity> getBlockEntityClass() {
        return DrumPackagerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DrumPackagerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.DRUM_PACKAGER.get();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide)
            return;

        updatePower(level, pos);
    }

    public static void updatePower(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DrumPackagerBlock packager))
            return;
        boolean wasPowered = state.getValue(POWERED);
        boolean isPowered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        if (wasPowered == isPowered)
            return;

        level.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_CLIENTS);
        packager.withBlockEntityDo(level, pos, DrumPackagerBlockEntity::resetPackagingDelay);
    }

    public static boolean tryTakeOutput(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof DrumPackagerBlockEntity packager)
                || packager.output.getStackInSlot(0).isEmpty())
            return false;
        if (!level.isClientSide)
            packager.giveOutputTo(player);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return tryTakeOutput(level, pos, player)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        return false;
    }
}
