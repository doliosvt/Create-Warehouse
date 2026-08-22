package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.spindle.createwarehouse.item.custom.FluidDrumItem;

import java.util.List;

public class DrumPackagerBlockEntity extends SmartBlockEntity {
    private static final int PACKAGING_DELAY_TICKS = 20;

    public final FluidTank fluid = new FluidTank(FluidDrumItem.CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide)
                notifyUpdate();
        }
    };

    public final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide)
                notifyUpdate();
        }
    };

    private final IFluidHandler topInput = new FillOnlyFluidHandler(fluid);
    private int packagingDelay = PACKAGING_DELAY_TICKS;

    public DrumPackagerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public IFluidHandler getTopInput() {
        return topInput;
    }

    public boolean giveOutputTo(Player player) {
        ItemStack readyDrum = output.extractItem(0, output.getSlotLimit(0), false);
        if (readyDrum.isEmpty())
            return false;

        if (!player.addItem(readyDrum) && !readyDrum.isEmpty())
            player.drop(readyDrum, false);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        boolean powered = getBlockState().hasProperty(DrumPackagerBlock.POWERED)
                && getBlockState().getValue(DrumPackagerBlock.POWERED);
        if (!powered) {
            resetPackagingDelay();
            return;
        }
        if (!output.getStackInSlot(0).isEmpty())
            return;
        if (--packagingDelay > 0)
            return;

        resetPackagingDelay();
        pullFromFluidSource();
    }

    public void resetPackagingDelay() {
        packagingDelay = PACKAGING_DELAY_TICKS;
    }

    public void pullFromFluidSource() {
        if (level == null || level.isClientSide || !output.getStackInSlot(0).isEmpty())
            return;

        int space = fluid.getSpace();
        if (space <= 0) {
            packageBufferedFluid();
            return;
        }

        // The actual external input is the top face of the protruding upper block.
        boolean transferred = transferFrom(worldPosition.above(2), Direction.DOWN, space);

        // Direct horizontal tank attachment remains supported as an additional convenience.
        if (!transferred) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (transferFrom(worldPosition.relative(direction), direction.getOpposite(), fluid.getSpace()))
                    break;
            }
        }
        packageBufferedFluid();
    }

    private boolean transferFrom(BlockPos sourcePos, Direction sourceSide, int maxAmount) {
        if (level == null || maxAmount <= 0)
            return false;

        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, sourceSide);
        if (source == null)
            return false;

        boolean transferred = false;
        int remaining = Math.min(maxAmount, fluid.getSpace());
        while (remaining > 0) {
            FluidStack simulated = source.drain(remaining, IFluidHandler.FluidAction.SIMULATE);
            int accepted = fluid.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0)
                break;

            FluidStack drained = source.drain(simulated.copyWithAmount(accepted),
                    IFluidHandler.FluidAction.EXECUTE);
            int filled = fluid.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0)
                break;

            transferred = true;
            remaining -= filled;
        }
        return transferred;
    }

    private void packageBufferedFluid() {
        if (fluid.isEmpty() || !output.getStackInSlot(0).isEmpty())
            return;

        FluidStack packaged = fluid.drain(fluid.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        output.setStackInSlot(0, FluidDrumItem.containing(packaged));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Fluid", fluid.writeToNBT(registries, new CompoundTag()));
        tag.put("Output", output.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fluid.readFromNBT(registries, tag.getCompound("Fluid"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
    }

    @Override
    public void destroy() {
        if (level == null || level.isClientSide)
            return;

        ItemStack readyDrum = output.getStackInSlot(0);
        if (!readyDrum.isEmpty())
            Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, readyDrum);
        if (!fluid.isEmpty())
            Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, FluidDrumItem.containing(fluid.getFluid()));
        output.setStackInSlot(0, ItemStack.EMPTY);
        fluid.setFluid(FluidStack.EMPTY);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
    }

    private record FillOnlyFluidHandler(IFluidHandler delegate) implements IFluidHandler {
        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return delegate.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return delegate.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return delegate.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return delegate.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
