package com.destbg.OrbisDepot.Models;

import com.destbg.OrbisDepot.Components.CrudeSigilPlayerData;
import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class CrudeOrbisSigilStorageModel implements OrbisDepotStorageContext {
    private final World world;
    private final CrudeSigilPlayerData crudeSigilPlayerData;
    private final DepotStorageData depotStorageData;

    public CrudeOrbisSigilStorageModel(@Nonnull World world, @Nonnull CrudeSigilPlayerData crudeSigilPlayerData, @Nonnull DepotStorageData depotStorageData) {
        this.world = world;
        this.crudeSigilPlayerData = crudeSigilPlayerData;
        this.depotStorageData = depotStorageData;
    }

    @NonNullDecl
    @Override
    public ItemContainer getUploadSlotContainer() {
        return crudeSigilPlayerData.getItemContainer();
    }

    @Override
    public UUID getSavedAttunement() {
        return crudeSigilPlayerData.getSelectedAttunement();
    }

    @Override
    public void setSavedAttunement(@Nullable UUID uuid) {
        crudeSigilPlayerData.setSelectedAttunement(uuid);
    }

    @Override
    public int getDepositSlotCount() {
        return Constants.CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY;
    }

    @Override
    public short getAdditionalStacks() {
        return Constants.CRUDE_SIGIL_UPLOAD_SLOT_ADDITIONAL_STACKS;
    }

    @Override
    public void resetUploadTimer() {
        crudeSigilPlayerData.resetElapsedTime();
    }

    @Override
    public float getUploadProgress() {
        return crudeSigilPlayerData.getElapsedTime() / crudeSigilPlayerData.getTickInterval();
    }

    @Override
    public float getTickIntervalSeconds() {
        return Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS;
    }

    @NonNullDecl
    @Override
    public World getWorld() {
        return world;
    }

    @Nonnull
    @Override
    public DepotStorageData getDepotStorageData() {
        return depotStorageData;
    }

    @Override
    public boolean isAutoRestoreEnabled() {
        return false;
    }

    @Override
    public boolean isCraftingIntegrationEnabled() {
        return false;
    }

    @Override
    public void setAutoRestore(boolean b) {

    }

    @Override
    public void setCraftingIntegration(boolean b) {
    }

    @Override
    public boolean isThrottleUiUpdates() {
        return true;
    }

    @Override
    public void setThrottleUiUpdates(boolean b) {
    }
}
