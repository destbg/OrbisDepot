package com.destbg.OrbisDepot.Commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class OrbisDepotCommand extends AbstractCommandCollection {

    public OrbisDepotCommand() {
        super("orbisdepot", "Operator configuration for the Orbis Depot mod.");
        addSubCommand(new SetUpgradeStacksCommand());
        addSubCommand(new SetUpgradeStorageCostCommand());
        addSubCommand(new SetUpgradeSpeedCommand());
        addSubCommand(new SetUpgradeSpeedCostCommand());
        addSubCommand(new SetTickSpeedCommand());
        addSubCommand(new SetBaseSpeedCommand());
        addSubCommand(new SetCrudeBaseSpeedCommand());
    }
}
