package net.spindle.createwarehouse.item;

import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.item.custom.CrateItem;
import net.spindle.createwarehouse.item.custom.CrateStyles;

import java.util.Locale;
import java.util.function.Supplier;

import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;

public class ModItems {

    static {
        WAREHOUSE_REGISTRATE.setCreativeTab(ModCreativeModeTabs.BASE_CREATIVE_TAB);
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateWarehouse.MODID);

    public static final DeferredItem<CrateItem> CRATE_ITEM = ITEMS.registerItem(
            "crate_item",
            CrateItem::new,
            new CrateItem.Properties().stacksTo(1)
    );

    // NOTE: this is how the packageItem is registered
//    static {
//        PackageStyles.PackageStyle style = CrateStyles.STYLES.getFirst();
//
//        ItemBuilder<PackageItem, CreateRegistrate> CRATEITEM = BuilderTransformers.packageItem(style);
//
//        crateItem.register();
//
//    }


//    public static ItemBuilder<PackageItem, CreateRegistrate> packageItem(PackageStyles.PackageStyle style) {
//        String size = "_" + style.width() + "x" + style.height();
//        return Create.registrate().item(style.getItemId()
//                        .getPath(), p -> new PackageItem(p, style))
//                .properties(p -> p.stacksTo(1))
//                .tag(AllTags.AllItemTags.PACKAGES.tag)
//                .model((c, p) -> {
//                    if (style.rare())
//                        p.withExistingParent(c.getName(), p.modLoc("item/package/custom" + size))
//                                .texture("2", p.modLoc("item/package/" + style.type()));
//                    else
//                        p.withExistingParent(c.getName(), p.modLoc("item/package/" + style.type() + size));
//                })
//                .lang((style.rare() ? "Rare"
//                        : style.type()
//                        .substring(0, 1)
//                        .toUpperCase(Locale.ROOT)
//                          + style.type()
//                        .substring(1))
//                        + " Package");
//    }



//    static {
//        ItemBuilder<CrateItem, CreateRegistrate> crateItem = BuilderTransformers.crateItem(new PackageStyles.PackageStyle("cardboard", 16, 16, 23f, false));
//        crateItem.register();
//        }
//    }

//    public static final ItemEntry<CrateItem> CRATE = Builders.crate(CrateStyles.STYLES.getFirst()).register();

    public static void register() {}
}
