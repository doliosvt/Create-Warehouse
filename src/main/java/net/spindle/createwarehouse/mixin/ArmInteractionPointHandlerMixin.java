package net.spindle.createwarehouse.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.ItemStack;
import net.spindle.createwarehouse.block.ModBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmInteractionPointHandler.class)
public abstract class ArmInteractionPointHandlerMixin {
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z",
                    ordinal = 0)
    )
    private static boolean createWarehouse$allowForkliftSelection(BlockEntry<?> entry, ItemStack stack) {
        return entry.isIn(stack) || stack.is(ModBlocks.FORKLIFT.asItem());
    }
}
