//package net.spindle.createwarehouse.item.custom;
//
//import com.simibubi.create.content.logistics.box.PackageStyles;
//import com.simibubi.create.foundation.data.CreateRegistrate;
//import com.tterrag.registrate.builders.ItemBuilder;
//import net.spindle.createwarehouse.CreateWarehouse;
//
//import java.util.Locale;
//
//public class Builders {
//    public static ItemBuilder<CrateItem, CreateRegistrate> jar(PackageStyles.PackageStyle style) {
//        String size = "_" + style.width() + "x" + style.height();
//        return CreateWarehouse.REGISTRATE.item(JarStyles.getItemId(style).getPath(),
//                        p -> new CrateItem(p, style))
//                .properties(p -> p.stacksTo(1))
//                .model((c, p) ->
//                        p.withExistingParent(c.getName(), p.modLoc("item/crate/" + style.type() + size)))
//                .lang(style.type()
//                        .substring(0, 1)
//                        .toUpperCase(Locale.ROOT)
//                        + style.type()
//                        .substring(1));
//    }
//}
