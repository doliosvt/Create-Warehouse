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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.spindle.createwarehouse.CreateWarehouse;

public class CratePackagerRenderer extends SafeBlockEntityRenderer<CratePackagerBlockEntity> {
    private static final PartialModel CRATE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateWarehouse.MODID, "item/crate_item"));

    public CratePackagerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(CratePackagerBlockEntity packager, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int light, int overlay) {
        ItemStack renderedBox = packager.getRenderedBox();
        if (renderedBox.isEmpty())
            return;

        SuperByteBuffer crate = CachedBuffers.partial(CRATE, Blocks.AIR.defaultBlockState());
        crate.translate(0, .25, 0)
                .light(light)
                .renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));
    }
}
