package net.spindle.createwarehouse.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.spindle.createwarehouse.block.custom.ForkliftBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmPlacementPacket.class)
public abstract class ArmPlacementPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void createWarehouse$configureForklift(ServerPlayer player, CallbackInfo callback) {
        ArmPlacementPacket packet = (ArmPlacementPacket) (Object) this;
        if (!(player.level().getBlockEntity(packet.pos()) instanceof ForkliftBlockEntity forklift))
            return;

        boolean configured = forklift.configureFromInteractionPoints(packet.tag());
        player.displayClientMessage(Component.translatable(configured
                ? "message.create_warehouse.forklift.configured"
                : "message.create_warehouse.forklift.invalid_configuration"), true);
        callback.cancel();
    }
}
