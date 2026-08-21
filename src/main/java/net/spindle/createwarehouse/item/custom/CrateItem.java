package net.spindle.createwarehouse.item.custom;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.spindle.createwarehouse.entity.ModEntityTypes;
import net.spindle.createwarehouse.entity.custom.CrateEntity;
import net.spindle.createwarehouse.item.ModItems;

import java.lang.ref.WeakReference;
import java.util.List;

public class CrateItem extends PackageItem {
    public static final int SLOTS = PackageItem.SLOTS;

    public CrateItem(Properties properties, PackageStyles.PackageStyle style) {
        super(properties, style);

        // Crates are produced only by the crate packager and must not be selected
        // randomly by Create's regular packagers.
        PackageStyles.ALL_BOXES.remove(this);
        (style.rare() ? PackageStyles.RARE_BOXES : PackageStyles.STANDARD_BOXES).remove(this);
    }

    public static boolean isCrate(ItemStack stack) {
        return stack.getItem() instanceof CrateItem;
    }

    @Override
    public String getDescriptionId() {
        return "item.create_warehouse.crate_item";
    }

    @Override
    public Entity createEntity(Level level, Entity originalEntity, ItemStack stack) {
        return CrateEntity.fromDroppedItem(level, originalEntity, stack);
    }

    public static ItemStack containing(List<ItemStack> stacks) {
        ItemStackHandler inventory = new ItemStackHandler(SLOTS);
        stacks.forEach(stack -> ItemHandlerHelper.insertItemStacked(inventory, stack, false));
        return containing(inventory);
    }

    public static ItemStack containing(ItemStackHandler inventory) {
        ItemStack crate = new ItemStack(ModItems.CRATE_ITEM.get());
        crate.set(AllDataComponents.PACKAGE_CONTENTS, ItemHelper.containerContentsFromHandler(inventory));
        return crate;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown())
            return open(context.getLevel(), player, context.getHand()).getResult();

        Vec3 point = context.getClickLocation();
        float height = style.height() / 16f;
        float radius = style.width() / 2f / 16f;

        if (context.getClickedFace() == Direction.DOWN)
            point = point.subtract(0, height + .25f, 0);
        else if (context.getClickedFace().getAxis().isHorizontal())
            point = point.add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(radius));

        AABB bounds = new AABB(point, point).inflate(radius, 0, radius).expandTowards(0, height, 0);
        Level level = context.getLevel();
        if (!level.getEntities(ModEntityTypes.CRATE.get(), bounds, entity -> true).isEmpty())
            return super.useOn(context);

        CrateEntity crate = new CrateEntity(level, point.x, point.y, point.z);
        ItemStack heldStack = context.getItemInHand();
        crate.setBox(heldStack.copy());
        level.addFreshEntity(crate);
        heldStack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int ticks) {
        if (!(entity instanceof Player player))
            return;

        int useTicks = getUseDuration(stack, entity) - ticks;
        float velocity = getPackageVelocity(useTicks);
        if (useTicks < 0 || velocity < 0.1f || level.isClientSide)
            return;

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL, .5f, .5f);

        ItemStack thrownStack = stack.copy();
        if (!player.getAbilities().instabuild)
            stack.shrink(1);

        Vec3 motion = entity.getLookAngle().scale(velocity * 2);
        Vec3 position = new Vec3(entity.getX(), entity.getY() + entity.getBoundingBox().getYsize() / 2f,
                entity.getZ()).add(motion);

        CrateEntity crate = new CrateEntity(level, position.x, position.y, position.z);
        crate.setBox(thrownStack);
        crate.setDeltaMovement(motion);
        crate.tossedBy = new WeakReference<>(player);
        level.addFreshEntity(crate);
    }
}
