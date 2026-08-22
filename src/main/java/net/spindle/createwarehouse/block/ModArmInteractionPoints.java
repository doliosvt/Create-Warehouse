package net.spindle.createwarehouse.block;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

public final class ModArmInteractionPoints {
    private static final DeferredRegister<ArmInteractionPointType> TYPES =
            DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, CreateWarehouse.MODID);

    public static final DeferredHolder<ArmInteractionPointType, PalletType> PALLET =
            TYPES.register("pallet", PalletType::new);

    private ModArmInteractionPoints() {}

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }

    public static class PalletType extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return findMaster(level, pos, state) != null;
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
        public void cycleMode() {}

        @Override
        public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
            if (!net.spindle.createwarehouse.block.custom.PalletBlock.isPalletCargo(stack))
                return stack;

            BlockPos masterPos = findMaster(level, pos, level.getBlockState(pos));
            if (masterPos == null || !(level.getBlockEntity(masterPos) instanceof PalletBlockEntity pallet)
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
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotCount(ArmBlockEntity armBlockEntity) {
            return 0;
        }

        @Override
        protected Vec3 getInteractionPositionVector() {
            BlockPos masterPos = findMaster(level, pos, level.getBlockState(pos));
            if (masterPos == null)
                return super.getInteractionPositionVector();
            return Vec3.atLowerCornerOf(masterPos).add(0, .25, 1);
        }
    }

    @Nullable
    private static BlockPos findMaster(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.PALLET))
            return pos;
        if (!state.is(ModBlocks.MULTIBLOCK_PART))
            return null;

        BlockPos masterPos = MultiblockPartBlock.getMasterPos(state, pos);
        return level.getBlockState(masterPos).is(ModBlocks.PALLET) ? masterPos : null;
    }
}
