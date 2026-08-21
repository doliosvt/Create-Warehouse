package net.spindle.createwarehouse.entity.custom;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.spindle.createwarehouse.entity.ModEntityTypes;

public class CrateEntity extends PackageEntity {


    public CrateEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityType.Builder<CrateEntity> configureDimensions(EntityType.Builder<CrateEntity> builder) {
        return builder.sized(1, 1);
    }

    public CrateEntity(Level worldIn, double x, double y, double z) {
        this(ModEntityTypes.CRATE.get(), worldIn);
        setPos(x, y, z);
        refreshDimensions();
    }

    public static CrateEntity fromDroppedItem(Level level, Entity originalEntity, ItemStack stack) {
        CrateEntity crate = ModEntityTypes.CRATE.get().create(level);
        if (crate == null)
            throw new IllegalStateException("Could not create crate entity");

        Vec3 position = originalEntity.position();
        crate.setPos(position);
        crate.setBox(stack);
        crate.setDeltaMovement(originalEntity.getDeltaMovement().scale(1.5f));
        return crate;
    }

    @Override
    public Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.WOOD_FALL, SoundEvents.WOOD_FALL);
    }
}
