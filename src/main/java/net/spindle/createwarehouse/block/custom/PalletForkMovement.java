package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.entity.custom.CarriedPalletEntity;
import net.spindle.createwarehouse.item.custom.PalletCarrierItem;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class PalletForkMovement extends HarvesterMovementBehaviour {
    private static final String CARRIED_PALLET = "CarriedPallet";
    private static final String PALLET_LOCAL_POS = "PalletLocalPos";
    private static final String CARRIED_ENTITY_UUID = "CarriedPalletEntityUuid";
    private static final String PICKUP_LOCKED = "PalletPickupLocked";
    private static final String PICKUP_CANDIDATE = "PalletPickupCandidate";
    private static final String PICKUP_ENTRY_COORDINATE = "PalletPickupEntryCoordinate";
    private static final String RELEASE_DESTINATION = "PalletReleaseDestination";
    private static final double MOVEMENT_EPSILON = 1.0e-5;
    private static final int RELEASE_LOCK_TICKS = 20 * 10;
    private static final Map<Level, Map<BlockPos, Long>> RELEASED_PALLETS = new WeakHashMap<>();

    /** The model's visual tines end one and a half blocks in front of its block centre. */
    private static final double ACTIVE_POINT_FROM_CENTRE = 1.0;
    /** At this point the two-block-long tines lie fully inside the pallet footprint. */
    private static final double FORK_CENTRE_INSERTION_DISTANCE = .5;

    @Override
    public boolean isActive(MovementContext context) {
        return !context.disabled;
    }

    @Override
    public Vec3 getActiveAreaOffset(MovementContext context) {
        return Vec3.atLowerCornerOf(context.state.getValue(PalletForkBlock.FACING).getNormal())
                .scale(ACTIVE_POINT_FROM_CENTRE);
    }

    @Override
    public void startMoving(MovementContext context) {
        if (!context.world.isClientSide() && hasPallet(context))
            ensureCarriedPalletEntity(context);
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (context.world.isClientSide() || hasPallet(context))
            return;
        if (context.data.getBoolean(PICKUP_LOCKED)) {
            if (findPalletForFork(context.world, pos) != null)
                return;
            context.data.remove(PICKUP_LOCKED);
        }
        if (!isPoweredAt(context, pos))
            advancePickup(context, pos);
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide() || context.position == null
                || context.contraption.entity == null)
            return;

        BlockPos activePos = getActivePos(context);
        if (!hasPallet(context))
            tryAdoptStationaryPallet(context);

        boolean powered = isPoweredAt(context, activePos);
        if (hasPallet(context)) {
            ensureCarriedPalletEntity(context);
            if ((powered || context.data.contains(RELEASE_DESTINATION)) && tryRelease(context))
                context.data.putBoolean(PICKUP_LOCKED, true);
            return;
        }

        BlockPos palletAtFork = findPalletMaster(context.world, activePos);
        if (!context.data.getBoolean(PICKUP_LOCKED) && palletAtFork != null
                && consumeReleaseLock(context.world, palletAtFork))
            context.data.putBoolean(PICKUP_LOCKED, true);

        if (powered)
            return;

        if (context.data.getBoolean(PICKUP_LOCKED)) {
            if (findPalletForFork(context.world, activePos) == null)
                context.data.remove(PICKUP_LOCKED);
            else
                return;
        }

        advancePickup(context, activePos);
    }

    /**
     * Create treats harvesters as actors which may enter selected world blocks
     * without making the whole contraption stall. The fork uses that collision
     * path only for a top, movable pallet; every other block remains solid.
     */
    @Override
    public boolean isValidCrop(Level level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean isValidOther(Level level, BlockPos pos, BlockState state) {
        return findPalletMaster(level, pos) != null;
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        // The fork is rendered by its regular moving block model.
    }

    @Nullable
    @Override
    @OnlyIn(Dist.CLIENT)
    public ActorVisual createVisual(VisualizationContext visualizationContext,
                                    VirtualRenderWorld simulationWorld,
                                    MovementContext movementContext) {
        return null;
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context.world.isClientSide())
            return;

        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        if (contraptionEntity == null)
            return;
        if (!hasPallet(context))
            tryFinalizePickupAtStop(context);
        if (!hasPallet(context))
            return;
        BlockPos localPalletPos = BlockPos.of(context.data.getLong(PALLET_LOCAL_POS));
        Vec3 palletPosition = contraptionEntity.toGlobalVector(
                Vec3.atLowerCornerOf(localPalletPos), 1);
        Vec3 movingForkPosition = contraptionEntity.toGlobalVector(
                Vec3.atLowerCornerOf(context.localPos), 1);
        BlockPos stationaryForkPos = getForkPos(context);
        Vec3 snappedPalletPosition = palletPosition.add(
                Vec3.atLowerCornerOf(stationaryForkPos).subtract(movingForkPosition));
        ItemStack carrier = PalletCarrierItem.containing(getPalletData(context));
        BlockPos releaseDestination = context.data.contains(RELEASE_DESTINATION)
                ? BlockPos.of(context.data.getLong(RELEASE_DESTINATION)) : null;
        CarriedPalletEntity.parkForPalletFork(
                context.world, contraptionEntity.getUUID(), context.localPos,
                palletPosition, stationaryForkPos, snappedPalletPosition, carrier,
                getTrackedEntityUuid(context), releaseDestination);
    }

    private void tryFinalizePickupAtStop(MovementContext context) {
        if (!context.data.contains(PICKUP_CANDIDATE)
                || !context.data.contains(PICKUP_ENTRY_COORDINATE))
            return;

        BlockPos candidate = BlockPos.of(context.data.getLong(PICKUP_CANDIDATE));
        if (!PalletBlock.canMoveStack(context.world, candidate)) {
            clearPickupCandidate(context);
            return;
        }

        double travelledInside = getForkCentreInsertionCoordinate(context)
                - context.data.getDouble(PICKUP_ENTRY_COORDINATE);
        if (travelledInside + MOVEMENT_EPSILON < FORK_CENTRE_INSERTION_DISTANCE)
            return;

        // The final piston step is applied after the last actor tick. Complete
        // the pickup before Create restores the fork blocks into the world.
        pickupPallet(context, candidate);
    }

    private void tryAdoptStationaryPallet(MovementContext context) {
        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        if (contraptionEntity == null)
            return;

        CarriedPalletEntity stationary = CarriedPalletEntity.getForStationaryPalletFork(
                context.world, getForkPos(context));
        if (stationary == null)
            return;

        CompoundTag palletData = PalletCarrierItem.getPalletData(stationary.getCarrier()).copy();
        Vec3 localPalletVector = contraptionEntity.toLocalVector(stationary.position(), 1);
        BlockPos localPalletPos = BlockPos.containing(localPalletVector.add(.25, .25, .25));
        context.data.put(CARRIED_PALLET, palletData);
        context.data.putLong(PALLET_LOCAL_POS, localPalletPos.asLong());
        context.data.putUUID(CARRIED_ENTITY_UUID, stationary.getUUID());
        BlockPos releaseDestination = stationary.consumePalletForkReleaseRequest();
        if (releaseDestination != null)
            context.data.putLong(RELEASE_DESTINATION, releaseDestination.asLong());
        stationary.attachToPalletFork(contraptionEntity, context.localPos, localPalletPos);
    }

    private void pickupPallet(MovementContext context, BlockPos palletPos) {

        CompoundTag palletData = PalletBlock.removeStackForTransport(context.world, palletPos);
        if (palletData == null)
            return;

        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        Vec3 localPalletVector = contraptionEntity.toLocalVector(
                Vec3.atLowerCornerOf(palletPos), 1);
        BlockPos localPalletPos = BlockPos.containing(localPalletVector.add(.25, .25, .25));
        context.data.put(CARRIED_PALLET, palletData.copy());
        context.data.putLong(PALLET_LOCAL_POS, localPalletPos.asLong());
        clearPickupCandidate(context);
        ensureCarriedPalletEntity(context);
    }

    private void advancePickup(MovementContext context, BlockPos activePos) {
        if (context.position == null || !isEnteringPallet(context)) {
            clearPickupCandidate(context);
            return;
        }

        BlockPos palletPos = findPalletForFork(context.world, activePos);
        if (!context.data.contains(PICKUP_CANDIDATE)) {
            if (palletPos == null)
                return;
            context.data.putLong(PICKUP_CANDIDATE, palletPos.asLong());
            context.data.putDouble(PICKUP_ENTRY_COORDINATE,
                    getPalletEntryCoordinate(palletPos, getWorldFacing(context)));
            return;
        }

        BlockPos candidate = BlockPos.of(context.data.getLong(PICKUP_CANDIDATE));
        if (!PalletBlock.canMoveStack(context.world, candidate)) {
            clearPickupCandidate(context);
            return;
        }
        if (palletPos != null && !palletPos.equals(candidate)) {
            context.data.putLong(PICKUP_CANDIDATE, palletPos.asLong());
            context.data.putDouble(PICKUP_ENTRY_COORDINATE,
                    getPalletEntryCoordinate(palletPos, getWorldFacing(context)));
            return;
        }

        double travelledInside = getForkCentreInsertionCoordinate(context)
                - context.data.getDouble(PICKUP_ENTRY_COORDINATE);
        if (travelledInside + MOVEMENT_EPSILON < FORK_CENTRE_INSERTION_DISTANCE)
            return;
        pickupPallet(context, candidate);
    }

    private static double getForkCentreInsertionCoordinate(MovementContext context) {
        Vec3 facingVector = Vec3.atLowerCornerOf(getWorldFacing(context).getNormal());
        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        if (contraptionEntity != null) {
            Vec3 forkCentre = contraptionEntity.toGlobalVector(
                    Vec3.atCenterOf(context.localPos), 1);
            return forkCentre.dot(facingVector);
        }

        Vec3 localActiveOffset = Vec3.atLowerCornerOf(
                context.state.getValue(PalletForkBlock.FACING).getNormal())
                .scale(ACTIVE_POINT_FROM_CENTRE);
        Vec3 activeOffset = context.rotation.apply(localActiveOffset);
        return context.position.subtract(activeOffset).dot(facingVector);
    }

    private static double getPalletEntryCoordinate(BlockPos palletPos, Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            int min = palletPos.getX() - 1;
            int max = palletPos.getX() + 1;
            return facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? min : -max;
        }

        int min = palletPos.getZ();
        int max = palletPos.getZ() + 2;
        return facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? min : -max;
    }

    public static boolean forceRelease(AbstractContraptionEntity contraptionEntity) {
        if (contraptionEntity == null || contraptionEntity.level().isClientSide())
            return false;
        for (var actor : contraptionEntity.getContraption().getActors()) {
            MovementContext context = actor.right;
            if (context == null || !actor.left.state().is(ModBlocks.PALLET_FORK)
                    || !hasPallet(context))
                continue;
            if (tryRelease(context))
                return true;
        }
        return false;
    }

    private static void clearPickupCandidate(MovementContext context) {
        context.data.remove(PICKUP_CANDIDATE);
        context.data.remove(PICKUP_ENTRY_COORDINATE);
    }

    private static boolean tryRelease(MovementContext context) {
        if (!hasPallet(context) || context.contraption.entity == null)
            return false;

        BlockPos destination;
        if (context.data.contains(RELEASE_DESTINATION)) {
            destination = BlockPos.of(context.data.getLong(RELEASE_DESTINATION));
        } else {
            BlockPos localPalletPos = BlockPos.of(context.data.getLong(PALLET_LOCAL_POS));
            Vec3 worldPalletVector = context.contraption.entity.toGlobalVector(
                    Vec3.atLowerCornerOf(localPalletPos), 1);
            destination = BlockPos.containing(worldPalletVector.add(.25, .25, .25));
        }
        if (!PalletBlock.placeTransported(context.world, destination, getPalletData(context)))
            return false;

        lockReleasedPallet(context.world, destination);
        discardCarriedPalletEntity(context);
        clearPallet(context);
        context.data.putBoolean(PICKUP_LOCKED, true);
        return true;
    }

    public static synchronized void lockReleasedPallet(Level level, BlockPos palletPos) {
        RELEASED_PALLETS.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(palletPos.immutable(), level.getGameTime() + RELEASE_LOCK_TICKS);
    }

    private static synchronized boolean consumeReleaseLock(Level level, BlockPos palletPos) {
        Map<BlockPos, Long> locks = RELEASED_PALLETS.get(level);
        if (locks == null)
            return false;
        Long expiry = locks.remove(palletPos);
        locks.entrySet().removeIf(entry -> entry.getValue() < level.getGameTime());
        if (locks.isEmpty())
            RELEASED_PALLETS.remove(level);
        return expiry != null && expiry >= level.getGameTime();
    }

    private void ensureCarriedPalletEntity(MovementContext context) {
        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        if (contraptionEntity == null || !hasPallet(context))
            return;

        CarriedPalletEntity tracked = getTrackedEntity(context);
        if (tracked != null)
            return;

        BlockPos localPalletPos = BlockPos.of(context.data.getLong(PALLET_LOCAL_POS));
        Vec3 worldPalletPos = contraptionEntity.toGlobalVector(
                Vec3.atLowerCornerOf(localPalletPos), 1);
        CarriedPalletEntity existing = CarriedPalletEntity.getForPalletFork(context.world,
                contraptionEntity.getUUID(), context.localPos, worldPalletPos);
        if (existing != null) {
            context.data.putUUID(CARRIED_ENTITY_UUID, existing.getUUID());
            return;
        }

        ItemStack carrier = PalletCarrierItem.containing(getPalletData(context));
        CarriedPalletEntity carried = CarriedPalletEntity.createForPalletFork(
                context.world, contraptionEntity, context.localPos, localPalletPos, carrier);
        context.world.addFreshEntity(carried);
        context.data.putUUID(CARRIED_ENTITY_UUID, carried.getUUID());
    }

    private static void discardCarriedPalletEntity(MovementContext context) {
        CarriedPalletEntity tracked = getTrackedEntity(context);
        if (tracked != null) {
            tracked.discard();
            return;
        }
        if (context.contraption.entity == null || context.position == null)
            return;
        CarriedPalletEntity.discardForPalletFork(context.world,
                context.contraption.entity.getUUID(), context.localPos, context.position);
    }

    @Nullable
    private static CarriedPalletEntity getTrackedEntity(MovementContext context) {
        UUID uuid = getTrackedEntityUuid(context);
        if (uuid == null || !(context.world instanceof ServerLevel serverLevel))
            return null;
        Entity entity = serverLevel.getEntity(uuid);
        return entity instanceof CarriedPalletEntity carried && carried.isAlive()
                ? carried : null;
    }

    @Nullable
    private static UUID getTrackedEntityUuid(MovementContext context) {
        return context.data.hasUUID(CARRIED_ENTITY_UUID)
                ? context.data.getUUID(CARRIED_ENTITY_UUID) : null;
    }

    private static boolean hasPallet(MovementContext context) {
        return context.data.contains(CARRIED_PALLET) && context.data.contains(PALLET_LOCAL_POS);
    }

    private static CompoundTag getPalletData(MovementContext context) {
        return context.data.getCompound(CARRIED_PALLET);
    }

    private static void clearPallet(MovementContext context) {
        context.data.remove(CARRIED_PALLET);
        context.data.remove(PALLET_LOCAL_POS);
        context.data.remove(CARRIED_ENTITY_UUID);
        context.data.remove(RELEASE_DESTINATION);
    }

    private static boolean isPoweredAt(MovementContext context, BlockPos activePos) {
        BlockPos forkPos = getForkPos(context);
        return context.world.hasNeighborSignal(forkPos)
                || context.world.hasNeighborSignal(activePos);
    }

    private static BlockPos getForkPos(MovementContext context) {
        if (context.contraption.entity != null) {
            Vec3 forkCentre = context.contraption.entity.toGlobalVector(
                    Vec3.atCenterOf(context.localPos), 1);
            return BlockPos.containing(forkCentre);
        }
        Vec3 localOffset = Vec3.atLowerCornerOf(
                context.state.getValue(PalletForkBlock.FACING).getNormal())
                .scale(ACTIVE_POINT_FROM_CENTRE);
        Vec3 worldOffset = context.rotation.apply(localOffset);
        return BlockPos.containing(context.position.subtract(worldOffset));
    }

    private static BlockPos getActivePos(MovementContext context) {
        return BlockPos.containing(context.position);
    }

    private static boolean isEnteringPallet(MovementContext context) {
        Direction facing = getWorldFacing(context);
        Vec3 facingVector = Vec3.atLowerCornerOf(facing.getNormal());
        return context.motion.dot(facingVector) > MOVEMENT_EPSILON;
    }

    private static Direction getWorldFacing(MovementContext context) {
        Vec3 localFacing = Vec3.atLowerCornerOf(
                context.state.getValue(PalletForkBlock.FACING).getNormal());
        Vec3 worldFacing = context.rotation.apply(localFacing);
        return Direction.getNearest(worldFacing.x, worldFacing.y, worldFacing.z);
    }

    @Nullable
    private static BlockPos findPalletForFork(Level level, BlockPos activePos) {
        return findPalletMaster(level, activePos);
    }

    @Nullable
    private static BlockPos findPalletMaster(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.PALLET))
            return PalletBlock.canMoveStack(level, pos) ? pos : null;
        if (!state.is(ModBlocks.MULTIBLOCK_PART))
            return null;

        BlockPos masterPos = MultiblockPartBlock.getMasterPos(state, pos);
        if (masterPos.getY() == pos.getY())
            return PalletBlock.canMoveStack(level, masterPos) ? masterPos : null;

        // The top crosspieces of a supported pallet belong to the lower
        // pallet's multiblock, but the fork passing through them is targeting
        // the movable pallet resting two blocks above that master.
        if (state.getValue(MultiblockPartBlock.PART_SHAPE)
                == MultiblockPartBlock.PartShape.PALLET_FRAME_TOP) {
            BlockPos supportedPalletPos = masterPos.above(2);
            if (PalletBlock.canMoveStack(level, supportedPalletPos))
                return supportedPalletPos;
            return PalletBlock.canMoveStack(level, masterPos) ? masterPos : null;
        }
        return null;
    }
}
