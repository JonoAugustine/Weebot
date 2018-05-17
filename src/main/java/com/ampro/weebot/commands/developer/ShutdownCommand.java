package com.ampro.weebot.commands.developer;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Safely shuts down all the bots, initiating proper database saving in
 * {@link com.ampro.main.Launcher}.
 *
 * @author Jonathan Augustine
 */
public class ShutdownCommand extends Command {

    public ShutdownCommand() {
        super(
                "ShutDown"
                , new ArrayList<>(Arrays.asList(
                        "killbots", "devkill", "tite"
                ))
                , "Safely shutdown the Weebots."
                , ""
                , false
                , true
                , 0
                , true
        );
    }

    /**
     * Begins global shutdown process.
     * @param bot The bot that called this event.
     * @param event {@link BetterMessageEvent}
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event))
            this.execute(bot, event);
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        event.reply("Shutting down all Weebots...");
        Launcher.shutdown();
    }

}
