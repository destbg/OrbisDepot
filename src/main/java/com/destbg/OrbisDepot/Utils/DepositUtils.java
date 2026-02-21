package com.destbg.OrbisDepot.Utils;

import com.destbg.OrbisDepot.Storage.VoidStorageManager;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class DepositUtils {

    private DepositUtils() {
    }

    public static boolean processSlot(@Nonnull ItemContainer container, short slot, @Nonnull UUID ownerUUID, int stackMultiplier) {
        if (slot < 0 || slot >= container.getCapacity()) {
            return true;
        }

        ItemStack stack = container.getItemStack(slot);
        if (stack == null || ItemStack.isEmpty(stack)) {
            return true;
        }

        if (isDepositEligible(stack)) {
            return true;
        }

        String itemId = stack.getItemId();
        int maxForItem = getMaxStack(stack) * stackMultiplier;
        long currentForItem = VoidStorageManager.get().getItemCount(ownerUUID, itemId);
        if (currentForItem >= maxForItem) {
            return false;
        }

        int qty = stack.getQuantity();

        if (qty <= 1) {
            container.removeItemStackFromSlot(slot);
        } else {
            container.setItemStackForSlot(slot, stack.withQuantity(qty - 1));
        }

        VoidStorageManager.get().addItems(ownerUUID, itemId, 1);
        notifyStorageChanged(ownerUUID);
        return false;
    }

    public static boolean isDepositEligible(@Nonnull ItemStack stack) {
        if (ItemStack.isEmpty(stack)) {
            return true;
        }
        Item item = stack.getItem();
        return item.getMaxStack() <= 1;
    }

    public static int getMaxStack(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        return item.getMaxStack();
    }

    public static int getMaxStack(@Nonnull String itemId) {
        return getMaxStack(new ItemStack(itemId, 1));
    }

    private static void notifyStorageChanged(@Nonnull UUID ownerUUID) {
        try {
            World world = Universe.get().getDefaultWorld();
            if (world != null) {
                CraftingUtils.onStorageChanged(ownerUUID, world);
            }
        } catch (Exception ignored) {
        }
    }
}
