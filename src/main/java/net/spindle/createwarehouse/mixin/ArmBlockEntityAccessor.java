package net.spindle.createwarehouse.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ArmBlockEntity.class)
public interface ArmBlockEntityAccessor {
    @Accessor("heldItem")
    ItemStack createWarehouse$getHeldItem();

    @Accessor("baseAngle")
    LerpedFloat createWarehouse$getBaseAngle();

    @Accessor("lowerArmAngle")
    LerpedFloat createWarehouse$getLowerArmAngle();

    @Accessor("upperArmAngle")
    LerpedFloat createWarehouse$getUpperArmAngle();

    @Accessor("headAngle")
    LerpedFloat createWarehouse$getHeadAngle();
}
