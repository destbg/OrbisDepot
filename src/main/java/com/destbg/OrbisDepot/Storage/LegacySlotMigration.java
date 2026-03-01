package com.destbg.OrbisDepot.Storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class LegacySlotMigration {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new Gson();
    private static final Type SLOT_LIST_TYPE = new TypeToken<List<SlotData>>() {
    }.getType();

    @Nullable
    private static Path dataDirectory;

    private LegacySlotMigration() {
    }

    public static void init(@Nonnull Path dir) {
        dataDirectory = dir;
    }

    public static void migrateSigilSlots(@Nonnull UUID playerUUID, @Nonnull ItemContainer target) {
        migratePlayerSlots("sigil_slots", playerUUID, target);
    }

    public static void migrateCrudeSigilSlots(@Nonnull UUID playerUUID, @Nonnull ItemContainer target) {
        migratePlayerSlots("crude_sigil_slots", playerUUID, target);
    }

    @Nullable
    public static UUID readLegacyDepotOwner(@Nonnull String posKey) {
        if (dataDirectory == null) {
            return null;
        }
        Path dir = dataDirectory.resolve("depot_slots");
        String fileName = posKey.replace(":", "_") + ".json";
        Path file = dir.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            String json = Files.readString(file);
            DepotFileData data = GSON.fromJson(json, DepotFileData.class);
            if (data == null || data.ownerUUID == null) {
                return null;
            }
            return UUID.fromString(data.ownerUUID);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log(
                    "Failed to read legacy depot owner for posKey %s", posKey);
            return null;
        }
    }

    public static void migrateDepotSlots(@Nonnull String posKey, @Nonnull ItemContainer target) {
        if (dataDirectory == null) {
            return;
        }
        Path dir = dataDirectory.resolve("depot_slots");
        String fileName = posKey.replace(":", "_") + ".json";
        Path file = dir.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String json = Files.readString(file);
            DepotFileData data = GSON.fromJson(json, DepotFileData.class);
            if (data == null || data.slots == null) {
                Files.deleteIfExists(file);
                tryDeleteEmptyDir(dir);
                return;
            }
            short slot = 0;
            for (SlotData sd : data.slots) {
                if (slot >= target.getCapacity()) {
                    break;
                }
                if (sd.itemId != null && sd.quantity > 0) {
                    ItemStack existing = target.getItemStack(slot);
                    if (existing == null || ItemStack.isEmpty(existing)) {
                        target.setItemStackForSlot(slot, new ItemStack(sd.itemId, sd.quantity));
                    }
                }
                slot++;
            }
            Files.delete(file);
            tryDeleteEmptyDir(dir);
            LOGGER.at(Level.INFO).log("Migrated legacy depot slot for posKey %s", posKey);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log(
                    "Failed to migrate legacy depot slot for posKey %s", posKey);
        }
    }

    private static void migratePlayerSlots(@Nonnull String folder,
                                            @Nonnull UUID playerUUID,
                                            @Nonnull ItemContainer target) {
        if (dataDirectory == null) {
            return;
        }
        Path dir = dataDirectory.resolve(folder);
        Path file = dir.resolve(playerUUID + ".json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String json = Files.readString(file);
            List<SlotData> slots = GSON.fromJson(json, SLOT_LIST_TYPE);
            if (slots == null || slots.isEmpty()) {
                Files.deleteIfExists(file);
                tryDeleteEmptyDir(dir);
                return;
            }
            short slot = 0;
            for (SlotData sd : slots) {
                if (slot >= target.getCapacity()) {
                    break;
                }
                if (sd.itemId != null && sd.quantity > 0) {
                    ItemStack existing = target.getItemStack(slot);
                    if (existing == null || ItemStack.isEmpty(existing)) {
                        target.setItemStackForSlot(slot, new ItemStack(sd.itemId, sd.quantity));
                    }
                }
                slot++;
            }
            Files.delete(file);
            tryDeleteEmptyDir(dir);
            LOGGER.at(Level.INFO).log("Migrated legacy %s for player %s", folder, playerUUID);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log(
                    "Failed to migrate legacy %s for player %s", folder, playerUUID);
        }
    }

    private static void tryDeleteEmptyDir(@Nonnull Path dir) {
        try {
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    if (stream.findAny().isEmpty()) {
                        Files.delete(dir);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private record SlotData(String itemId, int quantity) {
    }

    private static class DepotFileData {
        String ownerUUID;
        List<SlotData> slots;
    }
}
