package net.spindle.createwarehouse.item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.item.custom.CrateItem;
import net.spindle.createwarehouse.item.custom.CrateStyles;
import net.spindle.createwarehouse.item.custom.PalletCarrierItem;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateWarehouse.MODID);

    public static final DeferredItem<CrateItem> CRATE_ITEM = ITEMS.registerItem(
            "crate_item",
            properties -> new CrateItem(properties, CrateStyles.DEFAULT),
            new CrateItem.Properties().stacksTo(1)
    );

    public static final DeferredItem<PalletCarrierItem> PALLET_CARRIER = ITEMS.registerItem(
            "pallet_carrier",
            PalletCarrierItem::new,
            new PalletCarrierItem.Properties().stacksTo(1)
    );

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
