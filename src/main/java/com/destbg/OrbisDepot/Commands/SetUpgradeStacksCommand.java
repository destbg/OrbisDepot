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

public class SetUpgradeStacksCommand extends AbstractAsyncCommand {
    private final RequiredArg<List<Integer>> values = withListRequiredArg("values", "", ArgTypes.INTEGER);

    public SetUpgradeStacksCommand() {
        super("set-upgrade-stacks", "Sets the storage multipliers for each upgrade rank. Accepts " + (Constants.MAX_UPGRADE_RANK - 1) + " values. Default: [2,5,10,20]. Each multiplier applies to the item's base max stack size.");
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
            Constants.STORAGE_RANK_MULTIPLIERS[i + 1] = newValues.get(i);
        }
        TranslationUtils.refreshUpgradeDescriptions();
        Main.saveOperatorConfig();
        context.sendMessage(Message.raw("Storage upgrade stacks updated to: " + newValues));
        return CompletableFuture.completedFuture(null);
    }
}
