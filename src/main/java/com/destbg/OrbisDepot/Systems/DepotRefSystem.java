package com.destbg.OrbisDepot.Systems;

import com.destbg.OrbisDepot.Components.DepotChunkData;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.components.PlacedByInteractionComponent;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class DepotRefSystem extends RefChangeSystem<ChunkStore, PlacedByInteractionComponent> {

    @NonNullDecl
    @Override
    public ComponentType<ChunkStore, PlacedByInteractionComponent> componentType() {
        return InteractionModule.get().getPlacedByComponentType();
    }

    @Override
    public void onComponentAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl PlacedByInteractionComponent placedByInteractionComponent, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) {
            return;
        }

        PlacedByInteractionComponent placedBy = commandBuffer.getComponent(ref, InteractionModule.get().getPlacedByComponentType());
        if (placedBy == null) {
            return;
        }

        DepotChunkData generator = commandBuffer.getComponent(ref, DepotChunkData.getComponentType());
        if (generator != null) {
            generator.setOwnerUUID(placedBy.getWhoPlacedUuid());
            BlockSection section = commandBuffer.getComponent(info.getSectionRef(), BlockSection.getComponentType());
            if (section != null && section.setTicking(info.getIndex(), true)) {
                ChunkSection chunkSection = commandBuffer.getComponent(info.getSectionRef(), ChunkSection.getComponentType());
                if (chunkSection != null) {
                    chunkSection.markNeedsSaving();
                }
            }
        }
    }

    @Override
    public void onComponentSet(@NonNullDecl Ref<ChunkStore> ref, @NullableDecl PlacedByInteractionComponent placedByInteractionComponent, @NonNullDecl PlacedByInteractionComponent t1, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

    }

    @Override
    public void onComponentRemoved(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl PlacedByInteractionComponent placedByInteractionComponent, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

    }

    @NullableDecl
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                DepotChunkData.getComponentType(),
                PlacedByInteractionComponent.getComponentType()
        );
    }
}
