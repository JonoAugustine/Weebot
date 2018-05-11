package com.ampro.main.bot.commands;

import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A container class containing commands for smaller commands that could not
 * justify their own classes.
 * <P>
 *     Spam Command
 *     Ping-Pong command
 * </P>
 *
 * @author Jonathan Augustine
 */
public class MiscCommands {

    /**
     * Automatically deletes a message from a
     * {@link net.dv8tion.jda.core.entities.TextChannel TextChannel}
     * after a given time or 30 seconds by default.
     * Formatted: < callsign>< deleteme> [time] [message]
     *
     * @author Jonathan Augustine
     */
    public static final class SelfDestructMessageCommand extends Command {

        public SelfDestructMessageCommand() {
            super("SelfDestruct", new ArrayList<>(Arrays.asList("deleteme", "cleanthis", "deletethis", "covertracks")), "Deletes the marked message after the given amount of time (30 sec by default)", "<callsign><deleteme> [time] [message]", true, false, 0, false);
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
    }

    /**
     * Spam a message up to {@link Weebot#spamLimit} times.
     */
    public static final class SpamCommand extends Command {

        public SpamCommand() {
            super("Spam", new ArrayList<>(Arrays.asList("spamthis", "spamattack")), "Spam the chat", "<spam> [number_of_spams] [message]", false, false, 0, false);
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

    /**
     * Check the response time of the bot.
     */
    public static final class PingCommand extends Command {

        public PingCommand() {
            super("Ping"
                    , new ArrayList<>()
                    , "Check my response time"
                    ,""
                    , false
                    , false
                    , 0
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
            if (this.cleanArgs(bot, event).length > 1) {
                return;
            }
            event.reply("Pong...", m -> {
                long ping = event.getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS);
                m.editMessage("Pong: " + ping  + "ms | Websocket: " + event.getJDA().getPing() +
                        "ms").queue();
            });
        }

    }

}
