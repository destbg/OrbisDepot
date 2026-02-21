package com.destbg.OrbisDepot;

import com.destbg.OrbisDepot.Crafting.OrbisFieldCraftingWindow;
import com.destbg.OrbisDepot.Crafting.PlaceBlockDepotSystem;
import com.destbg.OrbisDepot.Crafting.UseBlockCraftingSystem;
import com.destbg.OrbisDepot.Interactions.OpenOrbisDepotInteraction;
import com.destbg.OrbisDepot.Interactions.OpenOrbisSigilInteraction;
import com.destbg.OrbisDepot.State.OrbisDepotBlockState;
import com.destbg.OrbisDepot.Storage.PlayerSettingsManager;
import com.destbg.OrbisDepot.Storage.VoidStorageManager;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.destbg.OrbisDepot.Utils.DepotSlotUtils;
import com.destbg.OrbisDepot.Utils.SigilSlotUtils;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static volatile boolean initialized = false;
    private static ComponentType<ChunkStore, OrbisDepotBlockState> orbisDepotComponentType;
    private ScheduledExecutorService depositScheduler;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCodecRegistry(Interaction.CODEC).register("Orbis_Sigil_Open", OpenOrbisSigilInteraction.class, OpenOrbisSigilInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Orbis_Depot_Open", OpenOrbisDepotInteraction.class, OpenOrbisDepotInteraction.CODEC);

        this.getEntityStoreRegistry().registerSystem(new UseBlockCraftingSystem());
        this.getEntityStoreRegistry().registerSystem(new PlaceBlockDepotSystem());
    }

    @Override
    protected void start() {
        try {
            LOGGER.at(Level.INFO).log("Registering block state...");
            this.getBlockStateRegistry().registerBlockState(
                    OrbisDepotBlockState.class,
                    Constants.ORBIS_DEPOT_STATE_ID,
                    OrbisDepotBlockState.CODEC
            );
            LOGGER.at(Level.INFO).log("Block state registered, resolving component type...");
            orbisDepotComponentType = getBlockStateComponentType();
            LOGGER.at(Level.INFO).log("Component type resolved: %s", orbisDepotComponentType);

            VoidStorageManager.init(getDataDirectory());
            LOGGER.at(Level.INFO).log("VoidStorageManager initialized.");
            PlayerSettingsManager.init(getDataDirectory());
            LOGGER.at(Level.INFO).log("PlayerSettingsManager initialized.");
            SigilSlotUtils.init(getDataDirectory());
            LOGGER.at(Level.INFO).log("SigilSlotUtils initialized.");
            DepotSlotUtils.init(getDataDirectory());
            LOGGER.at(Level.INFO).log("DepotSlotUtils initialized.");

            PermissionsModule.get().addGroupPermission("Adventure", Set.of(
                    Constants.PERM_DEPOT_USE,
                    Constants.PERM_SIGIL_USE
            ));

            depositScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "OrbisDepot-DepositScheduler");
                t.setDaemon(true);
                return t;
            });
            depositScheduler.scheduleAtFixedRate(() -> {
                try {
                    SigilSlotUtils.tickAll(0.1f);
                    DepotSlotUtils.tickAll(0.1f);
                } catch (Exception e) {
                    LOGGER.at(Level.SEVERE).withCause(e).log("Error in deposit processor");
                }
            }, 100, 100, TimeUnit.MILLISECONDS);

            Window.CLIENT_REQUESTABLE_WINDOW_TYPES.put(
                    WindowType.PocketCrafting,
                    OrbisFieldCraftingWindow::new
            );

            initialized = true;
            LOGGER.at(Level.INFO).log("Plugin started successfully.");
        } catch (Throwable t) {
            LOGGER.at(Level.SEVERE).withCause(t).log("FATAL: Plugin failed to start!");
        }
    }

    @Override
    protected void shutdown() {
        if (depositScheduler != null) {
            depositScheduler.shutdown();
            try {
                var _ = depositScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }

        CraftingUtils.cleanup();

        try {
            VoidStorageManager.get().saveAll();
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Error saving void storage on shutdown");
        }

        try {
            PlayerSettingsManager.get().saveAll();
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Error saving player settings on shutdown");
        }

        try {
            SigilSlotUtils.saveAll();
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Error saving sigil slots on shutdown");
        }

        try {
            DepotSlotUtils.saveAll();
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Error saving depot slots on shutdown");
        }

        LOGGER.at(Level.INFO).log("Plugin shut down.");
    }

    @SuppressWarnings("unchecked")
    private static ComponentType<ChunkStore, OrbisDepotBlockState> getBlockStateComponentType() {
        try {
            Class<?> moduleClass = Class.forName("com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule");
            Method get = moduleClass.getMethod("get");
            Object module = get.invoke(null);
            Method getComponentType = moduleClass.getMethod("getComponentType", Class.class);
            return (ComponentType<ChunkStore, OrbisDepotBlockState>) getComponentType.invoke(module, OrbisDepotBlockState.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve block state component type for " + OrbisDepotBlockState.class.getName(), e);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static ComponentType<ChunkStore, OrbisDepotBlockState> getOrbisDepotComponentType() {
        return orbisDepotComponentType;
    }
}
