package net.spindle.createwarehouse.block;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.custom.MultiblockPartBlock;
import net.spindle.createwarehouse.block.custom.PalletBlockEntity;
import net.spindle.createwarehouse.block.custom.PalletBlock;
import net.spindle.createwarehouse.entity.custom.CarriedPalletEntity;
import net.spindle.createwarehouse.item.custom.PalletCarrierItem;
import org.jetbrains.annotations.Nullable;

public final class ModArmInteractionPoints {
    private static final DeferredRegister<ArmInteractionPointType> TYPES =
            DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, CreateWarehouse.MODID);

    public static final DeferredHolder<ArmInteractionPointType, PalletType> PALLET =
            TYPES.register("pallet", PalletType::new);
    public static final DeferredHolder<ArmInteractionPointType, PalletDepotType> PALLET_DEPOT =
            TYPES.register("pallet_depot", PalletDepotType::new);

    private ModArmInteractionPoints() {}

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }

    public static class PalletType extends ArmInteractionPointType {
        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return findPalletMaster(level, pos, state) != null;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new PalletPoint(this, level, pos, state);
        }
    }

    private static class PalletPoint extends ArmInteractionPoint {
        private PalletPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }

        @Override
        public void cycleMode() {
            super.cycleMode();
        }

        @Override
        public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
            if (getMode() != Mode.DEPOSIT)
                return stack;

            BlockPos masterPos = findPalletMaster(level, pos, level.getBlockState(pos));
            if (masterPos == null)
                return stack;

            if (PalletCarrierItem.isCarrier(stack)) {
                BlockPos basePos = PalletBlock.findStackBase(level, masterPos);
                BlockPos destination = PalletBlock.getForkliftStackDestination(level, basePos);
                if (destination == null)
                    return stack;
                ItemStack remainder = stack.copy();
                remainder.shrink(1);
                if (simulate)
                    return remainder;
                if (!PalletBlock.placeTransported(level, destination,
                        PalletCarrierItem.getPalletData(stack)))
                    return stack;
                CarriedPalletEntity.discardForArm(level, armBlockEntity.getBlockPos());
                return remainder;
            }

            if (!PalletBlock.isPalletCargo(stack)
                    || !(level.getBlockEntity(masterPos) instanceof PalletBlockEntity pallet)
                    || pallet.isFull())
                return stack;

            if (!simulate && !pallet.addCrate(stack))
                return stack;

            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
            if (getMode() != Mode.TAKE || slot != 0 || amount <= 0)
                return ItemStack.EMPTY;

            BlockPos masterPos = findPalletMaster(level, pos, level.getBlockState(pos));
            if (masterPos == null || !PalletBlock.canMove(level, masterPos)
                    || !(level.getBlockEntity(masterPos) instanceof PalletBlockEntity pallet))
                return ItemStack.EMPTY;

            CompoundTag palletData = pallet.saveForTransport(level.registryAccess());
            ItemStack carrier = PalletCarrierItem.containing(palletData);
            if (simulate)
                return carrier;

            CompoundTag removedData = PalletBlock.removeForTransport(level, masterPos);
            if (removedData == null)
                return ItemStack.EMPTY;
            carrier = PalletCarrierItem.containing(removedData);
            CarriedPalletEntity entity = CarriedPalletEntity.create(level,
                    armBlockEntity.getBlockPos(), masterPos, carrier);
            level.addFreshEntity(entity);
            return carrier;
        }

        @Override
        public int getSlotCount(ArmBlockEntity armBlockEntity) {
            BlockPos masterPos = findPalletMaster(level, pos, level.getBlockState(pos));
            return getMode() == Mode.TAKE && masterPos != null && PalletBlock.canMove(level, masterPos) ? 1 : 0;
        }

        @Override
        protected Vec3 getInteractionPositionVector() {
            BlockPos masterPos = findPalletMaster(level, pos, level.getBlockState(pos));
            if (masterPos == null)
                return super.getInteractionPositionVector();
            return Vec3.atLowerCornerOf(masterPos).add(0, .25, 1);
        }
    }

    public static class PalletDepotType extends ArmInteractionPointType {
        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return findDepotMaster(level, pos, state) != null;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new PalletDepotPoint(this, level, pos, state);
        }
    }

    private static class PalletDepotPoint extends ArmInteractionPoint {
        private PalletDepotPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }

        @Override
        public void cycleMode() {
            super.cycleMode();
        }

        @Override
        public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
            if (!PalletCarrierItem.isCarrier(stack))
                return stack;

            BlockPos depotPos = findDepotMaster(level, pos, level.getBlockState(pos));
            if (depotPos == null)
                return stack;
            BlockPos palletPos = depotPos.above();
            if (!PalletBlock.canPlaceTransported(level, palletPos))
                return stack;

            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            if (simulate)
                return remainder;

            CompoundTag palletData = PalletCarrierItem.getPalletData(stack);
            if (!PalletBlock.placeTransported(level, palletPos, palletData))
                return stack;
            CarriedPalletEntity.discardForArm(level, armBlockEntity.getBlockPos());
            return remainder;
        }

        @Override
        public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
            if (getMode() != Mode.TAKE || slot != 0 || amount <= 0)
                return ItemStack.EMPTY;
            BlockPos depotPos = findDepotMaster(level, pos, level.getBlockState(pos));
            if (depotPos == null)
                return ItemStack.EMPTY;
            BlockPos palletPos = depotPos.above();
            if (!PalletBlock.canMove(level, palletPos)
                    || !(level.getBlockEntity(palletPos) instanceof PalletBlockEntity pallet))
                return ItemStack.EMPTY;

            ItemStack carrier = PalletCarrierItem.containing(
                    pallet.saveForTransport(level.registryAccess()));
            if (simulate)
                return carrier;
            CompoundTag removedData = PalletBlock.removeForTransport(level, palletPos);
            if (removedData == null)
                return ItemStack.EMPTY;
            carrier = PalletCarrierItem.containing(removedData);
            CarriedPalletEntity entity = CarriedPalletEntity.create(level,
                    armBlockEntity.getBlockPos(), palletPos, carrier);
            level.addFreshEntity(entity);
            return carrier;
        }

        @Override
        public int getSlotCount(ArmBlockEntity armBlockEntity) {
            BlockPos depotPos = findDepotMaster(level, pos, level.getBlockState(pos));
            return getMode() == Mode.TAKE && depotPos != null
                    && PalletBlock.canMove(level, depotPos.above()) ? 1 : 0;
        }

        @Override
        protected Vec3 getInteractionPositionVector() {
            BlockPos masterPos = findDepotMaster(level, pos, level.getBlockState(pos));
            if (masterPos == null)
                return super.getInteractionPositionVector();
            return Vec3.atLowerCornerOf(masterPos).add(0, 1.25, 1);
        }
    }

    @Nullable
    private static BlockPos findPalletMaster(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.PALLET))
            return pos;
        if (!state.is(ModBlocks.MULTIBLOCK_PART))
            return null;

        BlockPos masterPos = MultiblockPartBlock.getMasterPos(state, pos);
        return level.getBlockState(masterPos).is(ModBlocks.PALLET) ? masterPos : null;
    }

    @Nullable
    private static BlockPos findDepotMaster(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.PALLET_DEPOT))
            return pos;
        if (!state.is(ModBlocks.MULTIBLOCK_PART))
            return null;

        BlockPos masterPos = MultiblockPartBlock.getMasterPos(state, pos);
        return level.getBlockState(masterPos).is(ModBlocks.PALLET_DEPOT) ? masterPos : null;
    }
}
