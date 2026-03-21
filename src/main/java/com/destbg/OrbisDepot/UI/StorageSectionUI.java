package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepositUtils;
import com.destbg.OrbisDepot.Utils.InventoryUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.HashMap;
import java.util.Map;

public class StorageSectionUI {

    private final OrbisDepotStorageContext context;
    private String searchQuery = "";
    private DepotStorageData depotStorage;
    private final Map<String, Integer> renderedItemIndices = new HashMap<>();
    private final Map<String, Integer> lastRenderedQuantities = new HashMap<>();

    public StorageSectionUI(OrbisDepotStorageContext context) {
        this.context = context;
        depotStorage = context.getDepotStorageData();
    }

    public void setDepotStorage(DepotStorageData depotStorage) {
        this.depotStorage = depotStorage;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim().toLowerCase() : "";
    }

    public void build(@NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt) {
        short additionalStacks = context.getAdditionalStacks();

        String q = searchQuery.isEmpty() ? "" : searchQuery;
        Map<String, Integer> items = q.isEmpty()
                ? depotStorage.getItemContainer()
                : depotStorage.searchItems(q);

        cmd.clear("#StorageCards");
        renderedItemIndices.clear();
        lastRenderedQuantities.clear();

        int i = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemId = entry.getKey();
            int quantity = entry.getValue();
            if (quantity <= 0) {
                continue;
            }

            String sel = "#StorageCards[" + i + "]";

            cmd.append("#StorageCards", "Pages/OrbisDepotStorageItem.ui");
            cmd.set(sel + " #Slot.ItemId", itemId);

            int maxForItem = DepositUtils.getMaxStack(itemId, depotStorage.getStorageUpgradeRank(), additionalStacks);
            String qtyText = InventoryUtils.formatQuantity(quantity) + " / " + InventoryUtils.formatQuantity(maxForItem);
            cmd.set(sel + " #QuantityLabel.Text", qtyText);

            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton", EventData.of(Constants.KEY_ACTION, "withdraw:" + itemId));
            evt.addEventBinding(CustomUIEventBindingType.RightClicking, sel + " #SlotButton", EventData.of(Constants.KEY_ACTION, "withdraw-one:" + itemId));

            renderedItemIndices.put(itemId, i);
            lastRenderedQuantities.put(itemId, quantity);
            i++;
        }
    }

    public boolean buildTick(@NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt) {
        short additionalStacks = context.getAdditionalStacks();

        String q = searchQuery.isEmpty() ? "" : searchQuery;
        Map<String, Integer> items = q.isEmpty()
                ? depotStorage.getItemContainer()
                : depotStorage.searchItems(q);

        for (String itemId : items.keySet()) {
            if (!renderedItemIndices.containsKey(itemId)) {
                build(cmd, evt);
                return true;
            }
        }

        boolean changed = false;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemId = entry.getKey();
            int quantity = entry.getValue();
            Integer index = renderedItemIndices.get(itemId);
            if (index == null || quantity <= 0) {
                continue;
            }
            Integer last = lastRenderedQuantities.get(itemId);
            if (last != null && last == quantity) {
                continue;
            }
            String sel = "#StorageCards[" + index + "]";
            int maxForItem = DepositUtils.getMaxStack(itemId, depotStorage.getStorageUpgradeRank(), additionalStacks);
            String qtyText = InventoryUtils.formatQuantity(quantity) + " / " + InventoryUtils.formatQuantity(maxForItem);
            cmd.set(sel + " #QuantityLabel.Text", qtyText);
            lastRenderedQuantities.put(itemId, quantity);
            changed = true;
        }
        return changed;
    }

    public void handleWithdraw(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String itemId, boolean singleItem) {
        ItemContainer playerInv = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);

        int maxStack = DepositUtils.getMaxStack(itemId, depotStorage.getStorageUpgradeRank(), context.getAdditionalStacks());
        int requested = singleItem ? 1 : maxStack;
        int removed = depotStorage.tryRemoveItems(itemId, requested);
        if (removed <= 0) {
            return;
        }

        int given = InventoryUtils.giveToPlayer(playerInv, itemId, removed, context.getAdditionalStacks());
        if (given < removed) {
            depotStorage.addItem(itemId, removed - given);
        }
    }
}
