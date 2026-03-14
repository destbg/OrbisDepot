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
import java.util.concurrent.CompletableFuture;

public class SetTickSpeedCommand extends AbstractAsyncCommand {
    private static final long MIN_TICK_MS = 1000L / 30L; // 30 ticks per second
    private final RequiredArg<Integer> ms = withRequiredArg("ms", "", ArgTypes.INTEGER);

    public SetTickSpeedCommand() {
        super("set-tick-speed", "Sets how often (in ms) the mod checks for uploads and updates the progress bar. Default: 250ms. Minimum: " + MIN_TICK_MS + "ms. Setting this too low prevents players from interacting with the UI during a deposit.");
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        int value = context.get(ms);
        if (value < MIN_TICK_MS) {
            context.sendMessage(Message.raw(TranslationUtils.format("commands.setTickspeed.tooLow", MIN_TICK_MS)));
            return CompletableFuture.completedFuture(null);
        }
        Constants.UPLOAD_CLOCK_TICK_MS = value;
        Constants.recomputeCrudeSigilTicks();
        Main.saveOperatorConfig();
        context.sendMessage(Message.raw(TranslationUtils.format("commands.setTickspeed.success", value)));
        return CompletableFuture.completedFuture(null);
    }
}
