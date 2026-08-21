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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class MultiBlock2X1X2BaseBlock extends Block implements IWrenchable {

    public MultiBlock2X1X2BaseBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(DIRECTION, Direction.EAST)
        );
    }

    public static final EnumProperty<Direction> DIRECTION = BlockStateProperties.FACING;

    // TODO: Important Info - Facing value for identification,
    //  always the bottom right one of each side is looking to the right and the one looking east is the main one

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(DIRECTION));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockPos = context.getClickedPos();
        Level level = context.getLevel();
        byte counter;

        switch (context.getHorizontalDirection()) {
            case NORTH -> counter = 0;
            case EAST -> counter = 3;
            case SOUTH -> counter = 2;
            case WEST -> counter = 1;
            default -> {
                return null;
            }
        }

        for (byte i = 0; i <= 4; i++) {
            if (counter == 0 && checkSouth(blockPos, level).getValue(DIRECTION) != Direction.UP) {
                return checkSouth(blockPos, level);
            } else if (counter == 1 && checkEast(blockPos, level).getValue(DIRECTION) != Direction.UP) {
                return checkEast(blockPos, level);
            } else if (counter == 2 && checkNorth(blockPos, level).getValue(DIRECTION) != Direction.UP) {
                return checkNorth(blockPos, level);
            } else if (counter == 3 && checkWest(blockPos, level).getValue(DIRECTION) != Direction.UP) {
                return checkWest(blockPos, level);
            }
            counter++;
            if (counter > 3) counter = 0;
        }
        return null;
    }

    protected BlockState checkEast(BlockPos mainBlockPos, Level level){
        if (validatePlacement(mainBlockPos, level)) {
            return this.defaultBlockState().setValue(DIRECTION, Direction.EAST);
        }
        return this.defaultBlockState().setValue(DIRECTION, Direction.UP);
    }

    protected BlockState checkNorth(BlockPos mainBlockPos, Level level){
        if (validatePlacement(mainBlockPos.south(), level)) {
            return this.defaultBlockState().setValue(DIRECTION, Direction.NORTH);
        }
        return this.defaultBlockState().setValue(DIRECTION, Direction.UP);
    }

    protected BlockState checkWest(BlockPos mainBlockPos, Level level){
        if (validatePlacement(mainBlockPos.east().south(), level)) {
            return this.defaultBlockState().setValue(DIRECTION, Direction.WEST);
        }
        return this.defaultBlockState().setValue(DIRECTION, Direction.UP);
    }

    protected BlockState checkSouth(BlockPos mainBlockPos, Level level){
        if (validatePlacement(mainBlockPos.east(), level)) {
            return this.defaultBlockState().setValue(DIRECTION, Direction.SOUTH);
        }
        return this.defaultBlockState().setValue(DIRECTION, Direction.UP);
    }

    protected static boolean validatePlacement(BlockPos mainBlockPos, Level level) {
        if (
                level.getBlockState(mainBlockPos).canBeReplaced()
                && level.getBlockState(mainBlockPos.north()).canBeReplaced()
                && level.getBlockState(mainBlockPos.west()).canBeReplaced()
                && level.getBlockState(mainBlockPos.west().north()).canBeReplaced()
        ) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (state.getValue(DIRECTION) == Direction.EAST) {
            level.setBlock(pos.west(), state.setValue(DIRECTION, Direction.SOUTH), 3);
            level.setBlock(pos.west().north(), state.setValue(DIRECTION, Direction.WEST), 3);
            level.setBlock(pos.north(), state.setValue(DIRECTION, Direction.NORTH), 3);
        } else if (state.getValue(DIRECTION) == Direction.SOUTH) {
            level.setBlock(pos.north(), state.setValue(DIRECTION, Direction.WEST), 3);
            level.setBlock(pos.north().east(), state.setValue(DIRECTION, Direction.NORTH), 3);
            level.setBlock(pos.east(), state.setValue(DIRECTION, Direction.EAST), 3);
        } else if (state.getValue(DIRECTION) == Direction.WEST) {
            level.setBlock(pos.east(), state.setValue(DIRECTION, Direction.NORTH), 3);
            level.setBlock(pos.east().south(), state.setValue(DIRECTION, Direction.EAST), 3);
            level.setBlock(pos.south(), state.setValue(DIRECTION, Direction.SOUTH), 3);
        } else if (state.getValue(DIRECTION) == Direction.NORTH) {
            level.setBlock(pos.south(), state.setValue(DIRECTION, Direction.EAST), 3);
            level.setBlock(pos.south().west(), state.setValue(DIRECTION, Direction.SOUTH), 3);
            level.setBlock(pos.west(), state.setValue(DIRECTION, Direction.WEST), 3);
        }
    }

    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        removeAllOther(context.getLevel(), context.getClickedPos(), state);
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (explosion.interactsWithBlocks()) {
            removeAllOther(level, pos, state);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
        removeAllOther(level, pos, state);
        super.onDestroyedByPushReaction(state, level, pos, pushDirection, fluid);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeAllOther(level, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (
                !((state.getValue(DIRECTION) == Direction.EAST && validate(level, state, currentPos))
                || (state.getValue(DIRECTION) == Direction.SOUTH && validate(level, state, currentPos.east()))
                || (state.getValue(DIRECTION) == Direction.WEST && validate(level, state, currentPos.east().south()))
                || (state.getValue(DIRECTION) == Direction.NORTH && validate(level, state, currentPos.south())))
        ) {
            level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 35);
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    protected static boolean validate(LevelAccessor level, BlockState state, BlockPos mainBlockPos) {
        if (
                level.getBlockState(mainBlockPos).is(state.getBlock())
                && level.getBlockState(mainBlockPos.north()).is(state.getBlock())
                && level.getBlockState(mainBlockPos.west()).is(state.getBlock())
                && level.getBlockState(mainBlockPos.west().north()).is(state.getBlock())
                && level.getBlockState(mainBlockPos).getValue(DIRECTION) == Direction.EAST
                && level.getBlockState(mainBlockPos.north()).getValue(DIRECTION) == Direction.NORTH
                && level.getBlockState(mainBlockPos.west()).getValue(DIRECTION) == Direction.SOUTH
                && level.getBlockState(mainBlockPos.west().north()).getValue(DIRECTION) == Direction.WEST
        ) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    protected static void removeAllOther(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockState air = Blocks.AIR.defaultBlockState();
        if (state.getValue(DIRECTION) == Direction.EAST && validate(level, state, pos)) {
            level.setBlock(pos.west(), air, 35);
            level.setBlock(pos.west().north(), air, 35);
            level.setBlock(pos.north(), air, 35);
        } else if (state.getValue(DIRECTION) == Direction.SOUTH && validate(level, state, pos.east())) {
            level.setBlock(pos.north(), air, 35);
            level.setBlock(pos.north().east(), air, 35);
            level.setBlock(pos.east(), air, 35);
        } else if (state.getValue(DIRECTION) == Direction.WEST && validate(level, state, pos.east().south())) {
            level.setBlock(pos.east(), air, 35);
            level.setBlock(pos.east().south(), air, 35);
            level.setBlock(pos.south(), air, 35);
        } else if (state.getValue(DIRECTION) == Direction.NORTH && validate(level, state, pos.south())) {
            level.setBlock(pos.south(), air, 35);
            level.setBlock(pos.south().west(), air, 35);
            level.setBlock(pos.west(), air, 35);
        }
    }
}
