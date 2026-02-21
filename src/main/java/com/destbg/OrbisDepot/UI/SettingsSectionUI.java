package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Storage.PlayerSettingsManager;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import java.util.UUID;

final class SettingsSectionUI {

    private final UUID playerUUID;

    SettingsSectionUI(@Nonnull UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.append("#SettingsPanel", "Pages/OrbisDepotSettingsSection.ui");
        updateCheckboxes(cmd);
        bindEvents(evt);
    }

    void updateCheckboxes(@Nonnull UICommandBuilder cmd) {
        PlayerSettingsManager psm = PlayerSettingsManager.get();
        cmd.set("#AutoPlaceToggle #CheckBox.Value", psm.isAutoPlaceEnabled(playerUUID));
        cmd.set("#CraftingToggle #CheckBox.Value", psm.isCraftingIntegrationEnabled(playerUUID));
    }

    void bindEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AutoPlaceToggle #CheckBox",
                EventData.of(Constants.KEY_CHECKBOX, Constants.CHECKBOX_AUTO_PLACE), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CraftingToggle #CheckBox",
                EventData.of(Constants.KEY_CHECKBOX, Constants.CHECKBOX_CRAFTING), false);
    }

    boolean handleCheckbox(@Nonnull String checkboxId) {
        PlayerSettingsManager psm = PlayerSettingsManager.get();
        if (Constants.CHECKBOX_AUTO_PLACE.equals(checkboxId)) {
            psm.setAutoPlace(playerUUID, !psm.isAutoPlaceEnabled(playerUUID));
            return true;
        }
        if (Constants.CHECKBOX_CRAFTING.equals(checkboxId)) {
            psm.setCraftingIntegration(playerUUID, !psm.isCraftingIntegrationEnabled(playerUUID));
            return true;
        }
        return false;
    }
}
