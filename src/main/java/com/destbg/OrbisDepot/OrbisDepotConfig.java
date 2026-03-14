package com.destbg.OrbisDepot;

import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class OrbisDepotConfig {

    public static final BuilderCodec<OrbisDepotConfig> CODEC = BuilderCodec.builder(OrbisDepotConfig.class, OrbisDepotConfig::new)
            .append(new KeyedCodec<>("StorageMultiplier1", Codec.INTEGER), (c, v) -> c.storageMultiplier1 = v, c -> c.storageMultiplier1).add()
            .append(new KeyedCodec<>("StorageMultiplier2", Codec.INTEGER), (c, v) -> c.storageMultiplier2 = v, c -> c.storageMultiplier2).add()
            .append(new KeyedCodec<>("StorageMultiplier3", Codec.INTEGER), (c, v) -> c.storageMultiplier3 = v, c -> c.storageMultiplier3).add()
            .append(new KeyedCodec<>("StorageMultiplier4", Codec.INTEGER), (c, v) -> c.storageMultiplier4 = v, c -> c.storageMultiplier4).add()
            .append(new KeyedCodec<>("SpeedDivisor1", Codec.INTEGER), (c, v) -> c.speedDivisor1 = v, c -> c.speedDivisor1).add()
            .append(new KeyedCodec<>("SpeedDivisor2", Codec.INTEGER), (c, v) -> c.speedDivisor2 = v, c -> c.speedDivisor2).add()
            .append(new KeyedCodec<>("SpeedDivisor3", Codec.INTEGER), (c, v) -> c.speedDivisor3 = v, c -> c.speedDivisor3).add()
            .append(new KeyedCodec<>("SpeedDivisor4", Codec.INTEGER), (c, v) -> c.speedDivisor4 = v, c -> c.speedDivisor4).add()
            .append(new KeyedCodec<>("UpgradeCostStorage1", Codec.INTEGER), (c, v) -> c.upgradeCostStorage1 = v, c -> c.upgradeCostStorage1).add()
            .append(new KeyedCodec<>("UpgradeCostStorage2", Codec.INTEGER), (c, v) -> c.upgradeCostStorage2 = v, c -> c.upgradeCostStorage2).add()
            .append(new KeyedCodec<>("UpgradeCostStorage3", Codec.INTEGER), (c, v) -> c.upgradeCostStorage3 = v, c -> c.upgradeCostStorage3).add()
            .append(new KeyedCodec<>("UpgradeCostStorage4", Codec.INTEGER), (c, v) -> c.upgradeCostStorage4 = v, c -> c.upgradeCostStorage4).add()
            .append(new KeyedCodec<>("UpgradeCostSpeed1", Codec.INTEGER), (c, v) -> c.upgradeCostSpeed1 = v, c -> c.upgradeCostSpeed1).add()
            .append(new KeyedCodec<>("UpgradeCostSpeed2", Codec.INTEGER), (c, v) -> c.upgradeCostSpeed2 = v, c -> c.upgradeCostSpeed2).add()
            .append(new KeyedCodec<>("UpgradeCostSpeed3", Codec.INTEGER), (c, v) -> c.upgradeCostSpeed3 = v, c -> c.upgradeCostSpeed3).add()
            .append(new KeyedCodec<>("UpgradeCostSpeed4", Codec.INTEGER), (c, v) -> c.upgradeCostSpeed4 = v, c -> c.upgradeCostSpeed4).add()
            .append(new KeyedCodec<>("UploadClockTickMs", Codec.INTEGER), (c, v) -> c.uploadClockTickMs = v, c -> c.uploadClockTickMs).add()
            .append(new KeyedCodec<>("BaseUploadIntervalMs", Codec.INTEGER), (c, v) -> c.baseUploadIntervalMs = v, c -> c.baseUploadIntervalMs).add()
            .append(new KeyedCodec<>("CrudeBaseUploadIntervalMs", Codec.INTEGER), (c, v) -> c.crudeBaseUploadIntervalMs = v, c -> c.crudeBaseUploadIntervalMs).add()
            .build();

    private int storageMultiplier1 = 2;
    private int storageMultiplier2 = 5;
    private int storageMultiplier3 = 10;
    private int storageMultiplier4 = 20;
    private int speedDivisor1 = 2;
    private int speedDivisor2 = 3;
    private int speedDivisor3 = 4;
    private int speedDivisor4 = 8;
    private int upgradeCostStorage1 = 2;
    private int upgradeCostStorage2 = 4;
    private int upgradeCostStorage3 = 8;
    private int upgradeCostStorage4 = 32;
    private int upgradeCostSpeed1 = 4;
    private int upgradeCostSpeed2 = 8;
    private int upgradeCostSpeed3 = 16;
    private int upgradeCostSpeed4 = 64;
    private int uploadClockTickMs = 250;
    private int baseUploadIntervalMs = 2000;
    private int crudeBaseUploadIntervalMs = 4000;

    public OrbisDepotConfig() {}

    public void applyToConstants() {
        Constants.STORAGE_RANK_MULTIPLIERS[1] = storageMultiplier1;
        Constants.STORAGE_RANK_MULTIPLIERS[2] = storageMultiplier2;
        Constants.STORAGE_RANK_MULTIPLIERS[3] = storageMultiplier3;
        Constants.STORAGE_RANK_MULTIPLIERS[4] = storageMultiplier4;
        Constants.SPEED_RANK_DIVISORS[1] = speedDivisor1;
        Constants.SPEED_RANK_DIVISORS[2] = speedDivisor2;
        Constants.SPEED_RANK_DIVISORS[3] = speedDivisor3;
        Constants.SPEED_RANK_DIVISORS[4] = speedDivisor4;
        Constants.UPGRADE_COST_STORAGE[0] = upgradeCostStorage1;
        Constants.UPGRADE_COST_STORAGE[1] = upgradeCostStorage2;
        Constants.UPGRADE_COST_STORAGE[2] = upgradeCostStorage3;
        Constants.UPGRADE_COST_STORAGE[3] = upgradeCostStorage4;
        Constants.UPGRADE_COST_SPEED[0] = upgradeCostSpeed1;
        Constants.UPGRADE_COST_SPEED[1] = upgradeCostSpeed2;
        Constants.UPGRADE_COST_SPEED[2] = upgradeCostSpeed3;
        Constants.UPGRADE_COST_SPEED[3] = upgradeCostSpeed4;
        Constants.UPLOAD_CLOCK_TICK_MS = uploadClockTickMs;
        Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS = baseUploadIntervalMs / 1000.0f;
        Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS = crudeBaseUploadIntervalMs / 1000.0f;
        Constants.recomputeCrudeSigilTicks();
    }

    public void readFromConstants() {
        storageMultiplier1 = Constants.STORAGE_RANK_MULTIPLIERS[1];
        storageMultiplier2 = Constants.STORAGE_RANK_MULTIPLIERS[2];
        storageMultiplier3 = Constants.STORAGE_RANK_MULTIPLIERS[3];
        storageMultiplier4 = Constants.STORAGE_RANK_MULTIPLIERS[4];
        speedDivisor1 = Constants.SPEED_RANK_DIVISORS[1];
        speedDivisor2 = Constants.SPEED_RANK_DIVISORS[2];
        speedDivisor3 = Constants.SPEED_RANK_DIVISORS[3];
        speedDivisor4 = Constants.SPEED_RANK_DIVISORS[4];
        upgradeCostStorage1 = Constants.UPGRADE_COST_STORAGE[0];
        upgradeCostStorage2 = Constants.UPGRADE_COST_STORAGE[1];
        upgradeCostStorage3 = Constants.UPGRADE_COST_STORAGE[2];
        upgradeCostStorage4 = Constants.UPGRADE_COST_STORAGE[3];
        upgradeCostSpeed1 = Constants.UPGRADE_COST_SPEED[0];
        upgradeCostSpeed2 = Constants.UPGRADE_COST_SPEED[1];
        upgradeCostSpeed3 = Constants.UPGRADE_COST_SPEED[2];
        upgradeCostSpeed4 = Constants.UPGRADE_COST_SPEED[3];
        uploadClockTickMs = (int) Constants.UPLOAD_CLOCK_TICK_MS;
        baseUploadIntervalMs = Math.round(Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS * 1000);
        crudeBaseUploadIntervalMs = Math.round(Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS * 1000);
    }
}
