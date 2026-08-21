package net.spindle.createwarehouse.entity;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.spindle.createwarehouse.CreateWarehouse;
import net.spindle.createwarehouse.entity.custom.CrateEntity;
import net.spindle.createwarehouse.entity.custom.CrateRenderer;
import net.spindle.createwarehouse.entity.custom.CrateVisual;

public final class ModEntityTypes {

    public static final EntityEntry<CrateEntity> CRATE = ModEntityTypes.<CrateEntity>register("crate", CrateEntity::new, () -> CrateRenderer::new,
            MobCategory.MISC, 10, 3, true, false, CrateEntity::configureDimensions)
            .visual(() -> CrateVisual::new, true)
            .register();

    @SuppressWarnings("unchecked")
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

    private ModEntityTypes() {}

    public static void register() {}
}
