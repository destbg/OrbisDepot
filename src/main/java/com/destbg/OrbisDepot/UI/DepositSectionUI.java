package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Models.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepositUtils;
import com.destbg.OrbisDepot.Utils.InventoryUtils;
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

public class DepositSectionUI {

    private final OrbisDepotStorageContext context;
    public DepositSectionUI(OrbisDepotStorageContext context) {
        this.context = context;
    }

    public void build(@NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt) {
        ItemContainer depositSlots = context.getUploadSlotContainer();
        int slotCount = context.getDepositSlotCount();

        cmd.clear("#DepositSlots");
        boolean hasItems = false;
        for (int i = 0; i < slotCount; i++) {
            String sel = "#DepositSlots[" + i + "]";
            cmd.append("#DepositSlots", "Pages/OrbisDepotPlayerSlot.ui");

            if (i < depositSlots.getCapacity()) {
                ItemStack stack = depositSlots.getItemStack((short) i);

                if (stack != null && !ItemStack.isEmpty(stack)) {
                    hasItems = true;
                    cmd.set(sel + " #Slot.ItemId", stack.getItemId());

                    if (stack.getQuantity() > 1) {
                        cmd.set(sel + " #QuantityLabel.Text", String.valueOf(stack.getQuantity()));
                    }

                    evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton", EventData.of(Constants.KEY_ACTION, "cancel-upload:" + i));
                }
            }
        }

        applyProgressStatus(cmd, hasItems);
    }

    public void handleDeposit(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String slotStr) {
        int slotIndex;
        try {
            slotIndex = Integer.parseInt(slotStr);
        } catch (NumberFormatException e) {
            return;
        }

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

        if (slotIndex < 0 || slotIndex >= playerInv.getCapacity()) {
            return;
        }

        ItemStack clickedStack = playerInv.getItemStack((short) slotIndex);
        if (clickedStack == null || ItemStack.isEmpty(clickedStack)) {
            return;
        }

        ItemContainer depositSlots = context.getUploadSlotContainer();
        int depositSlotCount = context.getDepositSlotCount();

        short targetSlot = -1;
        for (short s = 0; s < Math.min(depositSlotCount, depositSlots.getCapacity()); s++) {
            ItemStack existing = depositSlots.getItemStack(s);
            if (existing != null && !ItemStack.isEmpty(existing) && existing.getItemId().equals(clickedStack.getItemId())) {
                targetSlot = s;
                break;
            }
        }
        if (targetSlot < 0) {
            for (short s = 0; s < Math.min(depositSlotCount, depositSlots.getCapacity()); s++) {
                ItemStack existing = depositSlots.getItemStack(s);
                if (existing == null || ItemStack.isEmpty(existing)) {
                    targetSlot = s;
                    break;
                }
            }
        }

        if (targetSlot < 0) {
            targetSlot = 0;
        }

        ItemStack existingInSlot = depositSlots.getItemStack(targetSlot);

        if (existingInSlot != null && !ItemStack.isEmpty(existingInSlot)) {
            if (!existingInSlot.getItemId().equals(clickedStack.getItemId())) {
                InventoryUtils.giveToPlayer(playerInv, existingInSlot, existingInSlot.getQuantity(), context.getAdditionalStacks());
                depositSlots.removeItemStackFromSlot(targetSlot);
                existingInSlot = null;
            }
        }

        int maxStack = DepositUtils.getMaxStack(clickedStack.getItem(), 1, context.getAdditionalStacks());
        int currentInSlot = (existingInSlot != null && !ItemStack.isEmpty(existingInSlot)) ? existingInSlot.getQuantity() : 0;
        int spaceInSlot = maxStack - currentInSlot;
        int toDeposit = Math.min(clickedStack.getQuantity(), spaceInSlot);

        if (toDeposit <= 0) {
            return;
        }

        if (currentInSlot > 0) {
            depositSlots.setItemStackForSlot(targetSlot, existingInSlot.withQuantity(currentInSlot + toDeposit));
        } else {
            depositSlots.setItemStackForSlot(targetSlot, new ItemStack(clickedStack.getItemId(), toDeposit));
        }

        int remaining = clickedStack.getQuantity() - toDeposit;
        if (remaining <= 0) {
            playerInv.removeItemStackFromSlot((short) slotIndex);
        } else {
            playerInv.setItemStackForSlot((short) slotIndex, clickedStack.withQuantity(remaining));
        }

        context.resetUploadTimer();
    }

    public void handleCancelUpload(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String slotStr) {
        int depositSlotIndex;
        try {
            depositSlotIndex = Integer.parseInt(slotStr);
        } catch (NumberFormatException e) {
            return;
        }

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

        ItemContainer depositSlots = context.getUploadSlotContainer();
        if (depositSlotIndex < 0 || depositSlotIndex >= depositSlots.getCapacity()) {
            return;
        }

        ItemStack stack = depositSlots.getItemStack((short) depositSlotIndex);
        if (stack == null || ItemStack.isEmpty(stack)) {
            return;
        }

        int given = InventoryUtils.giveToPlayer(playerInv, stack, stack.getQuantity(), context.getAdditionalStacks());
        if (given > 0) {
            int remaining = stack.getQuantity() - given;
            if (remaining <= 0) {
                depositSlots.removeItemStackFromSlot((short) depositSlotIndex);
            } else {
                depositSlots.setItemStackForSlot((short) depositSlotIndex, stack.withQuantity(remaining));
            }
        }

        context.resetUploadTimer();
    }

    private void applyProgressStatus(@NonNullDecl UICommandBuilder cmd, boolean hasItems) {
        String statusText;
        if (hasItems && hasNonStackableInSlots()) {
            statusText = "Can't deposit non-stackable items";
        } else if (!hasItems) {
            statusText = "Place items";
        } else {
            float tickInterval = context.getTickIntervalSeconds();
            int itemsPerMin = (int) Math.round(60.0 / tickInterval);
            statusText = "Depositing " + itemsPerMin + " items/slot/min";
        }
        cmd.set("#DepositStatusLabel.Text", statusText);
    }

    private boolean hasNonStackableInSlots() {
        ItemContainer depositSlots = context.getUploadSlotContainer();
        for (short i = 0; i < depositSlots.getCapacity(); i++) {
            ItemStack stack = depositSlots.getItemStack(i);
            if (stack != null && !ItemStack.isEmpty(stack) && DepositUtils.isDepositEligible(stack)) {
                return true;
            }
        }
        return false;
    }
}
