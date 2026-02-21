package com.destbg.OrbisDepot.Crafting;

import com.destbg.OrbisDepot.Storage.VoidStorageManager;
import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public final class VoidStorageItemContainer extends SimpleItemContainer {

    private final UUID playerUUID;

    public VoidStorageItemContainer(@Nonnull UUID playerUUID) {
        super((short) 1);
        this.playerUUID = playerUUID;
        refresh();
    }

    public void refresh() {
        Map<String, Long> voidItems = VoidStorageManager.get().getItems(playerUUID);

        this.lock.writeLock().lock();
        try {
            this.items.clear();

            short slot = 0;
            for (Map.Entry<String, Long> entry : voidItems.entrySet()) {
                long qty = entry.getValue();
                if (qty <= 0) {
                    continue;
                }
                int clampedQty = (int) Math.min(qty, Integer.MAX_VALUE);
                this.items.put(slot, new ItemStack(entry.getKey(), clampedQty));
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
            VoidStorageManager.get().removeItems(playerUUID, previous.getItemId(), previous.getQuantity());
            notifyStorageChanged();
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
            VoidStorageManager.get().removeItems(playerUUID, previous.getItemId(), removed);
            notifyStorageChanged();
        }
    }

    private void notifyStorageChanged() {
        try {
            World world = Universe.get().getDefaultWorld();
            if (world != null) {
                CraftingUtils.onStorageChanged(playerUUID, world);
            }
        } catch (Exception ignored) {
        }
    }
}
