package com.destbg.OrbisDepot.Utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.logging.Level;

public final class BlockStateUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setBlockInteractionState(String state, World world, Vector3i pos) {
        try {
            BlockType blockType = world.getBlockType(pos.getX(), pos.getY(), pos.getZ());
            if (blockType != null) {
                world.setBlockInteractionState(pos, blockType, state);
            }
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t).log("Failed to set block interaction state: " + state);
        }
    }
}
