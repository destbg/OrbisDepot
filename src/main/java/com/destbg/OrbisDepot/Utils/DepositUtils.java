package com.destbg.OrbisDepot.Utils;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import javax.annotation.Nonnull;

public final class DepositUtils {
    public static void attemptDeposit(@Nonnull ItemContainer itemContainer, @Nonnull DepotStorageData storage, int rank, short additionalStacks) {
        for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
            ItemStack stack = itemContainer.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }

            if (DepositUtils.isDepositEligible(stack)) {
                continue;
            }

            String itemId = stack.getItemId();
            int maxForItem = DepositUtils.getMaxStack(stack.getItem(), rank, additionalStacks);
            long currentForItem = storage.getItemCount(itemId);
            if (currentForItem >= maxForItem) {
                continue;
            }

            if (storage.isStorageFull()) {
                continue;
            }

            int qty = stack.getQuantity();

            if (qty <= 1) {
                itemContainer.removeItemStackFromSlot(slot);
            } else {
                itemContainer.setItemStackForSlot(slot, stack.withQuantity(qty - 1));
            }

            storage.addItem(itemId, 1);
        }
    }

    public static boolean isDepositEligible(@Nonnull ItemStack stack) {
        if (ItemStack.isEmpty(stack)) {
            return true;
        }
        Item item = stack.getItem();
        return item.getMaxStack() <= 1;
    }

    public static int getMaxStack(@Nonnull Item item, int rank, short additionalStacks) {
        int stackCount = item.getMaxStack();
        int rankMultiplier = Constants.STORAGE_RANK_MULTIPLIERS[Math.min(rank - 1, Constants.STORAGE_RANK_MULTIPLIERS.length - 1)];
        return (stackCount * rankMultiplier) + (stackCount * additionalStacks);
    }

    public static int getMaxStack(@Nonnull String itemId, int rank, short additionalStacks) {
        Item itemAsset = Constants.ITEM_ASSET_MAP.getAsset(itemId);
        if (itemAsset == null) {
            return 1;
        }

        int stackCount = itemAsset.getMaxStack();
        int rankMultiplier = Constants.STORAGE_RANK_MULTIPLIERS[Math.min(rank - 1, Constants.STORAGE_RANK_MULTIPLIERS.length - 1)];
        return (stackCount * rankMultiplier) + (stackCount * additionalStacks);
    }
}
