package com.ampro.weebot.commands;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.database.DatabaseManager;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@link Command} that manages the settings for the {@link Weebot}.
 * <p>Contains all commands relating to any {@link Weebot} settings, as such,
 * takes commands leading with the name of any setting.</p>
 *
 * TODO: Banning words
 *
 * @author Jonathan Augustine
 */
public class ManageSettingsCommand extends Command {

    /** Send a list of the Bot's settings. */
    public static final class ShowSettingsCommand extends Command {

        public ShowSettingsCommand() {
            super("Settings"
                    , new ArrayList<>(Arrays.asList("seesettings", "setting", "ssc"))
                    , "See my settings."
                    ,""
                    , true
                    , false
                    , 0
                    , false
                    , false
            );
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        }

        /**
         * Send a list of the Bot's settings.
         * @param bot The {@link Weebot} who called.
         * @param event The {@link BetterMessageEvent} that invoked
         */
        @Override
        protected final void execute(Weebot bot, BetterMessageEvent event) {
            StringBuilder sb = new StringBuilder();
            EmbedBuilder eb = Launcher.makeEmbedBuilder(
                    "Wanna learn about me?",null
                    , "Use *help msc* for help changing settings"
            );
            synchronized (bot) {
                eb.addField("I live here", bot.getGuild().getName(), true);
                boolean b = false;
                if (!bot.getNickname().equalsIgnoreCase("weebot")) {
                    eb.addField("My nickname is", bot.getNickname(), true);
                }
                eb.addField("Call me with",
                            bot.getCallsign() + " or @" + bot.getNickname(), true)
                  .addField("Explicit responses", bot.isExplicit() ? "on" : "off", true)
                  .addField("NSFW Commands", bot.isNSFW() ? "on" : "off", true)
                  .addField("Active Chatbot", bot.canParticipate() ? "on" : "off", true)
                  .addField("Spam Limit", bot.getSpamLimit() + "", true)
                  .addField("You can change any setting like this",
                            "<setting_name> [new_value]\nwhere [new_value] " +
                                    "can be either [true/on/false/off] or [abc123...]",
                            false);
            }
            event.reply(eb.build());
        }

        @Override
        public MessageEmbed getEmbedHelp() {
            return Launcher.makeEmbedBuilder("Show Settings",null,
                                             "See my settings.").build();

        }
    }

    public ManageSettingsCommand() {
        super("Settings"
                , new ArrayList<>(Arrays.asList(
                        "managesettings", "changesettings", "setting",
                        "nickname", "setname", "changename",
                        "callsign", "callwith", "callw", "prefix",
                        "explicit", "expl", "vulgar", "pottymouth",
                        "participate", "activeparticipate", "interrupt",
                        "livebot", "chatbot",
                        "naughty", "nsfw",
                        "spamlimit",
                        "msc"
                ))
                , "See and change my settings."
                ,"[true/false/on/off]"
                , true
                , false
                , 0
                , false
                , false
        );
    }

    /**
     * @param bot The {@link Weebot} which called this command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args;
        synchronized (bot) {
            args = this.cleanArgsLowerCase(bot, event);
        }
        //Small note: when two cases are lined-up (like name and nickname)
        //then both cases go to the next available code
        // (e.g. name and nickname cases both go to this.changeNickname)
        switch (args[0]) {
            case "setname":
            case "changename":
            case "nickname":
                this.changeNickName(bot, event);
                break;
            case "callsign":
            case "prefix":
            case "callwith":
            case "callw":
                this.callsign(bot, event);
                break;
            case "participate":
            case "interrupt":
                this.participate(bot, event);
                break;
            case "explicit":
            case "expl":
            case "vulgar":
            case "pottymouth":
                this.explicit(bot, event);
                break;
            case "nsfw":
            case "naughty":
                this.nsfw(bot, event);
                break;
            case "spamlimit":
                this.spamLimit(bot, event);
                break;
            default:
                return;
        }
        synchronized (Launcher.getDatabase()) {
            DatabaseManager.backUp(Launcher.getDatabase());
        }
    }

    /**
     * View or set the {@link Weebot#explicit} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void explicit(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            //Only respond to commands with the appropriate number of args
            switch (args.length) {
                case 1:
                    //If the command was just the name of the setting
                    event.reply("I am " + (bot.isExplicit() ? "" : "not ") + "explicit.");
                    return;
                case 2:
                    switch (args[1].toLowerCase()) {
                        case "true":
                        case "on":
                        case "yes":
                            if(bot.setExplicit(true))
                                event.reply("I am already explicit :smiling_imp:");
                            else
                                event.reply("I am now explicit :smiling_imp:");
                            return;
                        case "false":
                        case "off":
                        case "of":
                            if(bot.setExplicit(false))
                                event.reply("I am now clean :innocent:");
                            else
                                event.reply("I am already clean :innocent:");
                            return;
                        default:
                            event.reply("Sorry, " + args[1]
                                    + " is not an option. Please use the commands: "
                                    + "```" + bot.getCallsign()
                                    + "<explicit/expl/vulgar/pottymouth> "
                                    + "[true/on/false/off]```");

                            return;
                    }
                default:
                    event.reply("Sorry, " + String.join(" ", args).substring(args[0].length())
                            + " is not an option. Please use the commands: "
                            + "```" + bot.getCallsign()
                            + "<explicit/expl/vulgar/pottymouth> "
                            + "[true/on/false/off]```");
                    break;
            }
        }
    }

    /**
     * View or set the {@link Weebot#ACTIVE_PARTICIPATE} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void participate(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            //Only respond to commands with the appropriate number of args
            switch (args.length) {
                case 1:
                    event.reply("I will " + (bot.canParticipate() ? "" : "not ") + " join in on conversations.");
                    return;
                case 2:
                    switch (args[1]) {
                        case "true":
                        case "on":
                            if(bot.setACTIVE_PARTICIPATE(true))
                                event.reply("I can already join conversations");
                            else
                                event.reply("I will join conversations :grin:");
                            return;
                        case "false":
                        case "off":
                        case "of":
                            bot.setACTIVE_PARTICIPATE(false);
                            event.reply("I won't join conversations anymore.");
                            return;
                        default:
                            event.reply("Sorry, " + args[1]
                                    + " is not an option."
                                    + " Please use the commands: " + "```"
                                    + bot.getCallsign()
                                    + "<participate/interrupt> [true/on/false/off]```"
                            );
                            return;
                    }
                default:
                    event.reply("Sorry, " + String.join(" ", args)
                            .substring(args[0].length())
                            + " is not an option. Please use the commands: "
                            + "```" + bot.getCallsign()
                            + "<participate/interrupt> [true/on/false/off]```"
                    );
                    break;
            }
        }
    }

    /**
     * View or set the {@link Weebot#NSFW} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void nsfw(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = this.cleanArgsLowerCase(bot, event);

            switch (args.length) {
                case 1:
                    event.reply("I am " + (bot.isNSFW() ? "" : "not ") + "NSFW");
                    return;
                case 2:
                    switch (args[1]) {
                        case "true":
                        case "on":
                            if(bot.setNSFW(true))
                                event.reply("I am already NSFW :wink:");
                            else
                                event.reply("I am now NSFW :wink:");
                            break;
                        case "false":
                        case "off":
                        case "of":
                            if(!bot.setNSFW(false))
                                event.reply("I am already SFW :innocent:");
                            else
                                event.reply("I am now SFW :innocent:");
                            break;
                        default:
                            event.reply("Sorry, " + args[1] + " is not an option." + " Please use the command: " + "```" + bot.getCallsign() + "<nsfw/naughty>" + "[true/on/false/off]```");

                            return;
                    }
                    break;
                default:
                    event.reply("Sorry, " + String.join(" ", args).substring(args[0].length()) + " is not an option. Please use the command:```" + bot.getCallsign() + "<nsfw/naughty>" + "[true/on/false/off]```");
                    break;
            }
        }
    }

    /**
     * View or set the {@link Weebot#callsign} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void callsign(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            switch (args.length) {
                case 1:
                    //Send back the current callsign
                    event.reply("You can call me with " + bot.getCallsign()
                            + " or @" + bot.getNickname());
                    return;
                case 2:
                    //Set a new callsign (if under 3 char)
                    if(args[1].length() > 3) {
                        event.reply("Please keep the callsign under 4 characters.");
                        return;
                    } else {
                        bot.setCallsign(args[1]);
                        event.reply("You can now call me with ```" + args[1]
                                + "<command> ```or```@" + bot.getNickname() + "```");

                        return;
                    }
                default:
                    event.reply("Sorry, " + String.join(" ", args)
                            .substring(args[0].length())
                            + " is not an option. Please use the command:```"
                            + bot.getCallsign()
                            + "<callsign/prefix/callwith/callw> [new_prefix]```");
                    break;
            }
        }
    }

    /**
     * View or set the {@link Weebot#nickname} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void changeNickName(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = this.cleanArgs(bot, event);
            if(args.length < 2) {
                event.reply("Please provide a new name.```" + bot.getCallsign() + "<" + args[0] + "> <new name>```");
                return;
            }
            try {
                String newName = String.join(" ", args).substring(args[0].length()).trim();
                System.out.println(newName);
                //Change name on server
                Guild g = Launcher.getGuild(bot.getGuildID());
                Member self = g.getSelfMember();
                new GuildController(g).setNickname(self, newName).queue();
                //Change internal name
                bot.setNickname(newName);
                if(!newName.equalsIgnoreCase("weebot"))
                    event.reply("Hmm... " + newName + "... I like the sound of that!");
                else
                    event.reply("Hmm... Weebot... I like the sound of th-- wait!");
            } catch (InsufficientPermissionException e) {
                event.reply("I don't have permissions do that :pensive:");
            }
        }
    }

    /**
     * Set or get the limit on bot spam.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void spamLimit(Weebot bot, BetterMessageEvent event) {
        synchronized (bot) {
            String[] args = this.cleanArgsLowerCase(bot, event);
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
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder sb = new StringBuilder();

        eb.setColor(new Color(0x31FF00))
          .setAuthor("Weebot", null, Launcher.getJda().getSelfUser().getAvatarUrl())
          .setThumbnail(Launcher.getJda().getSelfUser().getAvatarUrl())
          .setFooter("Run by Weebot", Launcher.getJda().getSelfUser().getAvatarUrl());

        eb.setTitle("Weeb(B)ot Settings")
          .addField("Change My in-Sever NickName",
                    "setname <new nickname>\n*Aliases*: nickname, changename",
                    false);

        sb.append("callw <new_callsign>")
          .append("*Aliases*: callsign, callwith, prefix")
          .append("*Note*: The prefix must be under 4 characters long");
        eb.addField("Change my Callsign/Prefix", sb.toString(), false);
        sb.setLength(0);

        eb.addField("(Dis)Allow Bot to Use Explicit Language",
                    "expl on/off\n*Aliases:*explicit, vulgar, pottymouth",
                    false)
          .addField("(Dis)Allow the bot to use NSFW commands",
                    "nsfw on/off\n*Alias*: naughty",
                    false)
          .addField("Server-wide word bans", "(*Under construction*)", false)
          .addField("(Dis)Allow the bot to respond to actions not directed to it",
                    "(*Under construction*)", false);

        return eb.build();
    }

}
