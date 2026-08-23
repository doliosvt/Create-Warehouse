package net.spindle.createwarehouse.block.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.spindle.createwarehouse.client.ModPartialModels;
import net.spindle.createwarehouse.entity.custom.CarriedPalletEntity;
import net.spindle.createwarehouse.entity.custom.ForkliftKinematics;

public class ForkliftRenderer extends SafeBlockEntityRenderer<ForkliftBlockEntity> {
    private static final PartialModel COG = ModPartialModels.FORKLIFT_COG;
    private static final PartialModel BASE = ModPartialModels.FORKLIFT_BASE;
    private static final PartialModel LOWER_BODY = ModPartialModels.FORKLIFT_LOWER_BODY;
    private static final PartialModel UPPER_BODY = ModPartialModels.FORKLIFT_UPPER_BODY;
    private static final PartialModel FORK = ModPartialModels.FORKLIFT_FORK;

    public ForkliftRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(ForkliftBlockEntity forklift, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int light, int overlay) {
        CarriedPalletEntity carried = forklift.getLevel() == null ? null
                : CarriedPalletEntity.getForController(forklift.getLevel(), forklift.getBlockPos());

        float yaw = 0;
        float lowerAngle = ForkliftKinematics.REST_LOWER_ANGLE;
        float upperAngle = ForkliftKinematics.REST_UPPER_ANGLE;
        float forkAngle = ForkliftKinematics.REST_FORK_ANGLE;
        float cogAngle = 0;

        if (carried != null && carried.isForkliftTransfer()) {
            float animationProgress = carried.getForkliftAnimationProgress(partialTicks);
            ForkliftKinematics.ArmPose animationPose = ForkliftKinematics.getPose(
                    forklift.getBlockPos(), carried.getSourcePos(), carried.getTargetPos(),
                    animationProgress);
            yaw = animationPose.yaw();
            lowerAngle = animationPose.lowerAngle();
            upperAngle = animationPose.upperAngle();
            forkAngle = animationPose.forkAngle();
            cogAngle = animationProgress * 720;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        poseStack.pushPose();
        poseStack.translate(0, .375, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(cogAngle));
        render(COG, forklift, poseStack, buffer, light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, .375, 0);
        render(BASE, forklift, poseStack, buffer, light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, .5, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(lowerAngle));
        render(LOWER_BODY, forklift, poseStack, buffer, light);
        poseStack.popPose();

        double lowerRadians = Math.toRadians(lowerAngle);
        double jointY = .5 - Math.sin(lowerRadians) * ForkliftKinematics.LOWER_ARM_LENGTH;
        double jointZ = Math.cos(lowerRadians) * ForkliftKinematics.LOWER_ARM_LENGTH;

        poseStack.pushPose();
        poseStack.translate(0, jointY, jointZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(upperAngle));
        render(UPPER_BODY, forklift, poseStack, buffer, light);
        poseStack.popPose();

        double upperRadians = Math.toRadians(upperAngle);
        double forkY = jointY - Math.sin(upperRadians) * ForkliftKinematics.UPPER_ARM_LENGTH;
        double forkZ = jointZ + Math.cos(upperRadians) * ForkliftKinematics.UPPER_ARM_LENGTH;

        poseStack.pushPose();
        poseStack.translate(0, forkY, forkZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(forkAngle));
        render(FORK, forklift, poseStack, buffer, light);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void render(PartialModel model, ForkliftBlockEntity forklift, PoseStack poseStack,
                               MultiBufferSource buffer, int light) {
        CachedBuffers.partial(model, forklift.getBlockState())
                .light(light)
                .renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));
    }
}
