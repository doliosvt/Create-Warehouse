package net.spindle.createwarehouse.block;

import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.spindle.createwarehouse.block.custom.CratePackagerBlock;
import net.spindle.createwarehouse.block.custom.CratePackagerStructuralBlock;
import net.spindle.createwarehouse.block.custom.DrumPackagerBlock;
import net.spindle.createwarehouse.block.custom.ForkliftBlock;
import net.spindle.createwarehouse.block.custom.MultiblockDecorationBlock;
import net.spindle.createwarehouse.block.custom.MultiblockPartBlock;
import net.spindle.createwarehouse.block.custom.PalletBlock;
import net.spindle.createwarehouse.block.custom.StaticShapeBlock;
import net.spindle.createwarehouse.item.custom.FluidDrumItem;
import net.spindle.createwarehouse.item.custom.ForkliftItem;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;
import static net.spindle.createwarehouse.block.custom.MultiblockDecorationBlock.PartPlacement.at;
import static net.spindle.createwarehouse.block.custom.MultiblockPartBlock.PartShape.FULL;
import static net.spindle.createwarehouse.block.custom.MultiblockPartBlock.PartShape.LOW;

public final class ModBlocks {
    public static final BlockEntry<MultiblockPartBlock> MULTIBLOCK_PART = WAREHOUSE_REGISTRATE
            .block("multiblock_part", MultiblockPartBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(properties -> properties
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false))
            .register();

    public static final BlockEntry<DrumPackagerBlock> DRUM_PACKAGER = WAREHOUSE_REGISTRATE
            .block("drum_packager", properties -> new DrumPackagerBlock(properties,
                    Block.box(0, 0, 0, 16, 16, 16), at(0, 1, 0, FULL)))
            .initialProperties(SharedProperties::stone)
            .properties(properties -> properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .strength(3f)
                    .noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<CratePackagerBlock> CRATE_PACKAGER = WAREHOUSE_REGISTRATE
            .block("crate_packager", CratePackagerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(properties -> properties
                    .mapColor(MapColor.TERRACOTTA_BLUE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false))
            .simpleItem()
            .register();

    public static final BlockEntry<CratePackagerStructuralBlock> CRATE_PACKAGER_STRUCTURAL = WAREHOUSE_REGISTRATE
            .block("crate_packager_structural", CratePackagerStructuralBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(properties -> properties
                    .mapColor(MapColor.TERRACOTTA_BLUE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false))
            .register();

    public static final BlockEntry<MultiblockDecorationBlock> LARGE_DEPOT = metalMultiblock(
            "large_depot", Block.box(0, 0, 0, 16, 13, 16),
            at(-1, 0, 0, LOW), at(0, 0, 1, LOW), at(-1, 0, 1, LOW));
    public static final BlockEntry<PalletBlock> PALLET = WAREHOUSE_REGISTRATE
            .block("pallet", properties -> new PalletBlock(properties, Block.box(0, 0, 0, 16, 4, 16),
                    at(-1, 0, 0, MultiblockPartBlock.PartShape.PALLET),
                    at(0, 0, 1, MultiblockPartBlock.PartShape.PALLET),
                    at(-1, 0, 1, MultiblockPartBlock.PartShape.PALLET)))
            .initialProperties(SharedProperties::wooden)
            .properties(properties -> properties
                    .mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD)
                    .strength(2f)
                    .noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<MultiblockDecorationBlock> PALLET_DEPOT = metalMultiblock(
            "pallet_depot", Block.box(0, 0, 0, 16, 13, 16),
            at(-1, 0, 0, LOW), at(0, 0, 1, LOW), at(-1, 0, 1, LOW));
    public static final BlockEntry<StaticShapeBlock> FLUID_DRUM = WAREHOUSE_REGISTRATE
            .block("fluid_drum", properties -> new StaticShapeBlock(properties,
                    Block.box(2, 0, 2, 14, 14, 14)))
            .initialProperties(SharedProperties::stone)
            .properties(properties -> properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .strength(3f)
                    .noOcclusion())
            .item(FluidDrumItem::new)
            .properties(properties -> properties.stacksTo(1))
            .build()
            .register();
    public static final BlockEntry<StaticShapeBlock> ITEM_ELEVATOR_BOTTOM = metalDecoration(
            "item_elevator_bottom", Block.box(0, 0, 0, 16, 16, 16));
    public static final BlockEntry<StaticShapeBlock> ITEM_ELEVATOR_MIDDLE = metalDecoration(
            "item_elevator_middle", Block.box(0, 0, 0, 16, 16, 16));
    public static final BlockEntry<StaticShapeBlock> ITEM_ELEVATOR_TOP = metalDecoration(
            "item_elevator_top", Block.box(0, 0, 0, 16, 16, 16));
    public static final BlockEntry<ForkliftBlock> FORKLIFT = WAREHOUSE_REGISTRATE
            .block("forklift", properties -> new ForkliftBlock(properties,
                    Block.box(0, 0, 0, 16, 6, 16),
                    at(-1, 0, -1, MultiblockPartBlock.PartShape.FORKLIFT_BASE),
                    at(0, 0, -1, MultiblockPartBlock.PartShape.FORKLIFT_BASE),
                    at(-1, 0, 0, MultiblockPartBlock.PartShape.FORKLIFT_BASE_ARM_LEFT),
                    at(-1, 1, 1, MultiblockPartBlock.PartShape.FORKLIFT_ARM_LEFT),
                    at(0, 1, 1, MultiblockPartBlock.PartShape.FORKLIFT_ARM_RIGHT),
                    at(-1, 1, 0, MultiblockPartBlock.PartShape.FORKLIFT_TOP_LEFT),
                    at(0, 1, 0, MultiblockPartBlock.PartShape.FORKLIFT_TOP_RIGHT),
                    at(-1, 1, -1, MultiblockPartBlock.PartShape.FORKLIFT_TOP_LEFT),
                    at(0, 1, -1, MultiblockPartBlock.PartShape.FORKLIFT_TOP_RIGHT),
                    at(-1, 1, -2, MultiblockPartBlock.PartShape.FORKLIFT_TOP_LEFT),
                    at(0, 1, -2, MultiblockPartBlock.PartShape.FORKLIFT_TOP_RIGHT),
                    at(-1, 1, -3, MultiblockPartBlock.PartShape.FORKLIFT_TIP_LEFT),
                    at(0, 1, -3, MultiblockPartBlock.PartShape.FORKLIFT_TIP_RIGHT)))
            .initialProperties(SharedProperties::stone)
            .properties(properties -> properties
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .strength(3f)
                    .noOcclusion())
            .item(ForkliftItem::new)
            .build()
            .register();

    private ModBlocks() {}

    private static BlockEntry<StaticShapeBlock> metalDecoration(String name, VoxelShape shape) {
        return WAREHOUSE_REGISTRATE.block(name, properties -> new StaticShapeBlock(properties, shape))
                .initialProperties(SharedProperties::stone)
                .properties(properties -> properties
                        .mapColor(MapColor.COLOR_GRAY)
                        .sound(SoundType.NETHERITE_BLOCK)
                        .strength(3f)
                        .noOcclusion())
                .simpleItem()
                .register();
    }

    private static BlockEntry<StaticShapeBlock> woodenDecoration(String name, VoxelShape shape) {
        return WAREHOUSE_REGISTRATE.block(name, properties -> new StaticShapeBlock(properties, shape))
                .initialProperties(SharedProperties::wooden)
                .properties(properties -> properties
                        .mapColor(MapColor.WOOD)
                        .sound(SoundType.WOOD)
                        .strength(2f)
                        .noOcclusion())
                .simpleItem()
                .register();
    }

    private static BlockEntry<MultiblockDecorationBlock> metalMultiblock(
            String name, VoxelShape shape, MultiblockDecorationBlock.PartPlacement... parts) {
        return WAREHOUSE_REGISTRATE
                .block(name, properties -> new MultiblockDecorationBlock(properties, shape, parts))
                .initialProperties(SharedProperties::stone)
                .properties(properties -> properties
                        .mapColor(MapColor.COLOR_GRAY)
                        .sound(SoundType.NETHERITE_BLOCK)
                        .strength(3f)
                        .noOcclusion())
                .simpleItem()
                .register();
    }

    private static BlockEntry<MultiblockDecorationBlock> woodenMultiblock(
            String name, VoxelShape shape, MultiblockDecorationBlock.PartPlacement... parts) {
        return WAREHOUSE_REGISTRATE
                .block(name, properties -> new MultiblockDecorationBlock(properties, shape, parts))
                .initialProperties(SharedProperties::wooden)
                .properties(properties -> properties
                        .mapColor(MapColor.WOOD)
                        .sound(SoundType.WOOD)
                        .strength(2f)
                        .noOcclusion())
                .simpleItem()
                .register();
    }

    public static void register() {}
}
