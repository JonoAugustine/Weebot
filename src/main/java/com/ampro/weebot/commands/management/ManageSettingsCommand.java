package com.ampro.weebot.commands.management;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.miscellaneous.SpamCommand;
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

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@link Command} that manages the settings for the {@link Weebot}.
 * <p>Contains all commands relating to any {@link Weebot} settings, as such,
 * takes commands leading with the name of any setting.</p>
 *
 * TODO Seperate into subclasses
 *
 * @author Jonathan Augustine
 */
public class ManageSettingsCommand extends Command {

    /** Send a list of the Bot's settings. */
    public final class ShowSettingsCommand extends Command {

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

    /** View or set the {@link Weebot#nickname} setting. */
    public final class ChangeNameCommand extends Command {
        ChangeNameCommand() {
            super(
                    "ChangeName",
                    new ArrayList<>(Arrays.asList("nickname", "setname", "cnc")),
                    "Change my Server Nickname",
                    "<new nickname>",
                    true,
                    false,
                    0,
                    false,
                    false
            );
            this.userPermissions = new Permission[]{Permission.NICKNAME_MANAGE};
        }

        @Override
        protected void execute(Weebot bot, BetterMessageEvent event) {
            String[] args = this.cleanArgs(bot, event);
            if(args.length < 2) {
                event.reply("Please provide a new name.```" + bot.getCallsign()
                                    + args[0] + " <new name>```");
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

        @Override
        public MessageEmbed getEmbedHelp() {
            StringBuilder sb = new StringBuilder()
                    .append("*Required Permission: Manage Nicknames*\n")
                    .append("cnc <new name>\n")
                    .append("*Aliases: nickname, setname, changename*");
            return Launcher.makeEmbedBuilder("Change My Nickname", null,
                                                        sb.toString())
                           .build();
        }

    }

    /** View or set the {@link Weebot#callsign} setting. */
    public final class ChangeCallsignCommand extends Command {
        ChangeCallsignCommand() {
            super(
                    "ChangeCallsign",
                    new ArrayList<>(
                            Arrays.asList("callsign", "callwith", "callw", "prefix")),
                    "Change the Prefix used to call me (Under 4 characters)",
                    "<prefix>",
                    true,
                    false,
                    0,
                    false,
                    false
            );
            this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        }

        @Override
        protected void execute(Weebot bot, BetterMessageEvent event) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            switch (args.length) {
                case 1:
                    //Send back the current callsign
                    event.reply(
                            "You can call me with " + bot.getCallsign()
                                    + " or @" + bot.getNickname());
                    return;
                case 2:
                    //Set a new callsign (if under 3 char)
                    if(args[1].length() > 3) {
                        event.reply("Please keep the callsign under 4 characters.");
                        return;
                    } else {
                        bot.setCallsign(args[1]);
                        event.reply("You can now call me with ```" + args[1] +
                                            "<command> ```or```@" + bot
                                .getNickname() + "```");

                        return;
                    }
                default:
                    event.reply("Sorry, " + String.join(" ", args).substring(
                            args[0].length()) + " is not an option. Please use the command:```"
                                        + bot.getCallsign() + "<prefix> [new_prefix]```");
                    break;
            }
        }

        @Override
        public MessageEmbed getEmbedHelp() {
            StringBuilder sb = new StringBuilder()
                    .append("Change the symbol used to call Weebot commands.\n")
                    .append("*Required Permissions: Administrator\n\n")
                    .append("prefix <newPrefix>\n")
                    .append("*Aliases: callsign, callwith, callw, prefix*\n")
                    .append("**The Prefix must be under 4 characters:**\n")
                    .append(";;;; X | ;;; O\n");
            return Launcher.makeEmbedBuilder("Change My Callsign", null,
                                                        sb.toString())
                                      .build();
        }

    }

    public static final class SetExplicitCommand extends Command {
        SetExplicitCommand() {
            super(
                    "setexplicit",
                    new ArrayList<>(
                            Arrays.asList("explicit", "expl", "vulgar", "pottymouth")),
                    "(Dis)allow explicit commands.",
                    "<expl> <on/off>",
                    true,
                    false,
                    0,
                    false,
                    false
            );
            this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        }

        @Override
        protected void execute(Weebot bot, BetterMessageEvent event) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            //Only respond to commands with the appropriate number of args
            switch (args.length) {
                case 1:
                    //If the command was just the name of the setting
                    event.reply("*I am currently " + (bot.isExplicit() ? "" : "not ") +
                                        "explicit.*");
                    return;
                case 2:
                    switch (args[1].toLowerCase()) {
                        case "true":
                        case "on":
                        case "yes":
                            if(bot.setExplicit(true))
                                event.reply("*I am already explicit* :smiling_imp:");
                            else
                                event.reply("*I am now explicit *:smiling_imp:");
                            return;
                        case "false":
                        case "off":
                        case "of":
                            if(bot.setExplicit(false))
                                event.reply("*I am now clean* :innocent:");
                            else
                                event.reply("*I am already clean* :innocent:");
                            return;
                        default:
                            event.reply("*Sorry, " + args[1]
                                                + " is not an option. Please use the " +
                                                "commands*: "
                                                + "```" + bot.getCallsign()
                                                + "<explicit/expl/vulgar/pottymouth> "
                                                + "[true/on/false/off]```");

                            return;
                    }
                default:
                    event.reply("*Sorry, " + String.join(" ", args)
                                                   .substring(args[0].length())
                                        + " is not an option. Please use the commands:* "
                                        + "```" + bot.getCallsign()
                                        + "<expl> "
                                        + "[true/on/false/off]```");
                    break;
            }
        }

        @Override
        public MessageEmbed getEmbedHelp() {
            StringBuilder sb = new StringBuilder()
                    .append("Allow or disallow explicit commands.\n")
                    .append("*Required Permissions: Administrator*\n")
                    .append("<expl> <on/off/true/false>\n")
                    .append("*Aliases: setexplicit, explicit, vulgar, pottymouth*");
            return Launcher.makeEmbedBuilder("(Dis)allow my Explicit commands",
                                             null, sb.toString()).build();
        }

    }

    public static final class SetNSFWCommand extends Command {
        SetNSFWCommand() {
            super(
                    "SetNSWF",
                    new ArrayList<>(
                            Arrays.asList("nsfw", "naughty")),
                    "(Dis)allow NSFW commands.",
                    "<nsfw> <on/off>",
                    true,
                    false,
                    0,
                    false,
                    false
            );
            this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        }

        @Override
        protected void execute(Weebot bot, BetterMessageEvent event) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            switch (args.length) {
                case 1:
                    event.reply("*I am " + (bot.isNSFW() ? "" : "not ") + "NSFW*");
                    return;
                case 2:
                    switch (args[1]) {
                        case "true":
                        case "on":
                            if(bot.setNSFW(true))
                                event.reply("*I am already NSFW* :wink:");
                            else
                                event.reply("*I am now NSFW* :wink:");
                            break;
                        case "false":
                        case "off":
                        case "of":
                            if(!bot.setNSFW(false))
                                event.reply("*I am already SFW* :innocent:");
                            else
                                event.reply("*I am now SFW* :innocent:");
                            break;
                        default:
                            event.reply("*Sorry, " + args[1] + " is not an option."
                                                + " Please use the command:* " + "```"
                                                + bot.getCallsign() + "<nsfw/naughty>"
                                                + "[true/on/false/off]```");
                            return;
                    }
                    break;
                default:
                    event.reply("*Sorry, " + String.join(" ", args)
                                                  .substring(args[0].length())
                                        + " is not an option. Please use the command:*```"
                                        + bot.getCallsign() + "<nsfw/naughty>"
                                        + "[true/on/false/off]```");
                    break;
            }
        }

        @Override
        public MessageEmbed getEmbedHelp() {
            StringBuilder sb = new StringBuilder()
                    .append("Allow or disallow NSFW commands.\n")
                    .append("*Required Permissions: Administrator*\n")
                    .append("<nsfw> <on/off/true/false>\n")
                    .append("*Alias: naughty*");
            return Launcher.makeEmbedBuilder("(Dis)allow my NSFW commands",
                                             null, sb.toString()).build();
        }

    }

    public static final class SetParticipateCommand extends Command {
        SetParticipateCommand() {
            super(
                "SetParticipate",
                new ArrayList<>(
                        Arrays.asList("participate", "interrupt", "parti", "livebot")),
                "(Dis)allow Active Participation.",
                "<parti> <on/off>",
                true,
                false,
                0,
                false,
                false
            );
            this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        }

        @Override
        protected void execute(Weebot bot, BetterMessageEvent event) {
            String[] args = this.cleanArgsLowerCase(bot, event);
            //Only respond to commands with the appropriate number of args
            switch (args.length) {
                case 1:
                    event.reply("*I can " + (bot.canParticipate() ? "" : "not ")
                                        + " join in on conversations*.");
                    return;
                case 2:
                    switch (args[1]) {
                        case "true":
                        case "on":
                            if(bot.setActiveParticipate(true))
                                event.reply("*I can already join conversations*");
                            else
                                event.reply("*I will join conversations* :grin:");
                            return;
                        case "false":
                        case "off":
                        case "of":
                            bot.setActiveParticipate(false);
                            event.reply("*I won't join conversations anymore*.");
                            return;
                        default:
                            event.reply("*Sorry, " + args[1]
                                                + " is not an option."
                                                + " Please use the commands*: " + "```"
                                                + bot.getCallsign()
                                                + "<participate/interrupt> [true/on/false/off]```"
                            );
                            return;
                    }
                default:
                    event.reply("*Sorry, " + String.join(" ", args)
                                                  .substring(args[0].length())
                                        + " is not an option. Please use the commands*: "
                                        + "```" + bot.getCallsign()
                                        + "<participate/interrupt> [true/on/false/off]```"
                    );
                    break;
            }
        }

        @Override
        public MessageEmbed getEmbedHelp() {
            StringBuilder sb = new StringBuilder()
                    .append("(Dis)Allow the bot to respond ")
                    .append("to actions not directed to it.\n")
                    .append("*Required Permissions: Administrator*\n")
                    .append("<parti> <on/off/true/false>\n")
                    .append("*Aliases: participate, interrupt, livebot*");
            return Launcher.makeEmbedBuilder("(Dis)allow AI Bot Participation",
                                             null, sb.toString()).build();
        }

    }

    public ManageSettingsCommand() {
        super("Settings"
                , new ArrayList<>(Arrays.asList(
                        "managesettings", "changesettings", "setting",
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
        this.children = new Command[]{
                new ShowSettingsCommand(), new ChangeNameCommand(),
                new ChangeCallsignCommand(), new SetExplicitCommand(),
                new SetNSFWCommand(), new SetParticipateCommand()};

    }

    /**
     * Check if the event is one of the {@link Command#children} of the Settings Command.
     * @param bot The {@link Weebot} that called the command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event)) {
            String name = cleanArgsLowerCase(bot, event)[0];
            for (Command c : this.children) {
                if(c.isCommandFor(name)) {
                    c.run(bot, event);
                    return;
                }
            }
            this.execute(bot, event);
        }
    }

    /**
     * Show Settings Command
     * @param bot The {@link Weebot} which called this command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        new ShowSettingsCommand().run(bot, event);
    }

    @Override
    public boolean isCommandFor(String name) {
        boolean is = super.isCommandFor(name);
        if (!is) {
            for (Command c : this.children) {
                if (c.isCommandFor(name)) {
                    return true;
                }
            }
        }
        return is;
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder()
                .setTitle("Manage Weebot Settings");

        StringBuilder sb = new StringBuilder();
        if (this.userPermissions.length != 0) {
            sb.append("Required User Permissions:* ");
            for (Permission p : this.userPermissions)
                sb.append(p.getName()).append(", ");
            sb.setLength(sb.length() - 2);
            sb.append("*");
            eb.setDescription(sb.toString());
        }

        eb.addField("Change My in-Sever NickName",
                    "setname <new nickname>\n*Aliases*: nickname, changename",
                    false);
        sb.append("callw <new_callsign>\n")
          .append("*Aliases*: callsign, callwith, prefix\n")
          .append("*Note*: The prefix must be under 4 characters long");
        eb.addField("Change my Callsign/Prefix", sb.toString(), false);
        sb.setLength(0);
        eb.addField("(Dis)Allow Bot to Use Explicit Language",
                    "expl on/off\n*Aliases:*explicit, vulgar, pottymouth",
                    false)
          .addField("(Dis)Allow the bot to use NSFW commands",
                    "nsfw on/off\n*Alias*: naughty",
                    false)
          .addField("(Dis)Allow the bot to respond to actions not directed to it",
                    "(*Under construction*)", false);

        return eb.build();
    }

}
