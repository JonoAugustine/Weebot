package com.ampro.weebot.commands.miscellaneous;


import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Spam a message up to {@link Weebot#spamLimit} times.
 */
public class SpamCommand extends Command {

    public SpamCommand() {
        super("Spam", new ArrayList<>(Arrays.asList("spamthis", "spamattack")), "Spam the chat", "<spam> [number_of_spams] [message]", false, false, 0, false,
              false
        );
        this.setUserPermissions(new Permission[]{Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE});
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
     * Spam the chat.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        int limit;
        synchronized (bot) {
            limit = bot.getSpamLimit();
        }
        String[] args = this.cleanArgs(bot, event);
        int loop = 5;
        switch (args.length) {
            case 1:
                event.reply(this.getHelp() + " with ``" + this.getArgFormat() + "``");
                return;
            default:
                //Try to parse an int from the 2nd arg. If it fails, spam
                //the second arg the default number of times.
                try {
                    loop = Integer.parseInt(args[1]);
                    if(loop > limit) {
                        event.reply("That's a bit much... The limit is set to " + limit + ".");
                        return;
                    }
                    for (int i = 0; i < loop && i < limit; i++) {
                        event.reply(String.join(" ", args).substring(args[0].length()));
                    }
                } catch (NumberFormatException e) {
                    System.err.println(e.getLocalizedMessage());
                    for (int i = 0; i < loop && i < limit; i++) {
                        event.reply(String.join(" ", args)
                                .substring(args[0].length()));
                    }
                    return; //We're done
                }
        }
    }

}
