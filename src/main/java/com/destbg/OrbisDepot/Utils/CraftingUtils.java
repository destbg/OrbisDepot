package com.destbg.OrbisDepot.Utils;

import com.destbg.OrbisDepot.Crafting.VoidStorageItemContainer;
import com.destbg.OrbisDepot.Storage.PlayerSettingsManager;
import com.destbg.OrbisDepot.Storage.VoidStorageManager;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ExtraResources;
import com.hypixel.hytale.protocol.InventorySection;
import com.hypixel.hytale.protocol.ItemQuantity;
import com.hypixel.hytale.protocol.packets.window.UpdateWindow;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialExtraResourcesSection;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.builtin.crafting.window.FieldCraftingWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CraftingUtils {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<UUID, VoidStorageItemContainer> voidContainers = new ConcurrentHashMap<>();

    private CraftingUtils() {
    }

    public static void injectForPlayer(Ref<EntityStore> ref, Store<EntityStore> store, Window triggeringWindow) {
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            UUID uuid = playerRef.getUuid();
            if (!PlayerSettingsManager.get().isCraftingIntegrationEnabled(uuid)) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null || !InventoryUtils.hasOrbisSigil(player.getInventory())) {
                return;
            }

            Map<String, Long> voidItems = VoidStorageManager.get().getItems(uuid);
            if (voidItems.isEmpty()) {
                return;
            }

            PacketHandler packetHandler = playerRef.getPacketHandler();
            VoidStorageItemContainer voidContainer = voidContainers.computeIfAbsent(uuid, VoidStorageItemContainer::new);
            voidContainer.refresh();
            if (voidContainer.getCapacity() == 0) {
                return;
            }

            ItemQuantity[] voidQuantities = buildItemQuantities(voidContainer);
            if (voidQuantities.length == 0) {
                return;
            }

            if (triggeringWindow instanceof MaterialContainerWindow mcw) {
                injectBenchVoidStorage(mcw, triggeringWindow, packetHandler, voidContainer);
            } else if (triggeringWindow instanceof FieldCraftingWindow) {
                sendFieldCraftingExtraResources(triggeringWindow, packetHandler, voidQuantities);
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Error injecting void storage for player");
        }
    }

    public static void onBlockUsed(Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        CompletableFuture.runAsync(() -> {
            try {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }

                UUID uuid = playerRef.getUuid();
                if (!PlayerSettingsManager.get().isCraftingIntegrationEnabled(uuid)) {
                    return;
                }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    return;
                }

                if (!InventoryUtils.hasOrbisSigil(player.getInventory())) {
                    return;
                }

                Map<String, Long> voidItems = VoidStorageManager.get().getItems(uuid);
                if (voidItems.isEmpty()) {
                    return;
                }

                PacketHandler packetHandler = playerRef.getPacketHandler();
                VoidStorageItemContainer voidContainer = voidContainers.computeIfAbsent(uuid, VoidStorageItemContainer::new);
                voidContainer.refresh();
                if (voidContainer.getCapacity() == 0) {
                    return;
                }

                ItemQuantity[] voidQuantities = buildItemQuantities(voidContainer);
                if (voidQuantities.length == 0) {
                    return;
                }

                List<Window> windows = player.getWindowManager().getWindows();
                for (Window window : windows) {
                    if (window instanceof MaterialContainerWindow mcw) {
                        injectBenchVoidStorage(mcw, window, packetHandler, voidContainer);
                    } else if (window instanceof FieldCraftingWindow) {
                        sendFieldCraftingExtraResources(window, packetHandler, voidQuantities);
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error in post-block-use crafting injection");
            }
        }, world);
    }

    public static void onStorageChanged(UUID playerUUID, World world) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!PlayerSettingsManager.get().isCraftingIntegrationEnabled(playerUUID)) {
                    return;
                }

                PlayerRef playerRef = null;
                for (PlayerRef pr : world.getPlayerRefs()) {
                    if (pr.getUuid().equals(playerUUID)) {
                        playerRef = pr;
                        break;
                    }
                }
                if (playerRef == null) {
                    return;
                }

                Ref<EntityStore> playerEntity = playerRef.getReference();
                if (playerEntity == null) {
                    return;
                }

                Store<EntityStore> playerEntityStore = playerEntity.getStore();
                Player player = playerEntityStore.getComponent(playerEntity, Player.getComponentType());
                if (player == null) {
                    return;
                }

                if (!InventoryUtils.hasOrbisSigil(player.getInventory())) {
                    return;
                }

                PacketHandler packetHandler = playerRef.getPacketHandler();
                VoidStorageItemContainer voidContainer = voidContainers.computeIfAbsent(playerUUID, VoidStorageItemContainer::new);
                voidContainer.refresh();

                ItemQuantity[] voidQuantities = (voidContainer.getCapacity() > 0)
                        ? buildItemQuantities(voidContainer)
                        : new ItemQuantity[0];

                List<Window> windows = player.getWindowManager().getWindows();
                for (Window window : windows) {
                    if (voidQuantities.length == 0) {
                        break;
                    }
                    if (window instanceof MaterialContainerWindow mcw) {
                        injectBenchVoidStorage(mcw, window, packetHandler, voidContainer);
                    } else if (window instanceof FieldCraftingWindow) {
                        sendFieldCraftingExtraResources(window, packetHandler, voidQuantities);
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error updating crafting windows after storage change");
            }
        }, world);
    }

    public static void cleanup() {
        voidContainers.clear();
    }

    private static void injectBenchVoidStorage(MaterialContainerWindow mcw, Window window,
                                               PacketHandler packetHandler, VoidStorageItemContainer voidContainer) {
        MaterialExtraResourcesSection section = mcw.getExtraResourcesSection();
        ItemContainer existingContainer = section.getItemContainer();

        boolean alreadyInjected = false;
        if (existingContainer instanceof CombinedItemContainer combined) {
            alreadyInjected = combined.containsContainer(voidContainer);
        } else if (existingContainer == voidContainer) {
            alreadyInjected = true;
        }

        if (!alreadyInjected) {
            ItemContainer combinedContainer;
            if (existingContainer != null) {
                combinedContainer = new CombinedItemContainer(existingContainer, voidContainer);
            } else {
                combinedContainer = voidContainer;
            }
            section.setItemContainer(combinedContainer);
        }

        ItemQuantity[] combinedQuantities = buildItemQuantities(section.getItemContainer());
        section.setExtraMaterials(combinedQuantities);
        section.setValid(true);

        UpdateWindow packet = buildUpdatePacket(window, combinedQuantities);
        packetHandler.write(packet);
    }

    private static void sendFieldCraftingExtraResources(Window window, PacketHandler packetHandler,
                                                        ItemQuantity[] voidQuantities) {
        UpdateWindow packet = buildUpdatePacket(window, voidQuantities);
        packetHandler.write(packet);
    }

    private static UpdateWindow buildUpdatePacket(Window window, ItemQuantity[] quantities) {
        JsonObject data = window.getData();
        if (!data.has("nearbyChestCount") || data.get("nearbyChestCount").getAsInt() == 0) {
            data.addProperty("nearbyChestCount", 1);
        }

        InventorySection inventorySection = null;
        if (window instanceof ItemContainerWindow icw) {
            ItemContainer ic = icw.getItemContainer();
            inventorySection = ic.toPacket();
        }

        ExtraResources extraResources = new ExtraResources(quantities);
        return new UpdateWindow(
                window.getId(),
                data.toString(),
                inventorySection,
                extraResources);
    }

    private static ItemQuantity[] buildItemQuantities(ItemContainer container) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        short cap = container.getCapacity();
        for (short i = 0; i < cap; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && !ItemStack.isEmpty(stack)) {
                quantities.merge(stack.getItemId(), stack.getQuantity(), Integer::sum);
            }
        }

        ItemQuantity[] result = new ItemQuantity[quantities.size()];
        int idx = 0;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            result[idx++] = new ItemQuantity(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
