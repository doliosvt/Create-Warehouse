package net.spindle.createwarehouse.entity.custom;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CrateEntity extends PackageEntity {


    public CrateEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public CrateEntity(Level worldIn, double x, double y, double z) {
        super(worldIn, x, y, z);
    }

    @Override
    public Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.WOOD_FALL, SoundEvents.WOOD_FALL);
    }
}
