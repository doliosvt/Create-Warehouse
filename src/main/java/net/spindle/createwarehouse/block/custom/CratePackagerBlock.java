package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.block.ModBlockEntities;

import java.util.function.BiConsumer;

public class CratePackagerBlock extends PackagerBlock {
    public CratePackagerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CRATE_PACKAGER.get();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockPos = context.getClickedPos();
        Level level = context.getLevel();
        if (blockPos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(blockPos.above()).canBeReplaced(context))
            return null;
        return defaultBlockState().setValue(FACING, Direction.UP);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), ModBlocks.CRATE_PACKAGER_STRUCTURAL.getDefaultState(), 3);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        removeStructuralBlock(context.getLevel(), context.getClickedPos());
        return super.onSneakWrenched(state, context);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (explosion.interactsWithBlocks()) {
            removeStructuralBlock(level, pos);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
        removeStructuralBlock(level, pos);
        super.onDestroyedByPushReaction(state, level, pos, pushDirection, fluid);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeStructuralBlock(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()))
            removeStructuralBlock(level, pos);
        super.onRemove(state, level, pos, newState, moving);
    }

    protected static boolean validate(LevelAccessor level, BlockPos currentPos) {
        if (level.getBlockState(currentPos.above()).is(ModBlocks.CRATE_PACKAGER_STRUCTURAL)) {
            return true;
        }
        return false;
    }

    protected static void removeStructuralBlock(LevelAccessor level, BlockPos pos) {
        if (validate(level, pos)) {
            level.removeBlock(pos.above(), false);
        }
    }
}
