package com.destbg.OrbisDepot.Utils;

import com.destbg.OrbisDepot.Components.DepotStorageData;
import com.destbg.OrbisDepot.Storage.DepotStorageManager;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public final class DepotOwnerUtils {

    @Nullable
    public static DepotStorageData getIfReady(UUID ownerUuid) {
        return DepotStorageManager.get().get(ownerUuid);
    }

    public static void getAsync(UUID ownerUuid, Consumer<DepotStorageData> callback) {
        callback.accept(DepotStorageManager.get().getOrCreate(ownerUuid));
    }

    public static DepotStorageData getOrCreate(UUID ownerUuid) {
        return DepotStorageManager.get().getOrCreate(ownerUuid);
    }
}
