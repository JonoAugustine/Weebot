package com.ampro.main.commands;

import com.ampro.main.Launcher;
import com.ampro.main.entities.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A container class containing commands for smaller commands that could not
 * justify their own classes:
 * <P><i>
 *     Self Destruct Message Command    <br>
 *     Weebot Suggestions Command       <br>
 *     Spam Command                     <br>
 *     Shutdown Command                 <br>
 *     Ping-Pong command                <br>
 * </P></i>
 *
 * @author Jonathan Augustine
 */
public class MiscCommands {

    /**
     * Automatically deletes a message from a
     * {@link net.dv8tion.jda.core.entities.TextChannel TextChannel}
     * after a given time or 30 seconds by default. <br><i>
     * Formatted: < callsign>< deleteme> [time] [message]
     */
    public static final class SelfDestructMessageCommand extends Command {

        public SelfDestructMessageCommand() {
            super(
                    "SelfDestruct",
                    new ArrayList<>(Arrays.asList("deleteme", "cleanthis",
                            "deletethis", "covertracks")),
                    "Deletes the marked message after the given amount of time (30 sec by default)",
                    "<callsign><deleteme> <time or X> [message]",
                    true, false, 0, false);
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
     * A way for anyone in a Guild hosting a Weebot to make suggestions to the
     * developers.
     */
    public static final class WeebotSuggestionCommand extends Command {

        public static final class Suggestion {
            private final String suggestion;
            private final long authorID;
            private final long guildID;
            private final OffsetDateTime submitTime;

            public Suggestion(String suggestion, User author) {
                this.submitTime = OffsetDateTime.now();
                this.suggestion = suggestion;
                this.authorID = author.getIdLong();
                this.guildID = -1L;
            }

            public Suggestion(String suggestion, long authorID) {
                this.submitTime = OffsetDateTime.now();
                this.suggestion = suggestion;
                this.authorID = authorID;
                this.guildID = -1L;
            }

            public Suggestion(String suggestion, User author, Guild guild) {
                this.submitTime = OffsetDateTime.now();
                this.suggestion = suggestion;
                this.authorID = author.getIdLong();
                this.guildID = guild != null ? guild.getIdLong() : -1L;
            }

            public Suggestion(String suggestion, long authorID, long guildID) {
                this.submitTime = OffsetDateTime.now();
                this.suggestion = suggestion;
                this.authorID = authorID;
                this.guildID = guildID;
            }

            public long getAuthorID() { return this.authorID; }

            public long getGuildID() { return this.guildID; }

            public OffsetDateTime getSubmitTime() { return this.submitTime; }

            @Override
            public String toString() { return this.suggestion; }

        }

        public WeebotSuggestionCommand() {
            super(
                    "Suggest",
                    new ArrayList<>(Arrays.asList("suggestion", "sugg", "loadsuggs",
                                                  "seesuggs", "allsuggs")),
                    "Submit a suggestion to the Weebot developers right from Discord!",
                    "<suggest/suggestion/sugg> <Your Suggestion>",
                    false,
                    false,
                    0,
                    false
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
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : WeebotSuggestionCommand");
            thread.start();
        }

        /**
         * Performs the action of the command.
         *
         * @param bot
         *         The {@link Weebot} which called this command.
         * @param event
         *         The {@link BetterMessageEvent} that called the command.
         */
        @Override
        protected void execute(Weebot bot, BetterMessageEvent event) {
            String[] args = this.cleanArgs(bot, event);
            switch (args[0]) {
                case "loadsuggs":
                case "seesuggs":
                case "allsuggs":
                    if (isDev(event.getAuthor())) {
                        sendSuggestions(event);
                        return;
                    } else {
                        event.reply("Sorry, but only developers can see suggestions at " +
                                            "this time.");
                        return;
                    }
                default:
                    String sugg = String.join(" ",Arrays.copyOfRange(args,1,args.length))
                                        .trim();
                    Launcher.getDatabase()
                            .addSuggestion(new Suggestion(sugg, event.getAuthor(),
                                                          event.getGuild()));
                    event.reply("Thank you for your suggestion! We're working hard to " +
                                        "make Weebot as awesome as possible, but we " +
                                        "will try our best to include your suggestion!");
                    return;
            }
        }

        private void sendSuggestions(BetterMessageEvent event) {
            String out = "``` ";
            Collection<Suggestion> it = Launcher.getDatabase().getSuggestions().values();
            int i = 0;
            for (Suggestion s : it) {
                Guild g = Launcher.getGuild(s.guildID);
                out = out.concat(
                        s.submitTime.format(DateTimeFormatter.ofPattern("d-M-y hh:mm:ss"))
                        + " | by " + Launcher.getJda().getUserById(s.authorID).getName()
                        + " | on " + (g != null ? g.getName() : "Private Chat")
                        + "\n" + i++ + ") \"" + s + "\"\n\n"
                );
            }

            out += "```";
            if (it.size() > 50) {
                event.privateReply(out);
            } else
                event.reply(out);
        }

        private boolean isDev(User user) {
            return Launcher.checkDevID(user.getIdLong());
        }
    }

    /**
     * Safely shuts down all the bots, initiating proper database saving in
     * {@link com.ampro.main.Launcher}.
     *
     * @author Jonathan Augustine
     */
    public static final class ShutdownCommand extends Command {

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
