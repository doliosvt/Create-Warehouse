package net.spindle.createwarehouse.client;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.spindle.createwarehouse.block.custom.GantryAddress;
import net.spindle.createwarehouse.block.custom.GantryControlsBlockEntity;
import net.spindle.createwarehouse.block.custom.GantryControlsMovement;
import net.spindle.createwarehouse.network.GantryAddressTargetPayload;

@OnlyIn(Dist.CLIENT)
public class GantryAddressScreen extends Screen {
    private static final int STATIONARY_ENTITY = -1;

    private final int entityId;
    private final BlockPos controlsPos;
    private final String initialAddress;
    private final @Nullable MovementContext movementContext;
    private EditBox addressInput;
    private Component error = Component.empty();

    private GantryAddressScreen(int entityId, BlockPos controlsPos, String initialAddress,
            @Nullable MovementContext movementContext) {
        super(Component.translatable("gui.create_warehouse.gantry_controls.address_title"));
        this.entityId = entityId;
        this.controlsPos = controlsPos.immutable();
        this.initialAddress = initialAddress;
        this.movementContext = movementContext;
    }

    public static void openStationary(BlockPos pos, GantryControlsBlockEntity controls) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GantryAddressScreen(
                STATIONARY_ENTITY, pos, controls.getAddress(), null));
    }

    public static void openMoving(int entityId, BlockPos localPos, MovementContext context) {
        var selection = GantryControlsMovement.getOrCreateSelection(context);
        Minecraft.getInstance().setScreen(new GantryAddressScreen(
                entityId, localPos, GantryAddress.format(selection.currentIndex, selection.levelIndex), context));
    }

    @Override
    protected void init() {
        int fieldWidth = 180;
        int left = (width - fieldWidth) / 2;
        int top = height / 2 - 25;

        addressInput = new EditBox(font, left, top, fieldWidth, 22,
                Component.translatable("gui.create_warehouse.gantry_controls.address"));
        addressInput.setMaxLength(8);
        addressInput.setFilter(value -> value.matches("[0-9A-Za-z]*"));
        addressInput.setValue(initialAddress);
        addRenderableWidget(addressInput);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> submit())
                .bounds(left, top + 34, 86, 20)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(left + 94, top + 34, 86, 20)
                .build());
        setInitialFocus(addressInput);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submit() {
        var parsed = GantryAddress.parse(addressInput.getValue());
        if (parsed.isEmpty()) {
            error = Component.translatable("gui.create_warehouse.gantry_controls.invalid_address");
            return;
        }

        GantryAddress address = parsed.get();
        if (movementContext != null) {
            var selection = GantryControlsMovement.getOrCreateSelection(movementContext);
            selection.currentIndex = address.contactIndex();
            selection.levelIndex = address.levelIndex();
            selection.verticalEnabled = true;
        }
        PacketDistributor.sendToServer(new GantryAddressTargetPayload(
                entityId, controlsPos, address.contactNumber(), address.levelNumber()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("gui.create_warehouse.gantry_controls.address_hint"),
                width / 2, height / 2 - 44, 0xA0A0A0);
        if (!error.getString().isEmpty())
            graphics.drawCenteredString(font, error, width / 2, height / 2 + 38, 0xFF6060);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
