package net.spindle.createwarehouse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlock;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.block.custom.GantryAddress;
import net.spindle.createwarehouse.block.custom.GantryControlsBlockEntity;
import net.spindle.createwarehouse.block.custom.GantryControlsMovement.GantryStopSelection;

public final class GantryControlsRenderer {
    private GantryControlsRenderer() {}

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
            ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (!(context.temporaryData instanceof GantryStopSelection selection))
            return;
        if (!ModBlocks.GANTRY_CONTROLS.has(context.state))
            return;

        float buttonDepth = 0;
        if (context.contraption.getBlockEntityClientSide(context.localPos)
                instanceof GantryControlsBlockEntity controls)
            buttonDepth = -1 / 24f * controls.button.getValue(AnimationTickHolder.getPartialTicks(renderWorld));

        PoseStack poses = matrices.getViewProjection();
        var transform = TransformStack.of(poses);
        Direction facing = context.state.getValue(ContraptionControlsBlock.FACING);
        int light = LevelRenderer.getLightColor(renderWorld, context.localPos);

        poses.pushPose();
        transform.translate(context.localPos);
        poses.translate(0, buttonDepth, 0);
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        CachedBuffers.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_BUTTON, context.state, facing.getOpposite())
                .light(light)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(poses, solid);
        poses.popPose();

        Entity camera = Minecraft.getInstance().getCameraEntity();
        float distance = (float) (context.position == null || camera == null
                ? 0
                : context.position.distanceToSqr(camera.getEyePosition()));
        if (distance >= 100)
            return;

        String shortName = GantryAddress.format(selection.currentIndex, selection.levelIndex);
        String description = Component.translatable(
                "gui.create_warehouse.gantry_controls.address_selected", shortName).getString();
        Font font = Minecraft.getInstance().font;

        poses.pushPose();
        transform.translate(context.localPos);
        transform.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing)), Direction.UP);
        poses.translate(.4f, 1 + 2 / 16f, .5f);
        transform.rotate(AngleHelper.rad(67.5f), Direction.WEST);
        drawText(poses, buffer, font, shortName, .15f, buttonDepth, 12, 0xF3E9D2);
        if (distance < 20)
            drawText(poses, buffer, font, description, .06f, buttonDepth, 55, 0xF3E9D2);
        poses.popPose();
    }

    private static void drawText(PoseStack poses, MultiBufferSource buffer, Font font, String text,
            float y, float buttonDepth, int minimumWidth, int color) {
        int actualWidth = font.width(text);
        int width = Math.max(actualWidth, minimumWidth);
        float scale = 1 / (5f * (width - .5f));
        float heightCentering = (width - 8f) / 2;

        poses.pushPose();
        poses.translate(0, y, buttonDepth - .25f);
        poses.scale(scale, -scale, scale);
        poses.translate((float) Math.max(0, width - actualWidth) / 2, heightCentering, 0);
        NixieTubeRenderer.drawInWorldString(poses, buffer, text, color);
        poses.translate(.5f, .5f, -1 / 16f);
        NixieTubeRenderer.drawInWorldString(poses, buffer, text, Color.mixColors(color, 0, .35f));
        poses.popPose();
    }
}
