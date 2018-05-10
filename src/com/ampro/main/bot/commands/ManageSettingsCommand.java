package com.ampro.main.bot.commands;

import com.ampro.main.Launcher;
import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@link Command} that manages the settings for the {@link Weebot}.
 * <p>Contains all commands relating to any {@ling Weebot} settings, as such,
 * takes commands leading with the name of any setting.</p>
 *
 * @author Jonathan Augustine
 */
public class ManageSettingsCommand extends Command {

    public ManageSettingsCommand() {
        super("Settings"
                , new ArrayList<>(Arrays.asList(
                        "managesettings", "changesettings", "setting",
                        "nickname", "setname", "changename",
                        "callsign", "callwith", "callw", "prefix",
                        "explicit", "expl", "vulgar", "pottymouth",
                        "participate", "activeparticipate", "interrupt",
                        "livebot", "chatbot",
                        "naughty", "nsfw"
                ))
                , "See and change my settings."
                ,"[true/false/on/off]"
                , true
                , false
                , 0
                , false
        );
    }

    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if(this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : SettingsCommand");
            thread.start();
        }
    }

    /**
     * TODO settings controls
     * @param bot The {@link Weebot} which called this command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        //Small note: when two cases are lined-up (like name and nickname)
        //then both cases go to the next available code
        // (e.g. name and nickname cases both go to this.changeNickname)
        switch (args[0].toLowerCase()) {
            case "set":
            case "settings":
            case "setting":
                this.listServerSettings(bot, event);
                return;
            case "setname":
            case "changename":
            case "nickname":
                this.changeNickName(bot, event);
                return;
            case "callsign":
            case "prefix":
            case "callwith":
            case "callw":
                this.callsign(bot, event);
                return;
            case "participate":
            case "interrupt":
                this.participate(bot, event);
            case "explicit":
            case "expl":
            case "vulgar":
            case "pottymouth":
                this.explicit(bot, event);
                return;
            case "nsfw":
            case "naughty":
                this.nsfw(bot, event);
                return;
            default:
                return;
        }

    }

    /**
     * Send a list of the Bot's settings.
     * @param bot The {@link Weebot} who called.
     * @param event The {@link BetterMessageEvent} that invoked
     */
    private void listServerSettings(Weebot bot, BetterMessageEvent event) {
        String out = "Wanna learn about me?";

        out += "\n\n```";
        out += "I live here: " + Launcher.getGuild(bot.getGuildID()).getName();
        out += "\n";
        out += "I now go by: " + bot.getNickname();
        out += "\n";
        out += "Call me with: " + bot.getCallsign()+ " or @" +bot.getNickname();
        out += "\n";
        out += "I am " + (bot.isExplicit() ? "" : "not ") + "explicit";
        out += "\n";
        out += "I " + (bot.isNSFW() ? "am " : "not ") + "NSFW";
        out += "\n";
        out += "I " + (bot.canParticipate() ? "" : "don't ")
                + "join in on some conversations where I'n not called.";
        out += "```\n";
        out += "You can change any setting like this:" +
                "```<setting_name> [new_value]``` where ``[new_value]`` " +
                "can be either ``[true/on/false/off]`` or ``[abc...]``";
        //out += "\n";
        //out += "\n";
        out += "```";

        event.reply(out);
    }

    /**
     * View or set the {@link Weebot#explicit} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void explicit(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        //Only respond to commands with the appropriate number of args
        switch (args.length) {
            case 1:
                //If the command was just the name of the setting
                event.reply("I am " + (bot.isExplicit() ? "" : "not ")
                        + "explicit."
                );
                return;
            case 2:
                switch (args[1].toLowerCase()) {
                    case "true":
                    case "on":
                    case "yes":
                        if (bot.setExplicit(true))
                            event.reply("I am already explicit :smiling_imp:");
                        else
                            event.reply("I am now explicit :smiling_imp:");
                        return;
                    case "false":
                    case "off":
                    case "of":
                        if (bot.setExplicit(false))
                            event.reply("I am now clean :innocent:");
                        else
                            event.reply("I am already clean :innocent:");
                        return;
                    default:
                        event.reply("Sorry, " + args[1]
                                + " is not an option. Please use the commands: "
                                + "```" + bot.getCallsign()
                                + "<explicit/expl/vulgar/pottymouth> "
                                + "[true/on/false/off]```"
                        );
                        return;
                }
            default:
                event.reply("Sorry, " + String.join(" ", args)
                                            .substring(args[0].length())
                        + " is not an option. Please use the commands: "
                        + "```" + bot.getCallsign()
                        + "<explicit/expl/vulgar/pottymouth> "
                        + "[true/on/false/off]```"
                );
                return;
        }
    }

    /**
     * View or set the {@link Weebot#ACTIVE_PARTICIPATE} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void participate(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        //Only respond to commands with the appropriate number of args
        switch (args.length) {
            case 1:
                event.reply(
                        "I will " + (bot.canParticipate() ? "" : "not ")
                                + " join in on conversations."
                );
                return;
            case 2:
                switch (args[1]) {
                    case "true":
                    case "on":
                        if (bot.setACTIVE_PARTICIPATE(true))
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
                        event.reply("Sorry, " + args[1] + " is not an option."
                                + " Please use the commands: "
                                + "```" + bot.getCallsign() +
                                "<participate/interrupt> [true/on/false/off]```"
                        );
                        return;
                }
            default:
                event.reply("Sorry, " + String.join(" ", args)
                                             .substring(args[0].length())
                        + " is not an option. Please use the commands: "
                        + "```" + bot.getCallsign() +
                        "<participate/interrupt> [true/on/false/off]```"
                );
                return;
        }
    }

    /**
     * View or set the {@link Weebot#NSFW} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void nsfw(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        switch (args.length) {
            case 1:
                event.reply(
                        "I am " + (bot.isNSFW() ? "" : "not ") + "NSFW"
                );
                return;
            case 2:
                switch (args[1]) {
                    case "true":
                    case "on":
                        if (bot.setNSFW(true))
                            event.reply("I am already NSFW :wink:");
                        else
                            event.reply("I am now NSFW :wink:");
                        break;
                    case "false":
                    case "off":
                    case "of":
                        if (bot.setNSFW(false))
                            event.reply("I am already SFW :innocent:");
                        else
                            event.reply("I am now SFW :innocent:");
                        break;
                    default:
                        event.reply("Sorry, " + args[1] + " is not an option."
                                + " Please use the command: " + "```"
                                + bot.getCallsign() + "<nsfw/naughty>" +
                                "[true/on/false/off]```"
                        );
                        return;
                }
                break;
            default:
                event.reply("Sorry, " + String.join(" ", args)
                                                .substring(args[0].length())
                        + " is not an option. Please use the command:```"
                        + bot.getCallsign() + "<nsfw/naughty>" +
                        "[true/on/false/off]```"
                );
                return;
        }
    }

    /**
     * View or set the {@link Weebot#callsign} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void callsign(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        switch (args.length) {
            case 1:
                //Send back the current callsign
                event.reply(
                        "You can call me with " + bot.getCallsign()
                        + " or @" + bot.getNickname()
                );
                return;
            case 2:
                //Set a new callsign (if under 3 char)
                if (args[1].length() > 3) {
                    event.reply(
                            "Please keep the callsign under 4 characters."
                    );
                    return;
                } else {
                    bot.setCallsign(args[1]);
                    event.reply(
                            "You can now call me with ```" + args[1]
                            + "<command> ```or```@" + bot.getNickname() + "```"
                    );
                    return;
                }
            default:
                event.reply("Sorry, " + String.join(" ", args)
                        .substring(args[0].length())
                        + " is not an option. Please use the command:```"
                        + bot.getCallsign() +
                        "<callsign/prefix/callwith/callw> [new_prefix]```"
                );
                return;
        }
    }

    /**
     * View or set the {@link Weebot#nickname} setting.
     * @param bot The {@link Weebot} to view or modify.
     * @param event The {@link BetterMessageEvent} that invoked this command.
     */
    private void changeNickName(Weebot bot, BetterMessageEvent event) {
        String[] args = this.cleanArgs(bot, event);
        try {
            String newName = String.join(" ", args)
                    .substring(args[0].length());
            //Change name on server
            Guild g = Launcher.getGuild(bot.getGuildID());
            Member self = g.getSelfMember();
            new GuildController(g).setNickname(self, newName).queue();
            //Change internal name
            bot.setNickname(newName);
            if (!newName.equalsIgnoreCase("weebot"))
                event.reply("Hmm... " + newName
                        + "... I like the sound of that!"
                );
            else
                event.reply("Hmm... Weebot... I like the sound of th-- wait!");
        } catch (InsufficientPermissionException e) {
            event.reply("I don't have permissions do that :(");
        }
    }

    @Override
    protected void execute(BetterMessageEvent event) {}
}
