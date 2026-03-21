package com.destbg.OrbisDepot.Crafting;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public final class DepotStorageItemContainer extends SimpleItemContainer {

    private final UUID playerUUID;
    private final DepotStorageData depotStorageData;

    public DepotStorageItemContainer(@Nonnull UUID playerUUID, @Nonnull DepotStorageData depotStorageData) {
        super((short) 1);
        this.playerUUID = playerUUID;
        this.depotStorageData = depotStorageData;
        refresh();
    }

    public void refresh() {
        Map<String, Integer> items = depotStorageData.getItemContainer();

        this.lock.writeLock().lock();
        try {
            this.items.clear();
            short slot = 0;
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                int qty = entry.getValue();
                if (qty <= 0) {
                    continue;
                }
                this.items.put(slot, new ItemStack(entry.getKey(), qty));
                slot++;
            }
            this.capacity = slot;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    protected ItemStack internal_setSlot(short slot, ItemStack stack) {
        ItemStack snapshotItem = getItemStack(slot);
        int prevQty = snapshotItem != null && !ItemStack.isEmpty(snapshotItem) ? snapshotItem.getQuantity() : 0;
        int curQty = stack != null && !ItemStack.isEmpty(stack) ? stack.getQuantity() : 0;
        int toRemove = prevQty - curQty;
        if (toRemove > 0) {
            int actuallyRemoved = depotStorageData.tryRemoveItems(snapshotItem.getItemId(), toRemove);
            if (actuallyRemoved <= 0) {
                return snapshotItem;
            }
        }
        ItemStack old = super.internal_setSlot(slot, stack);
        if (toRemove > 0) {
            CraftingUtils.onStorageChanged(playerUUID);
        }
        return old;
    }

    @Override
    protected ItemStack internal_removeSlot(short slot) {
        ItemStack snapshotItem = getItemStack(slot);
        if (snapshotItem == null || ItemStack.isEmpty(snapshotItem)) {
            return super.internal_removeSlot(slot);
        }
        int actuallyRemoved = depotStorageData.tryRemoveItems(snapshotItem.getItemId(), snapshotItem.getQuantity());
        if (actuallyRemoved <= 0) {
            return null;
        }
        ItemStack previous = super.internal_removeSlot(slot);
        CraftingUtils.onStorageChanged(playerUUID);
        return previous;
    }
}
