package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class PalletBlockEntity extends SmartBlockEntity {
    public static final int CAPACITY = 8;

    private final NonNullList<ItemStack> crates = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);

    public PalletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public boolean addCrate(ItemStack stack) {
        for (int slot = 0; slot < crates.size(); slot++) {
            if (!crates.get(slot).isEmpty())
                continue;
            if (level == null || !PalletBlock.prepareCrateSlot(level, worldPosition, slot, stack))
                return false;

            ItemStack stored = stack.copy();
            stored.setCount(1);
            crates.set(slot, stored);
            notifyUpdate();
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return crates.stream().noneMatch(ItemStack::isEmpty);
    }

    @Override
    public void lazyTick() {
        if (level == null || level.isClientSide)
            return;

        PalletBlock.repairLowerParts(level, worldPosition);
        for (int slot = 0; slot < crates.size(); slot++) {
            if (!crates.get(slot).isEmpty())
                PalletBlock.prepareCrateSlot(level, worldPosition, slot, crates.get(slot));
        }
    }

    public ItemStack getCrate(int slot) {
        return crates.get(slot);
    }

    public CompoundTag saveForTransport(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, crates, registries);
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof PalletBlock)
                tag.putBoolean("Supports", state.getValue(PalletBlock.SUPPORTS));
        }
        return tag;
    }

    public void loadFromTransport(CompoundTag tag, HolderLookup.Provider registries) {
        crates.clear();
        ContainerHelper.loadAllItems(tag, crates, registries);
        if (level != null && !level.isClientSide) {
            for (int slot = 0; slot < crates.size(); slot++) {
                ItemStack cargo = crates.get(slot);
                if (!cargo.isEmpty())
                    PalletBlock.prepareCrateSlot(level, worldPosition, slot, cargo);
            }
        }
        notifyUpdate();
    }

    public static NonNullList<ItemStack> readTransportedCargo(CompoundTag tag,
                                                              HolderLookup.Provider registries) {
        NonNullList<ItemStack> cargo = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, cargo, registries);
        return cargo;
    }

    public void clearForTransport() {
        crates.clear();
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ContainerHelper.saveAllItems(tag, crates, registries);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        crates.clear();
        ContainerHelper.loadAllItems(tag, crates, registries);
    }

    @Override
    public void destroy() {
        if (level == null || level.isClientSide)
            return;

        for (ItemStack crate : crates) {
            if (!crate.isEmpty())
                Containers.dropItemStack(level,
                        worldPosition.getX() + .5,
                        worldPosition.getY() + .5,
                        worldPosition.getZ() + .5,
                        crate);
        }
        crates.clear();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }
}
