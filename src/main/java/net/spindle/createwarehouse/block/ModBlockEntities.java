package net.spindle.createwarehouse.block;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.custom.CratePackagerBlockEntity;

import java.util.function.Supplier;

import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;

public class ModBlockEntities {

public static final BlockEntityEntry<CratePackagerBlockEntity> CRATE_PACKAGER = WAREHOUSE_REGISTRATE
        .blockEntity("crate_packager", CratePackagerBlockEntity::new)
        .validBlocks(ModBlocks.CRATE_PACKAGER)
        .register();

    public static void register() {}

//    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
//            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateWarehouse.MODID);
//
//    public static final Supplier<BlockEntityType<CratePackagerBlockEntity>> CRATE_PACKAGER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
//            "crate_packager_block_entity",
//                    // The block entity type, created using a builder.
//                    () -> BlockEntityType.Builder.of(
//                        // The supplier to use for constructing the block entity instances.
//                        CratePackagerBlockEntity::new,
//                        // A vararg of blocks that can have this block entity.
//                        // This assumes the existence of the referenced blocks as DeferredBlock<Block>s.
//                        ModBlocks.CRATE_PACKAGER.get()
//                    )
//                            // Build using null; vanilla does some datafixer shenanigans with the parameter that we don't need.
//                            .build(null)
//    );
}
