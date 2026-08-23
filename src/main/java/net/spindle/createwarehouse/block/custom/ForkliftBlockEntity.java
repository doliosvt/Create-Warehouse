package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.spindle.createwarehouse.entity.custom.CarriedPalletEntity;
import net.spindle.createwarehouse.item.custom.PalletCarrierItem;
import net.spindle.createwarehouse.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ForkliftBlockEntity extends SmartBlockEntity {
    @Nullable
    private BlockPos inputPos;
    @Nullable
    private BlockPos outputPos;
    private EndpointType inputType = EndpointType.PALLET_STACK;
    private EndpointType outputType = EndpointType.PALLET_DEPOT;

    public ForkliftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || inputPos == null || outputPos == null
                || CarriedPalletEntity.existsForController(level, worldPosition))
            return;

        BlockPos source = getSourcePalletPos();
        BlockPos destination = getDestinationPalletPos();
        if (source == null || destination == null || source.equals(destination)
                || !PalletBlock.canMove(level, source)
                || !PalletBlock.canPlaceTransported(level, destination))
            return;

        CompoundTag palletData = PalletBlock.snapshotForTransport(level, source);
        if (palletData == null)
            return;

        ItemStack carrier = PalletCarrierItem.containing(palletData);
        CarriedPalletEntity carried = CarriedPalletEntity.createForForklift(
                level, worldPosition, source, destination, carrier);
        level.addFreshEntity(carried);
    }

    @Nullable
    private BlockPos getSourcePalletPos() {
        if (level == null || inputPos == null)
            return null;
        return inputType == EndpointType.PALLET_DEPOT
                ? inputPos.above()
                : PalletBlock.getTopPalletForForklift(level, inputPos);
    }

    @Nullable
    private BlockPos getDestinationPalletPos() {
        if (level == null || outputPos == null)
            return null;
        if (outputType == EndpointType.PALLET_DEPOT) {
            BlockPos destination = outputPos.above();
            return PalletBlock.canPlaceTransported(level, destination) ? destination : null;
        }
        return PalletBlock.getForkliftStackDestination(level, outputPos);
    }

    public boolean configureFromInteractionPoints(ListTag serializedPoints) {
        if (level == null)
            return false;

        Endpoint input = null;
        Endpoint output = null;
        for (Tag tag : serializedPoints) {
            if (!(tag instanceof CompoundTag pointTag))
                continue;
            ArmInteractionPoint point = ArmInteractionPoint.deserialize(pointTag, level, worldPosition);
            if (point == null)
                continue;
            Endpoint endpoint = resolveEndpoint(point.getPos());
            if (endpoint == null)
                continue;
            if (point.getMode() == Mode.TAKE && input == null)
                input = endpoint;
            else if (point.getMode() == Mode.DEPOSIT && output == null)
                output = endpoint;
        }

        if (input == null || output == null || input.equals(output))
            return false;
        inputPos = input.pos();
        inputType = input.type();
        outputPos = output.pos();
        outputType = output.type();
        notifyUpdate();
        return true;
    }

    @Nullable
    private Endpoint resolveEndpoint(BlockPos selectedPos) {
        if (level == null)
            return null;
        BlockState state = level.getBlockState(selectedPos);
        BlockPos masterPos = selectedPos;
        if (state.is(ModBlocks.MULTIBLOCK_PART)) {
            masterPos = MultiblockPartBlock.getMasterPos(state, selectedPos);
            state = level.getBlockState(masterPos);
        }
        if (state.is(ModBlocks.PALLET))
            return new Endpoint(PalletBlock.findStackBase(level, masterPos), EndpointType.PALLET_STACK);
        if (state.is(ModBlocks.PALLET_DEPOT))
            return new Endpoint(masterPos.immutable(), EndpointType.PALLET_DEPOT);
        return null;
    }

    @Nullable
    public BlockPos getInputPos() {
        return inputPos;
    }

    @Nullable
    public BlockPos getOutputPos() {
        return outputPos;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (inputPos != null)
            tag.putLong("InputPos", inputPos.asLong());
        if (outputPos != null)
            tag.putLong("OutputPos", outputPos.asLong());
        tag.putString("InputType", inputType.name());
        tag.putString("OutputType", outputType.name());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("InputPos")) {
            inputPos = BlockPos.of(tag.getLong("InputPos"));
            outputPos = tag.contains("OutputPos") ? BlockPos.of(tag.getLong("OutputPos")) : null;
            inputType = readType(tag.getString("InputType"), EndpointType.PALLET_STACK);
            outputType = readType(tag.getString("OutputType"), EndpointType.PALLET_DEPOT);
        } else {
            // Compatibility with the temporary empty-hand configuration format.
            inputPos = tag.contains("SourcePos") ? BlockPos.of(tag.getLong("SourcePos")) : null;
            outputPos = tag.contains("DepotPos") ? BlockPos.of(tag.getLong("DepotPos")) : null;
            inputType = EndpointType.PALLET_STACK;
            outputType = EndpointType.PALLET_DEPOT;
        }
    }

    private static EndpointType readType(String name, EndpointType fallback) {
        try {
            return EndpointType.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private record Endpoint(BlockPos pos, EndpointType type) {}

    private enum EndpointType {
        PALLET_STACK,
        PALLET_DEPOT
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(6);
    }
}
