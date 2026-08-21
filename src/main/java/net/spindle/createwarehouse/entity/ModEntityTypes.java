package net.spindle.createwarehouse.entity;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import com.simibubi.create.content.logistics.box.PackageVisual;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.block.custom.LargeDepot;
import net.spindle.createwarehouse.entity.custom.CrateEntity;
import net.spindle.createwarehouse.entity.custom.CrateRenderer;
import net.spindle.createwarehouse.entity.custom.CrateVisual;

import java.util.function.Supplier;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static net.spindle.createwarehouse.CreateWarehouse.WAREHOUSE_REGISTRATE;

public class ModEntityTypes {

    //public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
    //        DeferredRegister.create(Registries.ENTITY_TYPE, CreateWarehouse.MODID);

    public static final EntityEntry<PackageEntity> CRATE = register("crate", CrateEntity::new, () -> CrateRenderer::new,
            MobCategory.MISC, 10, 3, true, false, CrateEntity::build)
            .visual(() -> CrateVisual::new, true)
            .register();

    //public static void register(IEventBus eventBus) {
    //    ENTITY_TYPES.register(eventBus);
    //}

    private static <T extends Entity> CreateEntityBuilder<T, ?> register(String name, EntityType.EntityFactory<T> factory,
                                                                         NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer,
                                                                         MobCategory group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire,
                                                                         NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
        String id = Lang.asId(name);
        return (CreateEntityBuilder<T, ?>) CreateWarehouse.WAREHOUSE_REGISTRATE
                .entity(id, factory, group)
                .properties(b -> b.setTrackingRange(range)
                        .setUpdateInterval(updateFrequency)
                        .setShouldReceiveVelocityUpdates(sendVelocity))
                .properties(propertyBuilder)
                .properties(b -> {
                    if (immuneToFire)
                        b.fireImmune();
                })
                .renderer(renderer);
    }

    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(CRATE.get(), PackageEntity.createPackageAttributes()
                .build());
    }

    // NOTE: this does not work
    // public static final EntityEntry<PackageEntity> CRATE = register("crate", CrateEntity::new, () -> CrateRenderer::new,
    //        MobCategory.MISC, 10, 3, true, false, CrateEntity::build)
    //        .visual(() -> CrateVisual::new, true)
    //        .register();

    // NOTE: this mirrors what works for blocks and all the other stuff
    //public static final EntityEntry<PackageEntity> CRATE = WAREHOUSE_REGISTRATE.entity("crate",
    //                CrateEntity::new)
    //        .register();

    // NOTE: this does not work
    //private static <T extends Entity> CreateEntityBuilder<T, ?> register(String name, EntityType.EntityFactory<T> factory,
    //                                                                     NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer,
    //                                                                     MobCategory group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire,
    //                                                                     NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
    //    String id = Lang.asId(name);
    //    return (CreateEntityBuilder<T, ?>) Create.registrate()
    //            .entity(id, factory, group)
    //            .properties(b -> b.setTrackingRange(range)
    //                    .setUpdateInterval(updateFrequency)
    //                    .setShouldReceiveVelocityUpdates(sendVelocity))
    //            .properties(propertyBuilder)
    //            .properties(b -> {
    //                if (immuneToFire)
    //                    b.fireImmune();
    //            })
    //            .renderer(renderer);
    //}

    //public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
    //    event.put(CRATE.get(), PackageEntity.createPackageAttributes()
    //            .build());
    //}

    public static void register() {}
}
