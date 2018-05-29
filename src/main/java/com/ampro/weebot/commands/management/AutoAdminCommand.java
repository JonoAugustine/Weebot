package com.ampro.weebot.commands.management;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateOwnerEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Managemnet interface for changing the bot's admin rules and capabilities
 * TODO...all of it?
 */
public class AutoAdminCommand extends Command {


    /**
     * The administrative settings held by the Weebot, e.g. :<br>
     *     Channel-Banned words, infraction limits,
     *     banning or kicking threshold, exempted Roles (IDs)
     */
    public static final class AutoAdmin implements IPassive {

        /** A detailed record of user infractions in string. */
        private final class UserRecord {

            final long userID;

            final List<Message> wordInfractions;

            UserRecord(User user) {
                this.userID = user.getIdLong();
                this.wordInfractions = new ArrayList<>();
            }

            /**
             * Add a word infraction to the User's record.
             * @param message The message containing the banned word
             */
            void addWordInfraction(Message message) {
                this.wordInfractions.add(message);
            }

            /** @return The user infractions as an embed */
            MessageEmbed toEmbed() {
                EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
                eb.setColor(new Color(0xFB1800));
                StringBuilder sb = new StringBuilder();
                Guild guild = Launcher.getGuild(AutoAdmin.this.guildID);
                sb.append(guild.getName()).append(" ")
                  .append(guild.getMemberById(userID)).append(" Infraction Record.");
                eb.setTitle(sb.toString());
                sb.setLength(0);

                eb.setDescription("A history of this member's rule infractions.");

                if (!wordInfractions.isEmpty()) {
                    sb.setLength(0);
                    for (int i = 0; i < wordInfractions.size(); i++) {
                        sb.append((i + 1) + ".) ")
                          .append(wordInfractions.get(i).getContentStripped())
                          .append("\n");
                    }
                    eb.addField("Banned Word Usages", sb.toString(), false);
                }

                return eb.build();
            }

        }

        private boolean dead;
        private final Long guildID;

        /** Banned words
        Should be Channel or global
        Should a word-ban class be created?
         or just map the words to channel IDs or null?
        */

        /** Map of String keys to Channel ID list values */
        final ConcurrentHashMap<String, List<Long>> bannedWords;

        /* User records */
        private final ConcurrentHashMap<Long, UserRecord> userRecords;

        /* Exempted Roles & users */
        private final List<Long> exemptUsers;
        private final List<Long> exemptRoles;

        /* Kick threshold */
        int kickThresh;
        /* Soft-ban threshold */
        int sofThresh;
        /* Hard-ban threshold */
        int hardThresh;

        public AutoAdmin(Guild guild) {
            this.exemptRoles = new ArrayList<>();
            this.exemptUsers = new ArrayList<>();
            this.userRecords = new ConcurrentHashMap<>();
            this.bannedWords = new ConcurrentHashMap<>();
            this.guildID     = guild.getIdLong();
            this.dead        = false;
        }

        @Override
        public void accept(BetterMessageEvent event) {
            //TODO Scanning
            //Scan for banned word
            //Check thresholds
            //Act
            //  Warn user
            //  Kick/sban/ban
        }

        /**
         * Ban words globally
         * @param words The words
         *
         */
        private void banWords(String...words) {
            for (String word : words) {
                this.bannedWords.putIfAbsent(word, null);
            }
        }

        /**
         * Ban words in particular channels
         * @param words The words to ban
         * @param channels The channels to ban them in.
         */
        private void banWords(String[] words, Collection<TextChannel> channels) {
            if (channels.isEmpty()) {
                this.banWords(words);
                return;
            }
            //Convert channels to IDs
            List<Long> chIDs = new ArrayList<>();
            for (TextChannel channel : channels) {
                chIDs.add(channel.getIdLong());
            }
            //Add the banned words
            for (String w : words) {
                List<Long> ch = this.bannedWords.get(w);
                if (ch != null) {
                    ch.addAll(chIDs);
                } else {
                    this.bannedWords.put(w, chIDs);
                }
            }
        }

        /** @return The status of the autoadmin as an Embed */
        private MessageEmbed toEmbed() {
            EmbedBuilder eb = Launcher.makeEmbedBuilder(
                    Launcher.getGuild(guildID).getName() + " AutoAdmin", null,
                    "The Weebot AutoAdmin moderator settings and status.");
            //TODO
            return eb.build();
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

        @Override
        public boolean dead() {
            return this.dead;
        }
    }

    public AutoAdminCommand() {
        super(
                "AutoAdmin",
                new ArrayList<>(Arrays.asList("aac", "adminbot", "botadmin")),
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
        REMOVEEXEMPT
    }

    /** {@value} */
    private static final String NO_AA_FOUND = "I do not currently have any " +
            "administrative capabilities. Please use this command to enable the Weebot " +
            "AutoAdmin:```aac init```";

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        StringBuilder sb = new StringBuilder();
        String[] args = this.cleanArgs(bot, event);
        if (args.length < 2) {
            sb.append("Please use one of these commands").append("```")
              .append("aac banword <word>\n")
              .append("aac rmwrd <word_index> or aac rmwrd <word>")
              .append("aac seebanned\n").append("aac record <@member> [@member2]\n")
              .append("aac pardon <@member> [@member2]...").append("```");
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
                    bot.setAutoAdmin(null);
                    event.reply("I will no longer moderate text channels.");
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case ADDWORD:
                //Add banned word
                //aac banword <word> [word2]...[#textChannel] [#textChannel_2]...
                if (admin != null) {
                    //Parse words, stop if channel is found
                    String[] words = new String[args.length];
                    for (int i = 2; i < args.length; i++) {
                        if (args[i].startsWith("#")) break;
                        words[i - 2] = args[i];
                    }
                    admin.banWords(words, event.getMessage().getMentionedChannels());
                    event.reply("Words have been added to the banlist",
                                m -> event.deleteMessage()
                    );
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case REMOVEWORD:
                //remove banned words
                //aaac rmwrd <word> [word2]... [#textCahnnel] [#textChannel_2]...
                //aac rmwrd <word_num> [word2_num]...[#textCahnnel] [#textChannel_2]...
                break;
            case SEEWORDS:
                //see banned words (private)
                //aac bwrds
                if (admin != null) {
                    event.privateReply(admin.wordsToEmbed());
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case SEERECORD:
                //See user records
                //aac ir <@member> [@member2]...
                break;
            case CLEARRECORD:
                //Clear user records
                //aac pardon <@member> [@member2]...
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
          .append(" who violate the rules.\n*Required Permissions: Kick & Ban Member*");
        eb.setDescription(sb.toString());
        sb.setLength(0);

        eb.addField("Initialize AutoAdmin", "aac init", true)
          .addField("Disable AutoAdmin", "aac disable\n*Aliases: stop, dis*", true)
          .addBlankField(true);

        sb.append("aac rmwrd <word> [word2]... [#textCahnnel] [#textChannel_2]...\n")
          .append("aac rmwrd <word_num> [word2_num]...")
          .append("[#textCahnnel] [#textChannel_2]...\n")
          .append("*Aliases:unbanword*");
        eb.addField("Ban Word",
                    "aac banword <word> [word2]...[#textChannel] [#textChannel_2]..." +
                            "\n*Alias: addword*",
                    false)
          .addField("Unban word", sb.toString(), false)
          .addField("See Banned Words (in Private Message)",
                    "aac bwords\n*Aliases: bannedwords, seebannedwords, bwrds, words",
                    false);
        sb.setLength(0);

        eb.addField("See Member(s) Infraction Record(s)",
                    "aac ir <@Member> [@member2]...\n*Aliases: userrecord, record*",
                    false)
          .addField("Clear Member(s) Infraction Record(s)",
                    "aac pardon <@member> [@member2]...\n*Alias: clrrec*", false)
          .addBlankField(false);

        return eb.build();
    }
}
