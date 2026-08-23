package net.spindle.createwarehouse.compat;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.spindle.createwarehouse.item.custom.PalletCarrierItem;
import net.spindle.createwarehouse.mixin.ArmBlockEntityAccessor;

public final class ArmPalletAccess {
    private ArmPalletAccess() {}

    public static boolean isHoldingPallet(ArmBlockEntity arm) {
        return PalletCarrierItem.isCarrier(((ArmBlockEntityAccessor) arm).createWarehouse$getHeldItem());
    }
}
