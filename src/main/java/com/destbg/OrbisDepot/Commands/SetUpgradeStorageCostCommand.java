package com.destbg.OrbisDepot.Commands;

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

public class SetUpgradeStorageCostCommand extends AbstractAsyncCommand {
    private final RequiredArg<List<Integer>> values = withListRequiredArg("values", "", ArgTypes.INTEGER);

    public SetUpgradeStorageCostCommand() {
        super("set-upgrade-storage-cost", "Sets the Voidheart cost for each storage upgrade tier. Accepts " + (Constants.MAX_UPGRADE_RANK - 1) + " values. Default: 2,4,8,32.");
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        List<Integer> newValues = context.get(values);
        int expected = Constants.MAX_UPGRADE_RANK - 1;
        if (newValues.size() != expected) {
            context.sendMessage(Message.raw("Expected " + expected + " values, got " + newValues.size() + "."));
            return CompletableFuture.completedFuture(null);
        }
        for (int i = 0; i < newValues.size(); i++) {
            Constants.UPGRADE_COST_STORAGE[i] = newValues.get(i);
        }
        TranslationUtils.refreshUpgradeDescriptions();
        Main.saveOperatorConfig();
        context.sendMessage(Message.raw("Storage upgrade costs updated to: " + newValues));
        return CompletableFuture.completedFuture(null);
    }
}
