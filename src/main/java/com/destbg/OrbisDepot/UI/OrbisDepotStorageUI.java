package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.AttunedEntry;
import com.destbg.OrbisDepot.Models.*;
import com.destbg.OrbisDepot.Utils.BlockStateUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepotOwnerUtils;
import com.destbg.OrbisDepot.Utils.SoundUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class OrbisDepotStorageUI extends InteractiveCustomUIPage<StorageModel> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final PlayerRef playerRef;
    private final OrbisDepotStorageContext context;
    private volatile Ref<EntityStore> lastRef;
    private volatile Store<EntityStore> lastStore;
    private volatile long lastTickUpdateMs = 0;
    private final DepositSectionUI depositSection;
    private final StorageSectionUI storageSection;
    private final SettingsSectionUI settingsSection;
    private final InventorySectionUI inventorySection;
    private final AttunementSectionUI attunementSection;

    public OrbisDepotStorageUI(@Nonnull PlayerRef playerRef, @Nonnull OrbisDepotStorageContext context) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, StorageModel.CODEC);

        this.playerRef = playerRef;
        this.context = context;
        this.depositSection = new DepositSectionUI(context);
        this.storageSection = new StorageSectionUI(context);
        this.inventorySection = new InventorySectionUI();
        boolean isOwnerView;
        if (context instanceof OrbisDepotStorageModel) {
            isOwnerView = playerRef.getUuid().equals(context.getSavedAttunement());
        } else {
            isOwnerView = true;
        }
        this.attunementSection = new AttunementSectionUI(context, playerRef.getUuid(), playerRef.getUsername(), isOwnerView);

        if (context instanceof OrbisSigilStorageModel) {
            this.settingsSection = new SettingsSectionUI(context, false);
        } else if (context instanceof CrudeOrbisSigilStorageModel) {
            this.settingsSection = new SettingsSectionUI(context, true);
        } else {
            this.settingsSection = null;
        }
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        this.lastRef = ref;
        this.lastStore = store;
        syncSelectedStorage();
        registerInventoryChangeEvents(ref, store);
        uiCommandBuilder.append("Pages/OrbisDepotStorage.ui");

        String title = switch (context) {
            case OrbisDepotStorageModel _ -> "Orbis Depot";
            case OrbisSigilStorageModel _ -> "Orbis Sigil";
            case CrudeOrbisSigilStorageModel _ -> "Crude Orbis Sigil";
            default -> throw new IllegalStateException("Unexpected value: " + context);
        };
        uiCommandBuilder.set("#TitleText.Text", title);
        uiCommandBuilder.set("#SearchInput.Value", storageSection.getSearchQuery());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of(Constants.KEY_SEARCH_QUERY, "#SearchInput.Value"), false);

        boolean isSigilContext = context instanceof OrbisSigilStorageModel || context instanceof CrudeOrbisSigilStorageModel;
        if (attunementSection.hasAttunements()) {
            uiCommandBuilder.remove("#AttunementPlaceholder");
            attunementSection.build(uiCommandBuilder, uiEventBuilder);
        } else {
            uiCommandBuilder.remove("#AttunementContainer");
            uiCommandBuilder.remove("#AttunementSpacer");
        }
        if (isSigilContext) {
            uiCommandBuilder.remove("#SettingsPlaceholder");
        } else {
            uiCommandBuilder.remove("#SettingsContainer");
        }

        uiCommandBuilder.append("#DepositPanel", "Pages/OrbisDepotDepositSection.ui");
        uiCommandBuilder.append("#StoragePanel", "Pages/OrbisDepotStorageSection.ui");
        uiCommandBuilder.append("#InventoryPanel", "Pages/OrbisDepotInventorySection.ui");

        storageSection.build(uiCommandBuilder, uiEventBuilder);
        depositSection.build(uiCommandBuilder, uiEventBuilder);
        inventorySection.build(ref, uiCommandBuilder, uiEventBuilder, store);
        if (settingsSection != null) {
            settingsSection.build(uiCommandBuilder, uiEventBuilder);
        }
    }

    private void sendTickUpdate() {
        if (context.isThrottleUiUpdates()) {
            long now = System.currentTimeMillis();
            if (now - lastTickUpdateMs < 1000) {
                return;
            }
            lastTickUpdateMs = now;
        }
        syncSelectedStorage();
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        storageSection.build(cmd, evt);
        depositSection.build(cmd, evt);
        sendUpdate(cmd, evt, false);
    }

    public void notifyIfViewing(UUID storageOwnerUUID) {
        if (storageOwnerUUID.equals(attunementSection.getSelectedOwnerUUID())) {
            sendTickUpdate();
        }
    }

    public static void notifyViewersOf(UUID storageOwnerUUID, DepotStorageData targetStorage) {
        notifyPlayerIfViewing(storageOwnerUUID, storageOwnerUUID);
        for (AttunedEntry entry : targetStorage.getAttunedToOthers()) {
            notifyPlayerIfViewing(entry.ownerUUID(), storageOwnerUUID);
        }
    }

    private static void notifyPlayerIfViewing(UUID playerUUID, UUID storageOwnerUUID) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null) {
            return;
        }
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null) {
            return;
        }
        Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        CustomUIPage page = player.getPageManager().getCustomPage();
        if (page instanceof OrbisDepotStorageUI depotPage) {
            depotPage.notifyIfViewing(storageOwnerUUID);
        }
    }

    private void syncSelectedStorage() {
        UUID selected = attunementSection.getSelectedOwnerUUID();
        UUID myUUID = playerRef.getUuid();
        boolean isOwnStorage = selected == null || selected.equals(myUUID);

        if (settingsSection != null) {
            settingsSection.setViewingOwnStorage(isOwnStorage);
        }

        if (isOwnStorage) {
            storageSection.setDepotStorage(context.getDepotStorageData());
            return;
        }

        DepotStorageData targetStorage = DepotOwnerUtils.getIfReady(selected);
        storageSection.setDepotStorage(targetStorage != null ? targetStorage : context.getDepotStorageData());
    }

    private void registerInventoryChangeEvents(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Inventory inv = player.getInventory();
        if (inv == null) {
            return;
        }

        ItemContainer storage = inv.getStorage();
        ItemContainer hotbar = inv.getHotbar();

        if (storage != null) {
            storage.registerChangeEvent(_ -> onInventoryChanged());
        }
        if (hotbar != null) {
            hotbar.registerChangeEvent(_ -> onInventoryChanged());
        }
    }

    private void onInventoryChanged() {
        Ref<EntityStore> ref = lastRef;
        Store<EntityStore> store = lastStore;
        if (ref == null || store == null) {
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        inventorySection.build(ref, cmd, evt, store);
        sendUpdate(cmd, evt, false);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl StorageModel data) {
        String action = data.getAction();
        if (action != null && action.startsWith("deposit:")) {
            depositSection.handleDeposit(ref, store, action.substring("deposit:".length()));
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            depositSection.build(cmd, evt);
            inventorySection.build(ref, cmd, evt, store);
            sendUpdate(cmd, evt, false);
            return;
        }

        if (action != null && action.startsWith("withdraw:")) {
            storageSection.handleWithdraw(ref, store, action.substring("withdraw:".length()), false);
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            storageSection.build(cmd, evt);
            inventorySection.build(ref, cmd, evt, store);
            sendUpdate(cmd, evt, false);
            return;
        }

        if (action != null && action.startsWith("withdraw-one:")) {
            storageSection.handleWithdraw(ref, store, action.substring("withdraw-one:".length()), true);
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            storageSection.build(cmd, evt);
            inventorySection.build(ref, cmd, evt, store);
            sendUpdate(cmd, evt, false);
            return;
        }

        if (action != null && action.startsWith("cancel-upload:")) {
            depositSection.handleCancelUpload(ref, store, action.substring("cancel-upload:".length()));
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            depositSection.build(cmd, evt);
            inventorySection.build(ref, cmd, evt, store);
            sendUpdate(cmd, evt, false);
            return;
        }

        if (action != null && attunementSection.handleAction(action)) {
            syncSelectedStorage();
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            build(ref, cmd, evt, store);
            sendUpdate(cmd, evt, true);
            return;
        }

        if (action != null && settingsSection != null && settingsSection.handleAction(action)) {
            syncSelectedStorage();
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            build(ref, cmd, evt, store);
            sendUpdate(cmd, evt, true);
            return;
        }

        String checkbox = data.getCheckbox();
        if (checkbox != null && settingsSection != null) {
            settingsSection.handleCheckbox(checkbox);
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            storageSection.build(cmd, evt);
            sendUpdate(cmd, evt, false);
            return;
        }

        if (data.getSearchQuery() != null) {
            storageSection.setSearchQuery(data.getSearchQuery());
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            storageSection.build(cmd, evt);
            sendUpdate(cmd, evt, false);
        }
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        lastRef = null;
        lastStore = null;
        if (context instanceof OrbisDepotStorageModel depot) {
            try {
                Vector3i location = depot.getLocation();
                World world = depot.getWorld();

                BlockStateUtils.setBlockInteractionState("CloseWindow", world, location.getX(), location.getY(), location.getZ());
                SoundUtils.playSFX("SFX_Chest_Wooden_Close", location.getX(), location.getY(), location.getZ(), world.getEntityStore().getStore());
            } catch (Throwable t) {
                LOGGER.at(Level.WARNING).withCause(t).log("Failed to play chest close sound");
            }
        } else {
            CompletableFuture.runAsync(() -> {
                try {
                    AnimationUtils.playAnimation(ref, AnimationSlot.Action,
                            Constants.SIGIL_ANIM_SET, Constants.SIGIL_CLOSE_ANIM,
                            true, store);
                    SoundUtils.playSFXToPlayer("SFX_Orbis_Sigil_Close_Local", ref, store);
                } catch (Throwable t) {
                    LOGGER.at(Level.WARNING).withCause(t).log("Failed to play Sigil close animation");
                }
            }, context.getWorld());
        }
    }
}
