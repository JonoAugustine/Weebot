package com.ampro.main.bot.commands;

import com.ampro.main.Launcher;
import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                        "callsign", "callwith", "callw/", "prefix",
                        "explicit", "expl", "cancuss", "vulgar", "pottymouth",
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
/*
        switch (args[0] /*The command argument*) {
            case "set":
            case "settings":
            case "setting":
                this.listServerSettings(bot, event);
                return;
            case "name":
            case "nickname":
                this.changeNickName(bot, event);
                return;
            case "callsign":
            case "prefix":
                this.callsign(bot, event);
                return;
            case "participate":
            case "activeparticipate":
            case "interrupt":
                this.participate(bot, event);
            case "nsfw":
            case "naughty":
                this.nsfw(bot, event);
                return;
            case "explicit":
            case "expl":
            case "cuss":
                this.explicit(bot, event);
                return;
            default:
                return;
        }
*/
    }
/*
    /**
     * Set or Get {@code EXPLICIT}.
     * @param channel TextChannel called from
     * @param command command invoked
     *
    private void explicit(Weebot bot, BetterMessageEvent event) {
        //Only respond to commands with the appropriate number of args
        switch (command.length) {
            case 1:
                channel.sendMessage(
                        "I am " + (this.EXPLICIT ? "" : "not ") + "NSFW"
                ).queue();
                return;
            case 2:
                switch (command[1]) {
                    case "true":
                        this.EXPLICIT = true;
                        channel.sendMessage("I am now explicit" ).queue();
                        return;
                    case "false":
                        this.EXPLICIT = false;
                        channel.sendMessage("I am now clean" ).queue();
                        return;
                    default:
                        channel.sendMessage("Sorry, " + command[1]
                                + " is not an option. Please use the commands: "
                                + "```" + this.CALLSIGN + "explicit [true/on/false/off]```"
                                + "```" + this.CALLSIGN + "expl [true/on/false/off]```"
                                + "```" + this.CALLSIGN + "cuss [true/on/false/off]```"
                        ).queue();
                        return;
                }
            default:
                channel.sendMessage("Sorry, " + command[1]
                        + " is not an option. Please use the commands: "
                        + "```" + this.CALLSIGN + "explicit [true/on/false/off]```"
                        + "```" + this.CALLSIGN + "expl [true/on/false/off]```"
                        + "```" + this.CALLSIGN + "cuss [true/on/false/off]```"
                ).queue();
        }
    }

    /**
     * Set or Get {@code ACTIVEPARTICIPATE}.
     * @param channel TextChannel called from
     * @param command Command invoked
     *
    private void participate(Weebot bot, BetterMessageEvent event) {
        //Only respond to commands with the appropriate number of args
        switch (command.length) {
            case 1:
                channel.sendMessage(
                        "I will " + (this.ACTIVE_PARTICIPATE ? "" : "not ")
                                + " join in on conversations."
                ).queue();
                return;
            case 2:
                switch (command[1]) {
                    case "true":
                        this.ACTIVE_PARTICIPATE = true;
                        channel.sendMessage(
                                "I will join in on conversations."
                        ).queue();
                        return;
                    case "false":
                        this.ACTIVE_PARTICIPATE= false;
                        channel.sendMessage(
                                "I won't join in on conversations anymore."
                        ).queue();
                        return;
                    default:
                        channel.sendMessage("Sorry, " + command[1]
                                + " is not an option. Please use the commands: "
                                + "```" + this.CALLSIGN +
                                "participate " +
                                "[true/on/false/off]```"
                                + "```" + this.CALLSIGN +
                                "activeparticipate [true/on/false/off]```"
                                + "```" + this.CALLSIGN + "interrupt" +
                                " " +
                                "[true/on/false/off]```"
                        ).queue();
                        return;
                }
            default:
                channel.sendMessage("Sorry, " + command[1]
                        + " is not an option. Please use the commands: "
                        + "```" + this.CALLSIGN + "participate " +
                        "[true/on/false/off]```"
                        + "```" + this.CALLSIGN + "activeparticipate" +
                        " " +
                        "[true/on/false/off]```"
                        + "```" + this.CALLSIGN + "interrupt " +
                        "[true/on/false/off]```"
                ).queue();
        }
    }

    /**
     * Sets the bot's NSFW setting according to message containing true or false. <br>
     * Or sends whether or not the Bot is NSFW.
     * @param channel TextChannel called from
     * @param command Command used to invoke
     *
    private void nsfw(Weebot bot, BetterMessageEvent event) {
        switch (command.length) {
            case 1:
                channel.sendMessage(
                        "I am " + (this.NSFW ? "" : "not ") + "NSFW"
                ).queue();
                return;
            case 2:
                switch (command[1]) {
                    case "true":
                        this.NSFW = true;
                        channel.sendMessage("I am now NSFW" ).queue();
                        break;
                    case "false":
                        this.NSFW = false;
                        channel.sendMessage("I am now SFW" ).queue();
                        break;
                    default:
                        channel.sendMessage("Sorry, " + command[1]
                                + " is not an option. Please use the command: "
                                + "```" + this.CALLSIGN + "nsfw " +
                                "[true/on/false/off]```"
                        ).queue();
                        return;
                }
                break;
            default:
                channel.sendMessage("Sorry, " + String.join(" ", command[1])
                        + " is not an option. Please use the command: "
                        + "```" + this.CALLSIGN + "nsfw " +
                        "[true/on/false/off]```"
                ).queue();
        }
    }

    /**
     * Change the nickname of the bot for this server.
     * @param channel TextChannel called from
     * @param command The command used to call this method
     *
    private void changeNickName(Weebot bot, BetterMessageEvent event) {
        try {
            String newName = String.join(" ", command);
            //Change name on server
            Guild g = Launcher.getGuild(this.GUILD_ID);
            Member self = g.getSelfMember();
            new GuildController(g).setNickname(self, newName).queue();
            //Change internal name
            this.NICKNAME = newName;
            if (!newName.equalsIgnoreCase("weebot"))
                channel.sendMessage("Hmm... " + newName
                        + "... I like the sound of that!").queue();
            else
                channel
                        .sendMessage("Hmm... Weebot... I like the sound of th-- wait!")
                        .queue();
        } catch (InsufficientPermissionException e) {
            channel.sendMessage("I don't have permissions do that :(").queue();
        }
    }

    /**
     * Change or send the callsign (limited to 3 char).
     * @param channel TextChannel called from
     * @param command Command used to invoke
     *
    private void callsign(Weebot bot, BetterMessageEvent event) {
        switch (command.length) {
            case 1:
                //Send back the current callsign
                channel.sendMessage("You can call me with " + this.CALLSIGN
                        + " or @" + this.NICKNAME).queue();
                return;
            case 2:
                //Set a new callsign (if under 3 char)
                if (command[1].length() > 3) {
                    channel.sendMessage(
                            "Please keep the callsign under 4 characters."
                    ).queue();
                    return;
                } else {
                    this.CALLSIGN = command[1];
                    channel.sendMessage("You can now call me with ```" +
                            this.CALLSIGN
                            + "``` or ```@" + this.NICKNAME + "```")
                            .queue();
                    return;
                }
            default:
                channel.sendMessage(
                        "Sorry, I can't understand that command."
                                + "\nYou can change my callsign with these commands:"
                                + "```" + this.CALLSIGN + "prefix " +
                                "new_prefix```"
                                + "```@" + this.NICKNAME + " prefix " +
                                "new_prefix```"
                ).queue();
        }
    }
*/

    @Override
    protected void execute(BetterMessageEvent event) {}
}
