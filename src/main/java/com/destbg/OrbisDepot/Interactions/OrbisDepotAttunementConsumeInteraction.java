package com.destbg.OrbisDepot.Interactions;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public class OrbisDepotAttunementConsumeInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OrbisDepotAttunementConsumeInteraction> CODEC = BuilderCodec.builder(
            OrbisDepotAttunementConsumeInteraction.class, OrbisDepotAttunementConsumeInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Inventory inv = player.getInventory();
        if (inv == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        ItemContainer hotbar = inv.getHotbar();
        byte activeSlot = inv.getActiveHotbarSlot();
        if (hotbar == null || activeSlot < 0 || activeSlot >= hotbar.getCapacity()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack heldItem = hotbar.getItemStack(activeSlot);
        if (heldItem == null || ItemStack.isEmpty(heldItem)
                || !Constants.ATTUNEMENT_ITEM_ID.equals(heldItem.getItemId())) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        UUID myUUID = playerRef.getUuid();
        String myName = playerRef.getUsername();

        String crafterUuidStr = heldItem.getFromMetadataOrNull(Constants.META_CRAFTER_UUID, Codec.STRING);
        String crafterName = heldItem.getFromMetadataOrNull(Constants.META_CRAFTER_NAME, Codec.STRING);

        if (crafterUuidStr == null) {
            player.sendMessage(Message.raw("This attunement hasn't been bound yet.").color("#ffa502"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        UUID crafterUUID;
        try {
            crafterUUID = UUID.fromString(crafterUuidStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("This attunement item is corrupted.").color("#ff6b6b"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        if (crafterUUID.equals(myUUID)) {
            player.sendMessage(Message.raw("You can't attune to your own Depot.").color("#ff6b6b"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        String displayName = crafterName != null ? crafterName : crafterUuidStr;

        DepotStorageData myStorage = DepotStorageManager.get().getOrCreate(myUUID);

        if (myStorage.isAttunedTo(crafterUUID)) {
            player.sendMessage(Message.raw("You are already attuned to " + displayName + "'s Depot.").color("#ffa502"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        myStorage.attuneToMe(crafterUUID, displayName);
        DepotStorageManager.get().getOrCreate(crafterUUID).attune(myUUID, myName);

        int newQty = heldItem.getQuantity() - 1;
        if (newQty <= 0) {
            hotbar.setItemStackForSlot(activeSlot, null);
        } else {
            hotbar.setItemStackForSlot(activeSlot, heldItem.withQuantity(newQty));
        }

        player.sendMessage(Message.raw("You are now attuned to " + displayName + "'s Orbis Depot!").color("#2ed573"));
    }
}
