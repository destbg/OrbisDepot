package com.destbg.OrbisDepot.Interactions;

import com.destbg.OrbisDepot.Main;
import com.destbg.OrbisDepot.State.OrbisDepotBlockState;
import com.destbg.OrbisDepot.Storage.AttunementManager;
import com.destbg.OrbisDepot.Storage.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.UI.OrbisDepotStorageUI;
import com.destbg.OrbisDepot.Utils.BlockStateUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepotSlotUtils;
import com.destbg.OrbisDepot.Utils.SoundUtils;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
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

public class OpenOrbisDepotInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<OpenOrbisDepotInteraction> CODEC = BuilderCodec.builder(
            OpenOrbisDepotInteraction.class, OpenOrbisDepotInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {
        if (!Main.isInitialized()) {
            LOGGER.at(Level.WARNING).log("Plugin not initialized, cannot open Depot UI");
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
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

        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), Constants.PERM_DEPOT_USE)) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        BlockPosition targetBlock = interactionContext.getTargetBlock();
        if (targetBlock == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Vector3i pos = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
        if (Main.getOrbisDepotComponentType() == null) {
            LOGGER.at(Level.SEVERE).log("orbisDepotComponentType is null at pos %d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }
        Holder<ChunkStore> holder = world.getBlockComponentHolder(pos.getX(), pos.getY(), pos.getZ());
        OrbisDepotBlockState depotState = holder != null
                ? holder.getComponent(Main.getOrbisDepotComponentType())
                : null;
        if (depotState == null) {
            LOGGER.at(Level.WARNING).log("No block state found at pos %d,%d,%d (holder=%s)", pos.getX(), pos.getY(), pos.getZ(), holder);
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        String posKey = DepotSlotUtils.posKey(pos.getX(), pos.getY(), pos.getZ());
        depotState.setPositionKey(posKey);

        if (depotState.getOwnerUUID() == null) {
            depotState.setOwner(playerRef.getUuid());
        } else if (!depotState.isOwner(playerRef.getUuid())
                && !AttunementManager.get().isAttunedTo(playerRef.getUuid(), depotState.getOwnerUUID())) {
            player.sendMessage(Message.raw("You are not the owner of this Orbis Depot.").color("#ff6b6b"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        UUID depotOwner = depotState.getOwnerUUID();
        DepotSlotUtils.registerDepot(posKey, depotOwner);

        BlockStateUtils.setBlockInteractionState("OpenWindow", world, pos);
        SoundUtils.playSFX("SFX_Chest_Wooden_Open", pos.getX(), pos.getY(), pos.getZ(), store);

        OrbisDepotStorageContext context = new OrbisDepotStorageContext.Depot(posKey, depotOwner, world);
        CompletableFuture.runAsync(() -> {
            try {
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
