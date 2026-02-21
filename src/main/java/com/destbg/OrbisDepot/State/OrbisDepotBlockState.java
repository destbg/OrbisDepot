package com.destbg.OrbisDepot.State;

import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepotSlotUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.state.TickableBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.BreakValidatedBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class OrbisDepotBlockState extends ItemContainerState implements BreakValidatedBlockState, TickableBlockState {

    public static final BuilderCodec<OrbisDepotBlockState> CODEC = BuilderCodec.builder(OrbisDepotBlockState.class, OrbisDepotBlockState::new)
            .append(new KeyedCodec<>("Custom", Codec.BOOLEAN),
                    (_, _) -> {
                    },
                    _ -> false).add()
            .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
                    (state, container) -> {
                        if (container != null) {
                            setParentContainerField(state, container);
                        }
                    },
                    OrbisDepotBlockState::getNativeContainer).add()
            .append(new KeyedCodec<>("OwnerUUID", Codec.UUID_STRING),
                    (state, uuid) -> state.ownerUUID = uuid,
                    state -> state.ownerUUID).add()
            .append(new KeyedCodec<>("OwnerName", Codec.STRING),
                    (state, name) -> state.ownerName = name,
                    state -> state.ownerName).add()
            .build();

    private static void setParentContainerField(OrbisDepotBlockState state, SimpleItemContainer container) {
        try {
            java.lang.reflect.Field f = ItemContainerState.class.getDeclaredField("itemContainer");
            f.setAccessible(true);
            f.set(state, container);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set itemContainer field", e);
        }
    }

    private SimpleItemContainer getNativeContainer() {
        try {
            java.lang.reflect.Field f = ItemContainerState.class.getDeclaredField("itemContainer");
            f.setAccessible(true);
            return (SimpleItemContainer) f.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ItemContainer getItemContainer() {
        if (positionKey != null) {
            return DepotSlotUtils.getUploadSlotContainer(positionKey);
        }
        return super.getItemContainer();
    }

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private String ownerName;
    @Nullable
    private transient String positionKey;

    public OrbisDepotBlockState() {
        super();
    }

    @Override
    public boolean initialize(@NonNullDecl BlockType blockType) {
        boolean result = super.initialize(blockType);
        if (!result) {
            return false;
        }
        ItemContainer current = getItemContainer();
        if (current != null && current.getCapacity() != Constants.DEPOT_CAPACITY) {
            SimpleItemContainer resized = new SimpleItemContainer(Constants.DEPOT_CAPACITY);
            for (short i = 0; i < Math.min(current.getCapacity(), Constants.DEPOT_CAPACITY); i++) {
                ItemStack stack = current.getItemStack(i);
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    resized.setItemStackForSlot(i, stack);
                }
            }
            setParentContainerField(this, resized);
            resized.registerChangeEvent(EventPriority.LAST, this::onItemChange);
        }
        return true;
    }

    public boolean isOwner(@Nonnull UUID playerUUID) {
        return ownerUUID != null && ownerUUID.equals(playerUUID);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwner(@Nonnull UUID uuid, @Nonnull String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
    }

    public void setPositionKey(@Nonnull String posKey) {
        this.positionKey = posKey;
    }

    @Override
    public void tick(float deltaTime, int index, ArchetypeChunk<ChunkStore> chunk, Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {
        if (positionKey == null) {
            Ref<ChunkStore> ref = chunk.getReferenceTo(index);
            BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
            if (info == null) {
                return;
            }

            WorldChunk wc = store.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
            if (wc == null) {
                return;
            }

            int blockIndex = info.getIndex();
            int worldX = wc.getX() * 32 + ChunkUtil.xFromBlockInColumn(blockIndex);
            int y = ChunkUtil.yFromBlockInColumn(blockIndex);
            int worldZ = wc.getZ() * 32 + ChunkUtil.zFromBlockInColumn(blockIndex);

            positionKey = DepotSlotUtils.posKey(worldX, y, worldZ);
        }

        if (ownerUUID == null) {
            ownerUUID = DepotSlotUtils.getOwner(positionKey);
            if (ownerUUID == null) {
                return;
            }
        }

        DepotSlotUtils.registerDepot(positionKey, ownerUUID);
    }

    @Override
    public boolean canOpen(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        return true;
    }

    @Override
    public boolean canDestroy(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        return true;
    }

    @Override
    public void onItemChange(ItemContainer.ItemContainerChangeEvent event) {
        super.onItemChange(event);
    }
}
