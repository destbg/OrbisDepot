package com.destbg.OrbisDepot.Commands;

import com.destbg.OrbisDepot.Storage.DepotStorageManager;
import com.destbg.OrbisDepot.Main;
import com.destbg.OrbisDepot.Utils.Constants;
import com.destbg.OrbisDepot.Utils.TranslationUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SetUpgradeSpeedCommand extends AbstractAsyncCommand {
    private final RequiredArg<List<Integer>> values = withListRequiredArg("values", "", ArgTypes.INTEGER);

    public SetUpgradeSpeedCommand() {
        super("set-upgrade-speed", "Sets the upload speed divisors for each upgrade rank. Accepts " + (Constants.MAX_UPGRADE_RANK - 1) + " values. Default: [2,3,4,8]. Each value divides the base upload interval (e.g. 8 means 2s/8 = 250ms).");
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        List<Integer> newValues = context.get(values);
        int expected = Constants.MAX_UPGRADE_RANK - 1;
        if (newValues.size() != expected) {
            context.sendMessage(Message.raw(TranslationUtils.format("commands.wrongArgCount", expected, newValues.size())));
            return CompletableFuture.completedFuture(null);
        }
        for (int i = 0; i < newValues.size(); i++) {
            Constants.SPEED_RANK_DIVISORS[i + 1] = newValues.get(i);
        }
        DepotStorageManager.get().recomputeAllTickIntervals();
        TranslationUtils.refreshUpgradeDescriptions();
        Main.saveOperatorConfig();
        context.sendMessage(Message.raw(TranslationUtils.format("commands.setUpgradeSpeed.success", newValues)));
        return CompletableFuture.completedFuture(null);
    }
}
