package com.destbg.OrbisDepot.Utils;

import com.destbg.OrbisDepot.Components.CrudeSigilPlayerData;
import com.destbg.OrbisDepot.Components.DepotChunkData;
import com.destbg.OrbisDepot.Components.SigilPlayerData;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ComponentUtils {
    private static ComponentType<ChunkStore, DepotChunkData> depotPlayerDataComponent;
    private static ComponentType<EntityStore, CrudeSigilPlayerData> crudeSigilPlayerDataComponent;
    private static ComponentType<EntityStore, SigilPlayerData> sigilPlayerDataComponent;

    public static ComponentType<ChunkStore, DepotChunkData> getDepotPlayerDataComponent() {
        return depotPlayerDataComponent;
    }

    public static ComponentType<EntityStore, CrudeSigilPlayerData> getCrudeSigilPlayerDataComponent() {
        return crudeSigilPlayerDataComponent;
    }

    public static ComponentType<EntityStore, SigilPlayerData> getSigilPlayerDataComponent() {
        return sigilPlayerDataComponent;
    }

    public static void setup(ComponentRegistryProxy<EntityStore> entityRegistry) {
        crudeSigilPlayerDataComponent = entityRegistry.registerComponent(
                CrudeSigilPlayerData.class,
                "CrudeSigilPlayerDataComponent",
                CrudeSigilPlayerData.CODEC
        );
        sigilPlayerDataComponent = entityRegistry.registerComponent(
                SigilPlayerData.class,
                "SigilPlayerDataComponent",
                SigilPlayerData.CODEC
        );
    }

    @SuppressWarnings("removal")
    public static void setupDepotChunkDataComponent() {
        depotPlayerDataComponent = com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule.get()
                .getComponentType(DepotChunkData.class);
    }
}
