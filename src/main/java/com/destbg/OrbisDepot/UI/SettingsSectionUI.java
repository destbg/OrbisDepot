package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SettingsSectionUI {

    private static final String COLOR_PURCHASED = "#2e7d32";
    private static final String COLOR_REFUNDABLE = "#1a4a3a";
    private static final String COLOR_AVAILABLE = "#1a2530";
    private static final String COLOR_LOCKED = "#1a1a2a";
    private static final String COLOR_LINE = "#4a5568";
    private static final String COLOR_LINE_PURCHASED = "#2e7d32";

    private enum View {SETTINGS, TREE, CONFIRM}

    private final OrbisDepotStorageContext context;
    private final boolean locked;
    private boolean viewingOwnStorage = true;
    private View currentView = View.SETTINGS;
    @Nullable
    private String pendingUpgradeAction;

    public SettingsSectionUI(OrbisDepotStorageContext context, boolean locked) {
        this.context = context;
        this.locked = locked;
    }

    public void setViewingOwnStorage(boolean viewingOwnStorage) {
        this.viewingOwnStorage = viewingOwnStorage;
        if (!viewingOwnStorage) {
            currentView = View.SETTINGS;
            pendingUpgradeAction = null;
        }
    }

    public void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#SettingsPanel");
        cmd.clear("#UpgradeArea");

        switch (currentView) {
            case TREE -> {
                cmd.set("#SettingsTitle.Text", "UPGRADES");
                buildUpgradeTree(cmd, evt);
                buildBackButton(cmd, evt);
            }
            case CONFIRM -> {
                cmd.set("#SettingsTitle.Text", "UPGRADES");
                buildConfirmation(cmd, evt);
            }
            default -> {
                cmd.set("#SettingsTitle.Text", "SETTINGS");
                buildSettingsView(cmd, evt);
            }
        }
    }

    private void buildSettingsView(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        if (locked) {
            cmd.append("#SettingsPanel", "Pages/OrbisDepotSettingsLocked.ui");
            cmd.append("#UpgradeArea", "Pages/OrbisDepotUpgradeButtonDisabled.ui");
            return;
        }

        cmd.append("#SettingsPanel", "Pages/OrbisDepotSettingsSection.ui");
        updateCheckboxes(cmd);
        bindCheckboxEvents(evt);

        if (context.getDepotStorageData().getSpeedUpgradeRank() > 1) {
            cmd.append("#SettingsPanel", "Pages/OrbisDepotSpeedWarning.ui");
        }

        if (viewingOwnStorage) {
            cmd.append("#UpgradeArea", "Pages/OrbisDepotUpgradeButton.ui");
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#UpgradeOpenBtn",
                    EventData.of(Constants.KEY_ACTION, "upgrade-open"), false);
        }
    }

    private void buildUpgradeTree(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        DepotStorageData storage = context.getDepotStorageData();
        int storageRank = storage.getStorageUpgradeRank();
        int speedRank = storage.getSpeedUpgradeRank();

        cmd.append("#SettingsPanel", "Pages/OrbisDepotUpgradeTree.ui");

        buildColumn(cmd, evt, "#StorageSlots", Constants.UPGRADE_STORAGE_ITEM_IDS,
                storageRank, "storage");
        buildColumn(cmd, evt, "#SpeedSlots", Constants.UPGRADE_SPEED_ITEM_IDS,
                speedRank, "speed");
    }

    private void buildColumn(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull String containerSel, @Nonnull String[] itemIds, int currentRank, @Nonnull String type) {
        boolean isStorage = "storage".equals(type);
        int childIdx = 0;
        for (int tier = 0; tier < 4; tier++) {
            if (tier > 0) {
                String lineSel = containerSel + "[" + childIdx + "]";
                cmd.append(containerSel, "Pages/OrbisDepotUpgradeLine.ui");
                boolean prevPurchased = currentRank > tier;
                cmd.set(lineSel + " #Line.Background.Color", prevPurchased ? COLOR_LINE_PURCHASED : COLOR_LINE);
                childIdx++;
            }

            String sel = containerSel + "[" + childIdx + "]";
            cmd.append(containerSel, "Pages/OrbisDepotUpgradeSlot.ui");
            cmd.set(sel + " #Slot.ItemId", itemIds[tier]);

            boolean purchased = currentRank > tier + 1;
            boolean isNext = currentRank == tier + 1;
            boolean isRefundable = !isStorage && currentRank == tier + 2;

            String borderColor;
            if (isRefundable) {
                borderColor = COLOR_REFUNDABLE;
            } else if (purchased) {
                borderColor = COLOR_PURCHASED;
            } else if (isNext) {
                borderColor = COLOR_AVAILABLE;
            } else {
                borderColor = COLOR_LOCKED;
            }
            cmd.set(sel + " #SlotBorder.Background.Color", borderColor);

            if (isNext) {
                String action = "upgrade:" + type + ":" + (tier + 1);
                evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                        EventData.of(Constants.KEY_ACTION, action), false);
            } else if (isRefundable) {
                String action = "refund:" + type + ":" + (tier + 1);
                evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                        EventData.of(Constants.KEY_ACTION, action), false);
            }
            childIdx++;
        }
    }

    private void buildConfirmation(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        if (pendingUpgradeAction == null) {
            currentView = View.TREE;
            buildUpgradeTree(cmd, evt);
            buildBackButton(cmd, evt);
            return;
        }

        String[] parts = pendingUpgradeAction.split(":");
        boolean isRefund = "refund".equals(parts[0]);
        String type = parts[1];
        int tier = Integer.parseInt(parts[2]);

        boolean isStorage = "storage".equals(type);
        String[] itemIds = isStorage ? Constants.UPGRADE_STORAGE_ITEM_IDS : Constants.UPGRADE_SPEED_ITEM_IDS;
        int[] costs = isStorage ? Constants.UPGRADE_COST_STORAGE : Constants.UPGRADE_COST_SPEED;
        String itemId = itemIds[tier - 1];
        int cost = costs[tier - 1];

        DepotStorageData storage = context.getDepotStorageData();
        long voidhearts = storage.getItemCount(Constants.VOIDHEART_ITEM_ID);

        String title;
        String desc;
        String costLabel;
        if (isRefund) {
            title = "Refund " + (isStorage ? "Storage" : "Speed") + " Upgrade " + toRoman(tier);
            if (isStorage) {
                int toStacks = Constants.STORAGE_RANK_MULTIPLIERS[tier - 1];
                int fromStacks = Constants.STORAGE_RANK_MULTIPLIERS[tier];
                desc = "Downgrade capacity from " + fromStacks + " to " + toStacks + " stacks per item";
            } else {
                int fromRate = itemsPerMinPerSlot(tier + 1);
                int toRate = itemsPerMinPerSlot(tier);
                desc = "Downgrade deposit speed from " + fromRate + " to " + toRate + " items/slot/min";
            }
            costLabel = "Refund: " + cost + " Voidheart" + (cost != 1 ? "s" : "");
        } else {
            title = (isStorage ? "Storage" : "Speed") + " Upgrade " + toRoman(tier);
            if (isStorage) {
                int fromStacks = Constants.STORAGE_RANK_MULTIPLIERS[tier - 1];
                int toStacks = Constants.STORAGE_RANK_MULTIPLIERS[tier];
                desc = "Upgrade capacity from " + fromStacks + " to " + toStacks + " stacks per item";
            } else {
                int fromRate = itemsPerMinPerSlot(tier);
                int toRate = itemsPerMinPerSlot(tier + 1);
                desc = "Upgrade deposit speed from " + fromRate + " to " + toRate + " items/slot/min";
            }
            costLabel = "Cost: " + cost + " Voidheart" + (cost != 1 ? "s" : "");
        }

        cmd.append("#SettingsPanel", "Pages/OrbisDepotUpgradeConfirm.ui");
        cmd.set("#ConfirmSlot.ItemId", itemId);
        cmd.set("#ConfirmTitle.Text", title);
        cmd.set("#ConfirmDesc.Text", desc);
        cmd.set("#ConfirmCost.Text", costLabel);
        cmd.set("#ConfirmAvailable.Text", "Available: " + voidhearts);

        if (isRefund) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
                    EventData.of(Constants.KEY_ACTION, "upgrade-confirm"), false);
        } else if (voidhearts >= cost) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
                    EventData.of(Constants.KEY_ACTION, "upgrade-confirm"), false);
        }
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
                EventData.of(Constants.KEY_ACTION, "upgrade-cancel"), false);
    }

    private void buildBackButton(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.append("#UpgradeArea", "Pages/OrbisDepotUpgradeButton.ui");
        cmd.set("#UpgradeBtnLabel.Text", "Back to Settings");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#UpgradeOpenBtn",
                EventData.of(Constants.KEY_ACTION, "upgrade-back"), false);
    }

    public void updateCheckboxes(@Nonnull UICommandBuilder cmd) {
        if (locked) return;
        cmd.set("#AutoPlaceToggle #CheckBox.Value", context.isAutoRestoreEnabled());
        cmd.set("#CraftingToggle #CheckBox.Value", context.isCraftingIntegrationEnabled());
        cmd.set("#ThrottleUiToggle #CheckBox.Value", context.isThrottleUiUpdates());
    }

    public void bindCheckboxEvents(@Nonnull UIEventBuilder evt) {
        if (locked) {
            return;
        }
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AutoPlaceToggle #CheckBox",
                EventData.of(Constants.KEY_CHECKBOX, Constants.CHECKBOX_AUTO_PLACE), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CraftingToggle #CheckBox",
                EventData.of(Constants.KEY_CHECKBOX, Constants.CHECKBOX_CRAFTING), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ThrottleUiToggle #CheckBox",
                EventData.of(Constants.KEY_CHECKBOX, Constants.CHECKBOX_THROTTLE_UI), false);
    }

    public void handleCheckbox(@Nonnull String checkboxId) {
        if (locked) {
            return;
        }
        switch (checkboxId) {
            case Constants.CHECKBOX_AUTO_PLACE -> context.setAutoRestore(!context.isAutoRestoreEnabled());
            case Constants.CHECKBOX_CRAFTING -> context.setCraftingIntegration(!context.isCraftingIntegrationEnabled());
            case Constants.CHECKBOX_THROTTLE_UI -> context.setThrottleUiUpdates(!context.isThrottleUiUpdates());
        }
    }

    public boolean handleAction(@Nonnull String action) {
        return switch (action) {
            case "upgrade-open" -> {
                if (!locked && viewingOwnStorage) {
                    currentView = View.TREE;
                }
                yield true;
            }
            case "upgrade-back" -> {
                currentView = View.SETTINGS;
                pendingUpgradeAction = null;
                yield true;
            }
            case "upgrade-cancel" -> {
                currentView = View.TREE;
                pendingUpgradeAction = null;
                yield true;
            }
            case "upgrade-confirm" -> {
                if (pendingUpgradeAction != null && !locked && viewingOwnStorage) {
                    if (pendingUpgradeAction.startsWith("refund:")) {
                        executeRefund();
                    } else {
                        executePurchase();
                    }
                }
                currentView = View.TREE;
                pendingUpgradeAction = null;
                yield true;
            }
            default -> {
                if (action.startsWith("upgrade:storage:") || action.startsWith("upgrade:speed:")
                        || action.startsWith("refund:speed:")) {
                    if (!locked && viewingOwnStorage) {
                        pendingUpgradeAction = action;
                        currentView = View.CONFIRM;
                    }
                    yield true;
                }
                yield false;
            }
        };
    }

    private void executeRefund() {
        if (pendingUpgradeAction == null) {
            return;
        }

        String[] parts = pendingUpgradeAction.split(":");
        String type = parts[1];
        int tier = Integer.parseInt(parts[2]);

        boolean isStorage = "storage".equals(type);
        int[] costs = isStorage ? Constants.UPGRADE_COST_STORAGE : Constants.UPGRADE_COST_SPEED;
        int refund = costs[tier - 1];

        DepotStorageData storage = context.getDepotStorageData();
        int expectedRank = tier + 1;
        int currentRank = isStorage ? storage.getStorageUpgradeRank() : storage.getSpeedUpgradeRank();
        if (currentRank != expectedRank) {
            return;
        }

        storage.addItem(Constants.VOIDHEART_ITEM_ID, refund);
        if (isStorage) {
            storage.downgradeStorageCapacity();
        } else {
            storage.downgradeDepositSpeed();
        }
    }

    private void executePurchase() {
        if (pendingUpgradeAction == null) {
            return;
        }

        String[] parts = pendingUpgradeAction.split(":");
        String type = parts[1];
        int expectedRank = Integer.parseInt(parts[2]);

        boolean isStorage = "storage".equals(type);
        int[] costs = isStorage ? Constants.UPGRADE_COST_STORAGE : Constants.UPGRADE_COST_SPEED;
        int cost = costs[expectedRank - 1];

        DepotStorageData storage = context.getDepotStorageData();
        long voidhearts = storage.getItemCount(Constants.VOIDHEART_ITEM_ID);
        if (voidhearts < cost) {
            return;
        }

        int currentRank = isStorage ? storage.getStorageUpgradeRank() : storage.getSpeedUpgradeRank();
        if (currentRank != expectedRank) {
            return;
        }

        storage.removeItems(Constants.VOIDHEART_ITEM_ID, cost);
        if (isStorage) {
            storage.upgradeStorageCapacity();
        } else {
            storage.upgradeDepositSpeed();
        }
    }

    private static int itemsPerMinPerSlot(int speedRank) {
        int idx = Math.min(speedRank - 1, Constants.SPEED_RANK_DIVISORS.length - 1);
        float interval = Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS / Constants.SPEED_RANK_DIVISORS[idx];
        return Math.round(60f / interval);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(n);
        };
    }
}
