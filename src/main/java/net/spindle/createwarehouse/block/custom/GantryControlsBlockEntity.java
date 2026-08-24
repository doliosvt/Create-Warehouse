package net.spindle.createwarehouse.block.custom;

import java.util.List;

import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity.GantryLevel;
import net.spindle.createwarehouse.block.custom.GantryControllerBlockEntity.GantryStop;

public class GantryControlsBlockEntity extends ContraptionControlsBlockEntity {
    private int selectedIndex;
    private int selectedLevelIndex;
    private boolean verticalSelectionEnabled = true;
    private List<GantryStop> cachedStops = List.of();
    private List<GantryLevel> cachedLevels = List.of();
    private int scanCooldown;

    public GantryControlsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;
        if (scanCooldown-- <= 0) {
            refreshStops();
            scanCooldown = 40;
        }
    }

    public List<GantryStop> getStops() {
        refreshStops();
        return cachedStops;
    }

    public int getLevelCount() {
        refreshStops();
        return cachedLevels.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getSelectedLevelIndex() {
        return selectedLevelIndex;
    }

    public boolean isVerticalSelectionEnabled() {
        return verticalSelectionEnabled;
    }

    public String getAddress() {
        return GantryAddress.format(selectedIndex, selectedLevelIndex);
    }

    public int getSelectedLevelCoordinate() {
        if (cachedLevels.isEmpty() || selectedLevelIndex >= cachedLevels.size())
            return 0;
        return cachedLevels.get(selectedLevelIndex).coordinate();
    }

    public void setAddress(int contactIndex, int levelIndex) {
        selectedIndex = Mth.clamp(contactIndex, 0, 255);
        selectedLevelIndex = Mth.clamp(levelIndex, 0, 255);
        verticalSelectionEnabled = true;
        setChanged();
        if (level != null && !level.isClientSide())
            sendData();
    }

    private void refreshStops() {
        if (level == null)
            return;
        cachedStops = GantryControllerBlockEntity.findStopsFromReference(level, worldPosition);
        cachedLevels = GantryControllerBlockEntity.findVerticalLevelsFromReference(level, worldPosition);
    }

    @Override
    public void clearContent() {
        setAddress(0, 0);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Selection", selectedIndex);
        tag.putInt("LevelSelection", selectedLevelIndex);
        tag.putBoolean("VerticalEnabled", verticalSelectionEnabled);
        tag.putInt("LevelCount", cachedLevels.size());
        tag.putInt("LevelCoordinate", getSelectedLevelCoordinate());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        selectedIndex = Mth.clamp(tag.getInt("Selection"), 0, 255);
        selectedLevelIndex = Mth.clamp(tag.getInt("LevelSelection"), 0, 255);
        verticalSelectionEnabled = !tag.contains("VerticalEnabled") || tag.getBoolean("VerticalEnabled");
    }
}
