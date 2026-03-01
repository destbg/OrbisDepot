package com.destbg.OrbisDepot.Components;

import com.destbg.OrbisDepot.Utils.ComponentUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public class DepotChunkData extends ItemContainerState {

    public static final BuilderCodec<DepotChunkData> CODEC = BuilderCodec.builder(DepotChunkData.class, DepotChunkData::new)
            .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
                    (state, container) -> {
                        if (container != null) {
                            state.itemContainer = container;
                        }
                    },
                    s -> s.itemContainer).add()
            .append(new KeyedCodec<>("OwnerUUID", Codec.UUID_STRING),
                    (state, uuid) -> state.ownerUUID = uuid,
                    state -> state.ownerUUID).add()
            .build();

    private UUID ownerUUID;

    @Override
    public boolean initialize(@NonNullDecl BlockType blockType) {
        boolean result = super.initialize(blockType);
        if (!result) {
            return false;
        }
        ItemContainer current = getItemContainer();
        if (current != null && current.getCapacity() != Constants.DEPOT_SLOT_CAPACITY) {
            SimpleItemContainer resized = new SimpleItemContainer(Constants.DEPOT_SLOT_CAPACITY);
            for (short i = 0; i < Math.min(current.getCapacity(), Constants.DEPOT_SLOT_CAPACITY); i++) {
                ItemStack stack = current.getItemStack(i);
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    resized.setItemStackForSlot(i, stack);
                }
            }
            super.itemContainer = resized;
            resized.registerChangeEvent(EventPriority.LAST, this::onItemChange);
        }
        return true;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public static ComponentType<ChunkStore, DepotChunkData> getComponentType() {
        return ComponentUtils.getDepotPlayerDataComponent();
    }
}
