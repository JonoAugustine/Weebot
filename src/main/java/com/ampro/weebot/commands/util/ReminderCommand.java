package com.ampro.weebot.commands.util;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.database.Database;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Start or stop a {@link ReminderPool.Reminder}.
 */
public class ReminderCommand extends Command {

    /** A Pool of {@link Reminder reminders} */
    public static final class ReminderPool implements Iterable<ReminderPool.Reminder> {

        /** A Reminder that can last up to 30 days. */
        final class Reminder implements Runnable {

            /** The max number of seconds (30 days) {@value} */
            public static final long MAX_TIME = 43200 * 60;
            /** The min number of minuets (b/c of grouped reminder check {@value} */
            public static final int MIN_TIME = 1;

            private final long guildId;
            private final long channelId;

            private final String message;

            private final OffsetDateTime startDate;
            /** How many seconds will this last */
            private long lifeSpan;
            /** How many seconds are left */
            private long timeRemaining;
            /** Should this reminder be a lil annoying? */
            private final boolean bug;
            /** Should the reminder be sent to the channel recieved or private channel */
            private final boolean inGuild;

            /**
             * Make a Reminder.
             * @param event The Event that called this.
             * @param minutes How many minuets will this last
             * @param bugMe TODO
             * @param inGuild Whether this is inGuild (or in private)
             * @param message A message to display in the reminder
             */
            Reminder(BetterMessageEvent event, int minutes, boolean bugMe,
                     boolean inGuild, String message) {
                this.startDate = OffsetDateTime.now();
                this.lifeSpan = minutes * 60;
                this.timeRemaining = lifeSpan;
                this.bug = bugMe;
                this.inGuild = inGuild;
                if (!event.isPrivate()){
                    this.guildId = event.getGuild().getIdLong();
                    this.channelId = event.getTextChannel().getIdLong();
                }
                else {
                    this.guildId = -1;
                    this.channelId = -1;
                }
                this.message = message;
            }

            @Override
            public void run() {
                for (; timeRemaining > 0; timeRemaining--) {
                    try {
                        Thread.sleep(1000); //Wait 1 second
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                User user = Launcher.getJda().getUserById(authorId);
                StringBuilder sb = new StringBuilder(
                        user.getAsMention()
                ).append(" | ");
                if (message == null || message.isEmpty()) {
                    sb.append("Reminder!");
                } else sb.append(message);
                if (inGuild && guildId > -1)
                    Launcher.getGuild(guildId).getTextChannelById(channelId)
                            .sendMessage(sb.toString()).queue();
                else
                    user.openPrivateChannel().queue(
                            c -> c.sendMessage(sb.toString()).queue()
                    );

            }

            /** Update the lifeSpan after a loadDao from file */
            void startup() {
                long timeSince = ChronoUnit.SECONDS
                        .between(startDate, OffsetDateTime.now());
                timeRemaining = lifeSpan - timeSince;
            }

        }

        /** The number of concurrent {@link Reminder Reminders} a user can have: {@value} */
        public static final int USER_REMINDER_LIMIT = 5;
        public static final int PREMIUM_REMINDER_LIMIT = 15; //For expansion

        private final long authorId;

        private static transient ExecutorService poolExecutor;

        private Reminder[] pool;

        public ReminderPool(User user) {
            authorId = user.getIdLong();
            this.poolExecutor = Executors.newFixedThreadPool(PREMIUM_REMINDER_LIMIT);
            this.pool = Launcher.getDatabase().isPremium(user) ?
                        new Reminder[PREMIUM_REMINDER_LIMIT] :
                        new Reminder[USER_REMINDER_LIMIT];
        }

        /**
         * Re-add the Reminders to the threadpool on loadDao from file startup.
         */
        public void startup() {
            poolExecutor = Executors.newCachedThreadPool();
            for (Reminder reminder : pool) {
                if (reminder == null || reminder.timeRemaining <= 0) continue;
                reminder.startup();
                poolExecutor.submit(reminder);
            }
        }

        /**
         * Schedule a new Reminder
         * @param event
         * @param minutes
         * @param bugMe
         * @param inGuild
         * @param message
         * @return {@code false} if the {@link ReminderPool} is full
         */
        public boolean addReminder(BetterMessageEvent event, int minutes, boolean bugMe,
                                   boolean inGuild, String message) {
            Reminder reminder = new Reminder(event, minutes, bugMe, inGuild, message);
            if (checkPrem()) {
                try {
                    poolExecutor.submit(reminder);
                    pool[openIndex()] = reminder;
                    return true;
                } catch (IndexOutOfBoundsException e) {
                    return false;
                }
            }
            return false;
        }

        /**
         * @return The index of a valid space in the {@link ReminderPool#pool}. <br>
         *     -1 if non found
         */
        private int openIndex() {
            for (int i = 0; i < pool.length; i++) {
                if (pool[i] == null || pool[i].timeRemaining <= 0)
                    return i;
            }
            return -1;
        }

        /**
         * Ensures that the pool size matches the user's premium status
         * @return false if the pool cannot be added to b/c the pool is oversized.
         */
        private boolean checkPrem() {
            Database db = Launcher.getDatabase();
            if (db.isPremium(authorId) && pool.length == USER_REMINDER_LIMIT) {
                Reminder[] nPool = new Reminder[PREMIUM_REMINDER_LIMIT];
                System.arraycopy(pool,0, nPool,0, pool.length);
                pool = nPool;
                return true;
            } else if (!db.isPremium(authorId) && pool.length == PREMIUM_REMINDER_LIMIT) {
                for (int i = 0; i < pool.length; i++) {
                    if (pool[i] != null && i >= USER_REMINDER_LIMIT) {
                        return false;
                    }
                }
                Reminder[] nPool = new Reminder[USER_REMINDER_LIMIT];
                System.arraycopy(pool,0, nPool,0, pool.length);
                pool = nPool;
                return true;
            }  else return true;
        }

        public void shutdown() {
            poolExecutor.shutdownNow();
        }

        MessageEmbed toEmbed() {
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
            eb.setTitle("Your Set Reminders");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pool.length; i++) {
                if (pool[i] == null || pool[i].timeRemaining <= 0) continue;
                Reminder r = pool[i];
                sb.append((i + 1) + ".) ")
                  .append(r.startDate.plusSeconds(r.lifeSpan)
                                     .format(DateTimeFormatter.ofPattern
                                             ("dd/MM/yy HH:mm:ss")))
                  .append("\n\n");
            }
            if (sb.length() == 0) {
                sb.append("*You have no reminders. Use*\n")
                  .append("rc [here] [loudAlert] [Xm] [Yh] [Zd] [Reminder Message]")
                  .append("\n*to set a new reminder.*");
            }
            eb.setDescription(sb.toString());
            return eb.build();
        }

        @Override
        public Iterator<Reminder> iterator() {
            return new ArrayList<>(Arrays.asList(pool)).iterator();
        }

        @Override
        public void forEach(Consumer<? super Reminder> consumer) {
            new ArrayList<>(Arrays.asList(pool)).forEach(consumer);
        }

    }

    public ReminderCommand() {
        super(
               "Reminder",
               new String[]{"rc", "rem", "remindme"},
               "Set a Reminder for u pto 30 days.",
               "[here] [loudAlert] [Xm] [Yh] [Zd] [Reminder Message]",
               false,
               false,
               0,
               false,
               false
        );
    }

    private enum ACTION{START, SEE, CANCEL}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = cleanArgs(bot, event);
        if (args.length == 1) return;
        ACTION action = parseAction(args[1]);
        ReminderPool pool = Launcher.GLOBAL_WEEBOT.getReminderPool(event.getAuthor());

        switch (action) {
            case START:
                //rc [here] [bugme] [Xm] [Yh] [Zd] [message]
                if (pool == null) {
                    pool = new ReminderPool(event.getAuthor());
                    Launcher.GLOBAL_WEEBOT.addUserReminder(event.getAuthor(), pool);
                }
                if (pool.openIndex() == -1) {
                    event.reply("*You already have the maximum number of reminders.*");
                    return;
                }

                int dys = 0;
                int hrs = 0;
                int min = 0;
                boolean bugme = false;
                boolean here = false;
                int messIndex = 1;

                for (int i = 1; i < args.length; i++) {
                    if (args[i].matches("^[0-9]+[mM]$")) {
                        try {
                            min = Integer.parseInt(args[i].toLowerCase().replace("m",""));
                        } catch (NumberFormatException e) {
                            event.reply("*Sorry, " + args[i] + "is not a number");
                            return;
                        }
                        messIndex++;
                    } else if (args[i].matches("^[0-9]+[hH]$")) {
                        try {
                            hrs = Integer.parseInt(args[i].toLowerCase().replace("h",""));
                        } catch (NumberFormatException e) {
                            event.reply("*Sorry, " + args[i] + "is not a number");
                            return;
                        }
                        messIndex++;
                    } else if (args[i].matches("^[0-9]+[dD]$")) {
                        try {
                            dys = Integer.parseInt(args[i].toLowerCase().replace("d",""));
                        } catch (NumberFormatException e) {
                            event.reply("*Sorry, " + args[i] + "is not a number");
                            return;
                        }
                        messIndex++;
                    } else if (args[i].equalsIgnoreCase("bugme")) {
                        bugme = true;
                        messIndex++;
                    } else if (args[i].equalsIgnoreCase("here")) {
                        here = true;
                        messIndex++;
                    }
                }
                min = min + hrs * 60 + (dys * 24 * 60);
                if (min < 1) {
                    event.reply("*You haven't set a time...*");
                    return;
                }
                String mess = String.join(" ",
                                          Arrays.copyOfRange(args, messIndex, args.length)
                );
                if (event.isPrivate()) here = false;
                if(pool.addReminder(event, min, bugme, here, mess))
                    event.reply("*Reminder set.*");
                break;
            case SEE:
                if (pool != null) {
                    event.privateReply(pool.toEmbed());
                } else {
                    event.reply("*You have no running reminders.*");
                }
                break;
            case CANCEL:
                if (pool != null) {

                } else {
                    event.reply("*You have no running reminders.*");
                }
                break;
        }

    }

    /**
     * Parse an {@link ACTION} fromt a string.
     * @param arg The string to parse from.
     * @return The corrisponing {@link ACTION}
     *          or {@link ACTION#START} if on eis not found
     */
    private ACTION parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "seeall":
            case "all":
            case "see":
                return ACTION.SEE;
            case "end":
            case "cancel":
                return ACTION.CANCEL;
            default:
                return ACTION.START;
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
        StringBuilder sb = new StringBuilder()
                .append("Set a reminder for at least 1 minute or at most 30 days.")
                .append("\nYou can have up to *").append(ReminderPool.USER_REMINDER_LIMIT)
                .append("* reminders at the same time.\n")
                .append("*Aliases*: reminder, rem, remindme");

        eb.setTitle("Reminder")
          .setDescription(sb.toString());
        sb.setLength(0);

        sb.append("rc [here] [loudAlert] [Xm] [Yh] [Zd] [Reminder Message]\n\n")
          .append("*here*: send the reminder to this channel\n")
          .append("*loudAlert*: Spam mentions so you ***really*** notice.")
          .append("*Xm*: The number of minutes.\n")
          .append("*Yh*: The number of hours.\n")
          .append("*Zd*: The number of days.\n");
        eb.addField("Make a Reminder", sb.toString(), true);
        sb.setLength(0);

        sb.append("rc see\n*Aliases*: seeall, all");
        eb.addField("See Your Reminders", sb.toString(), true);

        return eb.build();
    }

}
