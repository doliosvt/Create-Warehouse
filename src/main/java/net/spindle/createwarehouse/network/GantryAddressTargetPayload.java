package net.spindle.createwarehouse.network;

import java.util.List;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.block.custom.GantryAddress;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity.GantryStop;
import net.spindle.createwarehouse.block.custom.GantryControlsBlockEntity;

/** Submits one combined address for either a stationary or moving controls panel. */
public record GantryAddressTargetPayload(int entityId, BlockPos controlsPos, int contactNumber, int levelNumber)
        implements CustomPacketPayload {
    private static final int STATIONARY_ENTITY = -1;

    public static final Type<GantryAddressTargetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateWarehouse.MODID, "gantry_address_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GantryAddressTargetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, GantryAddressTargetPayload::entityId,
                    BlockPos.STREAM_CODEC, GantryAddressTargetPayload::controlsPos,
                    ByteBufCodecs.INT, GantryAddressTargetPayload::contactNumber,
                    ByteBufCodecs.INT, GantryAddressTargetPayload::levelNumber,
                    GantryAddressTargetPayload::new);

    public static void handle(GantryAddressTargetPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player))
            return;
        if (payload.contactNumber() < 1 || payload.contactNumber() > GantryAddress.MAX_CONTACT
                || payload.levelNumber() < 1 || payload.levelNumber() > GantryAddress.MAX_LEVEL)
            return;

        GantryAddress address = new GantryAddress(payload.contactNumber(), payload.levelNumber());
        if (payload.entityId() == STATIONARY_ENTITY)
            handleStationary(player, payload.controlsPos(), address);
        else
            handleMoving(player, payload.entityId(), payload.controlsPos(), address);
    }

    private static void handleStationary(ServerPlayer player, BlockPos pos, GantryAddress address) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64)
            return;
        if (!(player.level().getBlockEntity(pos) instanceof GantryControlsBlockEntity controls))
            return;

        List<GantryStop> stops = controls.getStops();
        int levelCount = controls.getLevelCount();
        if (!validateAddress(player, address, stops.size(), levelCount))
            return;

        controls.setAddress(address.contactIndex(), address.levelIndex());
        callController(player, address, stops.get(address.contactIndex()));
    }

    private static void handleMoving(ServerPlayer player, int entityId, BlockPos localPos, GantryAddress address) {
        if (!(player.serverLevel().getEntity(entityId) instanceof GantryContraptionEntity entity))
            return;
        if (entity.distanceToSqr(player) > 50 * 50)
            return;

        var actor = entity.getContraption().getActorAt(localPos);
        if (actor == null || !ModBlocks.GANTRY_CONTROLS.has(actor.left.state()))
            return;

        List<GantryStop> stops = GantryControllerBlockEntity.findStopsForContraption(player.level(), entity);
        MovementContext movementContext = actor.right;
        int levelCount = movementContext == null || movementContext.blockEntityData == null
                ? 0
                : Math.max(0, movementContext.blockEntityData.getInt("LevelCount"));
        if (!validateAddress(player, address, stops.size(), levelCount))
            return;

        movementContext.blockEntityData.putInt("Selection", address.contactIndex());
        movementContext.blockEntityData.putInt("LevelSelection", address.levelIndex());
        movementContext.blockEntityData.putBoolean("VerticalEnabled", true);
        callController(player, address, stops.get(address.contactIndex()));
    }

    private static boolean validateAddress(ServerPlayer player, GantryAddress address,
            int contactCount, int levelCount) {
        if (address.contactNumber() > contactCount) {
            player.displayClientMessage(Component.translatable(
                    "message.create_warehouse.gantry_controls.invalid_contact",
                    address.contactNumber(), contactCount), true);
            return false;
        }
        if (levelCount > 0 && address.levelNumber() > levelCount) {
            player.displayClientMessage(Component.translatable(
                    "message.create_warehouse.gantry_controls.invalid_level",
                    address.levelNumber(), levelCount), true);
            return false;
        }
        return true;
    }

    private static void callController(ServerPlayer player, GantryAddress address, GantryStop stop) {
        boolean called = GantryControllerBlockEntity.callFromContact(
                player.level(), stop.contactPos(), address.levelIndex());
        player.displayClientMessage(Component.translatable(
                called
                        ? "message.create_warehouse.gantry_controls.called_address"
                        : "message.create_warehouse.gantry_controls.no_controller_address",
                address.value()), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
