package com.destbg.OrbisDepot.Storage;

import com.destbg.OrbisDepot.Components.DepotStorageData;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class DepotStorageManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Type RECORD_TYPE = new TypeToken<StorageRecord>() {
    }.getType();

    private static DepotStorageManager instance;

    private final Path storageDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, DepotStorageData> players = new ConcurrentHashMap<>();

    private DepotStorageManager(@Nonnull Path dataDirectory) {
        this.storageDir = dataDirectory.resolve("depot_storage");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create depot storage directory");
        }
    }

    public static void init(@Nonnull Path dataDirectory) {
        LegacyDataMigration.migrateIfNeeded(dataDirectory);
        instance = new DepotStorageManager(dataDirectory);
        instance.loadAll();
    }

    @Nonnull
    public static DepotStorageManager get() {
        if (instance == null) {
            throw new IllegalStateException("DepotStorageManager not initialized");
        }
        return instance;
    }

    @Nonnull
    public DepotStorageData getOrCreate(@Nonnull UUID uuid) {
        return players.computeIfAbsent(uuid, this::createEmpty);
    }

    @Nullable
    public DepotStorageData get(@Nonnull UUID uuid) {
        return players.get(uuid);
    }

    public void save(@Nonnull UUID uuid) {
        DepotStorageData data = players.get(uuid);
        Path file = storageDir.resolve(uuid + ".json");
        try {
            if (data == null) {
                Files.deleteIfExists(file);
                return;
            }
            StorageRecord record = new StorageRecord(
                    new HashMap<>(data.getItemContainer()),
                    new HashMap<>(data.getAttunedToOthersRaw()),
                    new HashMap<>(data.getAttunedToMeRaw()),
                    data.getStorageUpgradeRank(),
                    data.getSpeedUpgradeRank());
            Files.writeString(file, gson.toJson(record));
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save depot storage for %s", uuid);
        }
    }

    public void saveAll() {
        for (UUID uuid : players.keySet()) {
            save(uuid);
        }
        LOGGER.at(Level.INFO).log("Saved depot storage for %d players.", players.size());
    }

    private DepotStorageData createEmpty(UUID uuid) {
        DepotStorageData data = new DepotStorageData();
        data.setSaveCallback(() -> save(uuid));
        return data;
    }

    private void loadAll() {
        if (!Files.isDirectory(storageDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(storageDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list depot storage files");
        }
        LOGGER.at(Level.INFO).log("Loaded depot storage for %d players.", players.size());
    }

    private void loadFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            UUID uuid = UUID.fromString(name.substring(0, name.length() - ".json".length()));
            String json = Files.readString(file);
            StorageRecord record = gson.fromJson(json, RECORD_TYPE);
            if (record == null) {
                return;
            }

            int storageRank = record.storageUpgradeRank() > 0 ? record.storageUpgradeRank() : 1;
            int speedRank = record.speedUpgradeRank() > 0 ? record.speedUpgradeRank() : 1;
            DepotStorageData data = new DepotStorageData(
                    record.itemContainer() != null ? record.itemContainer() : new HashMap<>(),
                    record.attunedToOthers() != null ? record.attunedToOthers() : new HashMap<>(),
                    record.attunedToMe() != null ? record.attunedToMe() : new HashMap<>(),
                    storageRank,
                    speedRank);
            data.setSaveCallback(() -> save(uuid));
            players.put(uuid, data);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load depot storage file %s", file);
        }
    }

    private record StorageRecord(
            Map<String, Integer> itemContainer,
            Map<String, String> attunedToOthers,
            Map<String, String> attunedToMe,
            int storageUpgradeRank,
            int speedUpgradeRank
    ) {}
}
