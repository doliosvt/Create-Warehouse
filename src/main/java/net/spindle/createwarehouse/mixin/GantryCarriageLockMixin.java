package net.spindle.createwarehouse.mixin;

import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GantryCarriageBlockEntity.class)
public abstract class GantryCarriageLockMixin {
    @Inject(method = "shouldAssemble", at = @At("HEAD"), cancellable = true)
    private void createWarehouse$keepManagedCarriageLocked(CallbackInfoReturnable<Boolean> callback) {
        GantryCarriageBlockEntity carriage = (GantryCarriageBlockEntity) (Object) this;
        Level level = carriage.getLevel();
        if (level == null)
            return;

        BlockState state = carriage.getBlockState();
        if (!(state.getBlock() instanceof GantryCarriageBlock))
            return;
        Direction facing = state.getValue(GantryCarriageBlock.FACING);
        BlockPos shaftPos = carriage.getBlockPos().relative(facing.getOpposite());
        if (GantryControllerBlockEntity.isManagedVerticalShaftLocked(level, shaftPos))
            callback.setReturnValue(false);
    }
}
