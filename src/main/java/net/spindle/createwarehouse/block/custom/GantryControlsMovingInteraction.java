package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.spindle.createwarehouse.client.GantryAddressScreen;

public class GantryControlsMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
            AbstractContraptionEntity contraptionEntity) {
        if (!(contraptionEntity instanceof GantryContraptionEntity gantryEntity))
            return false;

        MovementContext context = getContext(gantryEntity, localPos);
        if (context == null)
            return false;

        BlockPos soundPos = BlockPos.containing(
                contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1));
        if (!contraptionEntity.level().isClientSide()) {
            AllSoundEvents.CONTROLLER_CLICK.play(contraptionEntity.level(), null, soundPos, 1, 1.5f);
            return true;
        }

        if (gantryEntity.getContraption().getBlockEntityClientSide(localPos)
                instanceof GantryControlsBlockEntity controls)
            controls.pressButton();
        CatnipServices.PLATFORM.executeOnClientOnly(
                () -> () -> GantryAddressScreen.openMoving(gantryEntity.getId(), localPos, context));
        return true;
    }

    private static MovementContext getContext(GantryContraptionEntity entity, BlockPos localPos) {
        var actor = entity.getContraption().getActorAt(localPos);
        return actor == null ? null : actor.right;
    }
}
