package com.destbg.OrbisDepot.Utils;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import javax.annotation.Nullable;

public final class InventoryUtils {

    private InventoryUtils() {
    }

    public static boolean hasOrbisSigil(@Nullable Inventory inv) {
        if (inv == null) {
            return false;
        }
        if (containerContains(inv.getCombinedHotbarFirst(), Constants.SIGIL_ITEM_ID)) {
            return true;
        }
        return containerContains(inv.getBackpack(), Constants.SIGIL_ITEM_ID);
    }

    private static boolean containerContains(@Nullable ItemContainer container, String itemId) {
        if (container == null) {
            return false;
        }
        short cap = container.getCapacity();
        for (short i = 0; i < cap; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && !ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId())) {
                return true;
            }
        }
        return false;
    }

    public static void addToActiveSlot(ItemContainer hotbar, byte activeSlot, String itemId) {
        int maxStack = DepositUtils.getMaxStack(itemId);

        if (activeSlot >= 0 && activeSlot < hotbar.getCapacity()) {
            ItemStack existing = hotbar.getItemStack(activeSlot);
            if (existing != null && !ItemStack.isEmpty(existing)
                    && existing.getItemId().equals(itemId)
                    && existing.getQuantity() < maxStack) {
                hotbar.setItemStackForSlot(activeSlot, existing.withQuantity(existing.getQuantity() + 1));
                return;
            }
            if (existing == null || ItemStack.isEmpty(existing)) {
                hotbar.setItemStackForSlot(activeSlot, new ItemStack(itemId, 1));
                return;
            }
        }

        short cap = hotbar.getCapacity();
        for (short i = 0; i < cap; i++) {
            ItemStack existing = hotbar.getItemStack(i);
            if (existing != null && !ItemStack.isEmpty(existing)
                    && existing.getItemId().equals(itemId)
                    && existing.getQuantity() < maxStack) {
                hotbar.setItemStackForSlot(i, existing.withQuantity(existing.getQuantity() + 1));
                return;
            }
        }

        for (short i = 0; i < cap; i++) {
            ItemStack existing = hotbar.getItemStack(i);
            if (existing == null || ItemStack.isEmpty(existing)) {
                hotbar.setItemStackForSlot(i, new ItemStack(itemId, 1));
                return;
            }
        }
    }

    public static String formatQuantity(long qty) {
        if (qty >= 1_000_000) {
            return String.format("%.1fM", qty / 1_000_000.0);
        }
        if (qty >= 1_000) {
            return String.format("%.1fK", qty / 1_000.0);
        }
        return String.valueOf(qty);
    }
}
