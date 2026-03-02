package com.destbg.OrbisDepot.Utils;

public class UploadClockUtils {

    public static long currentTick() {
        return System.currentTimeMillis() / Constants.UPLOAD_CLOCK_TICK_MS;
    }

    public static boolean shouldUpload(long lastUploadTick, long ticksPerInterval) {
        long now = currentTick();
        if (now <= lastUploadTick) {
            return false;
        }
        long lastBoundary = (now / ticksPerInterval) * ticksPerInterval;
        return lastBoundary > lastUploadTick;
    }
}
