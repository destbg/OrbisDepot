package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Models.StorageModel;
import com.destbg.OrbisDepot.Storage.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.Utils.BlockStateUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.DepositUtils;
import com.destbg.OrbisDepot.Utils.SoundUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

public class OrbisDepotStorageUI extends InteractiveCustomUIPage<StorageModel> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final ScheduledExecutorService REFRESH_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OrbisDepot-UIRefresh");
        t.setDaemon(true);
        return t;
    });

    private final PlayerRef playerRef;
    private final OrbisDepotStorageContext context;

    private final DepositSectionUI depositSection;
    private final StorageSectionUI storageSection;
    private final InventorySectionUI inventorySection;
    @Nullable
    private final SettingsSectionUI settingsSection;

    private volatile Ref<EntityStore> lastRef;
    private volatile Store<EntityStore> lastStore;
    private volatile ScheduledFuture<?> refreshTask;
    private volatile boolean pageOpen = false;

    public OrbisDepotStorageUI(@Nonnull PlayerRef playerRef, @Nonnull OrbisDepotStorageContext context) {
        super(playerRef, CustomPageLifetime.CanDismiss, StorageModel.CODEC);
        this.playerRef = playerRef;
        this.context = context;
        this.depositSection = new DepositSectionUI(context);
        this.storageSection = new StorageSectionUI(context);
        this.inventorySection = new InventorySectionUI();
        this.settingsSection = (context instanceof OrbisDepotStorageContext.Sigil)
                ? new SettingsSectionUI(playerRef.getUuid())
                : null;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt, @NonNullDecl Store<EntityStore> store) {
        this.lastRef = ref;
        this.lastStore = store;

        buildPage(cmd, evt, ref, store);
        startRefreshTask();
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        pageOpen = false;
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
        lastRef = null;
        lastStore = null;

        if (context instanceof OrbisDepotStorageContext.Depot depot) {
            triggerClose(depot);
        } else if (context instanceof OrbisDepotStorageContext.Sigil || context instanceof OrbisDepotStorageContext.CrudeSigil) {
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

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl StorageModel data) {
        UUID playerUUID = playerRef.getUuid();
        if (!context.isAllowed(playerUUID)) {
            return;
        }

        this.lastRef = ref;
        this.lastStore = store;

        String action = data.getAction();
        if (action != null) {
            if (action.startsWith("deposit:")) {
                depositSection.handleDeposit(ref, store, action.substring("deposit:".length()), this::giveToPlayer);
                sendActionUpdate(ref, store);
                return;
            }
            if (action.startsWith("withdraw:")) {
                storageSection.handleWithdraw(ref, store, action.substring("withdraw:".length()), false, this::giveToPlayer);
                sendActionUpdate(ref, store);
                return;
            }
            if (action.startsWith("withdraw-one:")) {
                storageSection.handleWithdraw(ref, store, action.substring("withdraw-one:".length()), true, this::giveToPlayer);
                sendActionUpdate(ref, store);
                return;
            }
            if (action.startsWith("cancel-upload:")) {
                depositSection.handleCancelUpload(ref, store, action.substring("cancel-upload:".length()), this::giveToPlayer);
                sendActionUpdate(ref, store);
                return;
            }
        }

        String checkbox = data.getCheckbox();
        if (checkbox != null && settingsSection != null && settingsSection.handleCheckbox(checkbox)) {
            sendActionUpdate(ref, store);
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

    private void buildPage(@NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder evt, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        boolean showSettings = settingsSection != null;
        cmd.append(showSettings ? "Pages/OrbisDepotStorage.ui" : "Pages/OrbisDepotStorage_NoSettings.ui");

        String title = switch (context) {
            case OrbisDepotStorageContext.Depot _ -> "Orbis Depot";
            case OrbisDepotStorageContext.Sigil _ -> "Orbis Sigil";
            case OrbisDepotStorageContext.CrudeSigil _ -> "Crude Orbis Sigil";
        };
        cmd.set("#TitleText.Text", title);
        cmd.set("#SearchInput.Value", storageSection.getSearchQuery());
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
                EventData.of(Constants.KEY_SEARCH_QUERY, "#SearchInput.Value"), false);

        cmd.append("#DepositPanel", "Pages/OrbisDepotDepositSection.ui");
        cmd.append("#StoragePanel", "Pages/OrbisDepotStorageSection.ui");
        cmd.append("#InventoryPanel", "Pages/OrbisDepotInventorySection.ui");

        storageSection.build(cmd, evt);
        depositSection.build(cmd, evt);
        inventorySection.build(ref, cmd, evt, store);
        if (settingsSection != null) {
            settingsSection.build(cmd, evt);
        }
    }

    private void startRefreshTask() {
        if (pageOpen) {
            return;
        }

        pageOpen = true;
        refreshTask = REFRESH_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                if (!pageOpen || lastRef == null || lastStore == null) {
                    return;
                }

                Ref<EntityStore> ref = lastRef;
                Store<EntityStore> store = lastStore;
                CompletableFuture.runAsync(() -> {
                    if (pageOpen) {
                        sendPeriodicUpdate(ref, store);
                    }
                }, context.getWorld());
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error in UI refresh");
            }
        }, Constants.REFRESH_INTERVAL_MS, Constants.REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void sendPeriodicUpdate(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        boolean storageChanged = storageSection.hasStorageItemsChanged();
        boolean depositChanged = depositSection.hasDepositStateChanged();
        boolean inventoryChanged = inventorySection.hasInventoryChanged(ref, store);

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();

        if (storageChanged) {
            storageSection.build(cmd, evt);
        } else {
            storageSection.update(cmd);
        }

        if (depositChanged) {
            depositSection.build(cmd, evt);
        } else {
            depositSection.update(cmd);
        }

        if (storageChanged || depositChanged || inventoryChanged) {
            inventorySection.build(ref, cmd, evt, store);
        }

        if (settingsSection != null) {
            settingsSection.bindEvents(evt);
        }

        sendUpdate(cmd, evt, false);
    }

    private void sendActionUpdate(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        buildPage(cmd, evt, ref, store);
        sendUpdate(cmd, evt, true);
    }

    private void triggerClose(OrbisDepotStorageContext.Depot depot) {
        try {
            String posKey = depot.posKey();
            String[] parts = posKey.split(":");
            if (parts.length != 3) {
                return;
            }

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            World world = depot.world();

            BlockStateUtils.setBlockInteractionState("CloseWindow", world, new Vector3i(x, y, z));
            SoundUtils.playSFX("SFX_Chest_Wooden_Close", x, y, z, world.getEntityStore().getStore());
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t).log("Failed to play chest close sound");
        }
    }

    private int giveToPlayer(ItemContainer playerInv, String itemId, int toGive) {
        int given = 0;
        short cap = playerInv.getCapacity();
        int maxStack = DepositUtils.getMaxStack(itemId);

        for (short i = 0; i < cap && toGive > 0; i++) {
            ItemStack existing = playerInv.getItemStack(i);
            if (existing != null && !ItemStack.isEmpty(existing) && existing.getItemId().equals(itemId)
                    && existing.getQuantity() < maxStack) {
                int space = maxStack - existing.getQuantity();
                int add = Math.min(toGive, space);
                playerInv.setItemStackForSlot(i, existing.withQuantity(existing.getQuantity() + add));
                given += add;
                toGive -= add;
            }
        }

        for (short i = 0; i < cap && toGive > 0; i++) {
            ItemStack existing = playerInv.getItemStack(i);
            if (existing == null || ItemStack.isEmpty(existing)) {
                int add = Math.min(toGive, maxStack);
                playerInv.setItemStackForSlot(i, new ItemStack(itemId, add));
                given += add;
                toGive -= add;
            }
        }

        return given;
    }
}
