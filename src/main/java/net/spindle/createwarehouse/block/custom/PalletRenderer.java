package net.spindle.createwarehouse.block.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import com.mojang.math.Axis;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.item.custom.FluidDrumItem;

public class PalletRenderer extends SafeBlockEntityRenderer<PalletBlockEntity> {
    private static final PartialModel CRATE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateWarehouse.MODID, "item/crate_item"));

    // Viewed from above: lower-left, upper-left, upper-right, lower-right.
    private static final double[][] CLOCKWISE_OFFSETS = {
            {-.875, .875}, {-.875, .125}, {-.125, .125}, {-.125, .875}
    };

    public PalletRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(PalletBlockEntity pallet, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int light, int overlay) {
        for (int slot = 0; slot < PalletBlockEntity.CAPACITY; slot++) {
            var cargo = pallet.getCrate(slot);
            if (cargo.isEmpty())
                continue;

            double[] offset = CLOCKWISE_OFFSETS[slot % CLOCKWISE_OFFSETS.length];
            int layer = slot / CLOCKWISE_OFFSETS.length;
            double y = .25 + layer * .75;
            if (FluidDrumItem.isDrum(cargo)) {
                SuperByteBuffer renderedDrum = CachedBuffers.block(ModBlocks.FLUID_DRUM.getDefaultState());
                poseStack.pushPose();
                poseStack.translate(offset[0] + .5, y + .5, offset[1] + .5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.translate(-.5, -.5, -.5);
                renderedDrum.light(light)
                        .nudge(slot)
                        .renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));
                poseStack.popPose();
                continue;
            }

            CachedBuffers.partial(CRATE, Blocks.AIR.defaultBlockState())
                    .translate(offset[0], y, offset[1])
                    .light(light)
                    .nudge(slot)
                    .renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));
        }
    }
}
