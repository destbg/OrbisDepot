package com.destbg.OrbisDepot.Models;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Components.SigilPlayerData;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class OrbisSigilStorageModel implements OrbisDepotStorageContext {
    private final World world;
    private final SigilPlayerData sigilPlayerData;
    private final DepotStorageData depotStorageData;

    public OrbisSigilStorageModel(@Nonnull World world, @Nonnull SigilPlayerData sigilPlayerData, @Nonnull DepotStorageData depotStorageData) {
        this.world = world;
        this.sigilPlayerData = sigilPlayerData;
        this.depotStorageData = depotStorageData;
    }

    @NonNullDecl
    @Override
    public ItemContainer getUploadSlotContainer() {
        return sigilPlayerData.getItemContainer();
    }

    @Override
    public UUID getSavedAttunement() {
        return sigilPlayerData.getSelectedAttunement();
    }

    @Override
    public void setSavedAttunement(@Nullable UUID uuid) {
        sigilPlayerData.setSelectedAttunement(uuid);
    }

    @Override
    public int getDepositSlotCount() {
        return Constants.SIGIL_UPLOAD_SLOT_CAPACITY;
    }

    @Override
    public short getAdditionalStacks() {
        return Constants.SIGIL_UPLOAD_SLOT_ADDITIONAL_STACKS;
    }

    @Override
    public void resetUploadTimer() {
        depotStorageData.resetElapsedTime();
    }

    @Override
    public float getUploadProgress() {
        return depotStorageData.getElapsedTime() / depotStorageData.getTickInterval();
    }

    @Override
    public float getTickIntervalSeconds() {
        return depotStorageData.getTickInterval();
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
        return sigilPlayerData.getAutoRestore();
    }

    @Override
    public boolean isCraftingIntegrationEnabled() {
        return sigilPlayerData.setCraftingIntegration();
    }

    @Override
    public void setAutoRestore(boolean b) {
        sigilPlayerData.setAutoRestore(b);
    }

    @Override
    public void setCraftingIntegration(boolean b) {
        sigilPlayerData.setCraftingIntegration(b);
    }

    @Override
    public boolean isThrottleUiUpdates() {
        return sigilPlayerData.isThrottleUiUpdates();
    }

    @Override
    public void setThrottleUiUpdates(boolean b) {
        sigilPlayerData.setThrottleUiUpdates(b);
    }
}
