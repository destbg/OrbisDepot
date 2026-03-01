package com.destbg.OrbisDepot;

import com.destbg.OrbisDepot.Crafting.OrbisFieldCraftingWindow;
import com.destbg.OrbisDepot.Crafting.PlaceBlockAutoRestoreSystem;
import com.destbg.OrbisDepot.Crafting.UseBlockCraftingSystem;
import com.destbg.OrbisDepot.Interactions.CrudeOrbisSigilOpenInteraction;
import com.destbg.OrbisDepot.Interactions.OrbisDepotAttunementConsumeInteraction;
import com.destbg.OrbisDepot.Interactions.OrbisDepotOpenInteraction;
import com.destbg.OrbisDepot.Interactions.OrbisSigilOpenInteraction;
import com.destbg.OrbisDepot.Components.DepotChunkData;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Storage.LegacySlotMigration;
import com.destbg.OrbisDepot.Systems.CrudeSigilTickingSystem;
import com.destbg.OrbisDepot.Systems.DepotRefSystem;
import com.destbg.OrbisDepot.Systems.DepotTickingSystem;
import com.destbg.OrbisDepot.Systems.SigilTickingSystem;
import com.destbg.OrbisDepot.Utils.ComponentUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {
    public Main(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        ComponentUtils.setup(this.getEntityStoreRegistry());

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
        this.getBlockStateRegistry().registerBlockState(
                DepotChunkData.class,
                Constants.ORBIS_DEPOT_STATE_ID,
                DepotChunkData.CODEC
        );
        ComponentUtils.setupDepotChunkDataComponent();
        DepotStorageManager.init(getDataDirectory());
        LegacySlotMigration.init(getDataDirectory());

        PermissionsModule.get().addGroupPermission("Adventure", Set.of(
                Constants.PERM_DEPOT_USE,
                Constants.PERM_SIGIL_USE
        ));

        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            Ref<EntityStore> ref = player.getReference();
            if (ref == null) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            UUID uuid = playerRef.getUuid();
            String name = playerRef.getUsername();

            event.getItemContainer().forEach((slot, stack) -> {
                if (stack == null || ItemStack.isEmpty(stack)) {
                    return;
                }
                if (!Constants.ATTUNEMENT_ITEM_ID.equals(stack.getItemId())) {
                    return;
                }

                String existingUUID = stack.getFromMetadataOrNull(Constants.META_CRAFTER_UUID, Codec.STRING);
                if (existingUUID != null) {
                    return;
                }

                ItemStack bound = stack
                        .withMetadata(Constants.META_CRAFTER_UUID, Codec.STRING, uuid.toString())
                        .withMetadata(Constants.META_CRAFTER_NAME, Codec.STRING, name);
                event.getItemContainer().setItemStackForSlot(slot, bound);
                player.sendMessage(Message.raw("Attunement bound to you. Give it to another player so they can access your Depot.").color("#7bed9f"));
            });
        });

        this.getChunkStoreRegistry().registerSystem(new DepotRefSystem());
        this.getChunkStoreRegistry().registerSystem(new DepotTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new SigilTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new CrudeSigilTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new UseBlockCraftingSystem());
        this.getEntityStoreRegistry().registerSystem(new PlaceBlockAutoRestoreSystem());

        Window.CLIENT_REQUESTABLE_WINDOW_TYPES.put(WindowType.PocketCrafting, OrbisFieldCraftingWindow::new);
    }

    @Override
    protected void shutdown() {
        DepotStorageManager.get().saveAll();
    }
}
