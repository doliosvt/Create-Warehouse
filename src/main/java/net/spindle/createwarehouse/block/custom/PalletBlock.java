package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.spindle.createwarehouse.block.ModBlockEntities;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.item.custom.CrateItem;
import net.spindle.createwarehouse.item.custom.FluidDrumItem;
import org.jetbrains.annotations.Nullable;

public class PalletBlock extends MultiblockDecorationBlock implements IBE<PalletBlockEntity> {
    public static final BooleanProperty SUPPORTS = BooleanProperty.create("supports");
    public static final int MAX_STACK_HEIGHT = 5;

    private static final BlockPos[] LOWER_PARTS = {
            new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(-1, 0, 1)
    };
    private static final BlockPos[] UPPER_PARTS = {
            new BlockPos(0, 1, 0), new BlockPos(-1, 1, 0),
            new BlockPos(0, 1, 1), new BlockPos(-1, 1, 1)
    };
    private static final BlockPos[] UPPER_PALLET_CELLS = {
            new BlockPos(0, 2, 0), new BlockPos(-1, 2, 0),
            new BlockPos(0, 2, 1), new BlockPos(-1, 2, 1)
    };
    private static final BlockPos[] CRATE_CELLS = {
            new BlockPos(-1, 0, 1), new BlockPos(-1, 0, 0),
            BlockPos.ZERO, new BlockPos(0, 0, 1)
    };
    private static final VoxelShape[] LOWER_CRATE_SHAPES = {
            Block.box(4, 4, 0, 16, 16, 12),
            Block.box(4, 4, 4, 16, 16, 16),
            Block.box(0, 4, 4, 12, 16, 16),
            Block.box(0, 4, 0, 12, 16, 12)
    };
    private static final VoxelShape[] LOWER_DRUM_OVERFLOW_SHAPES = {
            Block.box(4, 0, 0, 16, 2, 12),
            Block.box(4, 0, 4, 16, 2, 16),
            Block.box(0, 0, 4, 12, 2, 16),
            Block.box(0, 0, 0, 12, 2, 12)
    };
    private static final VoxelShape[] UPPER_CRATE_SHAPES = {
            Block.box(4, 0, 0, 16, 12, 12),
            Block.box(4, 0, 4, 16, 12, 16),
            Block.box(0, 0, 4, 12, 12, 16),
            Block.box(0, 0, 0, 12, 12, 12)
    };
    private static final VoxelShape[] UPPER_DRUM_SHAPES = {
            Block.box(4, 0, 0, 16, 14, 12),
            Block.box(4, 0, 4, 16, 14, 16),
            Block.box(0, 0, 4, 12, 14, 16),
            Block.box(0, 0, 0, 12, 14, 12)
    };
    private static final VoxelShape[] UPPER_CRATE_ABOVE_DRUM_SHAPES = {
            Block.box(4, 2, 0, 16, 14, 12),
            Block.box(4, 2, 4, 16, 14, 16),
            Block.box(0, 2, 4, 12, 14, 16),
            Block.box(0, 2, 0, 12, 14, 12)
    };
    private static final VoxelShape[] UPPER_DRUM_ABOVE_DRUM_SHAPES = {
            Block.box(4, 2, 0, 16, 16, 12),
            Block.box(4, 2, 4, 16, 16, 16),
            Block.box(0, 2, 4, 12, 16, 16),
            Block.box(0, 2, 0, 12, 16, 12)
    };

    public PalletBlock(Properties properties, VoxelShape shape, PartPlacement... parts) {
        super(properties, shape, parts);
        registerDefaultState(defaultBlockState().setValue(SUPPORTS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SUPPORTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return state.getValue(SUPPORTS)
                ? MultiblockPartBlock.PartShape.PALLET_FRAME_BOTTOM.getShape()
                : super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                           BlockPos pos, CollisionContext context) {
        return addCrateCollision(level, pos, pos, getShape(state, level, pos, context));
    }

    public static VoxelShape addCrateCollision(BlockGetter level, BlockPos masterPos,
                                                BlockPos partPos, VoxelShape baseShape) {
        if (!(level.getBlockEntity(masterPos) instanceof PalletBlockEntity pallet))
            return baseShape;

        BlockPos offset = partPos.subtract(masterPos);
        int layer = offset.getY();
        if (layer < 0 || layer > 1)
            return baseShape;

        int cell = getCrateCell(offset.getX(), offset.getZ());
        if (cell < 0)
            return baseShape;

        if (layer == 0) {
            ItemStack lowerCargo = pallet.getCrate(cell);
            return lowerCargo.isEmpty() ? baseShape : Shapes.or(baseShape, LOWER_CRATE_SHAPES[cell]);
        }

        VoxelShape result = baseShape;
        ItemStack lowerCargo = pallet.getCrate(cell);
        if (FluidDrumItem.isDrum(lowerCargo))
            result = Shapes.or(result, LOWER_DRUM_OVERFLOW_SHAPES[cell]);

        ItemStack upperCargo = pallet.getCrate(cell + CRATE_CELLS.length);
        if (!upperCargo.isEmpty()) {
            boolean lowerIsDrum = FluidDrumItem.isDrum(lowerCargo);
            VoxelShape upperShape = FluidDrumItem.isDrum(upperCargo)
                    ? (lowerIsDrum ? UPPER_DRUM_ABOVE_DRUM_SHAPES[cell] : UPPER_DRUM_SHAPES[cell])
                    : (lowerIsDrum ? UPPER_CRATE_ABOVE_DRUM_SHAPES[cell] : UPPER_CRATE_SHAPES[cell]);
            result = Shapes.or(result, upperShape);
        }
        return result;
    }

    private static int getCrateCell(int x, int z) {
        for (int cell = 0; cell < CRATE_CELLS.length; cell++) {
            BlockPos offset = CRATE_CELLS[cell];
            if (offset.getX() == x && offset.getZ() == z)
                return cell;
        }
        return -1;
    }

    public static boolean prepareCrateSlot(Level level, BlockPos masterPos, int slot, ItemStack cargo) {
        if (slot < CRATE_CELLS.length && !FluidDrumItem.isDrum(cargo))
            return true;

        BlockPos offset = CRATE_CELLS[slot % CRATE_CELLS.length].above();
        BlockPos partPos = masterPos.offset(offset);
        BlockState partState = level.getBlockState(partPos);
        if (isOwnedPart(partState, partPos, masterPos))
            return true;
        if (!partState.canBeReplaced())
            return false;

        level.setBlock(partPos, createPartState(offset,
                MultiblockPartBlock.PartShape.PALLET_CRATES_TOP), Block.UPDATE_ALL);
        return true;
    }

    @Override
    public Class<PalletBlockEntity> getBlockEntityClass() {
        return PalletBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PalletBlockEntity> getBlockEntityType() {
        return ModBlockEntities.PALLET.get();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;

        restoreSupportsIfConnected(level, pos);
        restoreSupportsIfConnected(level, pos.below(2));
    }

    private static void restoreSupportsIfConnected(Level level, BlockPos lowerPos) {
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!lowerState.is(ModBlocks.PALLET) || lowerState.getValue(SUPPORTS)
                || !level.getBlockState(lowerPos.above(2)).is(ModBlocks.PALLET))
            return;

        for (BlockPos offset : UPPER_PARTS) {
            BlockPos partPos = lowerPos.offset(offset);
            BlockState partState = level.getBlockState(partPos);
            if (!level.isInWorldBounds(partPos)
                    || (!partState.canBeReplaced() && !isOwnedPart(partState, partPos, lowerPos)))
                return;
        }

        attachSupports(level, lowerPos, lowerState);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return tryUseItem(stack, state, level, pos, player);
    }

    public static ItemInteractionResult tryUseItem(ItemStack stack, BlockState state, Level level,
                                                   BlockPos pos, Player player) {
        if (stack.is(ModBlocks.PALLET.asItem()))
            return tryAddUpperPallet(stack, state, level, pos, player);
        return tryAddCrate(stack, level, pos, player);
    }

    public static ItemInteractionResult tryAddCrate(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!isPalletCargo(stack) || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        PalletBlockEntity pallet = level.getBlockEntity(pos) instanceof PalletBlockEntity blockEntity
                ? blockEntity
                : null;
        if (pallet == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide || pallet.isFull())
            return ItemInteractionResult.SUCCESS;

        if (pallet.addCrate(stack) && !player.getAbilities().instabuild)
            stack.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }

    public static boolean isPalletCargo(ItemStack stack) {
        return CrateItem.isCrate(stack) || FluidDrumItem.isDrum(stack);
    }

    private static ItemInteractionResult tryAddUpperPallet(ItemStack stack, BlockState state, Level level,
                                                           BlockPos pos, Player player) {
        if (state.getValue(SUPPORTS) || countConnectedPallets(level, pos) >= MAX_STACK_HEIGHT)
            return ItemInteractionResult.SUCCESS;

        for (BlockPos offset : UPPER_PARTS) {
            BlockPos partPos = pos.offset(offset);
            if (!level.isInWorldBounds(partPos)
                    || (!level.getBlockState(partPos).canBeReplaced()
                    && !isOwnedPart(level.getBlockState(partPos), partPos, pos)))
                return ItemInteractionResult.SUCCESS;
        }
        for (BlockPos offset : UPPER_PALLET_CELLS) {
            BlockPos partPos = pos.offset(offset);
            if (!level.isInWorldBounds(partPos) || !level.getBlockState(partPos).canBeReplaced())
                return ItemInteractionResult.SUCCESS;
        }
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        BlockPos upperPalletPos = pos.above(2);
        BlockState upperPalletState = ModBlocks.PALLET.getDefaultState();
        level.setBlock(upperPalletPos, upperPalletState, Block.UPDATE_ALL);
        ModBlocks.PALLET.get().setPlacedBy(level, upperPalletPos, upperPalletState, player, stack);
        attachSupports(level, pos, state);

        if (!player.getAbilities().instabuild)
            stack.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }

    private static int countConnectedPallets(Level level, BlockPos pos) {
        int count = 1;

        BlockPos cursor = pos.below(2);
        while (count < MAX_STACK_HEIGHT && level.getBlockState(cursor).is(ModBlocks.PALLET)) {
            count++;
            cursor = cursor.below(2);
        }

        cursor = pos.above(2);
        while (count < MAX_STACK_HEIGHT && level.getBlockState(cursor).is(ModBlocks.PALLET)) {
            count++;
            cursor = cursor.above(2);
        }
        return count;
    }

    private static void attachSupports(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(SUPPORTS, true), Block.UPDATE_ALL);
        for (BlockPos offset : LOWER_PARTS) {
            BlockPos partPos = pos.offset(offset);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(ModBlocks.MULTIBLOCK_PART))
                level.setBlock(partPos,
                        partState.setValue(MultiblockPartBlock.PART_SHAPE,
                                MultiblockPartBlock.PartShape.PALLET_FRAME_BOTTOM),
                        Block.UPDATE_ALL);
        }
        for (BlockPos offset : UPPER_PARTS)
            level.setBlock(pos.offset(offset), createPartState(offset,
                    MultiblockPartBlock.PartShape.PALLET_FRAME_TOP), Block.UPDATE_ALL);
    }

    private static BlockState createPartState(BlockPos offset, MultiblockPartBlock.PartShape shape) {
        return ModBlocks.MULTIBLOCK_PART.getDefaultState()
                .setValue(MultiblockPartBlock.MASTER_X, 1 - offset.getX())
                .setValue(MultiblockPartBlock.MASTER_Y, 3 - offset.getY())
                .setValue(MultiblockPartBlock.MASTER_Z, 1 - offset.getZ())
                .setValue(MultiblockPartBlock.PART_SHAPE, shape);
    }

    private static boolean isOwnedPart(BlockState state, BlockPos partPos, BlockPos masterPos) {
        return state.is(ModBlocks.MULTIBLOCK_PART)
                && MultiblockPartBlock.getMasterPos(state, partPos).equals(masterPos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()))
            detachSupports(level, pos.below(2));
        IBE.onRemove(state, level, pos, newState);
        if (!state.is(newState.getBlock()))
            removeUpperParts(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void detachSupports(Level level, BlockPos lowerPos) {
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!lowerState.is(ModBlocks.PALLET) || !lowerState.getValue(SUPPORTS))
            return;

        level.setBlock(lowerPos, lowerState.setValue(SUPPORTS, false), Block.UPDATE_ALL);
        for (BlockPos offset : LOWER_PARTS) {
            BlockPos partPos = lowerPos.offset(offset);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(ModBlocks.MULTIBLOCK_PART)
                    && MultiblockPartBlock.getMasterPos(partState, partPos).equals(lowerPos))
                level.setBlock(partPos,
                        partState.setValue(MultiblockPartBlock.PART_SHAPE,
                                MultiblockPartBlock.PartShape.PALLET),
                        Block.UPDATE_ALL);
        }
        removeUpperParts(level, lowerPos);
    }

    private static void removeUpperParts(Level level, BlockPos masterPos) {
        for (BlockPos offset : UPPER_PARTS) {
            BlockPos partPos = masterPos.offset(offset);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(ModBlocks.MULTIBLOCK_PART)
                    && MultiblockPartBlock.getMasterPos(partState, partPos).equals(masterPos))
                level.destroyBlock(partPos, false);
        }
    }
}
