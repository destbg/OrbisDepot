package com.destbg.OrbisDepot.Components;

import com.destbg.OrbisDepot.Models.AttunedEntry;
import com.destbg.OrbisDepot.Utils.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DepotStorageData {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashMap<String, Integer> itemContainer;
    private final HashMap<String, String> attunedToOthers;
    private final HashMap<String, String> attunedToMe;
    private float tickInterval;
    private int storageUpgradeRank;
    private int speedUpgradeRank;
    private transient float elapsedTime;
    private Runnable saveCallback;

    public DepotStorageData() {
        itemContainer = new HashMap<>();
        attunedToOthers = new HashMap<>();
        attunedToMe = new HashMap<>();
        storageUpgradeRank = 1;
        speedUpgradeRank = 1;
        tickInterval = Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS;
    }

    public DepotStorageData(@Nonnull Map<String, Integer> itemContainer, @Nonnull Map<String, String> attunedToOthers, @Nonnull Map<String, String> attunedToMe, int storageUpgradeRank, int speedUpgradeRank) {
        this.itemContainer = new HashMap<>(itemContainer);
        this.attunedToOthers = new HashMap<>(attunedToOthers);
        this.attunedToMe = new HashMap<>(attunedToMe);
        this.storageUpgradeRank = storageUpgradeRank;
        this.speedUpgradeRank = speedUpgradeRank;
        this.tickInterval = computeTickInterval(speedUpgradeRank);
    }

    public void setSaveCallback(@Nullable Runnable callback) {
        this.saveCallback = callback;
    }

    private void save() {
        Runnable cb = saveCallback;
        if (cb != null) cb.run();
    }

    public float getTickInterval() {
        return tickInterval;
    }

    public int getStorageUpgradeRank() {
        return storageUpgradeRank;
    }

    public int getSpeedUpgradeRank() {
        return speedUpgradeRank;
    }

    public int getMaxStorageItems() {
        int idx = Math.min(storageUpgradeRank - 1, Constants.STORAGE_RANK_MULTIPLIERS.length - 1);
        return Constants.BASE_STORAGE_CAPACITY * Constants.STORAGE_RANK_MULTIPLIERS[idx];
    }

    public boolean isStorageFull() {
        return getTotalItemCount() >= getMaxStorageItems();
    }

    public void upgradeStorageCapacity() {
        if (storageUpgradeRank >= Constants.MAX_UPGRADE_RANK) {
            return;
        }
        storageUpgradeRank++;
        save();
    }

    public void downgradeStorageCapacity() {
        if (storageUpgradeRank <= 1) {
            return;
        }
        storageUpgradeRank--;
        save();
    }

    public void upgradeDepositSpeed() {
        if (speedUpgradeRank >= Constants.MAX_UPGRADE_RANK) {
            return;
        }
        speedUpgradeRank++;
        tickInterval = computeTickInterval(speedUpgradeRank);
        save();
    }

    public void downgradeDepositSpeed() {
        if (speedUpgradeRank <= 1) {
            return;
        }
        speedUpgradeRank--;
        tickInterval = computeTickInterval(speedUpgradeRank);
        save();
    }

    private static float computeTickInterval(int speedRank) {
        int idx = Math.min(speedRank - 1, Constants.SPEED_RANK_DIVISORS.length - 1);
        return Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS / Constants.SPEED_RANK_DIVISORS[idx];
    }

    public int getTotalItemCount() {
        lock.readLock().lock();
        try {
            return itemContainer.values().stream().mapToInt(Integer::intValue).sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    public float getElapsedTime() {
        return elapsedTime;
    }

    public void addElapsedTime(float dt) {
        this.elapsedTime += dt;
    }

    public void resetElapsedTime() {
        this.elapsedTime = 0f;
    }

    public Map<String, Integer> getItemContainer() {
        return itemContainer;
    }

    public Map<String, String> getAttunedToOthersRaw() {
        lock.readLock().lock();
        try {
            return new HashMap<>(attunedToOthers);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, String> getAttunedToMeRaw() {
        lock.readLock().lock();
        try {
            return new HashMap<>(attunedToMe);
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getItemCount(@Nonnull String itemId) {
        lock.readLock().lock();
        try {
            return itemContainer.getOrDefault(itemId, 0);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addItem(@Nonnull String itemId, int count) {
        int actualAdded;
        lock.writeLock().lock();
        try {
            int currentTotal = itemContainer.values().stream().mapToInt(Integer::intValue).sum();
            int space = Math.max(0, getMaxStorageItems() - currentTotal);
            actualAdded = Math.min(count, space);
            if (actualAdded > 0) {
                itemContainer.put(itemId, itemContainer.getOrDefault(itemId, 0) + actualAdded);
            }
        } finally {
            lock.writeLock().unlock();
        }
        if (actualAdded > 0) {
            save();
        }
    }

    public void removeItems(@Nonnull String itemId, int count) {
        lock.writeLock().lock();
        try {
            int current = itemContainer.getOrDefault(itemId, 0);
            int newCount = Math.max(0, current - count);
            if (newCount == 0) {
                itemContainer.remove(itemId);
            } else {
                itemContainer.put(itemId, newCount);
            }
        } finally {
            lock.writeLock().unlock();
        }
        save();
    }

    public Map<String, Integer> searchItems(@Nonnull String query) {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            return getItemContainer();
        }

        lock.readLock().lock();
        try {
            Map<String, Integer> results = new HashMap<>();
            for (Map.Entry<String, Integer> entry : itemContainer.entrySet()) {
                if (entry.getKey().toLowerCase().contains(q)) {
                    results.put(entry.getKey(), entry.getValue());
                }
            }
            return results;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void attune(@Nonnull UUID playerUUID, @Nonnull String playerName) {
        lock.writeLock().lock();
        try {
            attunedToOthers.put(playerUUID.toString(), playerName);
        } finally {
            lock.writeLock().unlock();
        }
        save();
    }

    public void attuneToMe(@Nonnull UUID playerUUID, @Nonnull String playerName) {
        lock.writeLock().lock();
        try {
            attunedToMe.put(playerUUID.toString(), playerName);
        } finally {
            lock.writeLock().unlock();
        }
        save();
    }

    public void removeAttunement(@Nonnull UUID playerUUID) {
        lock.writeLock().lock();
        try {
            attunedToOthers.remove(playerUUID.toString());
        } finally {
            lock.writeLock().unlock();
        }
        save();
    }

    public void removeAttunementToMe(@Nonnull UUID playerUUID) {
        lock.writeLock().lock();
        try {
            attunedToMe.remove(playerUUID.toString());
        } finally {
            lock.writeLock().unlock();
        }
        save();
    }

    public List<AttunedEntry> getAttunedToOthers() {
        lock.readLock().lock();
        try {
            return attunedToOthers.entrySet().stream()
                    .map(e -> new AttunedEntry(UUID.fromString(e.getKey()), e.getValue()))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<AttunedEntry> getAttunedToMe() {
        lock.readLock().lock();
        try {
            return attunedToMe.entrySet().stream()
                    .map(e -> new AttunedEntry(UUID.fromString(e.getKey()), e.getValue()))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isAttunedTo(UUID target) {
        lock.readLock().lock();
        try {
            return attunedToMe.containsKey(target.toString());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Nullable
    public String getPlayerName(UUID targetUUID) {
        lock.readLock().lock();
        try {
            return attunedToOthers.get(targetUUID.toString());
        } finally {
            lock.readLock().unlock();
        }
    }
}
