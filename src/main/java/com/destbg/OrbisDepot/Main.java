package com.destbg.OrbisDepot;

import com.destbg.OrbisDepot.Commands.OrbisDepotCommand;
import com.destbg.OrbisDepot.Crafting.OrbisFieldCraftingWindow;
import com.destbg.OrbisDepot.Crafting.PlaceBlockAutoRestoreSystem;
import com.destbg.OrbisDepot.Crafting.UseBlockCraftingSystem;
import com.destbg.OrbisDepot.Interactions.CrudeOrbisSigilOpenInteraction;
import com.destbg.OrbisDepot.Interactions.OrbisDepotAttunementConsumeInteraction;
import com.destbg.OrbisDepot.Interactions.OrbisDepotOpenInteraction;
import com.destbg.OrbisDepot.Interactions.OrbisSigilOpenInteraction;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Systems.*;
import com.destbg.OrbisDepot.Utils.ComponentUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.TranslationUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class Main extends JavaPlugin {
    private static Config<OrbisDepotConfig> operatorConfig;

    public Main(@NonNullDecl JavaPluginInit init) {
        super(init);
        operatorConfig = this.withConfig("OrbisDepotConfig", OrbisDepotConfig.CODEC);
    }

    public static void saveOperatorConfig() {
        operatorConfig.get().readFromConstants();
        operatorConfig.save();
    }

    @Override
    protected void setup() {
        operatorConfig.get().applyToConstants();

        ComponentUtils.setup(this.getEntityStoreRegistry(), this.getChunkStoreRegistry());

        this.getCodecRegistry(Interaction.CODEC).register(
                "Crude_Orbis_Sigil_Open",
                CrudeOrbisSigilOpenInteraction.class,
                CrudeOrbisSigilOpenInteraction.CODEC
        );
        this.getCodecRegistry(Interaction.CODEC).register(
                "Orbis_Sigil_Open",
                OrbisSigilOpenInteraction.class,
                OrbisSigilOpenInteraction.CODEC
        );
        this.getCodecRegistry(Interaction.CODEC).register(
                "Orbis_Depot_Open",
                OrbisDepotOpenInteraction.class,
                OrbisDepotOpenInteraction.CODEC
        );
        this.getCodecRegistry(Interaction.CODEC).register(
                "Orbis_Depot_Attune",
                OrbisDepotAttunementConsumeInteraction.class,
                OrbisDepotAttunementConsumeInteraction.CODEC
        );
    }

    @Override
    protected void start() {
        DepotStorageManager.init(getDataDirectory());
        this.getCommandRegistry().registerCommand(new OrbisDepotCommand());
        TranslationUtils.refreshUpgradeDescriptions();

        PermissionsModule.get().addGroupPermission("Adventure", Set.of(
                Constants.PERM_DEPOT_USE,
                Constants.PERM_SIGIL_USE
        ));

        this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            UUID uuid = playerRef.getUuid();
            String name = playerRef.getUsername();
            Holder<EntityStore> holder = event.getHolder();

            Consumer<ItemContainer.ItemContainerChangeEvent> handler = changeEvent -> {
                changeEvent.container().forEach((slot, stack) -> {
                    if (stack == null || ItemStack.isEmpty(stack)) {
                        return;
                    } else if (!Constants.ATTUNEMENT_ITEM_ID.equals(stack.getItemId())) {
                        return;
                    }

                    String existingUUID = stack.getFromMetadataOrNull(Constants.META_CRAFTER_UUID, Codec.STRING);
                    if (existingUUID != null) {
                        return;
                    }

                    ItemStack bound = stack
                            .withMetadata(Constants.META_CRAFTER_UUID, Codec.STRING, uuid.toString())
                            .withMetadata(Constants.META_CRAFTER_NAME, Codec.STRING, name);
                    changeEvent.container().setItemStackForSlot(slot, bound);

                    Ref<EntityStore> ref = playerRef.getReference();
                    if (ref == null || !ref.isValid()) {
                        return;
                    }

                    Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                    if (player == null) {
                        return;
                    }

                    player.sendMessage(Message.raw(TranslationUtils.get("messages.attunement.created")).color("#7bed9f"));
                });
            };

            InventoryComponent.Hotbar hotbar = holder.getComponent(InventoryComponent.Hotbar.getComponentType());
            if (hotbar != null) {
                hotbar.getInventory().registerChangeEvent(handler);
            }

            InventoryComponent.Storage storage = holder.getComponent(InventoryComponent.Storage.getComponentType());
            if (storage != null) {
                storage.getInventory().registerChangeEvent(handler);
            }
        });

        this.getChunkStoreRegistry().registerSystem(new DepotRefSystem());
        this.getChunkStoreRegistry().registerSystem(new DepotTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new SigilTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new CrudeSigilTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new ProgressTickSystem());
        this.getEntityStoreRegistry().registerSystem(new UseBlockCraftingSystem());
        this.getEntityStoreRegistry().registerSystem(new PlaceBlockAutoRestoreSystem());

        Window.CLIENT_REQUESTABLE_WINDOW_TYPES.put(WindowType.PocketCrafting, OrbisFieldCraftingWindow::new);
    }

    @Override
    protected void shutdown() {
        DepotStorageManager.get().saveAll();
    }
}
