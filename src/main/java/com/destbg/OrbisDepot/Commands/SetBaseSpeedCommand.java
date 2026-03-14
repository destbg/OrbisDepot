package com.destbg.OrbisDepot.Commands;

import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Utils.TranslationUtils;
import com.destbg.OrbisDepot.Main;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class SetBaseSpeedCommand extends AbstractAsyncCommand {
    private final RequiredArg<Integer> ms = withRequiredArg("ms", "", ArgTypes.INTEGER);

    public SetBaseSpeedCommand() {
        super("set-base-speed", "Sets the base upload interval (in ms) for the Orbis Depot and Orbis Sigil. Default: 2000ms. Cannot be lower than the tick speed.");
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        int value = context.get(ms);
        if (value < Constants.UPLOAD_CLOCK_TICK_MS) {
            context.sendMessage(Message.raw(TranslationUtils.format("commands.setBasespeed.tooLow", Constants.UPLOAD_CLOCK_TICK_MS)));
            return CompletableFuture.completedFuture(null);
        }
        Constants.UPLOAD_INTERVAL_SIGIL_AND_DEPOT_SECONDS = value / 1000.0f;
        DepotStorageManager.get().recomputeAllTickIntervals();
        TranslationUtils.refreshUpgradeDescriptions();
        Main.saveOperatorConfig();
        context.sendMessage(Message.raw(TranslationUtils.format("commands.setBasespeed.success", value)));
        return CompletableFuture.completedFuture(null);
    }
}
