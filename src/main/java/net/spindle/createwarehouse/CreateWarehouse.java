package net.spindle.createwarehouse;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.spindle.createwarehouse.block.ModBlockEntities;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.block.ModArmInteractionPoints;
import net.spindle.createwarehouse.entity.ModEntityTypes;
import net.spindle.createwarehouse.item.ModCreativeModeTabs;
import net.spindle.createwarehouse.item.ModDataComponents;
import net.spindle.createwarehouse.item.ModItems;
import net.spindle.createwarehouse.item.custom.CrateStyles;
import net.spindle.createwarehouse.client.ModPartialModels;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(CreateWarehouse.MODID)
public final class CreateWarehouse {
    public static final String MODID = "create_warehouse";
    public static final CreateRegistrate WAREHOUSE_REGISTRATE = CreateRegistrate.create(MODID);

    public CreateWarehouse(IEventBus modEventBus) {
        WAREHOUSE_REGISTRATE.registerEventListeners(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        WAREHOUSE_REGISTRATE.defaultCreativeTab(ModCreativeModeTabs.BASE_CREATIVE_TAB.getKey());
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register();
        ModBlockEntities.register();
        ModArmInteractionPoints.register(modEventBus);
        ModEntityTypes.register();
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
        modEventBus.addListener(ModEntityTypes::registerEntityAttributes);
        modEventBus.addListener(ClientModEvents::onClientSetup);
    }

    public static class ClientModEvents {
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModPartialModels.init();
            event.enqueueWork(() -> {
                PartialModel crateModel = PartialModel.of(
                        ResourceLocation.fromNamespaceAndPath(MODID, "item/crate_item"));
                AllPartialModels.PACKAGES.put(ModItems.CRATE_ITEM.getId(), crateModel);
                AllPartialModels.PACKAGES_TO_HIDE_AS.add(crateModel);
                AllPartialModels.PACKAGE_RIGGING.put(
                        ModItems.CRATE_ITEM.getId(),
                        PartialModel.of(CrateStyles.DEFAULT.getRiggingModel()));
            });
        }
    }
}
