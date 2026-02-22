package com.destbg.OrbisDepot.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class CrudeSigilSlotUtils {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Map<UUID, SimpleItemContainer> UPLOAD_SLOTS = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> UPLOAD_TIMERS = new ConcurrentHashMap<>();

    private static Path storageDir;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SLOT_LIST_TYPE = new TypeToken<List<SlotData>>() {
    }.getType();

    private CrudeSigilSlotUtils() {
    }

    public static void init(@Nonnull Path dataDirectory) {
        storageDir = dataDirectory.resolve("crude_sigil_slots");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create crude sigil slots directory");
        }
        loadAll();
    }

    @Nonnull
    public static SimpleItemContainer getUploadSlotContainer(@Nonnull UUID playerUUID) {
        return UPLOAD_SLOTS.computeIfAbsent(playerUUID, _ -> new SimpleItemContainer(Constants.CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY));
    }

    @Nonnull
    public static float[] getTimers(@Nonnull UUID playerUUID) {
        return UPLOAD_TIMERS.computeIfAbsent(playerUUID, _ -> new float[Constants.CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY]);
    }

    public static void resetTimers(@Nonnull UUID playerUUID) {
        float[] timers = UPLOAD_TIMERS.get(playerUUID);
        if (timers != null) {
            Arrays.fill(timers, 0f);
        }
    }

    public static float getTimer(@Nonnull UUID playerUUID, int slot) {
        float[] timers = UPLOAD_TIMERS.get(playerUUID);
        if (timers == null || slot < 0 || slot >= timers.length) {
            return 0f;
        }
        return timers[slot];
    }

    public static void tickAll(float deltaSeconds) {
        for (Map.Entry<UUID, SimpleItemContainer> entry : UPLOAD_SLOTS.entrySet()) {
            UUID playerUUID = entry.getKey();
            SimpleItemContainer container = entry.getValue();
            float[] timers = getTimers(playerUUID);

            for (short slot = 0; slot < container.getCapacity(); slot++) {
                var stack = container.getItemStack(slot);
                if (stack == null || ItemStack.isEmpty(stack)) {
                    if (slot < timers.length) {
                        timers[slot] = 0f;
                    }
                    continue;
                }

                if (slot < timers.length) {
                    timers[slot] += deltaSeconds;
                    while (timers[slot] >= Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS) {
                        timers[slot] -= Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS;
                        if (DepositUtils.processSlot(container, slot, playerUUID, 1)) {
                            timers[slot] = 0f;
                            break;
                        }
                        ItemStack remaining = container.getItemStack(slot);
                        if (remaining == null || ItemStack.isEmpty(remaining)) {
                            timers[slot] = 0f;
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void saveAll() {
        if (storageDir == null) {
            return;
        }

        int saved = 0;
        for (Map.Entry<UUID, SimpleItemContainer> entry : UPLOAD_SLOTS.entrySet()) {
            UUID playerUUID = entry.getKey();
            SimpleItemContainer container = entry.getValue();
            Path file = storageDir.resolve(playerUUID.toString() + ".json");

            List<SlotData> slots = new ArrayList<>();
            for (short i = 0; i < container.getCapacity(); i++) {
                ItemStack stack = container.getItemStack(i);
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    slots.add(new SlotData(stack.getItemId(), stack.getQuantity()));
                }
            }

            try {
                if (slots.isEmpty()) {
                    Files.deleteIfExists(file);
                } else {
                    Files.writeString(file, GSON.toJson(slots));
                    saved++;
                }
            } catch (IOException e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save crude sigil slots for %s", playerUUID);
            }
        }
        LOGGER.at(Level.INFO).log("Saved crude sigil deposit slots for %d players.", saved);
    }

    private static void loadAll() {
        if (storageDir == null || !Files.isDirectory(storageDir)) {
            return;
        }

        try (Stream<Path> files = Files.list(storageDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(CrudeSigilSlotUtils::loadFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list crude sigil slot files");
        }
    }

    private static void loadFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            UUID playerUUID = UUID.fromString(name.substring(0, name.length() - ".json".length()));
            String json = Files.readString(file);
            List<SlotData> slots = GSON.fromJson(json, SLOT_LIST_TYPE);
            if (slots == null || slots.isEmpty()) {
                return;
            }

            SimpleItemContainer container = getUploadSlotContainer(playerUUID);
            for (int i = 0; i < Math.min(slots.size(), container.getCapacity()); i++) {
                SlotData data = slots.get(i);
                if (data.itemId != null && data.quantity > 0) {
                    container.setItemStackForSlot((short) i, new ItemStack(data.itemId, data.quantity));
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load crude sigil slot file %s", file);
        }
    }

    private record SlotData(String itemId, int quantity) {
    }
}

