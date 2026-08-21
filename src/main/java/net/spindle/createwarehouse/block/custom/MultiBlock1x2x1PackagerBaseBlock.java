package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

public class MultiBlock1x2x1PackagerBaseBlock extends Block implements IWrenchable {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public MultiBlock1x2x1PackagerBaseBlock(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(
                this.defaultBlockState()
                        //.setValue(HALF, DoubleBlockHalf.LOWER)
                        .setValue(POWERED, Boolean.FALSE)
                        .setValue(LINKED, Boolean.FALSE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(HALF, POWERED, LINKED));
    }

    //@Nullable
    //@Override
    //public BlockState getStateForPlacement(BlockPlaceContext context) {
        //BlockPos blockPos = context.getClickedPos();
        //Level level = context.getLevel();
        //if (blockPos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockPos.above()).canBeReplaced(context)) {
        //    return this.defaultBlockState();
        //} else if (blockPos.getY() > level.getMinBuildHeight() && level.getBlockState(blockPos.below()).canBeReplaced(context)) {
        //    return this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER);
        //} else {
        //    return null;
        //}
    //}

    //@Override
    //public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    //    if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
    //        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    //    } else if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
    //        level.setBlock(pos.below(), state.setValue(HALF, DoubleBlockHalf.LOWER), 3);
    //    }
    //}

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
    //    removeOtherHalf(context.getLevel(), context.getClickedPos(), state);
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
    //    if (explosion.interactsWithBlocks()) {
    //        removeOtherHalf(level, pos, state);
    //    }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
    //    removeOtherHalf(level, pos, state);
        super.onDestroyedByPushReaction(state, level, pos, pushDirection, fluid);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        //removeOtherHalf(level, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }


    // Todo: test if updateShape is needed or if tick is better
    //@Override
    //protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
    //    if (!validate(level, state, currentPos)) {
    //        level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 35);
    //    }
    //    return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    //}

    //protected static boolean validate(LevelAccessor level, BlockState state, BlockPos currentPos) {
    //    DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
    //    if (doubleBlockHalf == DoubleBlockHalf.LOWER) {
    //        BlockPos otherHalfPos = currentPos.above();
    //        BlockState otherHalfState = level.getBlockState(otherHalfPos);
    //        if (otherHalfState.is(state.getBlock()) && otherHalfState.getValue(HALF) == DoubleBlockHalf.UPPER) {
    //            return Boolean.TRUE;
    //        } else return Boolean.FALSE;
    //    }
    //    if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
    //        BlockPos otherHalfPos = currentPos.below();
    //        BlockState otherHalfState = level.getBlockState(otherHalfPos);
    //        if (otherHalfState.is(state.getBlock()) && otherHalfState.getValue(HALF) == DoubleBlockHalf.LOWER) {
    //           return Boolean.TRUE;
    //        } else return Boolean.FALSE;
    //    }
    //    return Boolean.FALSE;
    //}

    //protected static void removeOtherHalf(LevelAccessor level, BlockPos pos, BlockState state) {
    //    if (validate(level, state, pos)) {
    //        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
    //            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 35);
    //        } else if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
    //            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 35);
    //        }
    //    }
    //}

}
