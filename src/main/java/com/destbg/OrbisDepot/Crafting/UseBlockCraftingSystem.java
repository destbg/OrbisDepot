package com.destbg.OrbisDepot.Crafting;

import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UseBlockCraftingSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {

    public UseBlockCraftingSystem() {
        super(UseBlockEvent.Post.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int entityIndex, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> buffer, @NonNullDecl UseBlockEvent.Post event) {
        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        World world = store.getExternalData().getWorld();
        CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS)
                .execute(() -> CraftingUtils.onBlockUsed(ref, store, world));
    }
}
