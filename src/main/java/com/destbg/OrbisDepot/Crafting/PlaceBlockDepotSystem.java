package com.destbg.OrbisDepot.Crafting;

import com.destbg.OrbisDepot.Storage.PlayerSettingsManager;
import com.destbg.OrbisDepot.Storage.VoidStorageManager;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.destbg.OrbisDepot.Utils.DepotSlotUtils;
import com.destbg.OrbisDepot.Utils.InventoryUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlaceBlockDepotSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PlaceBlockDepotSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int entityIndex, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> buffer, PlaceBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null || ItemStack.isEmpty(itemInHand)) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        String itemId = itemInHand.getItemId();

        if (Constants.DEPOT_ITEM_ID.equals(itemId)) {
            Vector3i pos = event.getTargetBlock();
            String posKey = DepotSlotUtils.posKey(pos.getX(), pos.getY(), pos.getZ());
            DepotSlotUtils.registerDepot(posKey, uuid);
            LOGGER.at(Level.INFO).log("Registered depot owner %s at %s", uuid, posKey);
        }

        if (!PlayerSettingsManager.get().isAutoPlaceEnabled(uuid)) {
            return;
        }

        if (VoidStorageManager.get().getItemCount(uuid, itemId) <= 0) {
            return;
        }

        World world;
        try {
            world = Universe.get().getDefaultWorld();
        } catch (Exception e) {
            return;
        }
        if (world == null) {
            return;
        }

        int quantityBefore = itemInHand.getQuantity();

        CompletableFuture.runAsync(() -> {
            try {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    return;
                }

                Inventory inv = player.getInventory();
                if (inv == null) {
                    return;
                }

                if (!InventoryUtils.hasOrbisSigil(inv)) {
                    return;
                }

                ItemContainer hotbar = inv.getHotbar();
                if (hotbar == null) {
                    return;
                }

                byte activeSlot = inv.getActiveHotbarSlot();
                ItemStack currentStack = (activeSlot >= 0 && activeSlot < hotbar.getCapacity())
                        ? hotbar.getItemStack(activeSlot) : null;
                int currentQty = (currentStack != null && !ItemStack.isEmpty(currentStack)
                        && currentStack.getItemId().equals(itemId))
                        ? currentStack.getQuantity() : 0;

                if (currentQty >= quantityBefore) {
                    return;
                }

                VoidStorageManager.get().removeItems(uuid, itemId, 1);
                InventoryUtils.addToActiveSlot(hotbar, activeSlot, itemId);

                CraftingUtils.onStorageChanged(uuid, world);
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error refunding item from void storage");
            }
        }, world);
    }
}
