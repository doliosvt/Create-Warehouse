package net.spindle.createwarehouse.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.spindle.createwarehouse.mixin.ArmBlockEntityAccessor;
import org.joml.Vector4f;

public final class ArmPalletRenderAccess {
    private ArmPalletRenderAccess() {}

    public static void positionPalletAtClaw(PoseStack poseStack, ArmBlockEntity arm, float partialTicks) {
        PoseStack calculation = new PoseStack();
        TransformStack transform = TransformStack.of(calculation);
        transform.center();

        if (arm.getBlockState().getValue(ArmBlock.CEILING))
            transform.rotateXDegrees(180);

        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        float baseAngle = accessor.createWarehouse$getBaseAngle().getValue(partialTicks);
        ArmRenderer.transformBase(transform, baseAngle);
        ArmRenderer.transformLowerArm(transform,
                accessor.createWarehouse$getLowerArmAngle().getValue(partialTicks) - 135);
        ArmRenderer.transformUpperArm(transform,
                accessor.createWarehouse$getUpperArmAngle().getValue(partialTicks) - 90);
        ArmRenderer.transformHead(transform,
                accessor.createWarehouse$getHeadAngle().getValue(partialTicks));

        Vector4f claw = new Vector4f(0, 0, 0, 1);
        calculation.last().pose().transform(claw);
        poseStack.translate(claw.x, claw.y, claw.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(baseAngle));
    }
}
