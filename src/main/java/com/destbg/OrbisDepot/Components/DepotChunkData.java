package com.destbg.OrbisDepot.Components;

import com.destbg.OrbisDepot.Utils.ComponentUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class DepotChunkData implements Component<ChunkStore> {

    public static final BuilderCodec<DepotChunkData> CODEC = BuilderCodec.builder(DepotChunkData.class, DepotChunkData::new)
            .appendInherited(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
                    (state, container) -> { if (container != null) { state.itemContainer = container; } },
                    s -> s.itemContainer,
                    (o, p) -> { if (p.itemContainer != null) { o.itemContainer = p.itemContainer.clone(); } }).add()
            .appendInherited(new KeyedCodec<>("OwnerUUID", Codec.UUID_STRING),
                    (state, uuid) -> state.ownerUUID = uuid,
                    state -> state.ownerUUID,
                    (o, p) -> o.ownerUUID = p.ownerUUID).add()
            .build();

    private SimpleItemContainer itemContainer;
    private UUID ownerUUID;

    public DepotChunkData() {
        itemContainer = new SimpleItemContainer(Constants.DEPOT_SLOT_CAPACITY);
    }

    private DepotChunkData(DepotChunkData clone) {
        this.itemContainer = clone.itemContainer;
        this.ownerUUID = clone.ownerUUID;
    }

    public SimpleItemContainer getItemContainer() {
        return itemContainer;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        return new DepotChunkData(this);
    }

    public static ComponentType<ChunkStore, DepotChunkData> getComponentType() {
        return ComponentUtils.getDepotPlayerDataComponent();
    }
}
