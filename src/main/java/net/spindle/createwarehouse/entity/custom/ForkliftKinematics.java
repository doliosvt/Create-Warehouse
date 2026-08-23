package net.spindle.createwarehouse.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Create-style arm movement: retract to a neutral pose, turn, then extend.
 * The carried pallet is derived from the rendered fork pose so both stay locked together.
 */
public final class ForkliftKinematics {
    public static final int TRAVEL_TICKS = 120;

    private static final float FOLD_FOR_INPUT_END = .10f;
    private static final float TURN_TO_INPUT_END = .20f;
    public static final float INPUT_END = .30f;
    private static final float RETRACT_END = .45f;
    private static final float TURN_TO_OUTPUT_END = .65f;
    public static final float OUTPUT_END = .80f;
    private static final float FOLD_AFTER_OUTPUT_END = .90f;
    private static final float RETURN_BASE_END = .95f;

    public static final double LOWER_ARM_LENGTH = 1.43755;
    public static final double UPPER_ARM_LENGTH = 1.8125;
    public static final float REST_LOWER_ANGLE = -45;
    public static final float REST_UPPER_ANGLE = 180;
    public static final float REST_FORK_ANGLE = 180;

    private static final float NEUTRAL_LOWER_ANGLE = -60;
    private static final float NEUTRAL_UPPER_ANGLE = -120;
    private static final double FORK_CENTER_OFFSET = .875;
    private static final double FORK_HEIGHT_OFFSET = .02;

    private static final ArmPose REST = new ArmPose(0, REST_LOWER_ANGLE,
            REST_UPPER_ANGLE, REST_FORK_ANGLE);

    private ForkliftKinematics() {}

    public static ArmPose getPose(BlockPos controller, BlockPos source, BlockPos target,
                                  float progress) {
        progress = Mth.clamp(progress, 0, 1);
        ArmPose sourcePose = getTargetPose(controller, source);
        ArmPose targetPose = getTargetPose(controller, target);
        ArmPose neutralAtRest = neutralPose(0);
        ArmPose neutralAtSource = neutralPose(sourcePose.yaw);
        ArmPose neutralAtTarget = neutralPose(targetPose.yaw);

        if (progress < FOLD_FOR_INPUT_END)
            return interpolate(REST, neutralAtRest,
                    ease(progress / FOLD_FOR_INPUT_END));
        if (progress < TURN_TO_INPUT_END)
            return interpolate(neutralAtRest, neutralAtSource,
                    ease((progress - FOLD_FOR_INPUT_END)
                            / (TURN_TO_INPUT_END - FOLD_FOR_INPUT_END)));
        if (progress < INPUT_END)
            return interpolate(neutralAtSource, sourcePose,
                    ease((progress - TURN_TO_INPUT_END) / (INPUT_END - TURN_TO_INPUT_END)));
        if (progress < RETRACT_END)
            return interpolate(sourcePose, neutralAtSource,
                    ease((progress - INPUT_END) / (RETRACT_END - INPUT_END)));
        if (progress < TURN_TO_OUTPUT_END)
            return interpolate(neutralAtSource, neutralAtTarget,
                    ease((progress - RETRACT_END) / (TURN_TO_OUTPUT_END - RETRACT_END)));
        if (progress < OUTPUT_END)
            return interpolate(neutralAtTarget, targetPose,
                    ease((progress - TURN_TO_OUTPUT_END) / (OUTPUT_END - TURN_TO_OUTPUT_END)));
        if (progress < FOLD_AFTER_OUTPUT_END)
            return interpolate(targetPose, neutralAtTarget,
                    ease((progress - OUTPUT_END) / (FOLD_AFTER_OUTPUT_END - OUTPUT_END)));
        if (progress < RETURN_BASE_END)
            return interpolate(neutralAtTarget, neutralAtRest,
                    ease((progress - FOLD_AFTER_OUTPUT_END)
                            / (RETURN_BASE_END - FOLD_AFTER_OUTPUT_END)));
        return interpolate(neutralAtRest, REST,
                ease((progress - RETURN_BASE_END) / (1 - RETURN_BASE_END)));
    }

    public static Vec3 getTransportedPalletPosition(BlockPos controller, BlockPos source,
                                                     BlockPos target, float progress) {
        progress = Mth.clamp(progress, 0, 1);
        if (progress <= INPUT_END)
            return Vec3.atLowerCornerOf(source);
        if (progress >= OUTPUT_END)
            return Vec3.atLowerCornerOf(target);

        ArmPose pose = getPose(controller, source, target, progress);
        Vec3 support = getForkSupportPosition(controller, pose);
        return support.add(0, -FORK_HEIGHT_OFFSET, -1);
    }

    public static float getTransportedPalletYaw(BlockPos controller, BlockPos source,
                                                BlockPos target, float progress) {
        progress = Mth.clamp(progress, 0, 1);
        if (progress <= INPUT_END)
            return 0;

        ArmPose sourcePose = getTargetPose(controller, source);
        ArmPose currentPose = getPose(controller, source, target,
                Math.min(progress, OUTPUT_END));
        return Mth.wrapDegrees(currentPose.yaw - sourcePose.yaw);
    }

    private static ArmPose neutralPose(float yaw) {
        return new ArmPose(yaw, NEUTRAL_LOWER_ANGLE,
                NEUTRAL_UPPER_ANGLE, REST_FORK_ANGLE);
    }

    private static ArmPose getTargetPose(BlockPos controller, BlockPos pallet) {
        double dx = pallet.getX() - controller.getX();
        double dz = pallet.getZ() + 1 - controller.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double horizontalDistance = Math.max(.05, distance - FORK_CENTER_OFFSET);
        double height = pallet.getY() + FORK_HEIGHT_OFFSET - controller.getY() - .5;
        ArmAngles angles = solveArm(horizontalDistance, height);
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dx, dz)) + 180);
        float lower = Mth.wrapDegrees(180 - angles.lower);
        float upper = Mth.wrapDegrees(180 - angles.upper);
        return new ArmPose(yaw, lower, upper, REST_FORK_ANGLE);
    }

    private static ArmAngles solveArm(double horizontalDistance, double height) {
        double minimumReach = Math.abs(UPPER_ARM_LENGTH - LOWER_ARM_LENGTH) + .05;
        double maximumReach = LOWER_ARM_LENGTH + UPPER_ARM_LENGTH - .05;
        double distance = Math.sqrt(horizontalDistance * horizontalDistance + height * height);
        if (distance < minimumReach || distance > maximumReach) {
            double clamped = Mth.clamp(distance, minimumReach, maximumReach);
            double scale = distance < 1.0e-4 ? 1 : clamped / distance;
            horizontalDistance *= scale;
            height *= scale;
        }

        double elbowCosine = (horizontalDistance * horizontalDistance + height * height
                - LOWER_ARM_LENGTH * LOWER_ARM_LENGTH - UPPER_ARM_LENGTH * UPPER_ARM_LENGTH)
                / (2 * LOWER_ARM_LENGTH * UPPER_ARM_LENGTH);
        double elbow = -Math.acos(Mth.clamp(elbowCosine, -1, 1));
        double shoulder = Math.atan2(height, horizontalDistance)
                - Math.atan2(UPPER_ARM_LENGTH * Math.sin(elbow),
                LOWER_ARM_LENGTH + UPPER_ARM_LENGTH * Math.cos(elbow));
        return new ArmAngles((float) -Math.toDegrees(shoulder),
                (float) -Math.toDegrees(shoulder + elbow));
    }

    private static Vec3 getForkSupportPosition(BlockPos controller, ArmPose pose) {
        double lowerRadians = Math.toRadians(pose.lowerAngle);
        double upperRadians = Math.toRadians(pose.upperAngle);
        double forkRadians = Math.toRadians(pose.forkAngle);
        double yawRadians = Math.toRadians(pose.yaw);

        double forkY = .5
                - Math.sin(lowerRadians) * LOWER_ARM_LENGTH
                - Math.sin(upperRadians) * UPPER_ARM_LENGTH;
        double forkZ = Math.cos(lowerRadians) * LOWER_ARM_LENGTH
                + Math.cos(upperRadians) * UPPER_ARM_LENGTH;
        double supportY = forkY - Math.sin(forkRadians) * FORK_CENTER_OFFSET;
        double supportZ = forkZ + Math.cos(forkRadians) * FORK_CENTER_OFFSET;

        return new Vec3(
                controller.getX() + Math.sin(yawRadians) * supportZ,
                controller.getY() + supportY,
                controller.getZ() + Math.cos(yawRadians) * supportZ);
    }

    private static ArmPose interpolate(ArmPose from, ArmPose to, float progress) {
        return new ArmPose(
                angleLerp(from.yaw, to.yaw, progress),
                angleLerp(from.lowerAngle, to.lowerAngle, progress),
                angleLerp(from.upperAngle, to.upperAngle, progress),
                angleLerp(from.forkAngle, to.forkAngle, progress));
    }

    private static float angleLerp(float from, float to, float progress) {
        return from + Mth.wrapDegrees(to - from) * progress;
    }

    private static float ease(float progress) {
        progress = Mth.clamp(progress, 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    public record ArmPose(float yaw, float lowerAngle, float upperAngle, float forkAngle) {}

    private record ArmAngles(float lower, float upper) {}
}
