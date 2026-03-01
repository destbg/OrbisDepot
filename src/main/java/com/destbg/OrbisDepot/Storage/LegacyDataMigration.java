package com.destbg.OrbisDepot.Storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class LegacyDataMigration {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LegacyDataMigration() {
    }

    public static void migrateFromRenamedDirectory(@Nonnull Path dataDirectory) {
        if (dataDirectory.getParent() == null) {
            return;
        }
        String oldName = dataDirectory.getFileName().toString().replace(" ", "");
        if (oldName.equals(dataDirectory.getFileName().toString())) {
            return;
        }
        Path oldDir = dataDirectory.getParent().resolve(oldName);
        if (!Files.isDirectory(oldDir)) {
            return;
        }

        LOGGER.at(Level.INFO).log(
                "Detected old data directory '%s' — copying into '%s'...",
                oldName, dataDirectory.getFileName());
        try {
            Files.createDirectories(dataDirectory);
            Files.walkFileTree(oldDir, new SimpleFileVisitor<>() {
                @Override
                @Nonnull
                public FileVisitResult preVisitDirectory(@Nonnull Path dir, @Nonnull BasicFileAttributes attrs) throws IOException {
                    Path target = dataDirectory.resolve(oldDir.relativize(dir));
                    Files.createDirectories(target);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @Nonnull
                public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
                    Path target = dataDirectory.resolve(oldDir.relativize(file));
                    if (!Files.exists(target)) {
                        Files.copy(file, target);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            deleteDirectory(oldDir);
            LOGGER.at(Level.INFO).log("Old data directory migration complete.");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log(
                    "Failed to migrate from old data directory '%s' — old files have not been removed", oldName);
        }
    }

    public static void migrateIfNeeded(@Nonnull Path dataDirectory) {
        Path voidStorageDir = dataDirectory.resolve("void_storage");
        if (!Files.isDirectory(voidStorageDir)) {
            return;
        }

        LOGGER.at(Level.INFO).log("Legacy OrbisDepot data detected — running one-time migration...");

        try {
            Path depotStorageDir = dataDirectory.resolve("depot_storage");
            Files.createDirectories(depotStorageDir);

            Map<UUID, String> playerNames = loadPlayerNames(dataDirectory);

            Map<UUID, Map<String, String>> attunedToMeByPlayer = new HashMap<>();
            Map<UUID, Map<String, String>> attunedToOthersByPlayer = new HashMap<>();
            loadAttunements(dataDirectory.resolve("attunements"), playerNames, attunedToMeByPlayer, attunedToOthersByPlayer);

            Set<UUID> allPlayers = new HashSet<>();
            collectUUIDsFromJsonDir(voidStorageDir, allPlayers);
            allPlayers.addAll(attunedToMeByPlayer.keySet());
            allPlayers.addAll(attunedToOthersByPlayer.keySet());

            int migrated = 0;
            for (UUID uuid : allPlayers) {
                Path newFile = depotStorageDir.resolve(uuid + ".json");
                if (Files.exists(newFile)) {
                    continue;
                }

                Map<String, Integer> itemContainer = loadVoidStorage(voidStorageDir, uuid);
                Map<String, String> attunedToMe = attunedToMeByPlayer.getOrDefault(uuid, new HashMap<>());
                Map<String, String> attunedToOthers = attunedToOthersByPlayer.getOrDefault(uuid, new HashMap<>());

                MigrationRecord record = new MigrationRecord(
                        itemContainer, attunedToOthers, attunedToMe, 1, 1);
                Files.writeString(newFile, GSON.toJson(record));
                migrated++;
            }

            LOGGER.at(Level.INFO).log("Migrated %d player(s). Removing legacy folders...", migrated);
            deleteLegacyData(dataDirectory);
            LOGGER.at(Level.INFO).log("Legacy data migration complete.");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to migrate legacy OrbisDepot data — old files have not been removed");
        }
    }

    private static Map<UUID, String> loadPlayerNames(@Nonnull Path dataDirectory) {
        Path file = dataDirectory.resolve("player_names.json");
        if (!Files.isRegularFile(file)) {
            return new HashMap<>();
        }
        try {
            Map<String, String> raw = GSON.fromJson(Files.readString(file), STRING_MAP_TYPE);
            if (raw == null) {
                return new HashMap<>();
            }
            Map<UUID, String> result = new HashMap<>();
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                try {
                    result.put(UUID.fromString(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to read player_names.json — player names may be missing");
            return new HashMap<>();
        }
    }

    private static void loadAttunements(@Nonnull Path attunementDir, @Nonnull Map<UUID, String> playerNames, @Nonnull Map<UUID, Map<String, String>> attunedToMeOut, @Nonnull Map<UUID, Map<String, String>> attunedToOthersOut) {
        if (!Files.isDirectory(attunementDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(attunementDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(file -> {
                try {
                    String name = file.getFileName().toString();
                    UUID playerUUID = UUID.fromString(name.substring(0, name.length() - ".json".length()));
                    JsonArray array = JsonParser.parseString(Files.readString(file)).getAsJsonArray();

                    Map<String, String> myAttunements = new HashMap<>();
                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        UUID ownerUUID = parseUUID(obj.get("ownerUUID"));
                        String ownerName = obj.has("ownerName")
                                ? obj.get("ownerName").getAsString()
                                : ownerUUID.toString().substring(0, 8);

                        myAttunements.put(ownerUUID.toString(), ownerName);

                        String playerName = playerNames.getOrDefault(playerUUID,
                                playerUUID.toString().substring(0, 8));
                        attunedToOthersOut.computeIfAbsent(ownerUUID, _ -> new HashMap<>())
                                .put(playerUUID.toString(), playerName);
                    }
                    if (!myAttunements.isEmpty()) {
                        attunedToMeOut.put(playerUUID, myAttunements);
                    }
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).withCause(e).log("Skipping unreadable attunement file: %s", file);
                }
            });
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to list attunements directory");
        }
    }

    private static void collectUUIDsFromJsonDir(@Nonnull Path dir, @Nonnull Set<UUID> out) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(file -> {
                try {
                    String n = file.getFileName().toString();
                    out.add(UUID.fromString(n.substring(0, n.length() - ".json".length())));
                } catch (IllegalArgumentException ignored) {
                }
            });
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to list directory: %s", dir);
        }
    }

    @Nonnull
    private static Map<String, Integer> loadVoidStorage(@Nonnull Path voidStorageDir, @Nonnull UUID uuid) {
        Path file = voidStorageDir.resolve(uuid + ".json");
        if (!Files.isRegularFile(file)) {
            return new HashMap<>();
        }
        try {
            JsonObject obj = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            Map<String, Integer> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                long count = entry.getValue().getAsLong();
                if (count > 0) {
                    result.put(entry.getKey(), (int) Math.min(count, Integer.MAX_VALUE));
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to read void_storage for %s", uuid);
            return new HashMap<>();
        }
    }

    private static UUID parseUUID(@Nonnull JsonElement element) {
        if (element.isJsonPrimitive()) {
            return UUID.fromString(element.getAsString());
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            long mostSigBits = obj.get("mostSigBits").getAsLong();
            long leastSigBits = obj.get("leastSigBits").getAsLong();
            return new UUID(mostSigBits, leastSigBits);
        }
        throw new IllegalArgumentException("Cannot parse UUID from JSON element: " + element);
    }

    private static void deleteLegacyData(@Nonnull Path dataDirectory) throws IOException {
        deleteDirectory(dataDirectory.resolve("void_storage"));
        deleteDirectory(dataDirectory.resolve("attunements"));
        deleteDirectory(dataDirectory.resolve("attunement_prefs"));
        deleteDirectory(dataDirectory.resolve("player_settings"));
        Files.deleteIfExists(dataDirectory.resolve("player_names.json"));
        // sigil_slots, crude_sigil_slots, and depot_slots are migrated lazily into ECS
        // components on first interaction — do not bulk-delete them here.
    }

    private static void deleteDirectory(@Nonnull Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            @Nonnull
            public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            @Nonnull
            public FileVisitResult postVisitDirectory(@Nonnull Path d, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private record MigrationRecord(
            Map<String, Integer> itemContainer,
            Map<String, String> attunedToOthers,
            Map<String, String> attunedToMe,
            int storageUpgradeRank,
            int speedUpgradeRank
    ) {
    }
}
