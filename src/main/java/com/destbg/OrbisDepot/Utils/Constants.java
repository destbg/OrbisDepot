package com.destbg.OrbisDepot.Utils;

public final class Constants {

    private Constants() {
    }

    public static final String ORBIS_DEPOT_STATE_ID = "OrbisDepot";
    public static final String PERM_DEPOT_USE = "orbisdepot.depot.use";
    public static final String PERM_SIGIL_USE = "orbisdepot.sigil.use";

    public static final String DEPOT_ITEM_ID = "Orbis_Depot";
    public static final String SIGIL_ITEM_ID = "Orbis_Sigil";
    public static final String CRUDE_SIGIL_ITEM_ID = "Crude_Orbis_Sigil";

    public static final String KEY_ACTION = "Action";
    public static final String KEY_SEARCH_QUERY = "@SearchQuery";
    public static final String KEY_SLOT_INDEX = "SlotIndex";
    public static final String KEY_CHECKBOX = "Checkbox";

    public static final String KEY_AUTO_PLACE = "autoPlaceFromDepot";
    public static final String KEY_CRAFTING_INTEGRATION = "craftingIntegration";

    public static final String CHECKBOX_AUTO_PLACE = "AutoPlace";
    public static final String CHECKBOX_CRAFTING = "Crafting";

    public static final float UPLOAD_INTERVAL_DEPOT_SECONDS = 2.0f;
    public static final float UPLOAD_INTERVAL_SIGIL_SECONDS = 2.0f;
    public static final float UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS = 4.0f;
    public static final short DEPOT_CAPACITY = 1;
    public static final short SIGIL_UPLOAD_SLOT_CAPACITY = 4;
    public static final short CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY = 2;

    public static final int PLAYER_INVENTORY_DISPLAY_ROWS = 5;
    public static final int SLOTS_PER_ROW = 9;
    public static final int PLAYER_INVENTORY_DISPLAY_SLOTS = PLAYER_INVENTORY_DISPLAY_ROWS * SLOTS_PER_ROW;

    public static final long REFRESH_INTERVAL_MS = 100;

    public static final String ATTUNEMENT_ITEM_ID = "Orbis_Depot_Attunement";
    public static final String META_CRAFTER_UUID = "orbis_crafter_uuid";
    public static final String META_CRAFTER_NAME = "orbis_crafter_name";

    public static final String SIGIL_ANIM_SET = "OrbisSpellbook";
    public static final String SIGIL_OPEN_ANIM = "CastHurlCharging";
    public static final String SIGIL_CLOSE_ANIM = "CastHurlCharged";
}
