package net.spindle.createwarehouse.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.spindle.createwarehouse.item.ModItems;

public class PalletCarrierItem extends Item {
    private static final String PALLET_DATA = "Pallet";

    public PalletCarrierItem(Properties properties) {
        super(properties);
    }

    public static ItemStack containing(CompoundTag palletData) {
        ItemStack carrier = new ItemStack(ModItems.PALLET_CARRIER.get());
        CompoundTag root = new CompoundTag();
        root.put(PALLET_DATA, palletData.copy());
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return carrier;
    }

    public static boolean isCarrier(ItemStack stack) {
        return stack.is(ModItems.PALLET_CARRIER.get());
    }

    public static CompoundTag getPalletData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null)
            return new CompoundTag();
        return customData.copyTag().getCompound(PALLET_DATA).copy();
    }
}
