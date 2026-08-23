package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.spindle.createwarehouse.block.ModBlocks;

import java.util.function.BiConsumer;

/**
 * Invisible, local collision cell belonging to a {@link MultiblockDecorationBlock}.
 * Minecraft deliberately skips collision shapes which only reach a diagonal block,
 * so every occupied cell of a decoration needs to be represented by a real block.
 */
public class MultiblockPartBlock extends Block implements IWrenchable {
    // IntegerProperty does not allow negative values. 0, 1 and 2 encode -1, 0 and 1.
    public static final IntegerProperty MASTER_X = IntegerProperty.create("master_x", 0, 2);
    public static final IntegerProperty MASTER_Y = IntegerProperty.create("master_y", 0, 3);
    public static final IntegerProperty MASTER_Z = IntegerProperty.create("master_z", 0, 4);
    public static final EnumProperty<PartShape> PART_SHAPE = EnumProperty.create("part_shape", PartShape.class);

    public MultiblockPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MASTER_X, 1)
                .setValue(MASTER_Y, 3)
                .setValue(MASTER_Z, 1)
                .setValue(PART_SHAPE, PartShape.FULL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MASTER_X, MASTER_Y, MASTER_Z, PART_SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART_SHAPE).shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        PartShape partShape = state.getValue(PART_SHAPE);
        if (partShape.isPallet()) {
            BlockPos masterPos = getMasterPos(state, pos);
            if (level.getBlockState(masterPos).is(ModBlocks.PALLET))
                return PalletBlock.addCrateCollision(level, masterPos, pos, partShape.shape);
        }
        return partShape.getCollisionShape();
    }

    public static BlockPos getMasterPos(BlockState state, BlockPos partPos) {
        return partPos.offset(
                state.getValue(MASTER_X) - 1,
                state.getValue(MASTER_Y) - 3,
                state.getValue(MASTER_Z) - 1);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos masterPos = getMasterPos(state, pos);
        BlockState masterState = level.getBlockState(masterPos);
        if (!masterPos.equals(pos) && masterState.getBlock() instanceof MultiblockDecorationBlock)
            return masterState.getBlock().getCloneItemStack(level, masterPos, masterState);
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (!context.getLevel().isClientSide())
            destroyMaster(context.getLevel(), context.getClickedPos(), state,
                    player == null || !player.isCreative(), player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand,
                                              net.minecraft.world.phys.BlockHitResult hitResult) {
        BlockPos masterPos = getMasterPos(state, pos);
        BlockState masterState = level.getBlockState(masterPos);
        if (masterState.is(ModBlocks.PALLET))
            return PalletBlock.tryUseItem(stack, masterState, level, masterPos, player);
        if (masterState.is(ModBlocks.DRUM_PACKAGER)
                && DrumPackagerBlock.tryTakeOutput(level, masterPos, player))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        BlockPos masterPos = getMasterPos(state, pos);
        if (level.getBlockState(masterPos).is(ModBlocks.DRUM_PACKAGER)
                && DrumPackagerBlock.tryTakeOutput(level, masterPos, player))
            return InteractionResult.sidedSuccess(level.isClientSide);
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide())
            destroyMaster(level, pos, state, !player.isCreative(), player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (explosion.interactsWithBlocks())
            destroyMaster(level, pos, state, true, null);
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos,
                                          Direction pushDirection, FluidState fluid) {
        destroyMaster(level, pos, state, true, null);
        super.onDestroyedByPushReaction(state, level, pos, pushDirection, fluid);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!(level.getBlockState(getMasterPos(state, pos)).getBlock() instanceof MultiblockDecorationBlock))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockPos masterPos = getMasterPos(state, pos);
            if (level.getBlockState(masterPos).is(ModBlocks.DRUM_PACKAGER))
                DrumPackagerBlock.updatePower(level, masterPos);
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    private static void destroyMaster(Level level, BlockPos partPos, BlockState state,
                                      boolean drop, Player player) {
        BlockPos masterPos = getMasterPos(state, partPos);
        if (level.getBlockState(masterPos).getBlock() instanceof MultiblockDecorationBlock)
            level.destroyBlock(masterPos, drop, player);
    }

    public enum PartShape implements StringRepresentable {
        FULL("full", Block.box(0, 0, 0, 16, 16, 16)),
        LOW("low", Block.box(0, 0, 0, 16, 13, 16)),
        PALLET("pallet", Block.box(0, 0, 0, 16, 4, 16)),
        PALLET_FRAME_BOTTOM("pallet_frame_bottom", Block.box(0, 0, 0, 16, 4, 16)),
        PALLET_FRAME_TOP("pallet_frame_top", Block.box(0, 13, 0, 16, 16, 16)),
        PALLET_CRATES_TOP("pallet_crates_top", Shapes.empty()),
        FORKLIFT_BASE("forklift_base", Block.box(0, 0, 0, 16, 6, 16)),
        FORKLIFT_BASE_ARM_LEFT("forklift_base_arm_left", Shapes.or(
                Block.box(0, 0, 0, 16, 6, 16),
                Block.box(12, 6, 0, 16, 16, 16))),
        FORKLIFT_ARM_LEFT("forklift_arm_left", Block.box(12, 0, 0, 16, 16, 16)),
        FORKLIFT_ARM_RIGHT("forklift_arm_right", Block.box(0, 0, 0, 4, 16, 16)),
        FORKLIFT_TOP_LEFT("forklift_top_left", Block.box(8, 0, 0, 16, 13, 16)),
        FORKLIFT_TOP_RIGHT("forklift_top_right", Block.box(0, 0, 0, 8, 13, 16)),
        FORKLIFT_TIP_LEFT("forklift_tip_left", Block.box(8, 7, 13, 16, 10, 16)),
        FORKLIFT_TIP_RIGHT("forklift_tip_right", Block.box(0, 7, 13, 8, 10, 16));

        private final String serializedName;
        private final VoxelShape shape;

        PartShape(String serializedName, VoxelShape shape) {
            this.serializedName = serializedName;
            this.shape = shape;
        }

        public VoxelShape getShape() {
            return shape;
        }

        public VoxelShape getCollisionShape() {
            return switch (this) {
                case FORKLIFT_BASE, FORKLIFT_BASE_ARM_LEFT -> Block.box(0, 0, 0, 16, 6, 16);
                case FORKLIFT_ARM_LEFT, FORKLIFT_ARM_RIGHT,
                     FORKLIFT_TOP_LEFT, FORKLIFT_TOP_RIGHT,
                     FORKLIFT_TIP_LEFT, FORKLIFT_TIP_RIGHT -> Shapes.empty();
                default -> shape;
            };
        }

        public boolean isPallet() {
            return this == PALLET || this == PALLET_FRAME_BOTTOM
                    || this == PALLET_FRAME_TOP || this == PALLET_CRATES_TOP;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
