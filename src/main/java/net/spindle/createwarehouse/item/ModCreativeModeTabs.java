package net.spindle.createwarehouse.item;

import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.entity.ModEntityTypes;

import java.util.function.Supplier;

import static net.spindle.createwarehouse.CreateWarehouse.MODID;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateWarehouse.MODID);

    private ModCreativeModeTabs() {};

//    public static final Supplier<CreativeModeTab> CREATE_WAREHOUSE_TAB = CREATIVE_MODE_TAB.register("create_warehouse_tab",
//            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.DRUM_PACKAGER.get()))
//                    .title(Component.translatable("creativetab.create_warehouse"))
//                    .displayItems((itemDisplayParameters, output) -> {
//                        output.accept(ModBlocks.DRUM_PACKAGER);
//                        output.accept(ModBlocks.CRATE_PACKAGER);
//                        output.accept(ModBlocks.LARGE_DEPOT);
//                    }).build());

//    private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = CREATIVE_MODE_TAB.register("base", () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.create_warehouse"))
            .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
            .icon(ModBlocks.DRUM_PACKAGER::asStack)
            .displayItems((itemDisplayParameters, output) -> {
                output.accept(ModBlocks.DRUM_PACKAGER.get());
                output.accept(ModBlocks.CRATE_PACKAGER.get());
                // TODO: crashed on inventory open
                output.accept(ModItems.CRATE_ITEM.get());
                // TODO: Large Depot is added somewhere else and crashes the game when added twice, find other add and remove it
                // output.accept(ModBlocks.LARGE_DEPOT.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
