package com.destbg.OrbisDepot.UI;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Models.AttunedEntry;
import com.destbg.OrbisDepot.Models.OrbisDepotStorageContext;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttunementSectionUI {

    private final OrbisDepotStorageContext context;
    private final DepotStorageData depotStorage;
    private final UUID playerUUID;
    private final String playerName;
    private final boolean isOwnerView;
    private UUID selectedOwnerUUID;

    private enum ConfirmationType {REVOKE, LEAVE}

    @Nullable
    private ConfirmationType pendingType;
    @Nullable
    private UUID pendingTargetUUID;
    @Nullable
    private String pendingTargetName;

    public AttunementSectionUI(OrbisDepotStorageContext context, @Nonnull UUID playerUUID, @Nonnull String playerName, boolean isOwnerView) {
        this.context = context;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.isOwnerView = isOwnerView;
        this.depotStorage = context.getDepotStorageData();

        UUID saved = context.getSavedAttunement();
        if (saved != null && isValidTarget(saved)) {
            this.selectedOwnerUUID = saved;
        } else {
            this.selectedOwnerUUID = playerUUID;
        }
    }

    private boolean isValidTarget(@Nonnull UUID target) {
        if (target.equals(playerUUID)) {
            return true;
        }

        return depotStorage.isAttunedTo(target);
    }

    @Nullable
    public UUID getSelectedOwnerUUID() {
        return selectedOwnerUUID;
    }

    public boolean hasAttunements() {
        if (!depotStorage.getAttunedToMe().isEmpty()) {
            return true;
        }

        return isOwnerView && !depotStorage.getAttunedToOthers().isEmpty();
    }

    public void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#AttunementPanel");

        if (pendingType != null) {
            buildConfirmation(cmd, evt);
            return;
        }

        buildNormal(cmd, evt);
    }

    private void buildConfirmation(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        String name = pendingTargetName != null ? pendingTargetName : "this player";
        boolean isLeave = pendingType == ConfirmationType.LEAVE;

        String prompt = isLeave ? "Leave depot of" : "Remove access for";
        String confirmLabel = isLeave ? "Yes, Leave" : "Yes, Remove";
        String confirmAction = isLeave ? "confirm-leave" : "confirm-revoke";
        String cancelAction = isLeave ? "cancel-leave" : "cancel-revoke";

        cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 16); }");
        cmd.appendInline("#AttunementPanel",
                "Label { Text: \"" + prompt + "\"; Style: (FontSize: 13, TextColor: #c8d6e5, HorizontalAlignment: Center); Anchor: (Height: 18); }");
        cmd.appendInline("#AttunementPanel",
                "Label { Text: \"" + name.replace("\"", "'") + "?\"; Style: (FontSize: 14, TextColor: #ffffff, RenderBold: true, HorizontalAlignment: Center); Anchor: (Height: 20); }");
        cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 16); }");

        cmd.appendInline("#AttunementPanel",
                "Button #ConfirmBtn { Anchor: (Height: 28, Left: 16, Right: 16);"
                        + " Style: (Default: (Background: #8b3a3a), Hovered: (Background: #a84444), Pressed: (Background: #6e2e2e));"
                        + " Label { Text: \"" + confirmLabel + "\"; Style: (FontSize: 13, TextColor: #ffffff, HorizontalAlignment: Center, VerticalAlignment: Center); } }");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
                EventData.of(Constants.KEY_ACTION, confirmAction), false);

        cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 6); }");

        cmd.appendInline("#AttunementPanel",
                "Button #CancelBtn { Anchor: (Height: 28, Left: 16, Right: 16);"
                        + " Style: (Default: (Background: #2b3a4a), Hovered: (Background: #3a4d60), Pressed: (Background: #1f2d3a));"
                        + " Label { Text: \"Cancel\"; Style: (FontSize: 13, TextColor: #c8d6e5, HorizontalAlignment: Center, VerticalAlignment: Center); } }");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
                EventData.of(Constants.KEY_ACTION, cancelAction), false);
    }

    private void buildNormal(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        List<AttunedEntry> attuned = depotStorage.getAttunedToMe();
        List<AttunedEntry> all = new ArrayList<>();
        all.add(new AttunedEntry(playerUUID, playerName));
        all.addAll(attuned);

        int childIdx = 0;

        for (AttunedEntry entry : all) {
            String sel = "#AttunementPanel[" + childIdx + "]";

            cmd.append("#AttunementPanel", "Pages/OrbisDepotAttunementEntry.ui");

            String label = entry.ownerUUID().equals(playerUUID) ? "My Depot" : entry.ownerName();
            boolean isSelected = entry.ownerUUID().equals(selectedOwnerUUID);

            cmd.set(sel + " #AttunementCheckbox #CheckBox.Value", isSelected);
            cmd.set(sel + " #AttunementCheckbox #AttunementLabel.Text", label);
            evt.addEventBinding(CustomUIEventBindingType.ValueChanged, sel + " #AttunementCheckbox #CheckBox",
                    EventData.of(Constants.KEY_ACTION, "select-depot:" + entry.ownerUUID()), false);
            childIdx++;
        }

        if (selectedOwnerUUID != null && !selectedOwnerUUID.equals(playerUUID)) {
            String selectedName = findAttunedName(selectedOwnerUUID);
            cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 6); }");
            childIdx++;
            cmd.appendInline("#AttunementPanel",
                    "Button #LeaveDepotBtn { Anchor: (Height: 24, Left: 16, Right: 16);"
                            + " Style: (Default: (Background: #3a2b2b), Hovered: (Background: #4a3535), Pressed: (Background: #2e2222));"
                            + " Label { Text: \"Leave " + selectedName.replace("\"", "'") + "'s Depot\"; Style: (FontSize: 12, TextColor: #e07070, HorizontalAlignment: Center, VerticalAlignment: Center); } }");
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#LeaveDepotBtn",
                    EventData.of(Constants.KEY_ACTION, "leave-depot:" + selectedOwnerUUID), false);
            childIdx++;
        }

        if (isOwnerView) {
            List<AttunedEntry> playersAttunedToMe = depotStorage.getAttunedToOthers();
            if (!playersAttunedToMe.isEmpty()) {
                cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 8); }");
                cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 1); Background: (Color: #2b3542); }");
                cmd.appendInline("#AttunementPanel", "Group { Anchor: (Height: 6); }");
                cmd.appendInline("#AttunementPanel", "Label { Text: \"MANAGE ACCESS\"; Style: (FontSize: 11, TextColor: #96a9be, RenderUppercase: true, RenderBold: true, HorizontalAlignment: Center); Anchor: (Height: 18); }");
                childIdx += 4;

                for (AttunedEntry attunedPlayerUUID : playersAttunedToMe) {
                    String sel = "#AttunementPanel[" + childIdx + "]";

                    cmd.append("#AttunementPanel", "Pages/OrbisDepotAttunementEntry.ui");
                    cmd.set(sel + " #AttunementCheckbox #AttunementLabel.Text", "X  " + attunedPlayerUUID.ownerName());
                    cmd.set(sel + " #AttunementCheckbox #CheckBox.Value", false);
                    evt.addEventBinding(CustomUIEventBindingType.ValueChanged, sel + " #AttunementCheckbox #CheckBox",
                            EventData.of(Constants.KEY_ACTION, "revoke-access:" + attunedPlayerUUID.ownerUUID()), false);
                    childIdx++;
                }
            }
        }
    }

    public boolean handleAction(@Nonnull String action) {
        if (action.startsWith("select-depot:")) {
            try {
                selectedOwnerUUID = UUID.fromString(action.substring("select-depot:".length()));
                context.setSavedAttunement(selectedOwnerUUID);
                return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (action.startsWith("revoke-access:")) {
            try {
                UUID targetUUID = UUID.fromString(action.substring("revoke-access:".length()));
                pendingType = ConfirmationType.REVOKE;
                pendingTargetUUID = targetUUID;
                pendingTargetName = findNameForPlayer(targetUUID);
                return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (action.startsWith("leave-depot:")) {
            try {
                UUID targetUUID = UUID.fromString(action.substring("leave-depot:".length()));
                pendingType = ConfirmationType.LEAVE;
                pendingTargetUUID = targetUUID;
                pendingTargetName = findAttunedName(targetUUID);
                return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        switch (action) {
            case "confirm-revoke" -> {
                if (pendingTargetUUID != null) {
                    depotStorage.removeAttunement(pendingTargetUUID);
                }
                clearPending();
                return true;
            }
            case "confirm-leave" -> {
                if (pendingTargetUUID != null) {
                    depotStorage.removeAttunementToMe(pendingTargetUUID);
                    if (pendingTargetUUID.equals(selectedOwnerUUID)) {
                        selectedOwnerUUID = playerUUID;
                        context.setSavedAttunement(playerUUID);
                    }
                }
                clearPending();
                return true;
            }
            case "cancel-revoke", "cancel-leave" -> {
                clearPending();
                return true;
            }
        }
        return false;
    }

    private void clearPending() {
        pendingType = null;
        pendingTargetUUID = null;
        pendingTargetName = null;
    }

    private String findNameForPlayer(UUID targetUUID) {
        String name = depotStorage.getPlayerName(targetUUID);
        if (name != null) {
            return name;
        }
        return targetUUID.toString().substring(0, 8);
    }

    private String findAttunedName(UUID ownerUUID) {
        List<AttunedEntry> entries = depotStorage.getAttunedToMe();
        for (AttunedEntry entry : entries) {
            if (entry.ownerUUID().equals(ownerUUID)) {
                return entry.ownerName();
            }
        }
        return ownerUUID.toString().substring(0, 8);
    }
}
