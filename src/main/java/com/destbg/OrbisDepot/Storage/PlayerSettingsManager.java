package com.destbg.OrbisDepot.Storage;

import com.destbg.OrbisDepot.Utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class PlayerSettingsManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {
    }.getType();

    private static PlayerSettingsManager instance;

    private final Path settingsDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, ConcurrentHashMap<String, Boolean>> playerSettings = new ConcurrentHashMap<>();

    private PlayerSettingsManager(@Nonnull Path dataDirectory) {
        this.settingsDir = dataDirectory.resolve("player_settings");
        try {
            Files.createDirectories(settingsDir);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create player settings directory");
        }
    }

    public static void init(@Nonnull Path dataDirectory) {
        instance = new PlayerSettingsManager(dataDirectory);
        instance.loadAll();
    }

    @Nonnull
    public static PlayerSettingsManager get() {
        if (instance == null) {
            throw new IllegalStateException("PlayerSettingsManager not initialized");
        }
        return instance;
    }

    public boolean isAutoPlaceEnabled(@Nonnull UUID player) {
        ConcurrentHashMap<String, Boolean> settings = playerSettings.get(player);
        if (settings == null) {
            return true;
        }
        return settings.getOrDefault(Constants.KEY_AUTO_PLACE, true);
    }

    public void setAutoPlace(@Nonnull UUID player, boolean enabled) {
        ConcurrentHashMap<String, Boolean> settings = playerSettings.computeIfAbsent(player, _ -> new ConcurrentHashMap<>());
        settings.put(Constants.KEY_AUTO_PLACE, enabled);
        save(player);
    }

    public boolean isCraftingIntegrationEnabled(@Nonnull UUID player) {
        ConcurrentHashMap<String, Boolean> settings = playerSettings.get(player);
        if (settings == null) {
            return true;
        }
        return settings.getOrDefault(Constants.KEY_CRAFTING_INTEGRATION, true);
    }

    public void setCraftingIntegration(@Nonnull UUID player, boolean enabled) {
        ConcurrentHashMap<String, Boolean> settings = playerSettings.computeIfAbsent(player, _ -> new ConcurrentHashMap<>());
        settings.put(Constants.KEY_CRAFTING_INTEGRATION, enabled);
        save(player);
    }

    public void save(@Nonnull UUID player) {
        ConcurrentHashMap<String, Boolean> settings = playerSettings.get(player);
        Path file = settingsDir.resolve(player + ".json");
        try {
            if (settings == null || settings.isEmpty()) {
                Files.deleteIfExists(file);
            } else {
                String json = gson.toJson(settings);
                Files.writeString(file, json);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save player settings for %s", player);
        }
    }

    public void saveAll() {
        for (UUID player : playerSettings.keySet()) {
            save(player);
        }
        LOGGER.at(Level.INFO).log("Saved player settings for %d players.", playerSettings.size());
    }

    private void loadAll() {
        if (!Files.isDirectory(settingsDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(settingsDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list player settings files");
        }
        LOGGER.at(Level.INFO).log("Loaded player settings for %d players.", playerSettings.size());
    }

    private void loadFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            UUID player = UUID.fromString(name.substring(0, name.length() - ".json".length()));
            String json = Files.readString(file);
            Map<String, Boolean> settings = gson.fromJson(json, MAP_TYPE);
            if (settings != null && !settings.isEmpty()) {
                playerSettings.put(player, new ConcurrentHashMap<>(settings));
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load player settings file %s", file);
        }
    }
}
