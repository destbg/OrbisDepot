package com.destbg.OrbisDepot.Utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.logging.Level;

public final class BlockStateUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setBlockInteractionState(String state, World world, int x, int y, int z) {
        try {
            BlockType blockType = world.getBlockType(x, y, z);
            if (blockType != null) {
                world.setBlockInteractionState(new Vector3i(x, y, z), blockType, state);
            }
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t).log("Failed to set block interaction state: " + state);
        }
    }
}
