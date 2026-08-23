package net.spindle.createwarehouse.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.spindle.createwarehouse.CreateWarehouse;

public final class ModPartialModels {
    public static final PartialModel FORKLIFT_COG = forklift("cog");
    public static final PartialModel FORKLIFT_BASE = forklift("base");
    public static final PartialModel FORKLIFT_LOWER_BODY = forklift("lower_body");
    public static final PartialModel FORKLIFT_UPPER_BODY = forklift("upper_body");
    public static final PartialModel FORKLIFT_FORK = forklift("fork");

    private ModPartialModels() {}

    public static void init() {}

    private static PartialModel forklift(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                CreateWarehouse.MODID, "block/forklift/" + name));
    }
}
