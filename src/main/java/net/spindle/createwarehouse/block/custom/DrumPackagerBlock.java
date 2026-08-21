package net.spindle.createwarehouse.block.custom;

public class DrumPackagerBlock extends MultiBlock1x2x1PackagerBaseBlock /*implements IBE<PackagerBlockEntity>,*/ {

    public DrumPackagerBlock(Properties properties) {
        super(properties);
    }

    // TODO: test contraption behavior with the packager

    // --------------------------------------------------------------------------------------------------------
    // Packager logic    currently on pause until the drum is working
//
//    @Override
//    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
//        if (AllItems.WRENCH.isIn(stack))
//            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
//        if (AllBlocks.FACTORY_GAUGE.isIn(stack))
//            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
//        if (AllBlocks.STOCK_LINK.isIn(stack) && !(state.hasProperty(LINKED) && state.getValue(LINKED)))
//            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
//        if (AllBlocks.PACKAGE_FROGPORT.isIn(stack))
//            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
//
//        if (onBlockEntityUseItemOn(level, pos, be -> {
//            if (be.heldBox.isEmpty()) {
//                if (be.animationTicks > 0)
//                    return ItemInteractionResult.SUCCESS;
//                if (PackageItem.isPackage(stack)) {
//                    if (level.isClientSide())
//                        return ItemInteractionResult.SUCCESS;
//                    if (!be.unwrapBox(stack.copy(), true))
//                        return ItemInteractionResult.SUCCESS;
//                    be.unwrapBox(stack.copy(), false);
//                    be.triggerStockCheck();
//                    stack.shrink(1);
//                    AllSoundEvents.DEPOT_PLOP.playOnServer(level, pos);
//                    if (stack.isEmpty())
//                        player.setItemInHand(hand, ItemStack.EMPTY);
//                    return ItemInteractionResult.SUCCESS;
//                }
//                return ItemInteractionResult.SUCCESS;
//            }
//            if (be.animationTicks > 0)
//                return ItemInteractionResult.SUCCESS;
//            if (!level.isClientSide()) {
//                player.getInventory()
//                        .placeItemBackInInventory(be.heldBox.copy());
//                AllSoundEvents.playItemPickup(player);
//                be.heldBox = ItemStack.EMPTY;
//                be.notifyUpdate();
//            }
//            return ItemInteractionResult.SUCCESS;
//        }).consumesAction())
//            return ItemInteractionResult.SUCCESS;
//
//        return ItemInteractionResult.SUCCESS;
//    }
//
//    @Override
//    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
//        super.onNeighborChange(state, level, pos, neighbor);
//        if (neighbor.relative(Direction.UP)
//                .equals(pos))
//            withBlockEntityDo(level, pos, PackagerBlockEntity::triggerStockCheck);
//    }
//
//    @Override
//    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
//                                boolean isMoving) {
//        if (worldIn.isClientSide)
//            return;
//        boolean previouslyPowered = state.getValue(POWERED);
//        if (previouslyPowered == worldIn.hasNeighborSignal(pos))
//            return;
//        worldIn.setBlock(pos, state.cycle(POWERED), 2);
//        if (!previouslyPowered)
//            withBlockEntityDo(worldIn, pos, PackagerBlockEntity::activate);
//    }
//
//    @Override
//    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
//        IBE.onRemove(pState, pLevel, pPos, pNewState);
//    }
//
//    @Override
//    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
//        return false;
//    }
//
//    @Override
//    public Class<PackagerBlockEntity> getBlockEntityClass() {
//        return PackagerBlockEntity.class;
//    }
//
//    @Override
//    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
//        return AllBlockEntityTypes.PACKAGER.get();
//    }
//
//    @Override
//    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
//        return false;
//    }
//
//    @Override
//    public boolean hasAnalogOutputSignal(BlockState pState) {
//        return true;
//    }
//
//    @Override
//    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
//        return getBlockEntityOptional(pLevel, pPos).map(pbe -> {
//                    boolean empty = pbe.inventory.getStackInSlot(0)
//                            .isEmpty();
//                    if (pbe.animationTicks != 0)
//                        empty = false;
//                    return empty ? 0 : 15;
//                })
//                .orElse(0);
//    }
}
