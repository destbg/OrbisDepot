package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.OrbisDepotStorageContext;
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

import java.util.Map;

public class StorageSectionUI {

    private final OrbisDepotStorageContext context;
    private String searchQuery = "";
    private DepotStorageData depotStorage;

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

            i++;
        }
    }

    public void handleWithdraw(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String itemId, boolean singleItem) {
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

        long available = depotStorage.getItemCount(itemId);
        if (available <= 0) {
            return;
        }

        int maxStack = DepositUtils.getMaxStack(itemId, depotStorage.getStorageUpgradeRank(), context.getAdditionalStacks());
        int requestedAmount = singleItem ? 1 : (int) Math.min(maxStack, available);
        int given = InventoryUtils.giveToPlayer(playerInv, itemId, requestedAmount, context.getAdditionalStacks());
        if (given > 0) {
            depotStorage.removeItems(itemId, given);
        }
    }
}
