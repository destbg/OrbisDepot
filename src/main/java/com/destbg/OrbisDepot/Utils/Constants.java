package com.destbg.OrbisDepot.Utils;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

public final class Constants {

    public static final DefaultAssetMap<String, Item> ITEM_ASSET_MAP = Item.getAssetMap();

    public static final String PERM_DEPOT_USE = "orbisdepot.depot.use";
    public static final String PERM_SIGIL_USE = "orbisdepot.sigil.use";

    public static final String SIGIL_ITEM_ID = "Orbis_Sigil";
    public static final String CRUDE_SIGIL_ITEM_ID = "Crude_Orbis_Sigil";

    public static final String KEY_ACTION = "Action";
    public static final String KEY_SEARCH_QUERY = "@SearchQuery";
    public static final String KEY_SLOT_INDEX = "SlotIndex";
    public static final String KEY_CHECKBOX = "Checkbox";

    public static final String CHECKBOX_AUTO_PLACE = "AutoPlace";
    public static final String CHECKBOX_CRAFTING = "Crafting";
    public static final String CHECKBOX_THROTTLE_UI = "ThrottleUi";

    public static final float UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS = 2.0f;
    public static final float UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS = 4.0f;
    public static final short DEPOT_SLOT_CAPACITY = 1;
    public static final short SIGIL_UPLOAD_SLOT_CAPACITY = 4;
    public static final short CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY = 2;

    public static final short DEPOT_UPLOAD_SLOT_ADDITIONAL_STACKS = 0;
    public static final short SIGIL_UPLOAD_SLOT_ADDITIONAL_STACKS = 1;
    public static final short CRUDE_SIGIL_UPLOAD_SLOT_ADDITIONAL_STACKS = 0;

    public static final int PLAYER_INVENTORY_DISPLAY_ROWS = 5;
    public static final int SLOTS_PER_ROW = 9;
    public static final int PLAYER_INVENTORY_DISPLAY_SLOTS = PLAYER_INVENTORY_DISPLAY_ROWS * SLOTS_PER_ROW;

    public static final String ATTUNEMENT_ITEM_ID = "Orbis_Depot_Attunement";
    public static final String META_CRAFTER_UUID = "orbis_crafter_uuid";
    public static final String META_CRAFTER_NAME = "orbis_crafter_name";

    public static final String VOIDHEART_ITEM_ID = "Ingredient_Voidheart";
    public static final int BASE_STORAGE_CAPACITY = 10_000;
    public static final int MAX_UPGRADE_RANK = 5;
    public static final int[] STORAGE_RANK_MULTIPLIERS = {1, 2, 5, 10, 20};
    public static final int[] SPEED_RANK_DIVISORS = {1, 2, 3, 4, 8};
    public static final int[] UPGRADE_COST_STORAGE = {2, 4, 8, 32};
    public static final int[] UPGRADE_COST_SPEED = {4, 8, 16, 64};
    public static final String[] UPGRADE_STORAGE_ITEM_IDS = {
            "OrbisUpgradeStorage1", "OrbisUpgradeStorage2", "OrbisUpgradeStorage3", "OrbisUpgradeStorage4"
    };
    public static final String[] UPGRADE_SPEED_ITEM_IDS = {
            "OrbisUpgradeSpeed1", "OrbisUpgradeSpeed2", "OrbisUpgradeSpeed3", "OrbisUpgradeSpeed4"
    };

    public static final String ORBIS_DEPOT_STATE_ID = "OrbisDepot";

    public static final String SIGIL_ANIM_SET = "OrbisSpellbook";
    public static final String SIGIL_OPEN_ANIM = "CastSigilOpen";
    public static final String SIGIL_CLOSE_ANIM = "CastSigilClose";
}
