package net.spindle.createwarehouse.block.custom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.TickPriority;
import net.spindle.createwarehouse.entity.custom.CarriedPalletEntity;

/** Controls a line of Create gantry shafts in the same way an elevator pulley controls a column. */
public class GantryControllerBlockEntity extends SplitShaftBlockEntity {
    private static final int MAX_SHAFT_LENGTH = 256;
    private static final int CONTACT_SEARCH_RADIUS = 8;

    private int targetCoordinate;
    private boolean targetAvailable;
    private boolean arrived;
    private int outputModifier;
    private BlockPos activeContact;
    private int targetLevelIndex;
    private boolean verticalStage;
    private boolean verticalArrived;
    private CycleStage cycleStage = CycleStage.IDLE;
    private double toolTravel;
    private double toolRetractedTravel;

    public GantryControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        arrived = true;
        targetLevelIndex = -1;
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        Direction output = getBlockState().getValue(GantryControllerBlock.FACING);
        if (face == output)
            return outputModifier;
        return 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide())
            return;

        updateMovement();
    }

    public void setTarget(int coordinate, BlockPos contactPos) {
        setTarget(coordinate, contactPos, -1);
    }

    public void setTarget(int coordinate, BlockPos contactPos, int levelIndex) {
        if (level == null || level.isClientSide())
            return;

        if (activeContact != null && !activeContact.equals(contactPos))
            GantryContactBlock.setCalling(level, activeContact, false);
        unlockVerticalLine();
        setLineLocked(false);
        targetCoordinate = coordinate;
        targetLevelIndex = Mth.clamp(levelIndex, -1, 255);
        targetAvailable = true;
        arrived = false;
        verticalArrived = false;
        verticalStage = false;
        cycleStage = CycleStage.HORIZONTAL_TARGET;
        toolTravel = 0;
        toolRetractedTravel = 0;
        activeContact = contactPos.immutable();
        GantryContactBlock.setCalling(level, activeContact, true);
        setChanged();
        sendData();
        updateMovement();
    }

    public boolean hasTarget() {
        return targetAvailable;
    }

    public int getTargetCoordinate() {
        return targetCoordinate;
    }

    private void updateMovement() {
        ShaftLine line = getShaftLine();
        if (line == null) {
            setOutputModifier(0);
            return;
        }

        switch (cycleStage) {
            case IDLE -> {
                setLineLocked(false);
                setOutputModifier(isShaftLinePowered(line) ? 1 : 0);
            }
            case HORIZONTAL_TARGET -> updateHorizontalMovement(line, false);
            case VERTICAL_TARGET -> updateVerticalMovement(line, false);
            case TOOL_EXTENDING, TOOL_RETRACTING -> updateToolMovement(line);
            case VERTICAL_HOME -> updateVerticalMovement(line, true);
            case HORIZONTAL_HOME -> updateHorizontalMovement(line, true);
        }
    }

    private void updateHorizontalMovement(ShaftLine line, boolean returningHome) {
        setLineLocked(false);
        int destination = targetCoordinate;
        GantryStop homeStop = null;
        if (returningHome) {
            homeStop = findHomeStop(line);
            if (homeStop == null) {
                setOutputModifier(0);
                return;
            }
            destination = homeStop.coordinate();
        }

        Double carriageCoordinate = findCarriageCoordinate(line);
        if (carriageCoordinate == null) {
            setOutputModifier(0);
            return;
        }

        double difference = destination - carriageCoordinate;
        if (Math.abs(difference) < .5) {
            setOutputModifier(0);
            if (returningHome) {
                completeCycle(homeStop);
                return;
            }

            if (!arrived && activeContact != null)
                GantryContactBlock.pulse(level, activeContact);
            arrived = true;
            if (targetLevelIndex >= 0) {
                verticalStage = true;
                cycleStage = CycleStage.VERTICAL_TARGET;
                setLineLocked(true);
            } else {
                cycleStage = CycleStage.IDLE;
            }
            setChanged();
            sendData();
            return;
        }

        arrived = false;
        float inputSpeed = getSpeed();
        if (inputSpeed == 0) {
            setOutputModifier(0);
            return;
        }

        int desiredCoordinateSign = difference > 0 ? 1 : -1;
        int shaftFacingSign = line.shaftFacing().getAxisDirection().getStep();
        int inputSign = inputSpeed > 0 ? 1 : -1;
        setOutputModifier(-desiredCoordinateSign * shaftFacingSign * inputSign);
    }

    private void updateVerticalMovement(ShaftLine horizontalLine, boolean returningHome) {
        if (horizontalLine == null) {
            setOutputModifier(0);
            return;
        }

        setLineLocked(true);
        VerticalSystem verticalSystem = findVerticalSystem(horizontalLine);
        if (verticalSystem == null) {
            setOutputModifier(0);
            return;
        }

        setVerticalLineLocked(verticalSystem.shaftLine(), false);

        List<GantryLevel> levels = createVerticalLevels(verticalSystem.shaftLine());
        if (levels.isEmpty()) {
            setOutputModifier(0);
            return;
        }

        int selectedLevel = returningHome ? 0 : Mth.clamp(targetLevelIndex, 0, levels.size() - 1);
        GantryLevel targetLevel = levels.get(selectedLevel);

        Double carriageCoordinate = findCarriageCoordinate(verticalSystem.shaftLine());
        if (carriageCoordinate == null) {
            setOutputModifier(0);
            return;
        }

        double difference = targetLevel.coordinate() - carriageCoordinate;
        if (Math.abs(difference) < .5) {
            setOutputModifier(0);
            verticalArrived = true;
            if (returningHome) {
                verticalStage = false;
                setLineLocked(false);
                cycleStage = CycleStage.HORIZONTAL_HOME;
            } else {
                setVerticalLineLocked(verticalSystem.shaftLine(), true);
                toolTravel = 0;
                toolRetractedTravel = 0;
                cycleStage = CycleStage.TOOL_EXTENDING;
            }
            setChanged();
            sendData();
            return;
        }

        verticalArrived = false;
        float inputSpeed = getSpeed();
        if (inputSpeed == 0) {
            setOutputModifier(0);
            return;
        }

        int desiredCoordinateSign = difference > 0 ? 1 : -1;
        int shaftFacingSign = verticalSystem.shaftLine().shaftFacing().getAxisDirection().getStep();
        int inputSign = inputSpeed > 0 ? 1 : -1;
        int transferSign = verticalSystem.transferModifier() > 0 ? 1 : -1;
        setOutputModifier(-desiredCoordinateSign * shaftFacingSign * inputSign * transferSign);
    }

    private void updateToolMovement(ShaftLine horizontalLine) {
        setLineLocked(true);
        VerticalSystem verticalSystem = findVerticalSystem(horizontalLine);
        if (verticalSystem == null) {
            setOutputModifier(0);
            return;
        }

        setVerticalLineLocked(verticalSystem.shaftLine(), true);
        if (cycleStage == CycleStage.TOOL_EXTENDING) {
            if (hasCarriedPalletNear(verticalSystem.shaftLine())) {
                cycleStage = CycleStage.TOOL_RETRACTING;
                toolRetractedTravel = 0;
                setOutputModifier(-1);
                setChanged();
                sendData();
                return;
            }

            setOutputModifier(1);
            toolTravel += Math.abs(getSpeed());
            if (toolTravel > 1_000_000)
                toolTravel = 1_000_000;
            return;
        }

        setOutputModifier(-1);
        toolRetractedTravel += Math.abs(getSpeed());
        if (toolRetractedTravel + .001 < toolTravel)
            return;

        setOutputModifier(0);
        setVerticalLineLocked(verticalSystem.shaftLine(), false);
        cycleStage = CycleStage.VERTICAL_HOME;
        verticalArrived = false;
        setChanged();
        sendData();
    }

    private boolean hasCarriedPalletNear(ShaftLine verticalLine) {
        BlockPos first = verticalLine.shafts().getFirst();
        BlockPos last = verticalLine.shafts().getLast();
        AABB searchBox = new AABB(
                Math.min(first.getX(), last.getX()), Math.min(first.getY(), last.getY()),
                Math.min(first.getZ(), last.getZ()), Math.max(first.getX(), last.getX()) + 1,
                Math.max(first.getY(), last.getY()) + 1, Math.max(first.getZ(), last.getZ()) + 1)
                .inflate(32);
        return !level.getEntitiesOfClass(CarriedPalletEntity.class, searchBox,
                CarriedPalletEntity::isPalletForkTransfer).isEmpty();
    }

    private GantryStop findHomeStop(ShaftLine line) {
        List<GantryStop> stops = findStopsOnLine(level, line.shafts().getFirst(), line.axis());
        return stops.isEmpty() ? null : stops.getFirst();
    }

    private void completeCycle(GantryStop homeStop) {
        setOutputModifier(0);
        setLineLocked(false);
        if (activeContact != null)
            GantryContactBlock.setCalling(level, activeContact, false);
        if (homeStop != null)
            GantryContactBlock.pulse(level, homeStop.contactPos());
        targetCoordinate = homeStop == null ? targetCoordinate : homeStop.coordinate();
        targetLevelIndex = 0;
        targetAvailable = false;
        arrived = true;
        verticalArrived = true;
        verticalStage = false;
        cycleStage = CycleStage.IDLE;
        activeContact = homeStop == null ? null : homeStop.contactPos();
        toolTravel = 0;
        toolRetractedTravel = 0;
        setChanged();
        sendData();
    }

    private VerticalSystem findVerticalSystem(ShaftLine horizontalLine) {
        for (BlockPos shaftPos : horizontalLine.shafts()) {
            for (Direction carriageDirection : Direction.values()) {
                if (carriageDirection.getAxis() == horizontalLine.axis())
                    continue;

                BlockPos carriagePos = shaftPos.relative(carriageDirection);
                BlockState carriageState = level.getBlockState(carriagePos);
                if (!AllBlocks.GANTRY_CARRIAGE.has(carriageState)
                        || carriageState.getValue(GantryCarriageBlock.FACING) != carriageDirection)
                    continue;
                if (!(carriageState.getBlock() instanceof GantryCarriageBlock carriageBlock)
                        || carriageBlock.getRotationAxis(carriageState) != Direction.Axis.Y)
                    continue;

                for (Direction verticalDirection : new Direction[] { Direction.UP, Direction.DOWN }) {
                    BlockPos verticalShaftPos = carriagePos.relative(verticalDirection);
                    BlockState verticalShaftState = level.getBlockState(verticalShaftPos);
                    if (!isShaftAlong(verticalShaftState, Direction.Axis.Y))
                        continue;

                    List<BlockPos> verticalShafts = collectShaftLine(level, verticalShaftPos, Direction.Axis.Y);
                    if (verticalShafts.isEmpty())
                        continue;
                    Direction shaftFacing = verticalShaftState.getValue(GantryShaftBlock.FACING);
                    float transferModifier = GantryCarriageBlockEntity.getGantryPinionModifier(
                            horizontalLine.shaftFacing(), carriageDirection);
                    return new VerticalSystem(
                            new ShaftLine(Direction.Axis.Y, shaftFacing, verticalShafts),
                            transferModifier);
                }
            }
        }
        return null;
    }

    private void setVerticalStage(boolean active) {
        if (verticalStage == active) {
            setLineLocked(active);
            return;
        }
        verticalStage = active;
        setLineLocked(active);
        setChanged();
        sendData();
    }

    private void setLineLocked(boolean locked) {
        if (level == null || level.isClientSide())
            return;
        BlockState state = getBlockState();
        if (!state.hasProperty(GantryControllerBlock.POWERING)
                || state.getValue(GantryControllerBlock.POWERING) == locked)
            return;
        level.setBlock(worldPosition, state.setValue(GantryControllerBlock.POWERING, locked), Block.UPDATE_ALL);
        level.updateNeighborsAt(worldPosition, state.getBlock());
    }

    private void setVerticalLineLocked(ShaftLine verticalLine, boolean locked) {
        if (level == null || level.isClientSide())
            return;
        for (BlockPos shaftPos : verticalLine.shafts()) {
            BlockState state = level.getBlockState(shaftPos);
            if (!isShaftAlong(state, Direction.Axis.Y))
                continue;
            boolean shouldBePowered = locked || level.hasNeighborSignal(shaftPos);
            if (state.getValue(GantryShaftBlock.POWERED) == shouldBePowered)
                continue;
            level.setBlock(shaftPos, state.setValue(GantryShaftBlock.POWERED, shouldBePowered), Block.UPDATE_ALL);
        }
    }

    private void unlockVerticalLine() {
        if (level == null || level.isClientSide())
            return;
        ShaftLine horizontalLine = getShaftLine();
        if (horizontalLine == null)
            return;
        VerticalSystem verticalSystem = findVerticalSystem(horizontalLine);
        if (verticalSystem != null)
            setVerticalLineLocked(verticalSystem.shaftLine(), false);
    }

    private boolean isShaftLinePowered(ShaftLine line) {
        for (BlockPos shaftPos : line.shafts()) {
            BlockState state = level.getBlockState(shaftPos);
            if (isShaftAlong(state, line.axis()) && state.getValue(GantryShaftBlock.POWERED))
                return true;
        }
        return false;
    }

    private void setOutputModifier(int modifier) {
        modifier = Mth.clamp(modifier, -1, 1);
        if (modifier == outputModifier)
            return;

        outputModifier = modifier;
        setChanged();
        sendData();

        if (level == null || level.isClientSide())
            return;

        RotationPropagator.handleRemoved(level, worldPosition, this);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 1, TickPriority.EXTREMELY_HIGH);
    }

    private ShaftLine getShaftLine() {
        Direction output = getBlockState().getValue(GantryControllerBlock.FACING);
        BlockPos first = worldPosition.relative(output);
        BlockState firstState = level.getBlockState(first);
        if (!isShaftAlong(firstState, output.getAxis()))
            return null;

        Direction shaftFacing = firstState.getValue(GantryShaftBlock.FACING);
        List<BlockPos> shafts = new ArrayList<>();
        BlockPos current = first;
        for (int i = 0; i < MAX_SHAFT_LENGTH; i++) {
            BlockState state = level.getBlockState(current);
            if (!isShaftAlong(state, output.getAxis()))
                break;
            shafts.add(current);
            current = current.relative(output);
        }
        return shafts.isEmpty() ? null : new ShaftLine(output.getAxis(), shaftFacing, shafts);
    }

    private Double findCarriageCoordinate(ShaftLine line) {
        BlockPos first = line.shafts().getFirst();
        BlockPos last = line.shafts().getLast();
        AABB searchBox = new AABB(
                Math.min(first.getX(), last.getX()) - 16,
                Math.min(first.getY(), last.getY()) - 16,
                Math.min(first.getZ(), last.getZ()) - 16,
                Math.max(first.getX(), last.getX()) + 17,
                Math.max(first.getY(), last.getY()) + 17,
                Math.max(first.getZ(), last.getZ()) + 17);

        for (GantryContraptionEntity entity : level.getEntitiesOfClass(GantryContraptionEntity.class, searchBox)) {
            if (!(entity.getContraption() instanceof GantryContraption contraption))
                continue;
            Direction carriageFacing = contraption.getFacing();
            if (carriageFacing.getAxis() == line.axis())
                continue;

            Vec3 anchor = entity.getAnchorVec();
            BlockPos shaftAtCarriage = BlockPos.containing(anchor).relative(carriageFacing.getOpposite());
            if (!isOnLine(shaftAtCarriage, first, line.axis()))
                continue;
            return entity.getAxisCoord();
        }

        for (BlockPos shaftPos : line.shafts()) {
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() == line.axis())
                    continue;
                BlockPos carriagePos = shaftPos.relative(direction);
                BlockState carriageState = level.getBlockState(carriagePos);
                if (!AllBlocks.GANTRY_CARRIAGE.has(carriageState))
                    continue;
                if (carriageState.getValue(GantryCarriageBlock.FACING) != direction)
                    continue;
                return (double) axisCoordinate(carriagePos, line.axis());
            }
        }

        return null;
    }

    private static boolean isOnLine(BlockPos pos, BlockPos linePos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getY() == linePos.getY() && pos.getZ() == linePos.getZ();
            case Y -> pos.getX() == linePos.getX() && pos.getZ() == linePos.getZ();
            case Z -> pos.getX() == linePos.getX() && pos.getY() == linePos.getY();
        };
    }

    private static int axisCoordinate(BlockPos pos, Direction.Axis axis) {
        return axis.choose(pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean isShaftAlong(BlockState state, Direction.Axis axis) {
        return AllBlocks.GANTRY_SHAFT.has(state)
                && state.getValue(GantryShaftBlock.FACING).getAxis() == axis;
    }

    /** Finds the shaft crossing a contact's station plane and calls the controller at either end. */
    public static boolean callFromContact(Level level, BlockPos contactPos) {
        return callFromContact(level, contactPos, -1);
    }

    public static boolean callFromContact(Level level, BlockPos contactPos, int levelIndex) {
        if (level.isClientSide())
            return false;

        ShaftCandidate candidate = findNearestHorizontalShaft(level, contactPos);
        if (candidate == null)
            return false;

        GantryControllerBlockEntity controller = findController(level, candidate.pos(), candidate.axis());
        if (controller == null)
            return false;

        controller.setTarget(axisCoordinate(candidate.pos(), candidate.axis()), contactPos, levelIndex);
        return true;
    }

    public static List<GantryLevel> findVerticalLevelsFromReference(Level level, BlockPos referencePos) {
        ShaftCandidate candidate = findNearestHorizontalShaft(level, referencePos);
        if (candidate == null)
            return List.of();
        GantryControllerBlockEntity controller = findController(level, candidate.pos(), candidate.axis());
        if (controller == null)
            return List.of();
        ShaftLine horizontalLine = controller.getShaftLine();
        if (horizontalLine == null)
            return List.of();
        VerticalSystem verticalSystem = controller.findVerticalSystem(horizontalLine);
        if (verticalSystem == null)
            return List.of();
        return createVerticalLevels(verticalSystem.shaftLine());
    }

    private static List<GantryLevel> createVerticalLevels(ShaftLine verticalLine) {
        if (verticalLine.axis() != Direction.Axis.Y || verticalLine.shafts().isEmpty())
            return List.of();

        int bottom = verticalLine.shafts().getFirst().getY();
        int top = verticalLine.shafts().getLast().getY();
        int count = Math.max(0, (top - bottom) / 2);
        if (count == 0)
            return List.of();

        List<GantryLevel> levels = new ArrayList<>(count);
        for (int number = 1; number <= count; number++)
            levels.add(new GantryLevel(bottom + number * 2));
        return levels;
    }

    public static List<GantryStop> findStopsFromReference(Level level, BlockPos referencePos) {
        ShaftCandidate candidate = findNearestHorizontalShaft(level, referencePos);
        if (candidate == null)
            return List.of();
        return findStopsOnLine(level, candidate.pos(), candidate.axis());
    }

    public static List<GantryStop> findStopsForContraption(Level level, GantryContraptionEntity entity) {
        if (!(entity.getContraption() instanceof GantryContraption contraption))
            return List.of();

        Direction carriageFacing = contraption.getFacing();
        BlockPos shaftPos = BlockPos.containing(entity.getAnchorVec()).relative(carriageFacing.getOpposite());
        BlockState shaftState = level.getBlockState(shaftPos);
        if (!AllBlocks.GANTRY_SHAFT.has(shaftState))
            return List.of();

        Direction.Axis axis = shaftState.getValue(GantryShaftBlock.FACING).getAxis();
        if (axis == Direction.Axis.Y)
            return List.of();
        return findStopsOnLine(level, shaftPos, axis);
    }

    private static List<GantryStop> findStopsOnLine(Level level, BlockPos shaftPos, Direction.Axis axis) {
        List<BlockPos> shafts = collectShaftLine(level, shaftPos, axis);
        if (shafts.isEmpty())
            return List.of();

        BlockPos lineReference = shafts.getFirst();
        Set<BlockPos> foundContacts = new HashSet<>();
        for (BlockPos currentShaft : shafts) {
            for (int first = -CONTACT_SEARCH_RADIUS; first <= CONTACT_SEARCH_RADIUS; first++) {
                for (int second = -CONTACT_SEARCH_RADIUS; second <= CONTACT_SEARCH_RADIUS; second++) {
                    BlockPos contactPos = switch (axis) {
                        case X -> currentShaft.offset(0, first, second);
                        case Y -> currentShaft.offset(first, 0, second);
                        case Z -> currentShaft.offset(first, second, 0);
                    };
                    if (!level.isLoaded(contactPos)
                            || !(level.getBlockState(contactPos).getBlock() instanceof GantryContactBlock))
                        continue;

                    ShaftCandidate nearest = findNearestShaft(level, contactPos, axis);
                    if (nearest == null || nearest.axis() != axis || !isOnLine(nearest.pos(), lineReference, axis))
                        continue;
                    foundContacts.add(contactPos.immutable());
                }
            }
        }

        GantryControllerBlockEntity controller = findController(level, shaftPos, axis);
        Comparator<GantryStop> ordering = Comparator.comparingInt(GantryStop::coordinate);
        if (controller != null) {
            int controllerCoordinate = axisCoordinate(controller.getBlockPos(), axis);
            ordering = Comparator.comparingInt(stop -> Math.abs(stop.coordinate() - controllerCoordinate));
        }

        return foundContacts.stream()
                .map(pos -> new GantryStop(axisCoordinate(pos, axis), pos))
                .sorted(ordering)
                .toList();
    }

    private static List<BlockPos> collectShaftLine(Level level, BlockPos shaftPos, Direction.Axis axis) {
        if (!isShaftAlong(level.getBlockState(shaftPos), axis))
            return List.of();

        BlockPos start = shaftPos;
        Direction negative = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        for (int i = 0; i < MAX_SHAFT_LENGTH - 1; i++) {
            BlockPos next = start.relative(negative);
            if (!isShaftAlong(level.getBlockState(next), axis))
                break;
            start = next;
        }

        List<BlockPos> shafts = new ArrayList<>();
        Direction positive = negative.getOpposite();
        BlockPos current = start;
        for (int i = 0; i < MAX_SHAFT_LENGTH; i++) {
            if (!isShaftAlong(level.getBlockState(current), axis))
                break;
            shafts.add(current);
            current = current.relative(positive);
        }
        return shafts;
    }

    private static ShaftCandidate findNearestHorizontalShaft(Level level, BlockPos contactPos) {
        ShaftCandidate x = findNearestShaft(level, contactPos, Direction.Axis.X);
        ShaftCandidate z = findNearestShaft(level, contactPos, Direction.Axis.Z);
        if (x == null)
            return z;
        if (z == null)
            return x;
        return squaredCrossSectionDistance(contactPos, x.pos(), x.axis())
                <= squaredCrossSectionDistance(contactPos, z.pos(), z.axis()) ? x : z;
    }

    private static ShaftCandidate findNearestShaft(Level level, BlockPos contactPos, Direction.Axis axis) {
        ShaftCandidate best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int first = -CONTACT_SEARCH_RADIUS; first <= CONTACT_SEARCH_RADIUS; first++) {
            for (int second = -CONTACT_SEARCH_RADIUS; second <= CONTACT_SEARCH_RADIUS; second++) {
                BlockPos candidatePos = switch (axis) {
                    case X -> contactPos.offset(0, first, second);
                    case Y -> contactPos.offset(first, 0, second);
                    case Z -> contactPos.offset(first, second, 0);
                };
                if (!level.isLoaded(candidatePos))
                    continue;
                if (!isShaftAlong(level.getBlockState(candidatePos), axis))
                    continue;

                int distance = first * first + second * second;
                if (distance >= bestDistance)
                    continue;
                best = new ShaftCandidate(candidatePos.immutable(), axis);
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int squaredCrossSectionDistance(BlockPos first, BlockPos second, Direction.Axis axis) {
        int dx = axis == Direction.Axis.X ? 0 : first.getX() - second.getX();
        int dy = axis == Direction.Axis.Y ? 0 : first.getY() - second.getY();
        int dz = axis == Direction.Axis.Z ? 0 : first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static GantryControllerBlockEntity findController(Level level, BlockPos shaftPos, Direction.Axis axis) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis)
                continue;

            BlockPos current = shaftPos;
            for (int i = 0; i < MAX_SHAFT_LENGTH; i++) {
                BlockPos next = current.relative(direction);
                BlockState nextState = level.getBlockState(next);
                if (isShaftAlong(nextState, axis)) {
                    current = next;
                    continue;
                }
                if (nextState.is(net.spindle.createwarehouse.block.ModBlocks.GANTRY_CONTROLLER)
                        && nextState.getValue(GantryControllerBlock.FACING) == direction.getOpposite()) {
                    BlockEntity blockEntity = level.getBlockEntity(next);
                    if (blockEntity instanceof GantryControllerBlockEntity controller)
                        return controller;
                }
                break;
            }
        }
        return null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("TargetAvailable", targetAvailable);
        tag.putInt("TargetCoordinate", targetCoordinate);
        tag.putBoolean("Arrived", arrived);
        tag.putInt("OutputModifier", outputModifier);
        tag.putInt("TargetLevelIndex", targetLevelIndex);
        tag.putBoolean("VerticalStage", verticalStage);
        tag.putBoolean("VerticalArrived", verticalArrived);
        tag.putInt("CycleStage", cycleStage.ordinal());
        tag.putDouble("ToolTravel", toolTravel);
        tag.putDouble("ToolRetractedTravel", toolRetractedTravel);
        if (activeContact != null) {
            tag.putInt("ContactX", activeContact.getX());
            tag.putInt("ContactY", activeContact.getY());
            tag.putInt("ContactZ", activeContact.getZ());
            tag.putBoolean("HasContact", true);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetAvailable = tag.getBoolean("TargetAvailable");
        targetCoordinate = tag.getInt("TargetCoordinate");
        arrived = tag.getBoolean("Arrived");
        outputModifier = Mth.clamp(tag.getInt("OutputModifier"), -1, 1);
        targetLevelIndex = tag.contains("TargetLevelIndex")
                ? Mth.clamp(tag.getInt("TargetLevelIndex"), -1, 255)
                : -1;
        verticalStage = tag.getBoolean("VerticalStage");
        verticalArrived = tag.getBoolean("VerticalArrived");
        if (tag.contains("CycleStage")) {
            int stage = Mth.clamp(tag.getInt("CycleStage"), 0, CycleStage.values().length - 1);
            cycleStage = CycleStage.values()[stage];
        } else if (verticalStage) {
            cycleStage = CycleStage.VERTICAL_TARGET;
        } else if (targetAvailable && !arrived) {
            cycleStage = CycleStage.HORIZONTAL_TARGET;
        } else {
            cycleStage = CycleStage.IDLE;
        }
        toolTravel = Math.max(0, tag.getDouble("ToolTravel"));
        toolRetractedTravel = Math.max(0, tag.getDouble("ToolRetractedTravel"));
        activeContact = tag.getBoolean("HasContact")
                ? new BlockPos(tag.getInt("ContactX"), tag.getInt("ContactY"), tag.getInt("ContactZ"))
                : null;
    }

    private record ShaftCandidate(BlockPos pos, Direction.Axis axis) {}

    private record ShaftLine(Direction.Axis axis, Direction shaftFacing, List<BlockPos> shafts) {}

    private record VerticalSystem(ShaftLine shaftLine, float transferModifier) {}

    private enum CycleStage {
        IDLE,
        HORIZONTAL_TARGET,
        VERTICAL_TARGET,
        TOOL_EXTENDING,
        TOOL_RETRACTING,
        VERTICAL_HOME,
        HORIZONTAL_HOME
    }

    public record GantryStop(int coordinate, BlockPos contactPos) {}

    public record GantryLevel(int coordinate) {}
}
