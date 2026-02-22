package com.destbg.OrbisDepot.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class AttunementManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Type SET_TYPE = new TypeToken<Set<AttunedEntry>>() {}.getType();

    private static AttunementManager instance;

    private final Path storageDir;
    private final Path prefsDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Set<AttunedEntry>> attunedTo = new ConcurrentHashMap<>();
    private final Map<String, UUID> selectedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    public record AttunedEntry(UUID ownerUUID, String ownerName) {
        @Override
        public boolean equals(Object o) {
            return o instanceof AttunedEntry e && ownerUUID.equals(e.ownerUUID);
        }

        @Override
        public int hashCode() {
            return ownerUUID.hashCode();
        }
    }

    private AttunementManager(@Nonnull Path dataDirectory) {
        this.storageDir = dataDirectory.resolve("attunements");
        this.prefsDir = dataDirectory.resolve("attunement_prefs");
        try {
            Files.createDirectories(storageDir);
            Files.createDirectories(prefsDir);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create attunements directory");
        }
    }

    public static void init(@Nonnull Path dataDirectory) {
        instance = new AttunementManager(dataDirectory);
        instance.loadAll();
    }

    @Nonnull
    public static AttunementManager get() {
        if (instance == null) {
            throw new IllegalStateException("AttunementManager not initialized");
        }
        return instance;
    }

    public void attune(@Nonnull UUID playerUUID, @Nonnull String playerName,
                       @Nonnull UUID ownerUUID, @Nonnull String ownerName) {
        playerNames.put(playerUUID, playerName);
        playerNames.put(ownerUUID, ownerName);
        Set<AttunedEntry> entries = attunedTo.computeIfAbsent(playerUUID, _ -> ConcurrentHashMap.newKeySet());
        entries.remove(new AttunedEntry(ownerUUID, ownerName));
        entries.add(new AttunedEntry(ownerUUID, ownerName));
        save(playerUUID);
        savePlayerNames();
    }

    public void removeAttunement(@Nonnull UUID playerUUID, @Nonnull UUID ownerUUID) {
        Set<AttunedEntry> entries = attunedTo.get(playerUUID);
        if (entries != null) {
            entries.removeIf(e -> e.ownerUUID.equals(ownerUUID));
            save(playerUUID);
        }
    }

    public void revokeAccess(@Nonnull UUID ownerUUID, @Nonnull UUID playerUUID) {
        removeAttunement(playerUUID, ownerUUID);
    }

    @Nonnull
    public List<AttunedEntry> getAttunedDepots(@Nonnull UUID playerUUID) {
        Set<AttunedEntry> entries = attunedTo.get(playerUUID);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(entries);
    }

    @Nonnull
    public List<UUID> getPlayersAttunedTo(@Nonnull UUID ownerUUID) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Set<AttunedEntry>> entry : attunedTo.entrySet()) {
            for (AttunedEntry ae : entry.getValue()) {
                if (ae.ownerUUID.equals(ownerUUID)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }

    public boolean isAttunedTo(@Nonnull UUID playerUUID, @Nonnull UUID ownerUUID) {
        Set<AttunedEntry> entries = attunedTo.get(playerUUID);
        if (entries == null) {
            return false;
        }
        return entries.stream().anyMatch(e -> e.ownerUUID.equals(ownerUUID));
    }

    public void setSelectedTarget(@Nonnull String contextKey, @Nonnull UUID targetUUID) {
        selectedTargets.put(contextKey, targetUUID);
        savePrefs(contextKey);
    }

    @Nullable
    public UUID getSelectedTarget(@Nonnull String contextKey) {
        return selectedTargets.get(contextKey);
    }

    @Nullable
    public String getPlayerName(@Nonnull UUID playerUUID) {
        return playerNames.get(playerUUID);
    }

    private void save(@Nonnull UUID playerUUID) {
        Set<AttunedEntry> entries = attunedTo.get(playerUUID);
        Path file = storageDir.resolve(playerUUID + ".json");
        try {
            if (entries == null || entries.isEmpty()) {
                Files.deleteIfExists(file);
            } else {
                Files.writeString(file, gson.toJson(entries));
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save attunements for %s", playerUUID);
        }
    }

    public void saveAll() {
        for (UUID player : attunedTo.keySet()) {
            save(player);
        }
        saveAllPrefs();
        savePlayerNames();
        LOGGER.at(Level.INFO).log("Saved attunements for %d players.", attunedTo.size());
    }

    private void savePrefs(@Nonnull String contextKey) {
        String safeKey = contextKey.replace(":", "_");
        Path file = prefsDir.resolve(safeKey + ".json");
        UUID target = selectedTargets.get(contextKey);
        try {
            if (target == null) {
                Files.deleteIfExists(file);
            } else {
                Files.writeString(file, gson.toJson(target.toString()));
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save attunement prefs for %s", contextKey);
        }
    }

    private void saveAllPrefs() {
        for (String key : selectedTargets.keySet()) {
            savePrefs(key);
        }
    }

    private void loadAllPrefs() {
        if (!Files.isDirectory(prefsDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(prefsDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(this::loadPrefFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list attunement pref files");
        }
    }

    private void loadPrefFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            String safeKey = name.substring(0, name.length() - ".json".length());
            String contextKey = safeKey.replace("_", ":");
            String json = Files.readString(file);
            String uuidStr = gson.fromJson(json, String.class);
            if (uuidStr != null) {
                selectedTargets.put(contextKey, UUID.fromString(uuidStr));
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load attunement pref file %s", file);
        }
    }

    private void savePlayerNames() {
        Path file = storageDir.getParent().resolve("player_names.json");
        try {
            Map<String, String> serializable = new HashMap<>();
            for (Map.Entry<UUID, String> e : playerNames.entrySet()) {
                serializable.put(e.getKey().toString(), e.getValue());
            }
            Files.writeString(file, gson.toJson(serializable));
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save player names");
        }
    }

    private void loadPlayerNames() {
        Path file = storageDir.getParent().resolve("player_names.json");
        if (!Files.isRegularFile(file)) return;
        try {
            String json = Files.readString(file);
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                for (Map.Entry<String, String> e : loaded.entrySet()) {
                    playerNames.put(UUID.fromString(e.getKey()), e.getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load player names");
        }
    }

    private void loadAll() {
        if (!Files.isDirectory(storageDir)) return;
        try (Stream<Path> files = Files.list(storageDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list attunement files");
        }
        loadAllPrefs();
        loadPlayerNames();
        LOGGER.at(Level.INFO).log("Loaded attunements for %d players.", attunedTo.size());
    }

    public void registerPlayerName(@Nonnull UUID playerUUID, @Nonnull String name) {
        String existing = playerNames.get(playerUUID);
        if (existing == null || !existing.equals(name)) {
            playerNames.put(playerUUID, name);
        }
    }

    private void loadFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            UUID player = UUID.fromString(name.substring(0, name.length() - ".json".length()));
            String json = Files.readString(file);
            Set<AttunedEntry> entries = gson.fromJson(json, SET_TYPE);
            if (entries != null && !entries.isEmpty()) {
                Set<AttunedEntry> concurrent = ConcurrentHashMap.newKeySet();
                concurrent.addAll(entries);
                attunedTo.put(player, concurrent);
                for (AttunedEntry ae : entries) {
                    playerNames.putIfAbsent(ae.ownerUUID, ae.ownerName);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load attunement file %s", file);
        }
    }
}
