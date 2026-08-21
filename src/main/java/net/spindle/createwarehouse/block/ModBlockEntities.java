package net.spindle.createwarehouse.block;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.spindle.createwarehouse.block.custom.CratePackagerBlockEntity;

import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;

public final class ModBlockEntities {
    public static final BlockEntityEntry<CratePackagerBlockEntity> CRATE_PACKAGER = WAREHOUSE_REGISTRATE
            .blockEntity("crate_packager", CratePackagerBlockEntity::new)
            .validBlocks(ModBlocks.CRATE_PACKAGER)
            .register();

    private ModBlockEntities() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CRATE_PACKAGER.get(),
                (blockEntity, context) -> blockEntity.inventory
        );
    }

    public static void register() {}
}
