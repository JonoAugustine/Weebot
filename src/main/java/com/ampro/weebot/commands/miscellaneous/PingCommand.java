package com.ampro.weebot.commands.miscellaneous;


import com.ampro.weebot.commands.Command;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;

import java.time.temporal.ChronoUnit;

/**
 * Check the response time of the bot.
 */
public class PingCommand extends Command {

    public PingCommand() {
        super("Ping"
                , new String[0]
                , "Check my response time"
                ,""
                , false
                , false
                , 0
                , false
                , false
        );
    }

    /**
     * Performs a check then runs the command.
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if(this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : SpamCommand");
            thread.start();
        }
    }

    /**
     * Reply with Pong and time it took to respond.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        if (cleanArgs(bot, event).length > 1) {
            return;
        }
        event.reply("Pong...", m -> {
            long ping = event.getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS);
            m.editMessage("Pong: " + ping  + "ms | Websocket: " + event.getJDA().getPing() +
                    "ms").queue();
        });
    }

}
