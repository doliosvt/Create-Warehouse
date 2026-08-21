package net.spindle.createwarehouse.item.custom;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageStyles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrateStyles {

    public record CrateStyle(String type, int width, int height, float riggingOffset, boolean rare) {
        public ResourceLocation getItemId() {
            String size = "_" + width + "x" + height;
            String id = type + "_package" + (rare ? "" : size);
            return Create.asResource(id);
        }

        public ResourceLocation getRiggingModel() {
            String size = width + "x" + height;
            return Create.asResource("item/package/rigging_" + size);
        }
    };

    public static final List<PackageStyles.PackageStyle> STYLES = ImmutableList.of(
            new PackageStyles.PackageStyle("crate", 16, 16, 19f, false)
    );

    public static final List<CrateItem> ALL_CRATES = new ArrayList<>();

    public static ItemStack getDefaultBox() {
        return new ItemStack(ALL_CRATES.getFirst());
    }
}
