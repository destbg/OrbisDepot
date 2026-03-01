package com.destbg.OrbisDepot.Interactions;

import com.destbg.OrbisDepot.Components.CrudeSigilPlayerData;
import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.CrudeOrbisSigilStorageModel;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Storage.LegacySlotMigration;
import com.destbg.OrbisDepot.UI.OrbisDepotStorageUI;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.InventoryUtils;
import com.destbg.OrbisDepot.Utils.SoundUtils;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CrudeOrbisSigilOpenInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<CrudeOrbisSigilOpenInteraction> CODEC = BuilderCodec.builder(
            CrudeOrbisSigilOpenInteraction.class, CrudeOrbisSigilOpenInteraction::new, SimpleInstantInteraction.CODEC
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

        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), Constants.PERM_SIGIL_USE)) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        if (InventoryUtils.hasOrbisSigil(player.getInventory())) {
            player.sendMessage(Message.raw("You cannot use the Crude Orbis Sigil while you have the fully restored version on you.").color("#ff6b6b"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        CrudeSigilPlayerData sigilPlayerData = commandBuffer.getComponent(ref, CrudeSigilPlayerData.getComponentType());
        if (sigilPlayerData == null) {
            sigilPlayerData = new CrudeSigilPlayerData();
            sigilPlayerData.setSelectedAttunement(playerRef.getUuid());
            LegacySlotMigration.migrateCrudeSigilSlots(playerRef.getUuid(), sigilPlayerData.getItemContainer());
            commandBuffer.putComponent(ref, CrudeSigilPlayerData.getComponentType(), sigilPlayerData);
        }

        DepotStorageData depotStorageData = DepotStorageManager.get().getOrCreate(playerRef.getUuid());

        final CrudeSigilPlayerData finalSigilPlayerData = sigilPlayerData;
        final DepotStorageData finalDepotStorageData = depotStorageData;
        CompletableFuture.runAsync(() -> {
            try {
                CrudeOrbisSigilStorageModel context = new CrudeOrbisSigilStorageModel(world, finalSigilPlayerData, finalDepotStorageData);
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) {
                    AnimationUtils.playAnimation(ref, AnimationSlot.Action,
                            Constants.SIGIL_ANIM_SET, Constants.SIGIL_OPEN_ANIM,
                            true, store);
                    SoundUtils.playSFXToPlayer("SFX_Orbis_Sigil_Open_Local", ref, store);
                    p.getPageManager().openCustomPage(ref, store, new OrbisDepotStorageUI(playerRef, context));
                }
            } catch (Throwable t) {
                LOGGER.at(Level.SEVERE).withCause(t).log("Failed to open Crude Sigil UI");
            }
        }, world);
    }
}
