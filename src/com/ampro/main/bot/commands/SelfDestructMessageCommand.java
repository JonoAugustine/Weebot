package com.ampro.main.bot.commands;

import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Automatically deletes a message from a
 * {@link net.dv8tion.jda.core.entities.TextChannel TextChannel}
 * after a given time or 30 seconds by default.
 * Formatted: < callsign>< deleteme> [time] [message]
 *
 * @author Jonathan Augustine
 */
public class SelfDestructMessageCommand extends Command {

    public SelfDestructMessageCommand() {
        super(
                "SelfDestruct",
                new ArrayList<>(Arrays.asList(
                        "deleteme", "cleanthis", "deletethis", "covertracks"
                )),
                "Deletes the marked message after the given amount of time (30 sec by default)",
                "<callsign><deleteme> [time] [message]",
                true,
                false,
                0,
                false
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
        if (this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : SelfDestructMessageCommand");
            thread.start();
        }
    }

    /**
     * Deletes the {@link net.dv8tion.jda.core.entities.Message Message} after
     * the given time span or 30 seconds if time not given.
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
    protected void execute(BetterMessageEvent event) {}
}
