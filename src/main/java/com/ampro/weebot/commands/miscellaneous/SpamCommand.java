package com.ampro.weebot.commands.miscellaneous;


import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed;

/**
 * Spam a message up to {@link Weebot#spamLimit} times.
 */
public class SpamCommand extends Command {

    public SpamCommand() {
        super("Spam",
              new String[]{"spamthis", "spamattack", "spamlimit"},
              "Spam the chat", "<spam> [number_of_spams] [message]",
              false, false, 0, false,
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
        int limit = bot.getSpamLimit();
        String[] args = cleanArgs(bot, event);
        int loop = 5;
        if (args[0].equalsIgnoreCase("spamlimit")) {
            setSpamLimit(bot, event);
            return;
        }
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

    /**
     * Set or get the limit on bot spam.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void setSpamLimit(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = cleanArgsLowerCase(bot, event);
            switch (args.length) {
                case 1:
                    event.reply(
                            "The spam limit is currently set to "
                                    + bot.getSpamLimit()
                    );
                    return;
                case 2:
                    try {
                        int limit = Math.abs(Integer.parseInt(args[1]));
                        bot.setSpamLimit(limit);
                        event.reply("The spam limit is now " + limit);
                    } catch (NumberFormatException e) {
                        event.reply("Sorry, I couldn't read that number." +
                                            " Try using a smaller number and make sure " +
                                            "there are no letters, symbols, or decimals."
                        );
                    }
                    return;
                default:
                    event.reply("Sorry, " + String.join(" ", args)
                                                  .substring(args[0].length())
                                        + " is not an option. Please use the command:```"
                                        + bot.getCallsign() + "<spamlimit> <new_limit>```"
                                        + "\n Where <new_limit> is an integer."
                    );
                    break;
            }
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        return Launcher.makeEmbedBuilder(
                "Spam the Chat", null,
                "<spam> [number_of_spams] [message]\n*Aliases: spamthis, spamattack*"
        ).addField("Set Spam Limit",
                   "spamlimit <number>\n*Required Permissions: Administrator*", false)
                       .build();
    }
}
