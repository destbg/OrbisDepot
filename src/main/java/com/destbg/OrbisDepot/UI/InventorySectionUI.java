package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Objects;

final class InventorySectionUI {

    private String[] lastSnapshot;

    boolean hasInventoryChanged(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return lastSnapshot != null;
        }

        Inventory inv = player.getInventory();
        if (inv == null) {
            return lastSnapshot != null;
        }

        ItemContainer combined = inv.getCombinedHotbarFirst();
        if (combined == null) {
            return lastSnapshot != null;
        }

        int slots = Math.clamp(combined.getCapacity(), 0, Constants.PLAYER_INVENTORY_DISPLAY_SLOTS);
        if (lastSnapshot == null || lastSnapshot.length != slots) {
            return true;
        }

        for (int i = 0; i < slots; i++) {
            ItemStack stack = combined.getItemStack((short) i);
            String current = null;
            if (stack != null && !ItemStack.isEmpty(stack)) {
                current = stack.getItemId() + ":" + stack.getQuantity();
            }
            if (!Objects.equals(current, lastSnapshot[i])) {
                return true;
            }
        }
        return false;
    }

    void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd,
               @NonNullDecl UIEventBuilder evt, @NonNullDecl Store<EntityStore> store) {
        cmd.clear("#PlayerInventoryCards");
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Inventory inv = player.getInventory();
        if (inv == null) {
            return;
        }

        ItemContainer combined = inv.getCombinedHotbarFirst();
        if (combined == null) {
            return;
        }

        short invCap = combined.getCapacity();
        int displaySlots = Math.clamp(invCap, 0, Constants.PLAYER_INVENTORY_DISPLAY_SLOTS);

        for (int row = 0; row < Constants.PLAYER_INVENTORY_DISPLAY_ROWS; row++) {
            cmd.appendInline("#PlayerInventoryCards", "Group { LayoutMode: Left; }");
            for (int col = 0; col < Constants.SLOTS_PER_ROW; col++) {
                int slotIndex;
                if (row < Constants.PLAYER_INVENTORY_DISPLAY_ROWS - 1) {
                    slotIndex = (row + 1) * Constants.SLOTS_PER_ROW + col;
                } else {
                    slotIndex = col;
                }
                cmd.append("#PlayerInventoryCards[" + row + "]", "Pages/OrbisDepotPlayerSlot.ui");
                ItemStack stack = (slotIndex < displaySlots) ? combined.getItemStack((short) slotIndex) : null;
                String sel = "#PlayerInventoryCards[" + row + "][" + col + "]";
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    cmd.set(sel + " #Slot.ItemId", stack.getItemId());
                    if (stack.getQuantity() > 1) {
                        cmd.set(sel + " #QuantityLabel.Text", String.valueOf(stack.getQuantity()));
                    }
                    String itemId = stack.getItemId();
                    if (!Constants.SIGIL_ITEM_ID.equals(itemId) && !Constants.CRUDE_SIGIL_ITEM_ID.equals(itemId)) {
                        evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                                EventData.of(Constants.KEY_ACTION, "deposit:" + slotIndex));
                    }
                }
            }
        }

        saveSnapshot(combined, displaySlots);
    }

    private void saveSnapshot(ItemContainer combined, int slots) {
        lastSnapshot = new String[slots];
        for (int i = 0; i < slots; i++) {
            ItemStack stack = combined.getItemStack((short) i);
            if (stack != null && !ItemStack.isEmpty(stack)) {
                lastSnapshot[i] = stack.getItemId() + ":" + stack.getQuantity();
            }
        }
    }
}
