package com.destbg.OrbisDepot.Interactions;

import com.destbg.OrbisDepot.Components.DepotChunkData;
import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.OrbisDepotStorageModel;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Storage.LegacySlotMigration;
import com.destbg.OrbisDepot.UI.OrbisDepotStorageUI;
import com.destbg.OrbisDepot.Utils.BlockStateUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.SoundUtils;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class OrbisDepotOpenInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<OrbisDepotOpenInteraction> CODEC = BuilderCodec.builder(
            OrbisDepotOpenInteraction.class, OrbisDepotOpenInteraction::new, SimpleInstantInteraction.CODEC
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

        BlockPosition targetBlock = interactionContext.getTargetBlock();
        if (targetBlock == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        Holder<ChunkStore> holder = world.getBlockComponentHolder(targetBlock.x, targetBlock.y, targetBlock.z);
        if (holder == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        DepotChunkData depotChunkData = holder.getComponent(DepotChunkData.getComponentType());
        if (depotChunkData == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        UUID ownerUUID = depotChunkData.getOwnerUUID();
        if (ownerUUID == null) {
            String posKey = targetBlock.x + ":" + targetBlock.y + ":" + targetBlock.z;
            ownerUUID = LegacySlotMigration.readLegacyDepotOwner(posKey);
            if (ownerUUID == null) {
                interactionContext.getState().state = InteractionState.Failed;
                return;
            }
            depotChunkData.setOwnerUUID(ownerUUID);
            LegacySlotMigration.migrateDepotSlots(posKey, depotChunkData.getItemContainer());
        }

        boolean isOwner = playerRef.getUuid().equals(ownerUUID);
        DepotStorageData myStorage = DepotStorageManager.get().getOrCreate(playerRef.getUuid());
        boolean isAttuned = myStorage.isAttunedTo(ownerUUID);
        if (!isOwner && !isAttuned) {
            player.sendMessage(Message.raw("You are not attuned to this Orbis Depot.").color("#ff6b6b"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        DepotStorageData ownerStorage = DepotStorageManager.get().getOrCreate(ownerUUID);

        BlockStateUtils.setBlockInteractionState("OpenWindow", world, targetBlock.x, targetBlock.y, targetBlock.z);
        SoundUtils.playSFX("SFX_Chest_Wooden_Open", targetBlock.x, targetBlock.y, targetBlock.z, store);

        final DepotStorageData finalOwnerStorage = ownerStorage;
        final DepotStorageData finalMyStorage = myStorage;
        CompletableFuture.runAsync(() -> {
            try {
                Vector3i location = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
                OrbisDepotStorageModel context = new OrbisDepotStorageModel(world, depotChunkData, finalOwnerStorage, finalMyStorage, location);
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) {
                    p.getPageManager().openCustomPage(ref, store, new OrbisDepotStorageUI(playerRef, context));
                }
            } catch (Throwable t) {
                LOGGER.at(Level.SEVERE).withCause(t).log("Failed to open Depot UI");
            }
        }, world);
    }
}
