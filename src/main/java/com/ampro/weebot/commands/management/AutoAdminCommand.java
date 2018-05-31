package com.ampro.weebot.commands.management;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Managemnet interface for changing the bot's admin rules and capabilities
 * TODO Exempted IDs
 * TODO Kick/ban thresh
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

            UserRecord(User user, Guild guild) {
                this.userID  = user.getIdLong();
                this.guildID = guild.getIdLong();
                this.wordInfractions = new ConcurrentHashMap<>();
            }

            /**
             * Add a word infraction to the User's record.
             * @param infs The word infractions to add
             */
            void addWordInfractions(String...infs) {
                for (String inf : infs) {
                    Integer n = wordInfractions.get(inf);
                    wordInfractions.put(inf, n == null ? 1 : n++);
                }
            }

            /**
             * Add a word infraction to the User's record.
             * @param infs The word infractions to add
             */
            void addWordInfraction(Collection<String> infs) {
                for (String inf : infs) {
                    Integer n = wordInfractions.get(inf);
                    wordInfractions.put(inf, n == null ? 1 : n + 1);
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
                  .append(this.wordInfractions.size());
                eb.setDescription(sb.toString());
                sb.setLength(0);

                if (!wordInfractions.isEmpty()) {
                    sb.setLength(0);
                    for (Map.Entry<String, Integer> e : wordInfractions.entrySet()) {
                        sb.append("\"").append(e.getKey()).append("\" : used ")
                          .append(e.getValue()).append(" times.").append("\n");
                    }
                    eb.addField("Banned Word Usages", sb.toString(), false);
                }

                return eb.build();
            }

            String memberName() {
                return Launcher.getGuild(guildID)
                               .getMemberById(userID).getEffectiveName();
            }

        }

        private boolean dead;

        private final Long guildID;

        /** Map of String keys to Channel ID list values */
        final ConcurrentHashMap<String, List<Long>> bannedWords;

        /* User records */
        private final ConcurrentHashMap<Long, UserRecord> userRecords;

        /* Exempted Roles & users TODO*/
        private final List<Long> exemptUsers;
        private final List<Long> exemptRoles;

        /** Kick threshold TODO*/
        int kickThresh;
        /** Soft-ban threshold TODO*/
        int sofThresh;
        /** Hard-ban threshold TODO*/
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
            if (dead) return;
            Member member = event.getMember();
            if (member.isOwner()
                || member.hasPermission(Permission.ADMINISTRATOR)) return;
            if (exempt(event.getMember())) return;

            String content = event.toString();
            List<String> b = checkWords(content, event.getTextChannel());
            //Respond to word infractions
            if (!b.isEmpty()) {
                event.deleteMessage();
                UserRecord rec = userRecords.get(member.getUser().getIdLong());
                if(rec == null) {
                    rec = new UserRecord(member.getUser(), event.getGuild());
                    userRecords.put(member.getUser().getIdLong(), rec);
                }

                rec.addWordInfraction(b);

                //Check thresholds TODO
                //Act
                //  Warn user
                //  Kick/sban/ban

                event.privateReply((infractionEmbed(event, b.toString())));
            }
        }

        /**
         * Check if a Member has been exempted from AutoAdmin tracking.
         * @param member The member to check
         * @return {@code false} if the Member is not in
         *          {@link AutoAdmin#exemptUsers} or {@link AutoAdmin#exemptRoles}.
         */
        private boolean exempt(Member member) {
            for (Role r : member.getRoles()) {
                if (exemptRoles.contains(r.getIdLong()))
                    return true;
            }
            return exemptUsers.contains(member.getUser().getIdLong());
        }

        /**
         * Clean an entire {@link TextChannel#getHistory()} on banned words.
         * @param channel The TextChannel to clean
         * @return -1 if the bot cannot view channel history <br>
         *         -2 if the bot cannot manage messages      <br>
         *          1 otherwise <br>
         */
        private boolean clean(TextChannel channel) {
            //TODO
            return false;
        }

        /**
         * Clean an entire {@link Guild Guild's} {@link TextChannel TextChannels} of
         * banned words.
         * @param guild The guild to clean
         * @return A {@link Map} of uncleaned TextChannels linked to an err code <br>
         *     -1 if the bot cannot view channel history <br>
         *     -2 if the bot cannot manage messages      <br>
         */
        private Map<TextChannel, Integer> cleanGuild(Guild guild) {
            Map<TextChannel, Integer> out = new TreeMap<>();
            //TODO
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
            for (Map.Entry<String, List<Long>> entry : bwords) {
                if (input.contains(entry.getKey())) {
                    if(entry.getValue().isEmpty()
                            || entry.getValue().contains(channel.getIdLong())) {

                        badWords.add(input.substring(input.indexOf(entry.getKey()),
                                                     entry.getKey().length())
                        );
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
                if (!ids.isEmpty()) ids.clear();
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
                chs.removeIf( (Long l) -> inIds.contains(l));
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
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder()
                                      .setColor(new Color(0xFB1800));

            eb.setTitle("AutoAdmin Caught an Infraction!");
            StringBuilder sb = new StringBuilder()
                    .append("In Server: ")
                    .append(event.getGuild().getName()).append("\nIn Channel:  ")
                    .append(event.getTextChannel().getName()).append("\nAt ")
                    .append(event.getCreationTime()
                                 .format(DateTimeFormatter.ofPattern("d-M-y hh:mm:ss")));
            eb.setDescription(sb.toString());
            sb.setLength(0);
            eb.addField("Infraction: " + inf.toString(), event.toString(), false);
            return eb.build();
        }

        /** @return The status of the autoadmin as an Embed */
        private MessageEmbed toEmbed() {
            EmbedBuilder eb = Launcher.makeEmbedBuilder(
                    Launcher.getGuild(guildID).getName() + " AutoAdmin", null,
                    "The Weebot AutoAdmin moderator settings and status.");
            //TODO
            //Kick and ban threshold
            //Exempted Roles
            //Members with records
            //Banned words

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
                    admin.dead = true;
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
                //Parse words, stop if channel is found
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
                    event.getMessage().getMentionedMembers().forEach(m -> {
                        if (admin.userRecords.remove(m.getUser().getIdLong()) != null)
                        m.getUser().openPrivateChannel().queue( c -> {
                            sb.append("*You have been pardoned by* **")
                              .append(event.getMember().getEffectiveName())
                              .append("** *in the* **")
                              .append(event.getGuild().getName())
                              .append("** *server. All previous rule infractions have")
                              .append(" been deleted*");
                            c.sendMessage(sb.toString()).queue();
                            sb.setLength(0);
                        });
                    });
                    event.reply("User records have been cleared.");
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
                //aac sk <#> TODO
                break;
            case SETBANTHRESH:
                //aac bk <#> TODO
                break;
            case ADDEXEMPT:
                //aac ex <@> [@2]... TODO
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
                    event.reply("The Members and|or Roles are now exempt from AutoAdmin.");
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case REMOVEEXEMPT:
                //aac rmex <@> [@2]... TODO
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
                    sb.append("The Members and|or Roles are no longer exempt ")
                      .append("from AutoAdmin.");
                    event.reply(sb.toString());
                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case CLEANCHANNEL:
                //aac clean <#> [#2]... TODO
                if (admin != null) {

                } else {
                    event.reply(NO_AA_FOUND);
                }
                break;
            case CLEANGUILD:
                //aac fc
                if (admin != null) {

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
            case "exempt":
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

        eb.addField("Clean TextChannels of Banned Words",
                    "aac clean <#TextChannel> [#TextChannel_2]...\n*Alias: cleanse*",
                    false)
          .addField("Clean All TextChannels of Banned Words",
                    "aac fc\n*Aliases: fullclean, guildclean, gclean*", false)
          .addBlankField(false);

        //Infractions
        eb.addField("Set Number of Infractions to Kick",
                    "aac sk <number>\n*Aliases: setkickthresh, setkick*", true)
          .addField("Set Number of Infractions to Ban",
                    "aac sb <number>\n*Aliases: setbanthresh, setban*", true);

        eb.addField("See Member Infraction Record(s)",
                    "aac ir <@Member> [@member2]...\n*Aliases: userrecord, record*",
                    true)
          .addField("Clear Member Infraction Record(s)",
                    "aac pardon <@member> [@member2]...\n*Alias: clrrec*", true);

        sb.append("aac ex <@Member> [@member2]...").append("\naac ex <@Role> [@Role2]...")
          .append("\n*Aliases: exempt, immune*");
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
