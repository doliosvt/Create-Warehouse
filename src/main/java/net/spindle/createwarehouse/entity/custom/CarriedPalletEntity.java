package net.spindle.createwarehouse.entity.custom;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.block.custom.PalletBlock;
import net.spindle.createwarehouse.block.custom.ForkliftBlockEntity;
import net.spindle.createwarehouse.entity.ModEntityTypes;
import net.spindle.createwarehouse.compat.ArmPalletAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CarriedPalletEntity extends Entity {
    private static final int STANDARD_ARM = 0;
    private static final int FORKLIFT = 1;
    private static final int PALLET_FORK = 2;
    private static final int STATIONARY_PALLET_FORK = 3;
    private static final EntityDataAccessor<ItemStack> CARRIER =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<BlockPos> ARM_POS =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> SOURCE_POS =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> TARGET_POS =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> CARRIER_MODE =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FORKLIFT_PICKED_UP =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FORKLIFT_RELEASED =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> CONTRAPTION_UUID =
            SynchedEntityData.defineId(CarriedPalletEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int missingPalletForkControllerTicks;

    public CarriedPalletEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static EntityType.Builder<CarriedPalletEntity> configureDimensions(
            EntityType.Builder<CarriedPalletEntity> builder) {
        return builder.sized(.25f, .25f);
    }

    public static CarriedPalletEntity create(Level level, BlockPos armPos, BlockPos sourcePos,
                                              ItemStack carrier) {
        CarriedPalletEntity entity = ModEntityTypes.CARRIED_PALLET.get().create(level);
        if (entity == null)
            throw new IllegalStateException("Could not create carried pallet entity");
        entity.entityData.set(CARRIER, carrier.copy());
        entity.entityData.set(ARM_POS, armPos.immutable());
        entity.entityData.set(SOURCE_POS, sourcePos.immutable());
        entity.entityData.set(TARGET_POS, sourcePos.immutable());
        entity.entityData.set(CARRIER_MODE, STANDARD_ARM);
        entity.entityData.set(FORKLIFT_PICKED_UP, false);
        entity.entityData.set(FORKLIFT_RELEASED, false);
        entity.setPos(armPos.getX(), armPos.getY(), armPos.getZ());
        return entity;
    }

    public static CarriedPalletEntity createForForklift(Level level, BlockPos controllerPos,
                                                         BlockPos sourcePos, BlockPos targetPos,
                                                         ItemStack carrier) {
        CarriedPalletEntity entity = ModEntityTypes.CARRIED_PALLET.get().create(level);
        if (entity == null)
            throw new IllegalStateException("Could not create carried pallet entity");
        entity.entityData.set(CARRIER, carrier.copy());
        entity.entityData.set(ARM_POS, controllerPos.immutable());
        entity.entityData.set(SOURCE_POS, sourcePos.immutable());
        entity.entityData.set(TARGET_POS, targetPos.immutable());
        entity.entityData.set(CARRIER_MODE, FORKLIFT);
        entity.entityData.set(FORKLIFT_PICKED_UP, false);
        entity.entityData.set(FORKLIFT_RELEASED, false);
        entity.setPos(sourcePos.getX(), sourcePos.getY(), sourcePos.getZ());
        return entity;
    }

    public static CarriedPalletEntity createForPalletFork(Level level,
                                                           AbstractContraptionEntity contraption,
                                                           BlockPos localForkPos,
                                                           BlockPos localPalletPos,
                                                           ItemStack carrier) {
        CarriedPalletEntity entity = ModEntityTypes.CARRIED_PALLET.get().create(level);
        if (entity == null)
            throw new IllegalStateException("Could not create carried pallet entity");
        entity.entityData.set(CARRIER, carrier.copy());
        entity.entityData.set(ARM_POS, localForkPos.immutable());
        entity.entityData.set(SOURCE_POS, localPalletPos.immutable());
        entity.entityData.set(TARGET_POS, localPalletPos.immutable());
        entity.entityData.set(CARRIER_MODE, PALLET_FORK);
        entity.entityData.set(CONTRAPTION_UUID, Optional.of(contraption.getUUID()));
        Vec3 position = contraption.toGlobalVector(Vec3.atLowerCornerOf(localPalletPos), 1);
        entity.setPos(position.x, position.y, position.z);
        return entity;
    }

    public ItemStack getCarrier() {
        return entityData.get(CARRIER);
    }

    public BlockPos getArmPos() {
        return entityData.get(ARM_POS);
    }

    public BlockPos getSourcePos() {
        return entityData.get(SOURCE_POS);
    }

    public BlockPos getTargetPos() {
        return entityData.get(TARGET_POS);
    }

    public boolean isForkliftTransfer() {
        return entityData.get(CARRIER_MODE) == FORKLIFT;
    }

    public boolean isPalletForkTransfer() {
        return entityData.get(CARRIER_MODE) == PALLET_FORK;
    }

    public boolean isStationaryPalletForkTransfer() {
        return entityData.get(CARRIER_MODE) == STATIONARY_PALLET_FORK;
    }

    public void attachToPalletFork(AbstractContraptionEntity contraption,
                                   BlockPos localForkPos, BlockPos localPalletPos) {
        entityData.set(ARM_POS, localForkPos.immutable());
        entityData.set(SOURCE_POS, localPalletPos.immutable());
        entityData.set(TARGET_POS, localPalletPos.immutable());
        entityData.set(CARRIER_MODE, PALLET_FORK);
        entityData.set(CONTRAPTION_UUID, Optional.of(contraption.getUUID()));
        missingPalletForkControllerTicks = 0;
        Vec3 position = contraption.toGlobalVector(Vec3.atLowerCornerOf(localPalletPos), 1);
        setPos(position.x, position.y, position.z);
    }

    public boolean isForkliftPickedUp() {
        return entityData.get(FORKLIFT_PICKED_UP);
    }

    public boolean isForkliftReleased() {
        return entityData.get(FORKLIFT_RELEASED);
    }

    public float getForkliftAnimationProgress(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks) / ForkliftKinematics.TRAVEL_TICKS, 0, 1);
    }

    public Vec3 getForkliftRenderPosition(float partialTicks) {
        return getForkliftPosition(getForkliftAnimationProgress(partialTicks));
    }

    public float getForkliftRenderYaw(float partialTicks) {
        return ForkliftKinematics.getTransportedPalletYaw(
                getArmPos(), getSourcePos(), getTargetPos(),
                getForkliftAnimationProgress(partialTicks));
    }

    private Vec3 getForkliftPosition(float animationProgress) {
        return ForkliftKinematics.getTransportedPalletPosition(
                getArmPos(), getSourcePos(), getTargetPos(), animationProgress);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CARRIER, ItemStack.EMPTY);
        builder.define(ARM_POS, BlockPos.ZERO);
        builder.define(SOURCE_POS, BlockPos.ZERO);
        builder.define(TARGET_POS, BlockPos.ZERO);
        builder.define(CARRIER_MODE, STANDARD_ARM);
        builder.define(FORKLIFT_PICKED_UP, false);
        builder.define(FORKLIFT_RELEASED, false);
        builder.define(CONTRAPTION_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (isPalletForkTransfer()) {
            tickPalletForkTransfer();
            return;
        }
        if (isStationaryPalletForkTransfer()) {
            tickStationaryPalletForkTransfer();
            return;
        }
        if (isForkliftTransfer()) {
            tickForkliftTransfer();
            return;
        }

        if (level().isClientSide || tickCount < 2)
            return;

        if (level().getBlockEntity(getArmPos()) instanceof ArmBlockEntity arm
                && ArmPalletAccess.isHoldingPallet(arm))
            return;

        CompoundTag palletData = net.spindle.createwarehouse.item.custom.PalletCarrierItem
                .getPalletData(getCarrier());
        if (PalletBlock.placeTransported(level(), getSourcePos(), palletData))
            discard();
    }

    private void tickPalletForkTransfer() {
        if (level().isClientSide)
            return;

        AbstractContraptionEntity contraption = null;
        Optional<UUID> contraptionUuid = entityData.get(CONTRAPTION_UUID);
        if (level() instanceof ServerLevel serverLevel && contraptionUuid.isPresent()) {
            Entity candidate = serverLevel.getEntity(contraptionUuid.get());
            if (candidate instanceof AbstractContraptionEntity movingContraption
                    && movingContraption.isAlive()
                    && movingContraption.getContraption().getActorAt(getArmPos()) != null)
                contraption = movingContraption;
        }

        if (contraption != null) {
            missingPalletForkControllerTicks = 0;
            Vec3 position = contraption.toGlobalVector(
                    Vec3.atLowerCornerOf(getSourcePos()), 1);
            setPos(position.x, position.y, position.z);
            return;
        }

        if (++missingPalletForkControllerTicks <= 20)
            return;
        CompoundTag palletData = net.spindle.createwarehouse.item.custom.PalletCarrierItem
                .getPalletData(getCarrier());
        BlockPos destination = BlockPos.containing(position().add(.25, .25, .25));
        if (PalletBlock.placeTransported(level(), destination, palletData))
            discard();
    }

    private void tickStationaryPalletForkTransfer() {
        if (level().isClientSide)
            return;

        BlockPos forkPos = getArmPos();
        if (level().getBlockState(forkPos).is(ModBlocks.PALLET_FORK)) {
            missingPalletForkControllerTicks = 0;
            if (level().hasNeighborSignal(forkPos))
                tryPlaceCarriedPallet();
            return;
        }

        // During Create assembly the block disappears before the new moving
        // actor can adopt this entity. Keep it alive through that short gap.
        if (++missingPalletForkControllerTicks <= 20)
            return;
        tryPlaceCarriedPallet();
    }

    private boolean tryPlaceCarriedPallet() {
        CompoundTag palletData = net.spindle.createwarehouse.item.custom.PalletCarrierItem
                .getPalletData(getCarrier());
        BlockPos destination = BlockPos.containing(position().add(.25, .25, .25));
        if (!PalletBlock.placeTransported(level(), destination, palletData))
            return false;
        discard();
        return true;
    }

    private void tickForkliftTransfer() {
        float animationProgress = getForkliftAnimationProgress(0);
        Vec3 position = getForkliftPosition(animationProgress);
        setPos(position.x, position.y, position.z);

        if (level().isClientSide)
            return;

        if (!(level().getBlockEntity(getArmPos()) instanceof ForkliftBlockEntity)) {
            if (isForkliftReleased())
                discard();
            else if (isForkliftPickedUp())
                restoreAtSource();
            else
                discard();
            return;
        }

        if (!isForkliftPickedUp()) {
            if (animationProgress < ForkliftKinematics.INPUT_END)
                return;

            CompoundTag palletData = PalletBlock.removeForTransport(level(), getSourcePos());
            if (palletData == null) {
                discard();
                return;
            }
            entityData.set(CARRIER,
                    net.spindle.createwarehouse.item.custom.PalletCarrierItem.containing(palletData));
            entityData.set(FORKLIFT_PICKED_UP, true);
        }

        if (!isForkliftReleased() && animationProgress >= ForkliftKinematics.OUTPUT_END) {
            CompoundTag palletData = net.spindle.createwarehouse.item.custom.PalletCarrierItem
                    .getPalletData(getCarrier());
            if (PalletBlock.placeTransported(level(), getTargetPos(), palletData))
                entityData.set(FORKLIFT_RELEASED, true);
        }

        if (animationProgress >= 1 && isForkliftReleased())
            discard();
    }

    private void restoreAtSource() {
        CompoundTag palletData = net.spindle.createwarehouse.item.custom.PalletCarrierItem
                .getPalletData(getCarrier());
        if (PalletBlock.placeTransported(level(), getSourcePos(), palletData))
            discard();
    }

    public static void discardForArm(Level level, BlockPos armPos) {
        findForArm(level, armPos).forEach(Entity::discard);
    }

    public static boolean existsForController(Level level, BlockPos controllerPos) {
        return !findForController(level, controllerPos).isEmpty();
    }

    public static CarriedPalletEntity getForController(Level level, BlockPos controllerPos) {
        List<CarriedPalletEntity> entities = findForController(level, controllerPos);
        return entities.isEmpty() ? null : entities.getFirst();
    }

    public static boolean existsForPalletFork(Level level, UUID contraptionUuid,
                                               BlockPos localForkPos, Vec3 around) {
        return !findForPalletFork(level, contraptionUuid, localForkPos, around).isEmpty();
    }

    @Nullable
    public static CarriedPalletEntity getForPalletFork(Level level, UUID contraptionUuid,
                                                        BlockPos localForkPos, Vec3 around) {
        List<CarriedPalletEntity> entities = findForPalletFork(
                level, contraptionUuid, localForkPos, around);
        if (entities.isEmpty())
            return null;
        entities.stream().skip(1).forEach(Entity::discard);
        return entities.getFirst();
    }

    public static void discardForPalletFork(Level level, UUID contraptionUuid,
                                            BlockPos localForkPos, Vec3 around) {
        findForPalletFork(level, contraptionUuid, localForkPos, around)
                .forEach(Entity::discard);
    }

    public static void parkForPalletFork(Level level, UUID contraptionUuid,
                                         BlockPos localForkPos, Vec3 around,
                                         BlockPos stationaryForkPos, Vec3 palletPosition,
                                         ItemStack carrier, @Nullable UUID carriedEntityUuid) {
        List<CarriedPalletEntity> moving = findForPalletFork(
                level, contraptionUuid, localForkPos, around);
        CarriedPalletEntity entity = null;
        if (carriedEntityUuid != null && level instanceof ServerLevel serverLevel) {
            Entity tracked = serverLevel.getEntity(carriedEntityUuid);
            if (tracked instanceof CarriedPalletEntity carried && carried.isAlive())
                entity = carried;
        }
        if (entity == null && !moving.isEmpty())
            entity = moving.getFirst();

        boolean created;
        if (entity == null) {
            entity = ModEntityTypes.CARRIED_PALLET.get().create(level);
            if (entity == null)
                return;
            created = true;
        } else {
            created = false;
        }
        CarriedPalletEntity keptEntity = entity;
        moving.stream().filter(candidate -> candidate != keptEntity).forEach(Entity::discard);

        BlockPos palletPos = BlockPos.containing(palletPosition.add(.25, .25, .25));
        entity.entityData.set(CARRIER, carrier.copy());
        entity.entityData.set(ARM_POS, stationaryForkPos.immutable());
        entity.entityData.set(SOURCE_POS, palletPos);
        entity.entityData.set(TARGET_POS, palletPos);
        entity.entityData.set(CARRIER_MODE, STATIONARY_PALLET_FORK);
        entity.entityData.set(CONTRAPTION_UUID, Optional.empty());
        entity.missingPalletForkControllerTicks = 0;
        entity.setPos(palletPosition.x, palletPosition.y, palletPosition.z);
        if (created)
            level.addFreshEntity(entity);
    }

    @Nullable
    public static CarriedPalletEntity getForStationaryPalletFork(Level level, BlockPos forkPos) {
        AABB bounds = new AABB(forkPos).inflate(4);
        List<CarriedPalletEntity> entities = level.getEntities(
                ModEntityTypes.CARRIED_PALLET.get(), bounds,
                entity -> entity.isStationaryPalletForkTransfer()
                        && entity.getArmPos().equals(forkPos));
        if (entities.isEmpty())
            return null;
        entities.stream().skip(1).forEach(Entity::discard);
        return entities.getFirst();
    }

    private static List<CarriedPalletEntity> findForArm(Level level, BlockPos armPos) {
        AABB bounds = new AABB(armPos).inflate(1);
        return level.getEntities(ModEntityTypes.CARRIED_PALLET.get(), bounds,
                entity -> entity.getArmPos().equals(armPos));
    }

    private static List<CarriedPalletEntity> findForController(Level level, BlockPos controllerPos) {
        AABB bounds = new AABB(controllerPos).inflate(32);
        return level.getEntities(ModEntityTypes.CARRIED_PALLET.get(), bounds,
                entity -> entity.getArmPos().equals(controllerPos));
    }

    private static List<CarriedPalletEntity> findForPalletFork(Level level, UUID contraptionUuid,
                                                                BlockPos localForkPos, Vec3 around) {
        AABB bounds = new AABB(around.x - 8, around.y - 8, around.z - 8,
                around.x + 8, around.y + 8, around.z + 8);
        return level.getEntities(ModEntityTypes.CARRIED_PALLET.get(), bounds,
                entity -> entity.isPalletForkTransfer()
                        && entity.getArmPos().equals(localForkPos)
                        && entity.entityData.get(CONTRAPTION_UUID)
                        .filter(contraptionUuid::equals).isPresent());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(CARRIER, ItemStack.parseOptional(registryAccess(), tag.getCompound("Carrier")));
        entityData.set(ARM_POS, BlockPos.of(tag.getLong("ArmPos")));
        entityData.set(SOURCE_POS, BlockPos.of(tag.getLong("SourcePos")));
        entityData.set(TARGET_POS, tag.contains("TargetPos")
                ? BlockPos.of(tag.getLong("TargetPos")) : getSourcePos());
        entityData.set(CARRIER_MODE, tag.getInt("CarrierMode"));
        entityData.set(FORKLIFT_PICKED_UP, tag.contains("ForkliftPickedUp")
                ? tag.getBoolean("ForkliftPickedUp") : isForkliftTransfer());
        entityData.set(FORKLIFT_RELEASED, tag.getBoolean("ForkliftReleased"));
        entityData.set(CONTRAPTION_UUID, tag.hasUUID("ContraptionUuid")
                ? Optional.of(tag.getUUID("ContraptionUuid")) : Optional.empty());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Carrier", getCarrier().saveOptional(registryAccess()));
        tag.putLong("ArmPos", getArmPos().asLong());
        tag.putLong("SourcePos", getSourcePos().asLong());
        tag.putLong("TargetPos", getTargetPos().asLong());
        tag.putInt("CarrierMode", entityData.get(CARRIER_MODE));
        tag.putBoolean("ForkliftPickedUp", isForkliftPickedUp());
        tag.putBoolean("ForkliftReleased", isForkliftReleased());
        entityData.get(CONTRAPTION_UUID).ifPresent(uuid -> tag.putUUID("ContraptionUuid", uuid));
    }
}
