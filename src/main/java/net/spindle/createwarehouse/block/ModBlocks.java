package net.spindle.createwarehouse.block;

import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.content.logistics.depot.MountedDepotInteractionBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherrackBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.custom.CratePackagerBlock;
import net.spindle.createwarehouse.block.custom.CratePackagerStructuralBlock;
import net.spindle.createwarehouse.block.custom.DrumPackagerBlock;
import net.spindle.createwarehouse.block.custom.LargeDepot;
import net.spindle.createwarehouse.item.ModItems;

import java.util.function.Supplier;


import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;

public class ModBlocks {
//    public static final DeferredRegister.Blocks BLOCKS =
//            DeferredRegister.createBlocks(CreateWarehouse.MODID);

//    public static final DeferredBlock<DrumPackagerBlock> DRUM_PACKAGER = registerBlock("drum_packager",
//            () -> new DrumPackagerBlock(BlockBehaviour.Properties.of()
//                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.COPPER)));

    public static final BlockEntry<DrumPackagerBlock> DRUM_PACKAGER = WAREHOUSE_REGISTRATE.block("drum_packager",
            DrumPackagerBlock::new)
            .transform(BuilderTransformers.packager())
            .simpleItem()
            .register();

//    public static final DeferredBlock<CratePackagerBlock> CRATE_PACKAGER = registerBlock("crate_packager",
//            () -> new CratePackagerBlock(BlockBehaviour.Properties.of()
//                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));

    public static final BlockEntry<CratePackagerBlock> CRATE_PACKAGER = WAREHOUSE_REGISTRATE.block("crate_packager",
                CratePackagerBlock::new)
            .transform(BuilderTransformers.packager())
            .simpleItem()
            .register();

//    public static final DeferredBlock<CratePackagerStructuralBlock> CRATE_PACKAGER_SRUCTURAL = registerBlock("crate_packager_structural",
//            () -> new CratePackagerStructuralBlock(BlockBehaviour.Properties.of()
//                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));

    public static final BlockEntry<CratePackagerStructuralBlock> CRATE_PACKAGER_STRUCTURAL = WAREHOUSE_REGISTRATE.block("crate_packager_structural",
                CratePackagerStructuralBlock::new)
            .transform(BuilderTransformers.packager())
            .simpleItem()
            .register();

//    public static final DeferredBlock<LargeDepot> LARGE_DEPOT = registerBlock("large_depot",
//            () -> new LargeDepot(BlockBehaviour.Properties.of()
//                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    // Large Depot axe and pickaxe, .initialProperties(SharedProperties::stone)

    public static final BlockEntry<LargeDepot> LARGE_DEPOT = WAREHOUSE_REGISTRATE.block("large_depot",
                LargeDepot::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY))
            .transform(axeOrPickaxe())
            .simpleItem()
//            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
//            .transform(displaySource(AllDisplaySources.ITEM_NAMES))
//            .onRegister(interactionBehaviour(new MountedDepotInteractionBehaviour()))
//            .transform(mountedItemStorage(AllMountedStorageTypes.DEPOT))
//            .item()
//            .transform(customItemModel("_", "block"))
            .register();


// TODO: update block sounds

//    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
//        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
//        registerBlockItem(name, toReturn);
//        return toReturn;
//    }
//
//    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
//        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
//    }

//    public static void register(IEventBus eventBus) {
//        BLOCKS.register(eventBus);
//    }
    public static void register() {};
}
