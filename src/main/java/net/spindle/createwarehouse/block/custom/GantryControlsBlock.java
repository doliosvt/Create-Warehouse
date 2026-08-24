package net.spindle.createwarehouse.block.custom;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlock;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.spindle.createwarehouse.block.ModBlockEntities;
import net.spindle.createwarehouse.client.GantryAddressScreen;

/** A floor-selector-style control panel for the contacts on a horizontal gantry line. */
public class GantryControlsBlock extends ContraptionControlsBlock {
    public static final MapCodec<GantryControlsBlock> CODEC = simpleCodec(GantryControlsBlock::new);

    public GantryControlsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, controls -> {
            if (!(controls instanceof GantryControlsBlockEntity gantryControls))
                return InteractionResult.PASS;
            controls.pressButton();
            if (level.isClientSide()) {
                CatnipServices.PLATFORM.executeOnClientOnly(
                        () -> () -> GantryAddressScreen.openStationary(pos, gantryControls));
            } else {
                AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, 1.5f);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public BlockEntityType<? extends GantryControlsBlockEntity> getBlockEntityType() {
        return ModBlockEntities.GANTRY_CONTROLS.get();
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
