package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.spindle.createwarehouse.client.GantryControlsRenderer;

public class GantryControlsMovement implements MovementBehaviour {
    @Override
    public void startMoving(MovementContext context) {
        if (context.blockEntityData == null)
            return;
        context.temporaryData = selectionFromData(context);
    }

    @Override
    public void tick(MovementContext context) {
        if (!context.world.isClientSide())
            return;
        if (!(context.temporaryData instanceof GantryStopSelection))
            startMoving(context);
        if (context.contraption.getBlockEntityClientSide(context.localPos)
                instanceof GantryControlsBlockEntity controls)
            controls.tickAnimations();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
            ContraptionMatrices matrices, MultiBufferSource buffer) {
        GantryControlsRenderer.renderInContraption(context, renderWorld, matrices, buffer);
    }

    public static GantryStopSelection getOrCreateSelection(MovementContext context) {
        if (context.temporaryData instanceof GantryStopSelection selection)
            return selection;
        GantryStopSelection selection = selectionFromData(context);
        context.temporaryData = selection;
        return selection;
    }

    private static GantryStopSelection selectionFromData(MovementContext context) {
        if (context.blockEntityData == null)
            return new GantryStopSelection(0, 0, 0, 0, false);
        return new GantryStopSelection(
                Math.max(0, context.blockEntityData.getInt("Selection")),
                Math.max(0, context.blockEntityData.getInt("LevelSelection")),
                Math.max(0, context.blockEntityData.getInt("LevelCount")),
                context.blockEntityData.getInt("LevelCoordinate"),
                context.blockEntityData.getBoolean("VerticalEnabled"));
    }

    public static class GantryStopSelection {
        public int currentIndex;
        public int currentCoordinate;
        public int stopCount;
        public int levelIndex;
        public int levelCoordinate;
        public int levelCount;
        public boolean verticalEnabled;

        public GantryStopSelection(int currentIndex, int levelIndex, int levelCount, int levelCoordinate,
                boolean verticalEnabled) {
            this.currentIndex = currentIndex;
            this.levelIndex = levelIndex;
            this.levelCount = levelCount;
            this.levelCoordinate = levelCoordinate;
            this.verticalEnabled = verticalEnabled;
        }
    }
}
