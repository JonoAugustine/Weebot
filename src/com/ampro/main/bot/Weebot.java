/**
 *  Copyright 2018 Jonathan Augustine, Daniel Ernst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ampro.main.bot;

import com.ampro.main.Launcher;
import com.ampro.main.game.Game;
import com.ampro.main.game.Player;
import com.ampro.main.listener.events.BetterEvent;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Weebot connected to a single {@code net.dv8tion.entities.Guild}. <br>
 * Each Weebot is assigned a {@code java.lang.String} consisting of the
 * hosting Guild's unique ID + "W" (e.g. "1234W") <br>
 * ? Should User relation to Weebot be gloabal or local <br>
 * ? This would likelly invole a User wrapper class to keep track of the relationship<br>
 * ? Though that would probably be needed anyway if we are to implement the<br>
 * ? User-bot good/bad relationship meter thing (which I really wannt to)<br>
 *
 * @author Jonathan Augsutine
 *
 */
public class Weebot implements Comparable<Weebot> {

	//General Static Info/Settings

	//Sever Specific Info
	/**
	 * The server
	 * (If the bot lives in a Guild/Server rather than a priv chat)
	 */
	private final Guild  GUILD;
	/* Name of the server/Guild */
	private final String SERVERNAME;
	/** Unique ID long of the Guild */
	private final String BOT_ID;

	//Bot Information
	/** The Guild member that is this */
	private final Member SELF;
	/** Guild's nickname for the bot. */
	private String NICKNAME;
	/** Argument prefix to call the bot<br>Default is "<>"*/
	private String CALLSIGN;

	/** Can this bot say explicit things? (false default) */
	private boolean EXPLICIT;
	/** Can this bot be used for NSFW? (false default) */
	private boolean NSFW;
	/** Can the bot jump into the conversation? */
	private boolean ACTIVEPARTICIPATE;

	/**
	 * Allow servers to disable some games.
	 * <br> ArrayList<\String>
	 */
	private List<Class<? extends Game>> GAMES_DISABLED;
	/** List of {@code Game}s currently Running */
	private List<Game<? extends Player>> GAMES_RUNNING;

	/**
	 * Sets up a Weebot for the server.
	 * Stores server <b> name </b> and <b> Unique ID long </b>
	 * @param guild Guild (server) the bot is in.
	 */
	public Weebot(Guild guild) {
		this.GUILD		= guild;
		this.SERVERNAME = guild.getName();
		this.BOT_ID		= guild.getIdLong() + "W";
		this.NICKNAME	= "Weebot";
		this.CALLSIGN	= "<>";
		this.ACTIVEPARTICIPATE = true;
		this.EXPLICIT	= false;
		this.NSFW		= false;
		this.SELF		= guild.getSelfMember();
		this.GAMES_RUNNING = new ArrayList<>();
		this.GAMES_DISABLED = new ArrayList<>();
	}

	/**
	 * Check if the message is valid for this bot.
	 * @param message JDA Message to validate
	 * @return {@code 1} if the message begins with the right {@code CALLSIGN}
	 * 			<br> {@code 2} if the message begins with the right {@code NICKNAME} <br>
	 * 			{@code 0} otherwise
	 */
	private int validateCallsign(Message message) {
		String call = message.getContentStripped().split(" ")[0];
		//Don't take commands with a space between the call sign and the command
		//It would just make life less easy
		if (call.startsWith(this.CALLSIGN) && call.length() > this.CALLSIGN.length())
			return 1;
		else if (call.equals("@" + this.NICKNAME))
			return 2;
		return 0;
	}

	/**
	 * Check if the message is valid for this bot.
	 * @param args split Sring[] arguments parsed from message stripped content
	 * @return {@code 1} if the message begins with the right {@code CALLSIGN}
	 * 			<br> {@code 2} if the message begins with the right {@code NICKNAME} <br>
	 * 			{@code 0} otherwise
	 */
	private int validateCallsign(String...args) {
		String call = args[0];
		//Don't take commands with a space between the call sign and the command
		//It would just make life less easy
		if (call.startsWith(this.CALLSIGN) && call.length() > this.CALLSIGN.length())
			return 1;
		else if (call.equals("@" + this.NICKNAME))
			return 2;
		return 0;
	}

	/**
	 * Take in a {@code com.ampro.listener.events.BetterEvent}
	 * and calls the appropriate command.
	 * @param event
	 */
	public void readEvent(BetterEvent event) {

	}

	/**
	 * Takes in a {@code Message} and calls the appropriate private method
	 * @param message JDA message to read
	 */
	public void read(Message message) {
		//Is this a valid call?
        int valid = this.validateCallsign(message);

        /** the validated command, stripped of its prefix. */
        String[] command;
        switch (this.validateCallsign(message)) {
            case 0: return;
            case 1:
                //Cut the callsign from the command (makes handling it easier)
                command = message.getContentStripped().toLowerCase()
                        .substring(this.CALLSIGN.length()).split(" ");
                break;
            case 2:
                //Cut the nickname from the next ( +1 to erase @ symbol )
                command = message.getContentStripped().toLowerCase()
                        .substring(this.NICKNAME.length() + 1)
                        .trim().split(" ");
                break;
            default: return;
        }

		//Don't respond to empty messages
		if (command.length == 0) {
			return;
		}

		//Actual responses and actions
        //Small note: when two cases are lined-up (like name and nickname)
        //then both cases go to the next available code
        // (e.g. name and nickname cases both go to this.changeNickname)
        switch (command[0] /*The command argument*/) {
            case "ping":
                this.pong(message.getTextChannel());
                return;
            case "spam":
                this.spam(message.getTextChannel(), command);
                return;
            case "name":
            case "nickname":
                this.changeNickName(message.getTextChannel(), command);
                return;
            case "callsign":
            case "prefix":
                this.callsign(message.getTextChannel(), command);
                return;
            case "participate":
            case "activeparticipate":
            case "interrupt":
                this.participate(message.getTextChannel(), command);
            case "nsfw":
            case "naughty":
                this.nsfw(message.getTextChannel(), command);
                return;
            case "explicit":
            case "expl":
            case "cuss":
                this.explicit(message.getTextChannel(), command);
                return;
            case "set":
            case "settings":
            case "setting":
                this.listServerSettings(message.getTextChannel());
                return;
            case "help":
                this.help(message.getAuthor(), message.getTextChannel(), command);
                return;
            case "dev":
                this.devRead(message.getTextChannel(), command);
                return;
            default:
                message.getTextChannel().sendMessage(
                                "Sorry, I don't recognize that command..."
                ).queue();
        }
	}

	/**
	 * Set or Get {@code EXPLICIT}.
	 * @param channel TextChannel called from
	 * @param command command invoked
	 */
	private void explicit(TextChannel channel, String[] command) {
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
	 */
	private void participate(TextChannel channel, String[] command) {
        //Only respond to commands with the appropriate number of args
        switch (command.length) {
            case 1:
                channel.sendMessage(
                    "I will " + (this.ACTIVEPARTICIPATE ? "" : "not ")
                        + " join in on conversations."
                ).queue();
                return;
            case 2:
                switch (command[1]) {
                    case "true":
                        this.ACTIVEPARTICIPATE = true;
                        channel.sendMessage(
                                "I will join in on conversations."
                        ).queue();
                        return;
                    case "false":
                        this.ACTIVEPARTICIPATE = false;
                        channel.sendMessage(
                                "I won't join in on conversations anymore."
                        ).queue();
                        return;
                    default:
                        channel.sendMessage("Sorry, " + command[1]
                                + " is not an option. Please use the commands: "
                                + "```" + this.CALLSIGN + "participate [true/on/false/off]```"
                                + "```" + this.CALLSIGN + "activeparticipate [true/on/false/off]```"
                                + "```" + this.CALLSIGN + "interrupt [true/on/false/off]```"
                        ).queue();
                        return;
                }
            default:
                channel.sendMessage("Sorry, " + command[1]
                        + " is not an option. Please use the commands: "
                        + "```" + this.CALLSIGN + "participate [true/on/false/off]```"
                        + "```" + this.CALLSIGN + "activeparticipate [true/on/false/off]```"
                        + "```" + this.CALLSIGN + "interrupt [true/on/false/off]```"
                ).queue();
        }
	}

	/**
	 * Sets the bot's NSFW setting according to message containing true or false. <br>
	 * Or sends whether or not the Bot is NSFW.
	 * @param channel TextChannel called from
	 * @param command Command used to invoke
	 */
	private void nsfw(TextChannel channel, String[] command) {
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
                                + "```" + this.CALLSIGN + "nsfw [true/on/false/off]```"
                        ).queue();
                        return;
                }
                break;
            default:
                channel.sendMessage("Sorry, " + String.join(" ", command[1])
                        + " is not an option. Please use the command: "
                        + "```" + this.CALLSIGN + "nsfw [true/on/false/off]```"
                ).queue();
		}
	}

	/**
	 * Change the nickname of the bot for this server.
	 * @param channel TextChannel called from
	 * @param command The command used to call this method
	 */
	private void changeNickName(TextChannel channel, String[] command) {
		try {
            String newName = String.join(" ", command);
			//Change name on server
            new GuildController(this.GUILD).setNickname(this.SELF, newName).queue();
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
	 */
	private void callsign(TextChannel channel, String[] command) {
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
                    channel.sendMessage("You can now call me with ```" + this.CALLSIGN
                            + "``` or ```@" + this.NICKNAME + "```").queue();
                    return;
                }
            default:
                channel.sendMessage(
                        "Sorry, I can't understand that command."
                        + "\nYou can change my callsign with these commands:"
                        + "```" + this.CALLSIGN + "prefix new_prefix```"
                        + "```@" + this.NICKNAME + " prefix new_prefix```"
                ).queue();
        }
	}


    /**
     * User help method. Can send list of all commands, or details
     * about one command to User in private chat.
     * @param inNeed User asking for help
     * @param channel TextChannel invoked from
     * @param command command array
     */
	private void help(User inNeed, TextChannel channel, String[] command) {
	    final String help = "```Weebot\n"
                + "SomeCommands" //TODO
                + "```";
        inNeed.openPrivateChannel().queue( (pChan) -> pChan.sendMessage(help).queue() );
    }

	/**
	 * List all (non-dev) commands.
	 * @param channel TextChannel to send to
	 */
	private void listCommands(TextChannel channel) {
		channel.sendMessage("TODO").queue();//TODO
		//This method makes me think that we should use
		//a list<string> of all commands and just use that
		//to check for all commands in read() and for listCommands
		//This would probably make expanding a lot easier later
	}

	/**
	 * List current Weebot settings.
	 * @param channel Text channel to send to
	 */
	private void listServerSettings(TextChannel channel) {
		String out = "```Wanna learn about me?";

		out += "\n\n";
		out += "I live here: " + this.SERVERNAME;
		out += "\n";
		out += "I now go by: " + this.NICKNAME;
		out += "\n";
		out += "Call me with: " + this.CALLSIGN;
		out += "\n";
		out += "I am " + (this.EXPLICIT ? "" : "not ") + "explicit";
		out += "\n";
		out += "I " + (this.NSFW ? "am " : "not ") + "NSFW";
		out += "\n";
		out += "I " + (this.ACTIVEPARTICIPATE ? "" : "don't ")
						+ "join in on some conversations :)";
		//out += "\n";
		//out += "\n";
		//out += "\n";
		out += "```";

		channel.sendMessage(out).queue();
	}

	/**
	 * Spams "SPAM ATTACK" for the number listed after spam. Default is 5.
	 * @param channel TextChannel to send spam to
     * @param command Command used to invode call to this method
	 */
	private void spam(TextChannel channel, String[] command) {
	    int loop;
	    try {
            loop = Integer.parseInt(command[1]);
            if (loop > 10) {
                channel.sendMessage("That's a bit much...").queue();
                return;
            }
        } catch (IndexOutOfBoundsException e) {
	        loop = 5;
        } catch (NumberFormatException e2) {
	        System.err.println("Failed to parse int from: " + command[1]);
	        loop = 5;
        }
    	for (int i = 0; i < loop; i++) {
    		channel.sendMessage("SPAM ATTACK").queue();
    	}
    }

    /** Ping-Pong */
	private void pong(TextChannel channel) {
         channel.sendMessage("Pong!").queue();
	}

	public String getNickname() {
		return this.NICKNAME;
	}

	/**
	 * @return String ID of the bot (1234W)
	 */
	public String getBotId() {
		return this.BOT_ID;
	}

	public long getGuildID() {
		return this.GUILD.getIdLong();
	}

    /**
     * The name of the bot's Guild (Unless private bot)
     * @return Name of the Guild
     */
	public String getGuildName() {
		return this.SERVERNAME;
	}

	//
	//
	//Dev-only Methods
	//
	//
	//

	/**
	 * Read method for dev-only commands
	 * @param channel TextChannel invoked from
	 * @param command String command array
	 */
	private void devRead(TextChannel channel, String[] command) {
        switch (command[1]) {
            case "listguilds":
            case "allguilds":
            case "listhomes":
            case "distrobution":
                Weebot.listGuilds(channel);
                return;
            case "help":
                channel.sendMessage(
                        "Well if ur the dev, just open the code, jesus."
                ).queue();
                //TODO lol
                return;
            case "kill":
                channel.sendMessage("Shutting down all Weebots...").queue();
                Launcher.getJDA().shutdown();
                return;
            default:
                channel.sendMessage(
                        "Don't recognize: " + String.join(" ", command)
                ).queue();
                return;
        }
    }

	/**
	 * Lists the dev commands
	 * @param channel
	 */
	private void devHelp(TextChannel channel) {
		//TODO
		channel.sendMessage("Still need to do this...").queue();
	}

	/**
	 * Send a list of each guild Weebot is a member of
	 * and that guild's Weebot
	 * @param channel {@code TextChannel} to send to
	 */
	private static void listGuilds(TextChannel channel) {
		String out = "```";
		Map<Guild, Weebot> map = Launcher.getGuilds();
		for(Map.Entry<Guild, Weebot> entry : map.entrySet()) {
			out += "\n";
			out += entry.getKey().getName() + " : " + entry.getValue().getNickname();
		}
		out += "```";
		channel.sendMessage(out).queue();

	}

	/**
	 * @return -1 if the Guild/Server ID is less than parameter's
	 * 			0 if equal to parameter's
	 * 			1 if greater than parameter's
	 */
	@Override
	public int compareTo(Weebot w2) {
		return (int) (this.getGuildID() - w2.getGuildID());
	}

}
