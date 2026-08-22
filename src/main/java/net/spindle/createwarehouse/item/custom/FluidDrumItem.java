package net.spindle.createwarehouse.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.spindle.createwarehouse.block.ModBlocks;
import net.spindle.createwarehouse.item.ModDataComponents;

import java.util.List;

public class FluidDrumItem extends BlockItem {
    public static final int CAPACITY = 8 * FluidType.BUCKET_VOLUME;

    public FluidDrumItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean isDrum(ItemStack stack) {
        return stack.is(ModBlocks.FLUID_DRUM.asItem());
    }

    public static ItemStack containing(FluidStack fluid) {
        ItemStack drum = ModBlocks.FLUID_DRUM.asStack();
        if (!fluid.isEmpty())
            drum.set(ModDataComponents.DRUM_FLUID, SimpleFluidContent.copyOf(fluid));
        return drum;
    }

    public static FluidStack getFluid(ItemStack drum) {
        return drum.getOrDefault(ModDataComponents.DRUM_FLUID, SimpleFluidContent.EMPTY).copy();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty())
            tooltipComponents.add(Component.translatable(fluid.getDescriptionId())
                    .append(" — " + fluid.getAmount() + " / " + CAPACITY + " mB")
                    .withStyle(ChatFormatting.GRAY));
    }
}
