package net.spindle.createwarehouse.block.custom;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.spindle.createwarehouse.block.ModBlockEntities;

public class ForkliftBlock extends MultiblockDecorationBlock implements IBE<ForkliftBlockEntity> {
    public ForkliftBlock(Properties properties, VoxelShape shape, PartPlacement... parts) {
        super(properties, shape, parts);
    }

    @Override
    public Class<ForkliftBlockEntity> getBlockEntityClass() {
        return ForkliftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ForkliftBlockEntity> getBlockEntityType() {
        return ModBlockEntities.FORKLIFT.get();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
