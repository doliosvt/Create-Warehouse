package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.common.Mod;
import net.spindle.createwarehouse.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

public class CratePackagerStructuralBlock extends SpongeBlock implements IWrenchable {

    public CratePackagerStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 5, 16);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.below(), ModBlocks.CRATE_PACKAGER.getDefaultState().setValue(FACING, Direction.DOWN), 3);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        removeMainBlock(context.getLevel(), context.getClickedPos());
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (explosion.interactsWithBlocks()) {
            removeMainBlock(level, pos);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
        removeMainBlock(level, pos);
        super.onDestroyedByPushReaction(state, level, pos, pushDirection, fluid);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeMainBlock(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected static boolean validate(LevelAccessor level, BlockPos currentPos) {
        if (level.getBlockState(currentPos.below()).is(ModBlocks.CRATE_PACKAGER)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    protected static void removeMainBlock(LevelAccessor level, BlockPos pos) {
        if (validate(level, pos)) {
            level.removeBlock(pos.below(), false);
        }
    }
}
