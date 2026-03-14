package com.destbg.OrbisDepot.Utils;

import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateTranslations;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.Universe;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TranslationUtils {

    private static final String[] ROMAN_NUMERALS = {"I", "II", "III", "IV", "V"};

    private static final String STORAGE_NAME_TEMPLATE_KEY = "server.items.OrbisUpgradeStorage.name";
    private static final String STORAGE_DESC_TEMPLATE_KEY = "server.items.OrbisUpgradeStorage.description";
    private static final String SPEED_NAME_TEMPLATE_KEY = "server.items.OrbisUpgradeSpeed.name";
    private static final String SPEED_DESC_TEMPLATE_KEY = "server.items.OrbisUpgradeSpeed.description";

    public static String get(String key) {
        String msg = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, "server." + key);
        return msg != null ? msg : key;
    }

    public static String format(String key, Object... args) {
        return String.format(get(key), args);
    }

    public static void refreshUpgradeDescriptions() {
        I18nModule i18n = I18nModule.get();
        String storageNameTpl = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, STORAGE_NAME_TEMPLATE_KEY);
        String storageDescTpl = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, STORAGE_DESC_TEMPLATE_KEY);
        String speedNameTpl = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, SPEED_NAME_TEMPLATE_KEY);
        String speedDescTpl = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, SPEED_DESC_TEMPLATE_KEY);
        assert storageNameTpl != null;
        assert storageDescTpl != null;
        assert speedNameTpl != null;
        assert speedDescTpl != null;

        Map<String, String> updates = new LinkedHashMap<>();

        for (int rank = 1; rank < Constants.MAX_UPGRADE_RANK; rank++) {
            String numeral = ROMAN_NUMERALS[rank - 1];
            int fromMult = Constants.STORAGE_RANK_MULTIPLIERS[rank - 1];
            int toMult = Constants.STORAGE_RANK_MULTIPLIERS[rank];
            int storageCost = Constants.UPGRADE_COST_STORAGE[rank - 1];
            updates.put("server.items.OrbisUpgradeStorage" + rank + ".name",
                    String.format(storageNameTpl, numeral));
            updates.put("server.items.OrbisUpgradeStorage" + rank + ".description",
                    String.format(storageDescTpl, fromMult, toMult, storageCost));

            int fromSpeed = speedAtRank(rank);
            int toSpeed = speedAtRank(rank + 1);
            int speedCost = Constants.UPGRADE_COST_SPEED[rank - 1];
            updates.put("server.items.OrbisUpgradeSpeed" + rank + ".name",
                    String.format(speedNameTpl, numeral));
            updates.put("server.items.OrbisUpgradeSpeed" + rank + ".description",
                    String.format(speedDescTpl, fromSpeed, toSpeed, speedCost));
        }

        putTranslations(updates);
    }

    private static int speedAtRank(int rank) {
        int idx = Math.min(rank - 1, Constants.SPEED_RANK_DIVISORS.length - 1);
        float interval = Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS / Constants.SPEED_RANK_DIVISORS[idx];
        return Math.round(60.0f / interval);
    }

    @SuppressWarnings("unchecked")
    private static void putTranslations(Map<String, String> updates) {
        try {
            Field languagesField = I18nModule.class.getDeclaredField("languages");
            languagesField.setAccessible(true);
            Map<String, Map<String, String>> languages = (Map<String, Map<String, String>>) languagesField.get(I18nModule.get());
            languages.computeIfAbsent(I18nModule.DEFAULT_LANGUAGE, _ -> new ConcurrentHashMap<>()).putAll(updates);
        } catch (Exception e) {
            I18nModule.get().getLogger().atWarning().withCause(e).log("Failed to update upgrade item translations");
        }
        Universe.get().broadcastPacketNoCache(new UpdateTranslations(UpdateType.AddOrUpdate, updates));
    }
}
