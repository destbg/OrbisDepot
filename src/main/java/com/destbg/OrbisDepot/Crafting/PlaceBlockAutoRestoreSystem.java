package com.destbg.OrbisDepot.Crafting;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Components.SigilPlayerData;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.destbg.OrbisDepot.Utils.InventoryUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
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

public class PlaceBlockAutoRestoreSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PlaceBlockAutoRestoreSystem() {
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

        SigilPlayerData sigilData = store.getComponent(ref, SigilPlayerData.getComponentType());
        if (sigilData == null || !sigilData.getAutoRestore()) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        DepotStorageData depotStorage = DepotStorageManager.get().get(uuid);
        if (depotStorage == null) {
            return;
        }
        String itemId = itemInHand.getItemId();

        if (depotStorage.getItemCount(itemId) <= 0) {
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

                depotStorage.removeItems(itemId, 1);
                InventoryUtils.addToActiveSlot(hotbar, activeSlot, itemId, (short) 0);
                CraftingUtils.onStorageChanged(uuid);
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error refilling item from depot storage");
            }
        }, world);
    }
}
