package com.destbg.OrbisDepot.Systems;

import com.destbg.OrbisDepot.Components.DepotChunkData;
import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.UI.OrbisDepotStorageUI;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepositUtils;
import com.destbg.OrbisDepot.Utils.DepotOwnerUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.UUID;

public class DepotTickingSystem extends EntityTickingSystem<ChunkStore> {
    @Override
    public void tick(float dt, int i, @NonNullDecl ArchetypeChunk<ChunkStore> chunk, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
        DepotChunkData data = chunk.getComponent(i, DepotChunkData.getComponentType());
        if (data == null) {
            return;
        }

        UUID ownerUuid = data.getOwnerUUID();
        if (ownerUuid == null) {
            return;
        }

        DepotStorageData storage = DepotOwnerUtils.getIfReady(ownerUuid);
        if (storage == null) {
            return;
        }

        storage.addElapsedTime(dt);
        if (storage.getElapsedTime() >= storage.getTickInterval()) {
            storage.resetElapsedTime();
            var itemContainer = data.getItemContainer();
            DepositUtils.attemptDeposit(itemContainer, storage, storage.getStorageUpgradeRank(), Constants.DEPOT_UPLOAD_SLOT_ADDITIONAL_STACKS);
            OrbisDepotStorageUI.notifyViewersOf(ownerUuid, storage);
        }
    }

    @NullableDecl
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(DepotChunkData.getComponentType());
    }
}
