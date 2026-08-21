package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.crate.BottomlessItemHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.*;
import com.simibubi.create.content.logistics.packagerLink.*;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

public class CratePackagerBlockEntity extends PackagerBlockEntity {

    private InventorySummary availableItems;
    private VersionedInventoryTrackerBehaviour invVersionTracker;

    public CratePackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(targetInventory = new InvManipulationBehaviour(this, CapManipulationBehaviourBase.InterfaceProvider.oppositeOfBlockFacing())
                .withFilter(this::supportsBlockEntity));
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));

        // deactivated advancements and CC integration
        // NOTE: Minecraft crashes on shutdown when CC integration is removed
        // Caused by: java.lang.NullPointerException: Cannot invoke "com.simibubi.create.compat.computercraft.AbstractComputerBehaviour.removePeripheral()" because "this.computerBehaviour" is null

        //behaviours.add(advancements = new AdvancementBehaviour(this, AllAdvancements.PACKAGER));
        behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
    }

    private boolean supportsBlockEntity(BlockEntity target) {
        return target instanceof VaultBlockEntity;
    }

    public InventorySummary getAvailableItems() {
        if (availableItems != null && invVersionTracker.stillWaiting(targetInventory.getInventory()))
            return availableItems;

        InventorySummary availableItems = new InventorySummary();

        IItemHandler targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler) {
            this.availableItems = availableItems;
            return availableItems;
        }

        if (targetInv instanceof BottomlessItemHandler bih) {
            availableItems.add(bih.getStackInSlot(0), BigItemStack.INF);
            this.availableItems = availableItems;
            return availableItems;
        }

        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            availableItems.add(targetInv.getStackInSlot(slot));
        }

        invVersionTracker.awaitNewVersion(targetInventory.getInventory());
        submitNewArrivals(this.availableItems, availableItems);
        this.availableItems = availableItems;
        return availableItems;
    }

    private void submitNewArrivals(InventorySummary before, InventorySummary after) {
        if (before == null || after.isEmpty())
            return;

        Set<RequestPromiseQueue> promiseQueues = new HashSet<>();

        for (Direction d : Iterate.directions) {
            if (!level.isLoaded(worldPosition.relative(d)))
                continue;

            BlockState adjacentState = level.getBlockState(worldPosition.relative(d));
            if (AllBlocks.FACTORY_GAUGE.has(adjacentState)) {
                if (FactoryPanelBlock.connectedDirection(adjacentState) != d)
                    continue;
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof FactoryPanelBlockEntity fpbe))
                    continue;
                if (!fpbe.restocker)
                    continue;
                for (FactoryPanelBehaviour behaviour : fpbe.panels.values()) {
                    if (!behaviour.isActive())
                        continue;
                    promiseQueues.add(behaviour.restockerPromises);
                }
            }

            if (AllBlocks.STOCK_LINK.has(adjacentState)) {
                if (PackagerLinkBlock.getConnectedDirection(adjacentState) != d)
                    continue;
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof PackagerLinkBlockEntity plbe))
                    continue;
                UUID freqId = plbe.behaviour.freqId;
                if (!Create.LOGISTICS.hasQueuedPromises(freqId))
                    continue;
                promiseQueues.add(Create.LOGISTICS.getQueuedPromises(freqId));
            }
        }

        if (promiseQueues.isEmpty())
            return;

        for (BigItemStack entry : after.getStacks())
            before.add(entry.stack, -entry.count);
        for (RequestPromiseQueue queue : promiseQueues)
            for (BigItemStack entry : before.getStacks())
                if (entry.count < 0)
                    queue.itemEnteredSystem(entry.stack, -entry.count);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        redstonePowered = compound.getBoolean("Active");
        animationInward = compound.getBoolean("AnimationInward");
        animationTicks = compound.getInt("AnimationTicks");
        signBasedAddress = compound.getString("SignAddress");
        customComputerAddress = compound.getString("ComputerAddress");
        hasCustomComputerAddress = compound.getBoolean("HasComputerAddress");
        heldBox = ItemStack.parseOptional(registries, compound.getCompound("HeldBox"));
        previouslyUnwrapped = ItemStack.parseOptional(registries, compound.getCompound("InsertedBox"));
        if (clientPacket)
            return;
        queuedExitingPackages = NBTHelper.readCompoundList(compound.getList("QueuedExitingPackages", Tag.TAG_COMPOUND),
                c -> CatnipCodecUtils.decode(BigItemStack.CODEC, registries, c)
                        .orElseThrow());
        if (compound.contains("LastSummary"))
            availableItems = CatnipCodecUtils.decodeOrNull(InventorySummary.CODEC, registries, compound.getCompound("LastSummary"));
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("Active", redstonePowered);
        compound.putBoolean("AnimationInward", animationInward);
        compound.putInt("AnimationTicks", animationTicks);
        compound.putString("SignAddress", signBasedAddress);
        compound.putString("ComputerAddress", customComputerAddress);
        compound.putBoolean("HasComputerAddress", hasCustomComputerAddress);
        compound.put("HeldBox", heldBox.saveOptional(registries));
        compound.put("InsertedBox", previouslyUnwrapped.saveOptional(registries));
        if (clientPacket)
            return;
        compound.put("QueuedExitingPackages", NBTHelper.writeCompoundList(queuedExitingPackages, bis -> {
            if (CatnipCodecUtils.encode(BigItemStack.CODEC, registries, bis)
                    .orElse(new CompoundTag()) instanceof CompoundTag ct)
                return ct;
            return new CompoundTag();
        }));
        if (availableItems != null)
            compound.put("LastSummary", CatnipCodecUtils.encode(InventorySummary.CODEC, registries, availableItems)
                    .orElseThrow());
    }
}
