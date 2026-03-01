package com.destbg.OrbisDepot.Systems;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Components.SigilPlayerData;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.UI.OrbisDepotStorageUI;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepositUtils;
import com.destbg.OrbisDepot.Utils.DepotOwnerUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class SigilTickingSystem extends EntityTickingSystem<EntityStore> {
    @Override
    public void tick(float v, int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        SigilPlayerData data = archetypeChunk.getComponent(i, SigilPlayerData.getComponentType());
        if (data == null) {
            return;
        }

        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        if (player == null) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID playerUUID = playerRef.getUuid();
        DepotStorageData playerStorage = DepotStorageManager.get().getOrCreate(playerUUID);

        UUID selectedAttunement = data.getSelectedAttunement();
        DepotStorageData targetStorage;
        if (selectedAttunement == null || selectedAttunement.equals(playerUUID)) {
            targetStorage = playerStorage;
        } else if (playerStorage.isAttunedTo(selectedAttunement)) {
            targetStorage = DepotOwnerUtils.getIfReady(selectedAttunement);
            if (targetStorage == null) {
                return;
            }
        } else {
            targetStorage = playerStorage;
        }

        playerStorage.addElapsedTime(v);
        if (playerStorage.getElapsedTime() >= playerStorage.getTickInterval()) {
            playerStorage.resetElapsedTime();
            SimpleItemContainer itemContainer = data.getItemContainer();
            DepositUtils.attemptDeposit(itemContainer, targetStorage, targetStorage.getStorageUpgradeRank(), Constants.SIGIL_UPLOAD_SLOT_ADDITIONAL_STACKS);

            UUID depositedToUUID = (selectedAttunement != null && !selectedAttunement.equals(playerUUID))
                    ? selectedAttunement
                    : playerUUID;
            OrbisDepotStorageUI.notifyViewersOf(depositedToUUID, targetStorage);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(SigilPlayerData.getComponentType());
    }
}
