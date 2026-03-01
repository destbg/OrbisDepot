package com.destbg.OrbisDepot.Utils;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Components.SigilPlayerData;
import com.destbg.OrbisDepot.Crafting.DepotStorageItemContainer;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CraftingUtils {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<UUID, DepotStorageItemContainer> depotContainers = new ConcurrentHashMap<>();

    public static void injectForPlayer(Ref<EntityStore> ref, Store<EntityStore> store, Window triggeringWindow) {
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            UUID uuid = playerRef.getUuid();

            SigilPlayerData sigilData = store.getComponent(ref, SigilPlayerData.getComponentType());
            if (sigilData == null || !sigilData.setCraftingIntegration()) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null || !InventoryUtils.hasOrbisSigil(player.getInventory())) {
                return;
            }

            DepotStorageData depotStorage = DepotStorageManager.get().get(uuid);
            if (depotStorage == null || depotStorage.getItemContainer().isEmpty()) {
                return;
            }

            PacketHandler packetHandler = playerRef.getPacketHandler();
            DepotStorageItemContainer depotContainer = depotContainers.computeIfAbsent(uuid,
                    u -> new DepotStorageItemContainer(u, depotStorage));
            depotContainer.refresh();
            if (depotContainer.getCapacity() == 0) {
                return;
            }

            ItemQuantity[] depotQuantities = buildItemQuantities(depotContainer);
            if (depotQuantities.length == 0) {
                return;
            }

            if (triggeringWindow instanceof MaterialContainerWindow mcw) {
                injectBenchDepotStorage(mcw, triggeringWindow, packetHandler, depotContainer);
            } else if (triggeringWindow instanceof FieldCraftingWindow) {
                sendFieldCraftingExtraResources(triggeringWindow, packetHandler, depotQuantities);
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Error injecting depot storage for player");
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

                SigilPlayerData sigilData = store.getComponent(ref, SigilPlayerData.getComponentType());
                if (sigilData == null || !sigilData.setCraftingIntegration()) {
                    return;
                }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null || !InventoryUtils.hasOrbisSigil(player.getInventory())) {
                    return;
                }

                DepotStorageData depotStorage = DepotStorageManager.get().get(uuid);
                if (depotStorage == null || depotStorage.getItemContainer().isEmpty()) {
                    return;
                }

                PacketHandler packetHandler = playerRef.getPacketHandler();
                DepotStorageItemContainer depotContainer = depotContainers.computeIfAbsent(uuid,
                        u -> new DepotStorageItemContainer(u, depotStorage));
                depotContainer.refresh();
                if (depotContainer.getCapacity() == 0) {
                    return;
                }

                ItemQuantity[] depotQuantities = buildItemQuantities(depotContainer);
                if (depotQuantities.length == 0) {
                    return;
                }

                List<Window> windows = player.getWindowManager().getWindows();
                for (Window window : windows) {
                    if (window instanceof MaterialContainerWindow mcw) {
                        injectBenchDepotStorage(mcw, window, packetHandler, depotContainer);
                    } else if (window instanceof FieldCraftingWindow) {
                        sendFieldCraftingExtraResources(window, packetHandler, depotQuantities);
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error in post-block-use crafting injection");
            }
        }, world);
    }

    public static void onStorageChanged(UUID playerUUID) {
        World world;
        try {
            world = Universe.get().getDefaultWorld();
        } catch (Exception e) {
            return;
        }
        if (world == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
                if (playerRef == null) {
                    return;
                }

                Ref<EntityStore> playerEntity = playerRef.getReference();
                if (playerEntity == null) {
                    return;
                }

                Store<EntityStore> playerStore = playerEntity.getStore();

                SigilPlayerData sigilData = playerStore.getComponent(playerEntity, SigilPlayerData.getComponentType());
                if (sigilData == null || !sigilData.setCraftingIntegration()) {
                    return;
                }

                Player player = playerStore.getComponent(playerEntity, Player.getComponentType());
                if (player == null || !InventoryUtils.hasOrbisSigil(player.getInventory())) {
                    return;
                }

                DepotStorageData depotStorage = DepotStorageManager.get().get(playerUUID);
                if (depotStorage == null) {
                    return;
                }

                PacketHandler packetHandler = playerRef.getPacketHandler();
                DepotStorageItemContainer depotContainer = depotContainers.computeIfAbsent(playerUUID,
                        u -> new DepotStorageItemContainer(u, depotStorage));
                depotContainer.refresh();

                ItemQuantity[] depotQuantities = depotContainer.getCapacity() > 0
                        ? buildItemQuantities(depotContainer)
                        : new ItemQuantity[0];

                List<Window> windows = player.getWindowManager().getWindows();
                for (Window window : windows) {
                    if (depotQuantities.length == 0) {
                        break;
                    }
                    if (window instanceof MaterialContainerWindow mcw) {
                        injectBenchDepotStorage(mcw, window, packetHandler, depotContainer);
                    } else if (window instanceof FieldCraftingWindow) {
                        sendFieldCraftingExtraResources(window, packetHandler, depotQuantities);
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Error updating crafting windows after storage change");
            }
        }, world);
    }

    private static void injectBenchDepotStorage(MaterialContainerWindow mcw, Window window, PacketHandler packetHandler, DepotStorageItemContainer depotContainer) {
        MaterialExtraResourcesSection section = mcw.getExtraResourcesSection();
        ItemContainer existingContainer = section.getItemContainer();

        boolean alreadyInjected = false;
        if (existingContainer instanceof CombinedItemContainer combined) {
            alreadyInjected = combined.containsContainer(depotContainer);
        } else if (existingContainer == depotContainer) {
            alreadyInjected = true;
        }

        if (!alreadyInjected) {
            ItemContainer combinedContainer;
            if (existingContainer != null) {
                combinedContainer = new CombinedItemContainer(existingContainer, depotContainer);
            } else {
                combinedContainer = depotContainer;
            }
            section.setItemContainer(combinedContainer);
        }

        ItemQuantity[] combinedQuantities = buildItemQuantities(section.getItemContainer());
        section.setExtraMaterials(combinedQuantities);
        section.setValid(true);

        UpdateWindow packet = buildUpdatePacket(window, combinedQuantities);
        packetHandler.write(packet);
    }

    private static void sendFieldCraftingExtraResources(Window window, PacketHandler packetHandler, ItemQuantity[] depotQuantities) {
        UpdateWindow packet = buildUpdatePacket(window, depotQuantities);
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
