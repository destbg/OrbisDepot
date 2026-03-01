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
        ItemStack previous = super.internal_setSlot(slot, stack);
        syncRemoval(previous, stack);
        return previous;
    }

    @Override
    protected ItemStack internal_removeSlot(short slot) {
        ItemStack previous = super.internal_removeSlot(slot);
        if (previous != null && !ItemStack.isEmpty(previous)) {
            depotStorageData.removeItems(previous.getItemId(), previous.getQuantity());
            CraftingUtils.onStorageChanged(playerUUID);
        }
        return previous;
    }

    private void syncRemoval(ItemStack previous, ItemStack current) {
        if (previous == null || ItemStack.isEmpty(previous)) {
            return;
        }
        int prevQty = previous.getQuantity();
        int curQty = (current != null && !ItemStack.isEmpty(current)) ? current.getQuantity() : 0;
        int removed = prevQty - curQty;
        if (removed > 0) {
            depotStorageData.removeItems(previous.getItemId(), removed);
            CraftingUtils.onStorageChanged(playerUUID);
        }
    }
}
