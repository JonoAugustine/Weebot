package com.ampro.weebot.commands.management;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Managemnet interface for changing the bot's admin rules and capabilities
 */
public class AutoAdminCommand extends Command {

    /**
     * The administrative settings held by the Weebot, e.g. :<br>
     *     Channel-Banned words, infraction limits,
     *     banning or kicking threshold, exempted Roles (IDs),
     *     cleaning channels of banned words
     */
    public static final class AutoAdmin implements IPassive {

        /** A detailed record of user infractions */
        private static final class UserRecord {

            final long userID;
            final long guildID;

            /** Map of banned word usages mapped to the number of times used */
            final Map<String, Integer> wordInfractions;

            /** The number of all infractions */
            private int infractions;
            /** The number of times the user has been kicked */
            private int kicks;
            /** The number of times the user has been banned */
            int bans;

            UserRecord(User user, Guild guild) {
                this.userID  = user.getIdLong();
                this.guildID = guild.getIdLong();
                this.wordInfractions = new ConcurrentHashMap<>();
            }

            /**
             * Add a word infraction to the User's record.
             * @param infs The word infractions to add
             */
            void addWordInfraction(Collection<String> infs) {
                for (String inf : infs) {
                    Integer n = wordInfractions.get(inf);
                    wordInfractions.put(inf, n == null ? 1 : n + 1);
                    infractions++;
                }
            }

            /** @return The user infractions as an embed */
            MessageEmbed toEmbed() {
                EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
                eb.setColor(new Color(0xFB1800));
                StringBuilder sb = new StringBuilder();
                Guild guild = Launcher.getGuild(guildID);
                if (guild == null) return null;
                sb.append(guild.getName()).append(" ")
                  .append(guild.getMemberById(userID).getEffectiveName())
                  .append(" Infraction Record.");
                eb.setTitle(sb.toString());
                sb.setLength(0);

                sb.append("A history of this member's rule infractions.")
                  .append("\n\n*Total number of Infractions*: ")
                  .append(this.infractions);
                eb.setDescription(sb.toString());
                sb.setLength(0);

                if (!wordInfractions.isEmpty()) {
                    sb.setLength(0);
                    for (Map.Entry<String, Integer> e : wordInfractions.entrySet()) {
                        sb.append("\"").append(e.getKey()).append("\" : used ")
                          .append(e.getValue()).append(" times.").append("\n");
                    }
                    eb.addField("Banned Word Usages", sb.toString(), false);
                    sb.setLength(0);
                }

                return eb.build();
            }

        }

        private boolean dead;

        private final Long guildID;

        /** Map of String keys to Channel ID list values */
        private final ConcurrentHashMap<String, List<Long>> bannedWords;

        /* User records */
        private final ConcurrentHashMap<Long, UserRecord> userRecords;

        /* Exempted Roles & users */
        private final List<Long> exemptUsers;
        private final List<Long> exemptRoles;

        /* Time of last clean call */
        private OffsetDateTime lastClean;

        /** Kick threshold */
        private int kickThresh;
        /** Soft-ban threshold */
        private int sofThresh;
        /** Hard-ban threshold */
        private int hardThresh;

        //STATS
        private int numKicked;
        private int numBanned;
        private int infCaught;

        AutoAdmin(Guild guild) {
            this.exemptRoles = new ArrayList<>();
            this.exemptUsers = new ArrayList<>();
            this.userRecords = new ConcurrentHashMap<>();
            this.bannedWords = new ConcurrentHashMap<>();
            this.guildID     = guild.getIdLong();
            this.dead        = false;
        }

        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {
            if (dead) return;
            Member member = event.getMember();
            if (member.isOwner()
                || member.hasPermission(Permission.ADMINISTRATOR)) return;
            if (isExempt(event.getMember())) return;

            boolean infracted = false;
            UserRecord rec = userRecords.get(member.getUser().getIdLong());

            String content = event.toString();
            //Respond to word infractions
            List<String> b = checkWords(content, event.getTextChannel());
            if (!b.isEmpty()) {

                if(rec == null) {
                    rec = new UserRecord(member.getUser(), event.getGuild());
                    userRecords.put(member.getUser().getIdLong(), rec);
                }

                rec.addWordInfraction(b);
                infCaught++; //Stat track

                b.forEach(
                        s -> event.privateReply((infractionEmbed(event, s)))
                );
                infracted = true;
            }

            //Check thresholds
            if (infracted) {
                event.deleteMessage();
                GuildController controller = Launcher.getGuild(guildID)
                                                     .getController();
                //We use .complete() instead of event.reply() because we need
                //to send the message before we kick/ban them in order to send
                //the message at all
                threshCheck(rec, controller, event.getAuthor(), event.getMember(),
                            event.getMessage());
            }

        }

        /**
         * Check if a Member has been exempted from AutoAdmin tracking.
         * @param member The member to check
         * @return {@code false} if the Member is not in
         *          {@link AutoAdmin#exemptUsers} or {@link AutoAdmin#exemptRoles}.
         */
        private boolean isExempt(Member member) {
            for (Role r : member.getRoles()) {
                if (exemptRoles.contains(r.getIdLong()))
                    return true;
            }
            return exemptUsers.contains(member.getUser().getIdLong());
        }

        /** @return {@code true} if the 2 week clean cooldown has ended */
        private boolean cleanOnCooldown() {
            if (lastClean == null) return false;
            return ChronoUnit.WEEKS.between(lastClean, OffsetDateTime.now()) <= 2;
        }

        /**
         * Clean an last 2,000 messages in  {@link TextChannel#getHistory()} on banned
         * words.
         * @param channel The TextChannel to clean
         * @return -1 if the bot cannot view channel history <br>
         *         -2 if the bot cannot manage messages      <br>
         *          1 otherwise <br>
         */
        private int clean(TextChannel channel) {
            List<Message> messages = new ArrayList<>(2000);
            int i = 2000;
            try {
                for (Message message : channel.getIterableHistory().cache(false)) {
                    messages.add(message);
                    if (--i <= 0) break;
                }
            } catch (InsufficientPermissionException e) {
                return -1;
            }

            for (Message m : messages) {
                List<String> b = checkWords(m.getContentStripped(), m.getTextChannel());
                //Respond to word infractions
                if(!b.isEmpty()) {
                    m.delete().queue(messages::remove);
                    UserRecord rec = userRecords.get(m.getAuthor().getIdLong());
                    if(rec == null) {
                        rec = new UserRecord(m.getAuthor(), m.getGuild());
                        userRecords.put(m.getAuthor().getIdLong(), rec);
                    }

                    rec.addWordInfraction(b);
                    infCaught++;

                    GuildController controller = Launcher.getGuild(guildID)
                                                         .getController();
                    //We use .complete() instead of event.reply() because we need
                    //to send the message before we kick/ban them in order to send
                    //the message at all
                    threshCheck(rec, controller, m.getAuthor(), m.getMember(), m);

                    m.getAuthor().openPrivateChannel()
                     .queue(c -> infractionEmbed(m, b.toString()));
                }
            }
            this.lastClean = OffsetDateTime.now();
            if (messages.size() == 2000)
                return -2;
            return 1;
        }

        private void threshCheck(UserRecord rec, GuildController controller, User author,
                                 Member member, Message m) {
            if (kickThresh > 0 && rec.infractions % kickThresh == 0) {
                PrivateChannel c = author.openPrivateChannel().complete();
                rec.kicks++;
                numKicked++;
                if (hardThresh > 0 && rec.kicks >= hardThresh) {
                    rec.bans++;
                    numBanned++;
                    c.sendMessage(banEmbed(member)).complete();
                    controller.ban(member, 7,
                                   "You have been kicked too many times.")
                              .complete();
                } else {
                    c.sendMessage(kickEmbed(member)).complete();
                    controller.kick(member,
                                    "You have broken the rules too many times."
                    ).complete();
                }
            }
        }

        /**
         * Clean last 2,000 messages in each {@link TextChannel} of {@link Guild Guild's}
         * banned words.
         * @param guild The guild to clean
         * @return A {@link Map} of uncleaned TextChannels linked to an err code <br>
         *     -1 if the bot cannot view channel history <br>
         *     -2 if the bot cannot manage messages      <br>
         */
        private Map<String, Integer> cleanGuild(Guild guild) {
            Map<String, Integer> out = new TreeMap<>();
            for (TextChannel c : guild.getTextChannels()) {
                out.put(c.getName(), clean(c));
            }
            return out;
        }

        /**
         * Scan for banned words in the input string. Will catch words using symbols as
         * well. <br>
         *     Modified from
         * <a href="https://gist.github.com/PimDeWitte/c04cc17bc5fa9d7e3aee6670d4105941#file-efficient-bad-word-filter">
         *     PimDeWitte#Efficient Bad Word Filter
         *     </a>
         * @param input The string to scan
         * @param channel The Channel received in
         * @return A non-null {@link List} containing all banned words found in the
         *          string. Can be empty.
         */
        private List<String> checkWords(String input, TextChannel channel) {
            // remove leetspeak
            input = input.replaceAll("1","i")
                         .replaceAll("!","i")
                         .replaceAll("3","e")
                         .replaceAll("4","a")
                         .replaceAll("@","a")
                         .replaceAll("5","s")
                         .replaceAll("7","t")
                         .replaceAll("0","o")
                         .replaceAll("9","g")
                         .toLowerCase().replaceAll("[^a-zA-Z]", "");

            ArrayList<String> badWords = new ArrayList<>();

            Set<Map.Entry<String, List<Long>>> bwords = this.bannedWords.entrySet();
            //Scan for banned word
            //TODO Not adding each instance of the word, only that it conatins it
            for (Map.Entry<String, List<Long>> entry : bwords) {
                if (input.contains(entry.getKey())) {
                    if(entry.getValue().isEmpty()
                            || entry.getValue().contains(channel.getIdLong())) {
                        try {
                            badWords.add(input.substring(input.indexOf(entry.getKey()),
                                                         entry.getKey().length()
                            ));
                        } catch (Exception ignored){}
                    }
                }
            }

            /* TODO iterate over each letter in the word
            for(int start = 0; start < input.length(); start++) {
                // from each letter, keep going to find bad words until either
                // the end of the sentence is reached, or the max word length is reached.
                for(int offset = 1; offset < (input.length()+1 - start); offset++)  {
                    String wordToCheck = input.substring(start, start + offset);
                    if(this.bannedWords.containsKey(wordToCheck)) {
                        // for example, if you want to say the word bass,
                        // that should be possible.
                        List<Long> ignoreCheck = this.bannedWords.get(wordToCheck);
                        boolean ignore = false;
                        for(int s = 0; s < ignoreCheck.length; s++ ) {
                            if(input.contains(ignoreCheck.get(s))) {
                                ignore = true;
                                break;
                            }
                        }
                        if(!ignore) {
                            badWords.add(wordToCheck);
                        }
                    }
                }
            }*/

            return badWords;
        }

        /**
         * Ban words globally
         * @param words The words
         *
         */
        private void globalBanWords(String...words) {
            for (String word : words) {
                if (word == null) continue;
                List<Long> ids = this.bannedWords.putIfAbsent(word.toLowerCase(),
                                                              new ArrayList<>());
                //noinspection ConstantConditions
                if (ids != null && !ids.isEmpty()) ids.clear();
            }
        }

        /**
         * Ban words in particular channels
         * @param words The words to ban
         * @param channels The channels to ban them in.
         */
        private void banWords(String[] words, Collection<TextChannel> channels) {
            if (channels.isEmpty()) {
                this.globalBanWords(words);
                return;
            }
            //Convert channels to IDs
            List<Long> chIDs = new ArrayList<>();
            for (TextChannel channel : channels) {
                chIDs.add(channel.getIdLong());
            }
            //Add the banned words
            for (String w : words) {
                if (w == null) continue;
                List<Long> ch = this.bannedWords.get(w.toLowerCase());
                if (ch != null) {
                    ch.addAll(chIDs);
                } else {
                    this.bannedWords.put(w.toLowerCase(), chIDs);
                }
            }
        }

        /**
         * Remove words from {@link AutoAdmin#bannedWords}.
         * @param words The words to unban
         */
        private void unBanWords(String...words) {
            for (String word : words) {
                if (word != null) this.bannedWords.remove(word);
            }
        }

        /**
         * Remove word bans from specific channels.
         * @param words The words to unban
         * @param channles The channels to remove the ban from
         */
        private void unBanWords(String[] words, Collection<TextChannel> channles) {
            if (channles == null || channles.isEmpty()) {
                unBanWords(words);
                return;
            }

            List<Long> inIds = new ArrayList<>();
            channles.forEach( c -> inIds.add(c.getIdLong()));

            for (String w : words) {
                if (w == null) continue;
                List<Long> chs = this.bannedWords.get(w);
                //Check if the word is banned at all
                if(chs == null) continue;
                chs.removeIf(inIds::contains);
            }

        }

        private MessageEmbed wordsToEmbed() {
            Guild guild = Launcher.getGuild(guildID);
            EmbedBuilder eb = Launcher.makeEmbedBuilder(
                    guild.getName() + " AutoAdmin Banned Words",
                    null,
                    "Banned words tracked and moderated by the Weebot AutoAdmin");

            StringBuilder sb = new StringBuilder();
            this.bannedWords.forEach((word, channels) -> {
                sb.append(word);
                if (channels != null && !channels.isEmpty()) {
                    sb.append(" --- Banned in: ");
                    for (Long channel : channels) {
                        sb.append(guild.getTextChannelById(channel).getName())
                          .append(" ");
                    }
                }
                sb.append("\n");
            });

            eb.addField("Banned Words", sb.toString(), false);

            return eb.build();
        }

        private MessageEmbed infractionEmbed(BetterMessageEvent event, Object inf) {
            return infractionEmbed(event.getMessage(), inf);
        }

        private MessageEmbed infractionEmbed(Message message, Object inf) {
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder()
                                      .setColor(new Color(0xFB1800));

            eb.setTitle("AutoAdmin Caught an Infraction!");
            StringBuilder sb = new StringBuilder()
                    .append("In Server: ")
                    .append(message.getGuild().getName()).append("\nIn Channel:  ")
                    .append(message.getTextChannel().getName()).append("\nAt ")
                    .append(message.getCreationTime()
                                 .format(DateTimeFormatter.ofPattern("d-M-y hh:mm:ss")));
            eb.setDescription(sb.toString());
            sb.setLength(0);
            eb.addField("Infraction: " + inf.toString(), message.getContentStripped(),
                        false);
            return eb.build();
        }

        /**
         * Delete one or more members' record and unban them if banned.
         * @param members The members to pardon.
         */
        private void pardon(List<Member> members) {
            members.forEach( m -> pardon(m.getUser()));
        }

        /**
         * Delete one or more members' record and unban them if banned.
         * @param users The members to pardon.
         */
        private void pardon(Collection<User> users) {
            users.forEach( m -> pardon(m));
        }

        /**
         * Delete a member's record and unban them if banned.
         * Then notify them of their pardon.
         * @param user The member to pardon
         */
        private void pardon(User user) {
            GuildController controller = Launcher.getGuild(guildID).getController();
            String pardon = "*You have been pardoned in* **"
                    + controller.getGuild().getName()
                    + "**. *All records have been cleared and any bans uplifted.*";
            userRecords.remove(user.getIdLong());
            controller.unban(user).queue();
            user.openPrivateChannel().queue( c -> c.sendMessage(pardon).queue() );
        }

        /**
         * A User's private post-kicked message embed.
         * @param member The member to build the embed around.
         * @return A User's private post-kicked message embed.
         */
        private MessageEmbed kickEmbed(Member member) {
            UserRecord rec = userRecords.get(member.getUser().getIdLong());
            StringBuilder sb = new StringBuilder();
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder()
                                      .setColor(new Color(0xFB1800));
            eb.setTitle("You have been kicked by AutoAdmin!");
            sb.append("You have been kicked from **")
              .append(member.getGuild().getName())
              .append("** after breaking too many rules.\n");

            eb.setDescription(sb.toString());
            sb.setLength(0);

            if (!rec.wordInfractions.isEmpty()) {
                sb.setLength(0);
                for (Map.Entry<String, Integer> e : rec.wordInfractions.entrySet()) {
                    sb.append("\"").append(e.getKey()).append("\" : used ")
                      .append(e.getValue()).append(" times.").append("\n");
                }
                eb.addField("Banned Word Usages", sb.toString(), false);
                sb.setLength(0);
            }

            sb.append("You have a total record of ").append(rec.infractions)
              .append(" infractions and ").append(rec.kicks).append(" kick(s).");
            if (hardThresh > 0)
                sb.append("\n***If you are kicked ")
                  .append(hardThresh - rec.kicks)
                  .append(" more times, you will be banned from the server.***");
            eb.addField("YOUR RECORD", sb.toString(), false);

            return eb.build();

        }

        /**
         * A User's private post-banned message embed.
         * @param member The member to build the embed around.
         * @return A User's private post-banned message embed.
         */
        private MessageEmbed banEmbed(Member member) {
            UserRecord rec = userRecords.get(member.getUser().getIdLong());
            StringBuilder sb = new StringBuilder();
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder()
                                      .setColor(new Color(0xFB1800));
            eb.setTitle("You have been banned by AutoAdmin!");
            sb.append("You have been ***banned*** from **")
              .append(member.getGuild().getName())
              .append("** after breaking too many rules.\n");

            eb.setDescription(sb.toString());
            sb.setLength(0);

            if (!rec.wordInfractions.isEmpty()) {
                sb.setLength(0);
                for (Map.Entry<String, Integer> e : rec.wordInfractions.entrySet()) {
                    sb.append("\"").append(e.getKey()).append("\" : used ")
                      .append(e.getValue()).append(" times.").append("\n");
                }
                eb.addField("Banned Word Usages", sb.toString(), false);
                sb.setLength(0);
            }

            sb.append("You have a total record of ").append(rec.infractions)
              .append(" infractions, ").append(rec.kicks).append(" kicks")
              .append(", and ").append(rec.bans).append(" bans.");
            eb.addField("YOUR RECORD", sb.toString(), false);

            return eb.build();
        }

        /** @return The status of the autoadmin as an Embed */
        private MessageEmbed toEmbed() {
            StringBuilder sb = new StringBuilder();
            EmbedBuilder eb = Launcher.makeEmbedBuilder(
                    Launcher.getGuild(guildID).getName() + " AutoAdmin", null,
                    "The Weebot AutoAdmin moderator settings, status, and stats.");

            //Stats//
            sb.append("Infractions Caught: ").append(infCaught).append("\n")
              .append("Members Kicked: ").append(numKicked).append("\n")
              .append("Members Banned: ").append(numBanned);
            eb.addField("Stats", sb.toString(), true);
            sb.setLength(0);

            //Settings//
            //Kick and ban threshold
            if (kickThresh > 0)
                eb.addField("Infractions to Auto-Kick", kickThresh+"", true);
            if (hardThresh > 0)
                eb.addField("Kicks to Auto-Ban", hardThresh+"", true);

            //Exempted
            Guild guild = Launcher.getGuild(guildID);
            if (!exemptUsers.isEmpty()) {
                exemptUsers.forEach( id -> {
                    try {
                        sb.append(guild.getMemberById(id).getEffectiveName()).append(", ");
                    } catch (IndexOutOfBoundsException ignored) {}
                });
                sb.setLength(sb.length() - 2);
                eb.addField("Exempted Members", sb.toString(), true);
                sb.setLength(0);
            }
            if (!exemptRoles.isEmpty()) {
                exemptRoles.forEach( id -> {
                    try {
                        sb.append(guild.getRoleById(id).getName()).append(", ");
                    } catch (IndexOutOfBoundsException ignored) {}
                });
                sb.setLength(sb.length() - 2);
                eb.addField("Exempted Roles", sb.toString(), true);
                sb.setLength(0);
            }
            //Members with records
            if (!userRecords.isEmpty()) {
                userRecords.forEach( (id, rec) -> {
                    try {
                        sb.append(guild.getMemberById(id).getEffectiveName()).append
                                ("\n");
                    } catch (IndexOutOfBoundsException ignored) {}
                });
                eb.addField("Members With Infraction Records", sb.toString(), true);
                sb.setLength(0);
            }
            //Banned words
            if (!bannedWords.isEmpty()) {
                bannedWords.forEach( (word, cIdList) -> {
                    sb.append("\"").append(word).append("\"");
                    if (!cIdList.isEmpty()) {
                        sb.append(" ; Banned in :");
                        cIdList.forEach( cId -> sb.append(guild.getTextChannelById(cId).getName())
                                              .append(", "));
                        sb.setLength(sb.length() - 2);
                    }
                    sb.append("\n");
                });
                eb.addField("Banned Words", sb.toString(), true);
            }
            return eb.build();
        }

        @Override
        public boolean dead() {
            return this.dead;
        }

    }

    public AutoAdminCommand() {
        super(
                "AutoAdmin",
                new String[]{"aac", "adminbot", "botadmin"},
                "Control the Bot's admin capabilities and rules.",
                null,
                true,
                false,
                0,
                false,
                false
        );
        this.userPermissions = new Permission[]{Permission.KICK_MEMBERS};
    }

    private enum ACTION {
        INIT,
        DISABLE,
        ADDWORD,
        REMOVEWORD,
        SEEWORDS,
        SEERECORD,
        CLEARRECORD,
        SEEADMIN,
        SETKICKTHRESH,
        SETBANTHRESH,
        ADDEXEMPT,
        REMOVEEXEMPT,
        CLEANCHANNEL,
        CLEANGUILD
    }

    /** {@value} */
    private static final String NO_AA_FOUND = "I do not currently have any " +
            "administrative capabilities. Please use this command to enable the Weebot " +
            "AutoAdmin:```aac init```";

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        StringBuilder sb = new StringBuilder();
        String[] args = cleanArgs(bot, event);
        if (args.length < 2) {
            sb.append("Please use ``help aac`` for a list of commands");
            event.reply(sb.toString());
            return;
        }
        ACTION action = this.parseAction(args[1]);
        if (action == null) {
            sb.append("Please use one of these commands").append("```")
              .append("aac banword <word>\n")
              .append("aac rmwrd <word_index> or aac rmwrd <word>")
              .append("aac seebanned\n").append("aac record <@member> [@member2]\n")
              .append("aac pardon <@member> [@member2]...").append("```");
            event.reply(sb.toString());
            return;
        }
        AutoAdmin admin = bot.getAutoAdmin();

        switch (action) {
            case INIT:
                if (admin == null) {
                    bot.setAutoAdmin(new AutoAdmin(event.getGuild()));
                    sb.append("My AutoAdmin capabilities are now active.")
                      .append(" For help and commands, use ```help aac```");
                    event.reply(sb.toString());
                } else {
                    sb.append("My AutoAdmin capabilities are already active.")
                      .append(" For help and commands, use ```help aac```");
                    event.reply(sb.toString());
                }
                break;
            case DISABLE:
                if (admin != null) {
                    admin.dead = true;
                    event.reply("I will no longer moderate text channels.");
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case ADDWORD:
                //aac banword <word> [word2]...[#textChannel] [#textChannel_2]...
                if (admin != null) {
                    //Parse words, stop if channel is found
                    String[] words = new String[args.length];
                    for (int i = 2; i < args.length; i++) {
                        if (args[i].startsWith("#")) break;
                        words[i - 2] = args[i];
                    }
                    admin.banWords(words, event.getMessage().getMentionedChannels());
                    event.reply("Words have been added to the banlist", m -> {
                        event.deleteMessage();
                        try {
                            Thread.sleep(10 * 1000);
                        } catch (InterruptedException ignored) {}
                        m.delete().queue();
                    });
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case REMOVEWORD:
                //aaac rmwrd <word> [word2]... [#textCahnnel] [#textChannel_2]...
                if (admin != null) {
                    String[] words = new String[args.length];
                    for (int i = 2; i < args.length; i++) {
                        if(args[i].startsWith("#"))
                            break;
                        words[i - 2] = args[i];
                    }
                    admin.unBanWords(words, event.getMessage().getMentionedChannels());
                    event.reply("Words have been removed or edited", m -> {
                        event.deleteMessage();
                        try {
                            Thread.sleep(10 * 1000);
                        } catch (InterruptedException ignored) {
                        }
                        m.delete().queue();
                    });
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case SEEWORDS:
                //aac bwrds
                if (admin != null) {
                    event.privateReply(admin.wordsToEmbed());
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case SEERECORD:
                //aac ir <@member> [@member2]...
                if (admin != null) {
                    Collection<Member> ment = event.getMessage().getMentionedMembers();
                    if (ment == null || ment.isEmpty()) {
                        sb.append("You must mention one or more members as such")
                          .append("```aac ir <@member> [@member2]...```");
                        event.reply(sb.toString());
                        return;
                    }
                    for (Member m : ment) {
                        AutoAdmin.UserRecord rec
                                = admin.userRecords.get(m.getUser().getIdLong());
                        if (rec == null) {
                            event.privateReply(m.getEffectiveName() + " has no record.");
                            continue;
                        }
                        event.privateReply(rec.toEmbed());
                        event.deleteMessage();
                    }
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case CLEARRECORD:
                //aac pardon <@member> [@member2]...
                if (admin != null) {
                    admin.pardon(event.getMessage().getMentionedUsers());
                    admin.pardon(event.getMessage().getMentionedMembers());
                    event.reply("*User records have been cleared*.");
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case SEEADMIN:
                //aac status
                if (admin != null) {
                    event.privateReply(admin.toEmbed());
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case SETKICKTHRESH:
                //aac sk <#>
                if (admin != null) {
                    int i;
                    try {
                        i = Integer.parseInt(args[2]);
                        if (i < 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        event.reply("*Please use a number between 0 and 2,147,483,647.*");
                        return;
                    }
                    admin.kickThresh = i;
                    event.reply(
                            "The number of infractions to auto-kick a member is now "+i);
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case SETBANTHRESH:
                //aac bk <#>
                if (admin != null) {
                    int i;
                    try {
                        i = Integer.parseInt(args[2]);
                        if(i < 0)
                            throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        event.reply("*Please use a number between 0 and 2,147,483,647.*");
                        return;
                    }
                    admin.hardThresh = i;
                    event.reply(
                            "The number of infractions to auto-ban a member is now " + i);
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case ADDEXEMPT:
                //aac ex <@> [@2]...
                if (admin != null) {
                    if (event.getMessage().getMentionedMembers().isEmpty()
                            && event.getMessage().getMentionedRoles().isEmpty()) {
                        sb.append("*You must mention roles or members like this*")
                          .append("```aac ex @Memeber @Role```");
                        event.reply(sb.toString());
                        return;
                    }
                    event.getMessage().getMentionedMembers().forEach(
                            m -> admin.exemptUsers.add(m.getUser().getIdLong())
                    );
                    event.getMessage().getMentionedRoles().forEach(
                            r -> admin.exemptRoles.add(r.getIdLong())
                    );
                    event.reply("The Members and|or Roles are now isExempt from AutoAdmin.");
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case REMOVEEXEMPT:
                //aac rmex <@> [@2]...
                if (admin != null) {
                    if (event.getMessage().getMentionedMembers().isEmpty()
                            && event.getMessage().getMentionedRoles().isEmpty()) {
                        sb.append("*You must mention roles or members like this*")
                          .append("```aac rmex @Memeber @Role```");
                        event.reply(sb.toString());
                        return;
                    }
                    event.getMessage().getMentionedMembers().forEach(
                            m -> admin.exemptUsers.remove(m.getUser().getIdLong())
                    );
                    event.getMessage().getMentionedRoles().forEach(
                            r -> admin.exemptRoles.remove(r.getIdLong())
                    );
                    sb.append("The Members and|or Roles are no longer isExempt ")
                      .append("from AutoAdmin.");
                    event.reply(sb.toString());
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case CLEANCHANNEL:
                //aac clean <#> [#2]...
                if (admin != null) {
                    if(event.getMessage().getMentionedChannels().isEmpty()) {
                        sb.append("*Please mention one or more TextChannels to")
                          .append(" clean, like this:```")
                          .append("aac clean #TextChannel #TextChannel_2...```");
                        event.reply(sb.toString());
                        return;
                    }
                    if (admin.cleanOnCooldown()) {
                        String lc = null;
                        if (admin.lastClean != null)
                            lc = admin.lastClean
                                    .format(DateTimeFormatter.ofPattern("d-M-y"));
                        sb.append("*The last time you used any ``aac clean`` ")
                          .append("command was under 2 weeks ago, please wait")
                          .append(" until the 2 week cooldown has ended. ")
                          .append(lc != null ? "(Last used " + lc + ")*" : "*");
                        event.reply(sb.toString());
                        return;
                    }
                    bot.lock();
                    List<String> manage = new ArrayList<>();
                    List<String> history = new ArrayList<>();
                    List<String> clean = new ArrayList<>();
                    event.getMessage().getMentionedChannels().forEach(c -> {
                        switch (admin.clean(c)) {
                            case -1:
                                history.add(c.getName());
                                break;
                            case -2:
                                manage.add(c.getName());
                                break;
                            default:
                                clean.add(c.getName());
                                break;
                        }
                    });
                    if(!clean.isEmpty()) {
                        sb.append("*The following TextChannels have been ").append
                                ("cleaned of banned words*: ```")
                          .append(clean.toString()).append("```");
                    }
                    if(!manage.isEmpty()) {
                        sb.append("\n\n*The following TextChannels could ").append
                                ("clean because I do not have permission")
                          .append(" to manage messages*```:")
                          .append(manage.toString()).append("```");
                    }
                    if(!history.isEmpty()) {
                        sb.append("\n\n*The following TextChannels could ")
                          .append("clean because I do not have permission")
                          .append(" to view their message history*```:")
                          .append(history.toString()).append("```");
                    }
                    if (sb.length() == 0) {
                        sb.append("*No channels were cleaned.*");
                    }
                    event.reply(sb.toString());
                    bot.unlock();
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case CLEANGUILD:
                //aac fc
                if (admin != null) {
                    if (admin.cleanOnCooldown()) {
                        String lc = null;
                        if (admin.lastClean != null)
                            lc = admin.lastClean
                                    .format(DateTimeFormatter.ofPattern("d-M-y"));
                        sb.append("*The last time you used any ``aac clean`` ")
                          .append("command was under 2 weeks ago, please wait")
                          .append(" until the 2 week cooldown has ended. ")
                          .append(lc != null ? "(Last used " + lc + ")*" : "*");
                        event.reply(sb.toString());
                        return;
                    }
                    bot.lock();
                    List<String> manage = new ArrayList<>();
                    List<String> history = new ArrayList<>();
                    List<String> clean = new ArrayList<>();
                    admin.cleanGuild(event.getGuild()).forEach((name, err) -> {
                        switch (err) {
                            case -1:
                                history.add(name);
                                break;
                            case -2:
                                manage.add(name);
                                break;
                            default:
                                clean.add(name);
                                break;
                        }
                    });
                    if(!clean.isEmpty()) {
                        sb.append("*The following TextChannels have been ")
                          .append("cleaned of banned words*: ```")
                          .append(clean.toString()).append("```");
                    }
                    if(!manage.isEmpty()) {
                        sb.append("\n\n*The following TextChannels could ")
                          .append("clean because I do not have permission")
                          .append(" to manage messages*```:")
                          .append(manage.toString()).append("```");
                    }
                    if(!history.isEmpty()) {
                        sb.append("\n\n*The following TextChannels could ")
                          .append("clean because I do not have permission")
                          .append(" to view their message history*```:")
                          .append(history.toString()).append("```");
                    }
                    event.reply(sb.toString());
                    bot.unlock();
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
        }

    }

    /**
     * Parse an {@link ACTION action} from a string.
     *
     * @param arg
     *         The string to parse from.
     *
     * @return The action parsed or null if no action was found.
     */
    private ACTION parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "init":
                return ACTION.INIT;
            case "disable":
            case "stop":
            case "dis":
                return ACTION.DISABLE;
            case "banword":
            case "addword":
                return ACTION.ADDWORD;
            case "unbanword":
            case "rmwrd":
                return ACTION.REMOVEWORD;
            case "bannedwords":
            case "seebannedwords":
            case "bwrds":
            case "words":
                return ACTION.SEEWORDS;
            case "userrecord":
            case "record":
            case "ir":
                return ACTION.SEERECORD;
            case "clrrec":
            case "clrec":
            case "pardon":
                return ACTION.CLEARRECORD;
            case "status":
            case "sitch":
                return ACTION.SEEADMIN;
            case "setkick":
            case "kickthresh":
            case "sk":
                    return ACTION.SETKICKTHRESH;
            case "setban":
            case "setbanthresh":
            case "sb":
                    return ACTION.SETBANTHRESH;
            case "immune":
            case "isExempt":
            case "ex":
                return ACTION.ADDEXEMPT;
            case "removeexempt":
            case "rmex":
            return ACTION.REMOVEEXEMPT;
            case "cleanse":
            case "clean":
            case "clch":
                return ACTION.CLEANCHANNEL;
            case "guildclean":
            case "gclean":
            case "fullclean":
            case "fc":
                    return ACTION.CLEANGUILD;
            default:
                return null;
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        StringBuilder sb = new StringBuilder();

        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();

        eb.setTitle("AutoAdmin");
        sb.append("Use the bot to moderate text channels, warn, kick, and ban members")
          .append(" who violate the rules.")
          .append("\n*Required User Permissions: Kick & Ban Member, Manage Messages*")
          .append("\n*Required Bot  Permissions: Kick & Ban Member, Manage Messages*");
        eb.setDescription(sb.toString());
        sb.setLength(0);

        //INIT/Disable
        eb.addField("Initialize AutoAdmin", "aac init", true)
          .addField("Disable AutoAdmin", "aac disable\n*Aliases: stop, dis*", true)
          .addBlankField(true);

        //Banned Words
        sb.append("aac rmwrd <word> [word2]... [#textCahnnel] [#textChannel_2]...\n")
          .append("*Aliases: unbanword*\n")
          .append("**NOTE**: To globally unban a word with non-global bans")
          .append(", do not include a #TextChannel.");
        eb.addField("Ban Word(s) Globally or Channel-Specific",
                    "aac banword <word> [word2]...[#textChannel] [#textChannel_2]..." +
                            "\n*Alias: addword*",
                    false)
          .addField("Unban word", sb.toString(), false)
          .addField("See Banned Words (in Private Message)",
                    "aac bwords\n*Aliases: bannedwords, seebannedwords, bwrds, words*",
                    false);
        sb.setLength(0);

        eb.addField("Clean TextChannels of Banned Words (Past 2,000 Messages)",
                    "aac clean <#TextChannel> [#TextChannel_2]...\n*Alias: cleanse*",
                    false)
          .addField("Clean All TextChannels of Banned Words (Past 2,000 Messages)",
                    "aac fc\n*Aliases: fullclean, guildclean, gclean*", false)
          .addField("NOTE: THIS WILL LOCK THE WEEBOT |",
                    "**While cleaning text channels, the bot will be locked to prevent " +
                            "any confusion. It is best to use these commands while " +
                            "most members are offline (asleep, at school/work, etc)**",
                    true)
          .addField("NOTE: CLEAN CAN BE USED ONCE PER 2 WEEKS",
                    "**These command is a heavy tax on the Weebot system (and Discord " +
                            "API gets pissy), so it can be used only once per week**",
                    true)
          .addBlankField(false);

        //Infractions
        eb.addField("Set Number of Infractions to Kick",
                    "aac sk <number>\n*Aliases: setkickthresh, setkick*", true)
          .addField("Set Number of Kicks to Ban",
                    "aac sb <number>\n*Aliases: setban*\n*Set to 0 to disable*",
                    true);

        eb.addField("See Member Infraction Record(s)",
                    "aac ir <@Member> [@member2]...\n*Aliases: userrecord, record*",
                    true)
          .addField("Clear Member Infraction Record(s)",
                    "aac pardon <@member> [@member2]...\n*Alias: clrrec*", true);

        sb.append("aac ex <@Member> [@member2]...").append("\naac ex <@Role> [@Role2]...")
          .append("\n*Aliases: isExempt, immune*");
        eb.addField("Exempt Members & Roles", sb.toString(), true);
        sb.setLength(0);

        sb.append("aac rmex <@Member> [@Member2]...")
          .append("\naac rmex <@Role> [@Role2]...").append("\n*Aliases: removeexempt*");
        eb.addField("Remove Exemption(s)", sb.toString(), true);
        sb.setLength(0);

        eb.addBlankField(false).addField("See AutoAdmin Status & Stats",
                    "aac status\n*Aliases: sitch*", true)
          .addBlankField(false);

        return eb.build();

    }

}
