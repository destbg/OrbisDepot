package com.destbg.OrbisDepot.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;

public final class SoundUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void playSFX(String sound, double x, double y, double z, Store<EntityStore> store) {
        try {
            int index = SoundEvent.getAssetMap().getIndex(sound);
            SoundUtil.playSoundEvent3d(index, SoundCategory.SFX, x, y, z, store);
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t).log("Error playing sound: " + sound);
        }
    }

    public static void playSFXToPlayer(String sound, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        try {
            int index = SoundEvent.getAssetMap().getIndex(sound);
            TransformComponent transform = store.getComponent(playerRef, EntityModule.get().getTransformComponentType());
            if (transform == null) {
                return;
            }

            Vector3d pos = transform.getPosition();
            SoundUtil.playSoundEvent3dToPlayer(playerRef, index, SoundCategory.SFX, pos, store);
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t).log("Error playing sound to player: " + sound);
        }
    }
}
