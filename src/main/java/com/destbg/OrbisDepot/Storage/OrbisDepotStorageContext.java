package com.destbg.OrbisDepot.Storage;

import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.CrudeSigilSlotUtils;
import com.destbg.OrbisDepot.Utils.DepotSlotUtils;
import com.destbg.OrbisDepot.Utils.SigilSlotUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.UUID;

public sealed interface OrbisDepotStorageContext permits OrbisDepotStorageContext.Depot, OrbisDepotStorageContext.Sigil, OrbisDepotStorageContext.CrudeSigil {

    @Nonnull
    ItemContainer getUploadSlotContainer();

    boolean isAllowed(@Nonnull UUID playerUUID);

    UUID getOwnerUUID();

    int getDepositSlotCount();

    int getStackMultiplier();

    void resetUploadTimer();

    float getUploadProgress();

    @Nonnull
    World getWorld();

    record Depot(@Nonnull String posKey, @Nonnull UUID ownerUUID,
                 @Nonnull World world) implements OrbisDepotStorageContext {
        @Override
        @Nonnull
        public ItemContainer getUploadSlotContainer() {
            return DepotSlotUtils.getUploadSlotContainer(posKey);
        }

        @Override
        public boolean isAllowed(@Nonnull UUID playerUUID) {
            return ownerUUID.equals(playerUUID)
                    || AttunementManager.get().isAttunedTo(playerUUID, ownerUUID);
        }

        @Override
        public UUID getOwnerUUID() {
            return ownerUUID;
        }

        @Override
        public int getDepositSlotCount() {
            return Constants.DEPOT_CAPACITY;
        }

        @Override
        public int getStackMultiplier() {
            return 1;
        }

        @Override
        public void resetUploadTimer() {
            DepotSlotUtils.resetTimer(posKey);
        }

        @Override
        public float getUploadProgress() {
            float timer = DepotSlotUtils.getTimer(posKey);
            return Math.min(1f, timer / Constants.UPLOAD_INTERVAL_DEPOT_SECONDS);
        }

        @Override
        @Nonnull
        public World getWorld() {
            return world;
        }
    }

    record Sigil(@Nonnull PlayerRef playerRef, @Nonnull World world) implements OrbisDepotStorageContext {
        @Override
        @Nonnull
        public ItemContainer getUploadSlotContainer() {
            return SigilSlotUtils.getUploadSlotContainer(playerRef.getUuid());
        }

        @Override
        public boolean isAllowed(@Nonnull UUID playerUUID) {
            return playerRef.getUuid().equals(playerUUID);
        }

        @Override
        public UUID getOwnerUUID() {
            return playerRef.getUuid();
        }

        @Override
        public int getDepositSlotCount() {
            return Constants.SIGIL_UPLOAD_SLOT_CAPACITY;
        }

        @Override
        public int getStackMultiplier() {
            return 2;
        }

        @Override
        public void resetUploadTimer() {
            SigilSlotUtils.resetTimers(playerRef.getUuid());
        }

        @Override
        public float getUploadProgress() {
            UUID uuid = playerRef.getUuid();
            ItemContainer container = SigilSlotUtils.getUploadSlotContainer(uuid);
            for (short i = 0; i < container.getCapacity(); i++) {
                ItemStack stack = container.getItemStack(i);
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    float timer = SigilSlotUtils.getTimer(uuid, i);
                    return Math.min(1f, timer / Constants.UPLOAD_INTERVAL_SIGIL_SECONDS);
                }
            }
            return 0f;
        }

        @Override
        @Nonnull
        public World getWorld() {
            return world;
        }
    }

    record CrudeSigil(@Nonnull PlayerRef playerRef, @Nonnull World world) implements OrbisDepotStorageContext {
        @Override
        @Nonnull
        public ItemContainer getUploadSlotContainer() {
            return CrudeSigilSlotUtils.getUploadSlotContainer(playerRef.getUuid());
        }

        @Override
        public boolean isAllowed(@Nonnull UUID playerUUID) {
            return playerRef.getUuid().equals(playerUUID);
        }

        @Override
        public UUID getOwnerUUID() {
            return playerRef.getUuid();
        }

        @Override
        public int getDepositSlotCount() {
            return Constants.CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY;
        }

        @Override
        public int getStackMultiplier() {
            return 1;
        }

        @Override
        public void resetUploadTimer() {
            CrudeSigilSlotUtils.resetTimers(playerRef.getUuid());
        }

        @Override
        public float getUploadProgress() {
            UUID uuid = playerRef.getUuid();
            ItemContainer container = CrudeSigilSlotUtils.getUploadSlotContainer(uuid);
            for (short i = 0; i < container.getCapacity(); i++) {
                ItemStack stack = container.getItemStack(i);
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    float timer = CrudeSigilSlotUtils.getTimer(uuid, i);
                    return Math.min(1f, timer / Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS);
                }
            }
            return 0f;
        }

        @Override
        @Nonnull
        public World getWorld() {
            return world;
        }
    }
}
