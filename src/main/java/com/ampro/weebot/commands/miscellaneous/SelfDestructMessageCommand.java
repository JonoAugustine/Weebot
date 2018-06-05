package com.ampro.weebot.commands.miscellaneous;


import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Automatically deletes a message from a
 * {@link net.dv8tion.jda.core.entities.TextChannel TextChannel}
 * after a given time or 30 seconds by default. <br><i>
 * Formatted: < callsign>< deleteme> [time] [message]
 */
public class SelfDestructMessageCommand extends Command {

    public SelfDestructMessageCommand() {
        super(
                "SelfDestruct",
                new String[]{ "deleteme", "cleanthis", "deletethis","covertracks",
                        "whome?", "podh", "sdc"},
              "Deletes the marked message after the given amount of time (30 sec by "
                      + "default)",
              "<callsign><deleteme> <time or X> [message]",
                true, false, 0, false, false
        );
    }
    /**
     * Performs a check then runs the command in a new thread.
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
            thread.setName(bot.getBotId() + " : SelfDestructMessageCommand");
            thread.start();
        }
    }

    /**
     * Deletes the {@link net.dv8tion.jda.core.entities.Message Message} after the given time
     * span or 30 seconds if time not given.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args;
        synchronized (bot) {
            args = this.cleanArgs(bot, event);
        }
        int sec = 1000;
        int time;

        //Get the time span arg
        switch (args.length) {
            default: //Command and time and/or message
                try {
                    time = Integer.parseInt(args[1]) * sec;
                } catch (NumberFormatException e) {
                    time = 30 * sec;
                }
                break;
            case 1: //Just the command
                time = 30 * sec;
                break;
        }

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.deleteMessage();
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        String d =
        "Deletes the marked message after the given amount of time (30 sec by default)";
        EmbedBuilder eb = Launcher.makeEmbedBuilder("Self Destruct Message",
                                                    null,
                                                    d);
        return eb.build();
    }

    @Override
    public String getHelp() {
        StringBuilder sb = new StringBuilder();

        sb.append("```Self Destruct Command Help:\n\n")
          .append("selfdestruct [Whatever you want to say]\n\n")
          .append("'selfdestruct' can be replaced with any of these:\n")
          .append(this.aliases)
          .append("```");

        return sb.toString();
    }

}
