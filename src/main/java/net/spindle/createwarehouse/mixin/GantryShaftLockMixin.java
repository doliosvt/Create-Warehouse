package net.spindle.createwarehouse.mixin;

import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GantryShaftBlock.class)
public abstract class GantryShaftLockMixin {
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void createWarehouse$preserveManagedLock(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo callback) {
        if (!GantryControllerBlockEntity.isManagedVerticalShaftLocked(level, pos))
            return;
        if (!state.getValue(GantryShaftBlock.POWERED))
            level.setBlock(pos, state.setValue(GantryShaftBlock.POWERED, true), Block.UPDATE_CLIENTS);
        callback.cancel();
    }
}
