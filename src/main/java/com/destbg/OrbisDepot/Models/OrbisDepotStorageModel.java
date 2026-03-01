package com.destbg.OrbisDepot.Models;

import com.destbg.OrbisDepot.Components.DepotChunkData;
import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class OrbisDepotStorageModel implements OrbisDepotStorageContext {
    private final World world;
    private final DepotChunkData depotChunkData;
    private final DepotStorageData ownerStorageData;
    private final DepotStorageData playerStorageData;
    private final Vector3i location;

    public OrbisDepotStorageModel(@Nonnull World world, @Nonnull DepotChunkData depotChunkData, @Nonnull DepotStorageData ownerStorageData, @Nonnull DepotStorageData playerStorageData, @Nonnull Vector3i location) {
        this.world = world;
        this.depotChunkData = depotChunkData;
        this.ownerStorageData = ownerStorageData;
        this.playerStorageData = playerStorageData;
        this.location = location;
    }

    @SuppressWarnings("deprecation")
    private DepotChunkData getLiveData() {
        DepotChunkData state = (DepotChunkData) world.getState(location.getX(), location.getY(), location.getZ(), true);
        if (state != null) {
            return state;
        }
        return depotChunkData;
    }

    @NonNullDecl
    @Override
    public ItemContainer getUploadSlotContainer() {
        return getLiveData().getItemContainer();
    }

    @Override
    public UUID getSavedAttunement() {
        return getLiveData().getOwnerUUID();
    }

    @Override
    public void setSavedAttunement(@Nullable UUID uuid) {
        getLiveData().setOwnerUUID(uuid);
    }

    @Override
    public int getDepositSlotCount() {
        return Constants.DEPOT_SLOT_CAPACITY;
    }

    @Override
    public short getAdditionalStacks() {
        return Constants.DEPOT_UPLOAD_SLOT_ADDITIONAL_STACKS;
    }

    @Override
    public void resetUploadTimer() {
        ownerStorageData.resetElapsedTime();
    }

    @Override
    public float getUploadProgress() {
        return ownerStorageData.getElapsedTime() / ownerStorageData.getTickInterval();
    }

    @Override
    public float getTickIntervalSeconds() {
        return ownerStorageData.getTickInterval();
    }

    @NonNullDecl
    @Override
    public World getWorld() {
        return this.world;
    }

    @Nonnull
    @Override
    public DepotStorageData getDepotStorageData() {
        return playerStorageData;
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
        return false;
    }

    @Override
    public void setThrottleUiUpdates(boolean b) {
    }

    @Nonnull
    public Vector3i getLocation() {
        return location;
    }
}
