package net.spindle.createwarehouse.entity.custom;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;

public class CrateVisual extends PackageVisual {
    public CrateVisual(VisualizationContext ctx, PackageEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
    }
}
