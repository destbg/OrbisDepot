package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Storage.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.Storage.VoidStorageManager;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepositUtils;
import com.destbg.OrbisDepot.Utils.InventoryUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class StorageSectionUI {

    private final OrbisDepotStorageContext context;
    private final List<String> displayedItemIds = new ArrayList<>();
    private String searchQuery = "";

    StorageSectionUI(OrbisDepotStorageContext context) {
        this.context = context;
    }

    String getSearchQuery() {
        return searchQuery;
    }

    void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim().toLowerCase() : "";
    }

    boolean hasStorageItemsChanged() {
        UUID owner = context.getOwnerUUID();
        if (owner == null) {
            return false;
        }

        String q = searchQuery.isEmpty() ? "" : searchQuery;
        Map<String, Long> items = q.isEmpty()
                ? VoidStorageManager.get().getItems(owner)
                : VoidStorageManager.get().searchItems(owner, q);

        List<String> currentIds = new ArrayList<>();
        for (Map.Entry<String, Long> entry : items.entrySet()) {
            if (entry.getValue() > 0) {
                currentIds.add(entry.getKey());
            }
        }
        return !currentIds.equals(displayedItemIds);
    }

    void build(@NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt) {
        UUID owner = context.getOwnerUUID();
        if (owner == null) {
            return;
        }

        int stackMultiplier = context.getStackMultiplier();

        String q = searchQuery.isEmpty() ? "" : searchQuery;
        Map<String, Long> items = q.isEmpty()
                ? VoidStorageManager.get().getItems(owner)
                : VoidStorageManager.get().searchItems(owner, q);

        displayedItemIds.clear();
        cmd.clear("#StorageCards");

        int i = 0;
        for (Map.Entry<String, Long> entry : items.entrySet()) {
            String itemId = entry.getKey();
            long quantity = entry.getValue();
            if (quantity <= 0) {
                continue;
            }

            displayedItemIds.add(itemId);
            String sel = "#StorageCards[" + i + "]";

            cmd.append("#StorageCards", "Pages/OrbisDepotStorageItem.ui");
            cmd.set(sel + " #Slot.ItemId", itemId);

            long maxForItem = (long) DepositUtils.getMaxStack(itemId) * stackMultiplier;
            String qtyText = InventoryUtils.formatQuantity(quantity) + " / " + InventoryUtils.formatQuantity(maxForItem);
            cmd.set(sel + " #QuantityLabel.Text", qtyText);

            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton", EventData.of(Constants.KEY_ACTION, "withdraw:" + itemId));
            evt.addEventBinding(CustomUIEventBindingType.RightClicking, sel + " #SlotButton", EventData.of(Constants.KEY_ACTION, "withdraw-one:" + itemId));

            i++;
        }
    }

    void update(@NonNullDecl UICommandBuilder cmd) {
        UUID owner = context.getOwnerUUID();
        if (owner == null) {
            return;
        }

        Map<String, Long> allItems = VoidStorageManager.get().getItems(owner);
        int stackMultiplier = context.getStackMultiplier();

        for (int i = 0; i < displayedItemIds.size(); i++) {
            String itemId = displayedItemIds.get(i);
            long quantity = allItems.getOrDefault(itemId, 0L);
            long maxForItem = (long) DepositUtils.getMaxStack(itemId) * stackMultiplier;
            String sel = "#StorageCards[" + i + "]";
            cmd.set(sel + " #QuantityLabel.Text", InventoryUtils.formatQuantity(quantity) + " / " + InventoryUtils.formatQuantity(maxForItem));
        }
    }

    void handleWithdraw(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String itemId, boolean singleItem, ItemTransferUtil transferUtil) {
        UUID owner = context.getOwnerUUID();
        if (owner == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Inventory inv = player.getInventory();
        if (inv == null) {
            return;
        }

        ItemContainer playerInv = inv.getCombinedHotbarFirst();
        if (playerInv == null) {
            return;
        }

        VoidStorageManager vsm = VoidStorageManager.get();
        long available = vsm.getItemCount(owner, itemId);
        if (available <= 0) {
            return;
        }

        int maxStack = DepositUtils.getMaxStack(itemId);
        int requestedAmount = singleItem ? 1 : (int) Math.min(maxStack, available);
        int given = transferUtil.giveToPlayer(playerInv, itemId, requestedAmount);
        if (given > 0) {
            vsm.removeItems(owner, itemId, given);
        }
    }
}
