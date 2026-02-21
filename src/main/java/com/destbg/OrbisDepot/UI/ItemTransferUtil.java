package com.destbg.OrbisDepot.UI;

import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

@FunctionalInterface
interface ItemTransferUtil {
    int giveToPlayer(ItemContainer playerInv, String itemId, int amount);
}
