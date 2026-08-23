package net.spindle.createwarehouse.block.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.spindle.createwarehouse.block.ModBlocks;

public class DrumPackagerRenderer extends SafeBlockEntityRenderer<DrumPackagerBlockEntity> {
    public DrumPackagerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(DrumPackagerBlockEntity packager, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int light, int overlay) {
        if (packager.output.getStackInSlot(0).isEmpty())
            return;

        SuperByteBuffer drum = CachedBuffers.block(ModBlocks.FLUID_DRUM.getDefaultState());
        drum.translate(0, .25, 0)
                .light(light)
                .renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));
    }
}
