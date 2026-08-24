package net.spindle.createwarehouse.block;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.spindle.createwarehouse.block.custom.CratePackagerBlockEntity;
import net.spindle.createwarehouse.block.custom.CratePackagerRenderer;
import net.spindle.createwarehouse.block.custom.DrumPackagerBlockEntity;
import net.spindle.createwarehouse.block.custom.DrumPackagerRenderer;
import net.spindle.createwarehouse.block.custom.ForkliftBlockEntity;
import net.spindle.createwarehouse.block.custom.ForkliftRenderer;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity;
import net.spindle.createwarehouse.block.custom.GantryControlsBlockEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import net.spindle.createwarehouse.block.custom.MultiblockPartBlock;
import net.spindle.createwarehouse.block.custom.PalletBlockEntity;
import net.spindle.createwarehouse.block.custom.PalletRenderer;
import net.spindle.createwarehouse.item.ModDataComponents;
import net.spindle.createwarehouse.item.custom.FluidDrumItem;

import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;

public final class ModBlockEntities {
    public static final BlockEntityEntry<GantryControllerBlockEntity> GANTRY_CONTROLLER = WAREHOUSE_REGISTRATE
            .blockEntity("gantry_controller", GantryControllerBlockEntity::new)
            .visual(() -> SplitShaftVisual::new, false)
            .validBlocks(ModBlocks.GANTRY_CONTROLLER)
            .renderer(() -> SplitShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<GantryControlsBlockEntity> GANTRY_CONTROLS = WAREHOUSE_REGISTRATE
            .blockEntity("gantry_controls", GantryControlsBlockEntity::new)
            .validBlocks(ModBlocks.GANTRY_CONTROLS)
            .renderer(() -> ContraptionControlsRenderer::new)
            .register();

    public static final BlockEntityEntry<DrumPackagerBlockEntity> DRUM_PACKAGER = WAREHOUSE_REGISTRATE
            .blockEntity("drum_packager", DrumPackagerBlockEntity::new)
            .validBlocks(ModBlocks.DRUM_PACKAGER)
            .renderer(() -> DrumPackagerRenderer::new)
            .register();

    public static final BlockEntityEntry<CratePackagerBlockEntity> CRATE_PACKAGER = WAREHOUSE_REGISTRATE
            .blockEntity("crate_packager", CratePackagerBlockEntity::new)
            .validBlocks(ModBlocks.CRATE_PACKAGER)
            .renderer(() -> CratePackagerRenderer::new)
            .register();

    public static final BlockEntityEntry<PalletBlockEntity> PALLET = WAREHOUSE_REGISTRATE
            .blockEntity("pallet", PalletBlockEntity::new)
            .validBlocks(ModBlocks.PALLET)
            .renderer(() -> PalletRenderer::new)
            .register();

    public static final BlockEntityEntry<ForkliftBlockEntity> FORKLIFT = WAREHOUSE_REGISTRATE
            .blockEntity("forklift", ForkliftBlockEntity::new)
            .validBlocks(ModBlocks.FORKLIFT)
            .renderer(() -> ForkliftRenderer::new)
            .register();

    private ModBlockEntities() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CRATE_PACKAGER.get(),
                (blockEntity, context) -> blockEntity.inventory
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                DRUM_PACKAGER.get(),
                (blockEntity, context) -> blockEntity.output
        );
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidHandlerItemStack(
                        ModDataComponents.DRUM_FLUID, stack, FluidDrumItem.CAPACITY),
                ModBlocks.FLUID_DRUM
        );
        event.registerBlock(
                Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    if (side != Direction.UP || !state.is(ModBlocks.MULTIBLOCK_PART))
                        return null;
                    BlockPos masterPos = MultiblockPartBlock.getMasterPos(state, pos);
                    if (!level.getBlockState(masterPos).is(ModBlocks.DRUM_PACKAGER)
                            || !(level.getBlockEntity(masterPos) instanceof DrumPackagerBlockEntity packager))
                        return null;
                    return packager.getTopInput();
                },
                ModBlocks.MULTIBLOCK_PART.get()
        );
    }

    public static void register() {}
}
