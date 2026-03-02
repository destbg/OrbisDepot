package com.destbg.OrbisDepot.Systems;

import com.destbg.OrbisDepot.UI.OrbisDepotStorageUI;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class ProgressTickSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public void tick(float dt, int i, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        Player player = chunk.getComponent(i, Player.getComponentType());
        if (player == null) {
            return;
        }

        CustomUIPage page = player.getPageManager().getCustomPage();
        if (page instanceof OrbisDepotStorageUI depotPage) {
            depotPage.sendProgressUpdate();
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
}
