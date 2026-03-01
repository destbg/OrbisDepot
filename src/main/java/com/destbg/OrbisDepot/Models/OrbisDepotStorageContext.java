package com.destbg.OrbisDepot.Models;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public interface OrbisDepotStorageContext {
    @Nonnull
    ItemContainer getUploadSlotContainer();

    UUID getSavedAttunement();

    void setSavedAttunement(@Nullable UUID uuid);

    int getDepositSlotCount();

    short getAdditionalStacks();

    void resetUploadTimer();

    float getUploadProgress();

    float getTickIntervalSeconds();

    @Nonnull
    World getWorld();

    @Nonnull
    DepotStorageData getDepotStorageData();

    boolean isAutoRestoreEnabled();

    boolean isCraftingIntegrationEnabled();

    void setAutoRestore(boolean b);

    void setCraftingIntegration(boolean b);

    boolean isThrottleUiUpdates();

    void setThrottleUiUpdates(boolean b);
}
