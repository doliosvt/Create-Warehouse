package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.spindle.createwarehouse.item.ModItems;
import net.spindle.createwarehouse.item.custom.CrateItem;

import java.util.List;

public class CratePackagerBlockEntity extends PackagerBlockEntity {
    public CratePackagerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void attemptToSend(List<PackagingRequest> queuedRequests) {
        boolean heldSlotWasEmpty = heldBox.isEmpty();
        int previousQueueSize = queuedExitingPackages.size();

        super.attemptToSend(queuedRequests);

        if (heldSlotWasEmpty && !heldBox.isEmpty())
            heldBox = asCrate(heldBox);

        for (int index = previousQueueSize; index < queuedExitingPackages.size(); index++) {
            BigItemStack queued = queuedExitingPackages.get(index);
            queued.stack = asCrate(queued.stack);
        }
    }

    private static ItemStack asCrate(ItemStack stack) {
        if (stack.isEmpty() || CrateItem.isCrate(stack))
            return stack;
        return stack.transmuteCopy(ModItems.CRATE_ITEM.get());
    }
}
