package net.spindle.createwarehouse.entity.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.compat.ArmPalletAccess;
import net.spindle.createwarehouse.compat.ArmPalletRenderAccess;
import net.spindle.createwarehouse.block.custom.PalletBlockEntity;
import net.spindle.createwarehouse.block.custom.PalletRenderer;
import net.spindle.createwarehouse.item.custom.PalletCarrierItem;

public class CarriedPalletRenderer extends EntityRenderer<CarriedPalletEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CarriedPalletRenderer(EntityRendererProvider.Context context) {
        super(context);
        blockRenderer = context.getBlockRenderDispatcher();
        shadowRadius = 0;
    }

    @Override
    public void render(CarriedPalletEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        if (entity.isForkliftTransfer()
                && (!entity.isForkliftPickedUp() || entity.isForkliftReleased())) {
            super.render(entity, yaw, partialTicks, poseStack, buffer, light);
            return;
        }

        poseStack.pushPose();
        if (entity.isForkliftTransfer()) {
            Vec3 interpolated = new Vec3(
                    Mth.lerp(partialTicks, entity.xOld, entity.getX()),
                    Mth.lerp(partialTicks, entity.yOld, entity.getY()),
                    Mth.lerp(partialTicks, entity.zOld, entity.getZ()));
            Vec3 exact = entity.getForkliftRenderPosition(partialTicks);
            poseStack.translate(exact.x - interpolated.x,
                    exact.y - interpolated.y, exact.z - interpolated.z);
            poseStack.translate(0, 0, 1);
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getForkliftRenderYaw(partialTicks)));
            poseStack.translate(0, 0, -1);
        } else if (entity.level().getBlockEntity(entity.getArmPos()) instanceof ArmBlockEntity arm
                && ArmPalletAccess.isHoldingPallet(arm) && !entity.isForkliftTransfer()) {
            ArmPalletRenderAccess.positionPalletAtClaw(poseStack, arm, partialTicks);
            poseStack.translate(0, -.25, -1);
        } else if (!entity.isForkliftTransfer()) {
            poseStack.translate(0, 0, -1);
        }

        blockRenderer.renderSingleBlock(ModBlocks.PALLET.getDefaultState(), poseStack, buffer,
                light, OverlayTexture.NO_OVERLAY);
        NonNullList<ItemStack> cargo = PalletBlockEntity.readTransportedCargo(
                PalletCarrierItem.getPalletData(entity.getCarrier()), entity.registryAccess());
        PalletRenderer.renderCargo(cargo::get, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CarriedPalletEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
