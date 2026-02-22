package com.destbg.OrbisDepot.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class DepotSlotUtils {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Map<String, SimpleItemContainer> UPLOAD_SLOTS = new ConcurrentHashMap<>();
    private static final Map<String, float[]> UPLOAD_TIMERS = new ConcurrentHashMap<>();
    private static final Map<String, UUID> SLOT_OWNERS = new ConcurrentHashMap<>();
    private static final Map<String, UUID> DEPOSIT_TARGETS = new ConcurrentHashMap<>();

    private static Path storageDir;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DepotSlotUtils() {
    }

    public static void init(@Nonnull Path dataDirectory) {
        storageDir = dataDirectory.resolve("depot_slots");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create depot slots directory");
        }
        loadAll();
    }

    public static String posKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    @Nonnull
    public static SimpleItemContainer getUploadSlotContainer(@Nonnull String posKey) {
        return UPLOAD_SLOTS.computeIfAbsent(posKey, _ -> new SimpleItemContainer((short) 1));
    }

    @Nonnull
    public static float[] getTimers(@Nonnull String posKey) {
        return UPLOAD_TIMERS.computeIfAbsent(posKey, _ -> new float[1]);
    }

    public static void resetTimer(@Nonnull String posKey) {
        float[] timers = UPLOAD_TIMERS.get(posKey);
        if (timers != null) {
            timers[0] = 0f;
        }
    }

    public static float getTimer(@Nonnull String posKey) {
        float[] timers = UPLOAD_TIMERS.get(posKey);
        return timers != null ? timers[0] : 0f;
    }

    public static void registerDepot(@Nonnull String posKey, @Nonnull UUID ownerUUID) {
        SLOT_OWNERS.put(posKey, ownerUUID);
    }

    @Nullable
    public static UUID getOwner(@Nonnull String posKey) {
        return SLOT_OWNERS.get(posKey);
    }

    public static void setDepositTarget(@Nonnull String posKey, @Nonnull UUID targetUUID) {
        DEPOSIT_TARGETS.put(posKey, targetUUID);
    }

    @Nonnull
    public static UUID getDepositTarget(@Nonnull String posKey) {
        UUID target = DEPOSIT_TARGETS.get(posKey);
        if (target != null) {
            return target;
        }
        UUID owner = SLOT_OWNERS.get(posKey);
        return owner != null ? owner : UUID.randomUUID();
    }

    public static void tickAll(float deltaSeconds) {
        for (Map.Entry<String, UUID> entry : SLOT_OWNERS.entrySet()) {
            String posKey = entry.getKey();
            UUID depositTarget = getDepositTarget(posKey);
            SimpleItemContainer container = UPLOAD_SLOTS.get(posKey);
            if (container == null) {
                continue;
            }

            float[] timers = getTimers(posKey);
            var stack = container.getItemStack((short) 0);
            if (stack == null || ItemStack.isEmpty(stack)) {
                timers[0] = 0f;
                continue;
            }

            timers[0] += deltaSeconds;
            while (timers[0] >= Constants.UPLOAD_INTERVAL_DEPOT_SECONDS) {
                timers[0] -= Constants.UPLOAD_INTERVAL_DEPOT_SECONDS;
                if (!DepositUtils.processSlot(container, (short) 0, depositTarget, 1)) {
                    timers[0] = 0f;
                    break;
                }
                var remaining = container.getItemStack((short) 0);
                if (remaining == null || ItemStack.isEmpty(remaining)) {
                    timers[0] = 0f;
                    break;
                }
            }
        }
    }

    public static void saveAll() {
        if (storageDir == null) {
            return;
        }
        int saved = 0;
        for (Map.Entry<String, UUID> entry : SLOT_OWNERS.entrySet()) {
            String posKey = entry.getKey();
            UUID owner = entry.getValue();
            SimpleItemContainer container = UPLOAD_SLOTS.get(posKey);
            Path file = storageDir.resolve(posKey.replace(":", "_") + ".json");

            List<SlotData> slots = new ArrayList<>();
            if (container != null) {
                for (short i = 0; i < container.getCapacity(); i++) {
                    ItemStack stack = container.getItemStack(i);
                    if (stack != null && !ItemStack.isEmpty(stack)) {
                        slots.add(new SlotData(stack.getItemId(), stack.getQuantity()));
                    }
                }
            }

            try {
                if (slots.isEmpty()) {
                    Files.deleteIfExists(file);
                } else {
                    DepotFileData data = new DepotFileData(owner.toString(), slots);
                    Files.writeString(file, GSON.toJson(data));
                    saved++;
                }
            } catch (IOException e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save depot slots for %s", posKey);
            }
        }
        LOGGER.at(Level.INFO).log("Saved depot deposit slots for %d depots.", saved);
    }

    private static void loadAll() {
        if (storageDir == null || !Files.isDirectory(storageDir)) {
            return;
        }

        try (Stream<Path> files = Files.list(storageDir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(DepotSlotUtils::loadFile);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list depot slot files");
        }

        LOGGER.at(Level.INFO).log("Loaded depot deposit slots for %d depots.", SLOT_OWNERS.size());
    }

    private static void loadFile(@Nonnull Path file) {
        try {
            String name = file.getFileName().toString();
            String posKey = name.substring(0, name.length() - ".json".length()).replace("_", ":");
            String json = Files.readString(file);
            DepotFileData data = GSON.fromJson(json, DepotFileData.class);
            if (data == null || data.ownerUUID == null || data.slots == null || data.slots.isEmpty()) {
                return;
            }

            UUID owner = UUID.fromString(data.ownerUUID);
            SLOT_OWNERS.put(posKey, owner);

            SimpleItemContainer container = getUploadSlotContainer(posKey);
            for (int i = 0; i < Math.min(data.slots.size(), container.getCapacity()); i++) {
                SlotData sd = data.slots.get(i);
                if (sd.itemId != null && sd.quantity > 0) {
                    container.setItemStackForSlot((short) i, new ItemStack(sd.itemId, sd.quantity));
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load depot slot file %s", file);
        }
    }

    private record SlotData(String itemId, int quantity) {
    }

    private record DepotFileData(String ownerUUID, List<SlotData> slots) {
    }
}
