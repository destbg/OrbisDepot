package com.destbg.OrbisDepot.Commands;

import com.destbg.OrbisDepot.Main;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class SetCrudeBaseSpeedCommand extends AbstractAsyncCommand {
    private final RequiredArg<Integer> ms = withRequiredArg("ms", "", ArgTypes.INTEGER);

    public SetCrudeBaseSpeedCommand() {
        super("set-crude-base-speed", "Sets the base upload interval (in ms) for the Crude Orbis Sigil. Default: 4000ms. Cannot be lower than the tick speed.");
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        int value = context.get(ms);
        if (value < Constants.UPLOAD_CLOCK_TICK_MS) {
            context.sendMessage(Message.raw("Crude base speed cannot be lower than the tick speed (" + Constants.UPLOAD_CLOCK_TICK_MS + "ms)."));
            return CompletableFuture.completedFuture(null);
        }
        Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS = value / 1000.0f;
        Constants.recomputeCrudeSigilTicks();
        Main.saveOperatorConfig();
        context.sendMessage(Message.raw("Crude Orbis Sigil base upload speed set to " + value + "ms."));
        return CompletableFuture.completedFuture(null);
    }
}
