package com.destbg.OrbisDepot.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class VoidStorageManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static VoidStorageManager instance;

    private final Path storageDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, ConcurrentHashMap<String, Long>> playerStorage = new ConcurrentHashMap<>();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {
    }.getType();

    private VoidStorageManager(@Nonnull Path dataDirectory) {
        this.storageDir = dataDirectory.resolve("void_storage");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create void storage directory");
        }
    }

    public static void init(@Nonnull Path dataDirectory) {
        instance = new VoidStorageManager(dataDirectory);
        instance.loadAll();
    }

    @Nonnull
    public static VoidStorageManager get() {
        if (instance == null) {
            throw new IllegalStateException("VoidStorageManager not initialized");
        }
        return instance;
    }

    public void addItems(@Nonnull UUID player, @Nonnull String itemId, long count) {
        if (count <= 0) {
            return;
        }

        ConcurrentHashMap<String, Long> items = playerStorage.computeIfAbsent(player, _ -> new ConcurrentHashMap<>());
        items.merge(itemId, count, Long::sum);
        saveLater(player);
    }

    public void removeItems(@Nonnull UUID player, @Nonnull String itemId, long count) {
        if (count <= 0) {
            return;
        }

        ConcurrentHashMap<String, Long> items = playerStorage.get(player);
        if (items == null) {
            return;
        }

        Long current = items.get(itemId);
        if (current == null || current <= 0) {
            return;
        }

        long removed = Math.min(current, count);
        long remaining = current - removed;
        if (remaining <= 0) {
            items.remove(itemId);
        } else {
            items.put(itemId, remaining);
        }
        saveLater(player);
    }

    @Nonnull
    public Map<String, Long> getItems(@Nonnull UUID player) {
        ConcurrentHashMap<String, Long> items = playerStorage.get(player);
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }

        return new LinkedHashMap<>(items);
    }

    @Nonnull
    public Map<String, Long> searchItems(@Nonnull UUID player, @Nonnull String query) {
        String q = query.trim().toLowerCase();
        Map<String, Long> all = getItems(player);
        if (q.isEmpty()) {
            return all;
        }

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : all.entrySet()) {
            if (entry.getKey().toLowerCase().contains(q)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public long getItemCount(@Nonnull UUID player, @Nonnull String itemId) {
        ConcurrentHashMap<String, Long> items = playerStorage.get(player);
        if (items == null) {
            return 0;
        }

        return items.getOrDefault(itemId, 0L);
    }

    private void saveLater(@Nonnull UUID player) {
        save(player);
    }

    public void save(@Nonnull UUID player) {
        ConcurrentHashMap<String, Long> items = playerStorage.get(player);
        Path file = storageDir.resolve(player + ".json");
        try {
            if (items == null || items.isEmpty()) {
                Files.deleteIfExists(file);
            } else {
                String json = gson.toJson(items);
                Files.writeString(file, json);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save void storage for %s", player);
        }
    }

    public void saveAll() {
        for (UUID player : playerStorage.keySet()) {
            save(player);
        }
        LOGGER.at(Level.INFO).log("Saved void storage for %d players.", playerStorage.size());
    }

    private void loadAll() {
        if (!Files.isDirectory(storageDir)) return;
        try (Stream<Path> files = Files.list(storageDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list void storage files");
        }
        LOGGER.at(Level.INFO).log("Loaded void storage for %d players.", playerStorage.size());
    }

    private void loadFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            UUID player = UUID.fromString(name.substring(0, name.length() - ".json".length()));
            String json = Files.readString(file);
            Map<String, Long> items = gson.fromJson(json, MAP_TYPE);
            if (items != null && !items.isEmpty()) {
                playerStorage.put(player, new ConcurrentHashMap<>(items));
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load void storage file %s", file);
        }
    }
}
