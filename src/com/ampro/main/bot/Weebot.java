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
import com.ampro.main.bot.commands.Command;
import com.ampro.main.game.Game;
import com.ampro.main.game.Player;
import com.ampro.main.listener.events.BetterEvent;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

//import org.joda.time.LocalDateTime;

/**
 * A representation of a Weebot entity linked to a Guild.<br>
 *     Contains a reference to the Guild hosting the bot and
 *     the settings applied to the bot by said Guild. <br><br>
 *     Each Weebot is assigned an ID String consisting of the
 *     hosting Guild's unique ID + "W" (e.g. "1234W") <br><br>
 *
 * Development plans TODO: <br>
 *      Add a {@code Refresh} method to call on startup that updates and settings
 *      (like {@link Weebot#NICKNAME}) that were changed during downtime.
 *
 * <br><br>
 * Development Questions TODO: <br>
 * ? Should User relation to Weebot be gloabal or local? This would likelly
 * invole a User wrapper class to keep track of the relationship Though that
 * would probably be needed anyway if we are to implement the User-bot good/bad
 * relationship meter thing (which I really want to)
 * <br>
 *
 *
 * @author Jonathan Augsutine
 *
 */
public class Weebot implements Comparable<Weebot> {

    /** Hosting guild */
    private final long GUILD_ID;
    /** The Bot's ID string ending in "W". */
    private final String BOT_ID;
    /** The date the bot was added to the Database */
    //TODO private LocalDateTime BIRTHDAY;

    /** Bot's nickname in hosting guild */
    private String NICKNAME;
    /** Guils's command string to call the bot */
    private String CALLSIGN;
    /** Whether the bot is able to use explicit language */
    private boolean EXPLICIT;
    /** Words banned on the server */
    private List<String> BANNED_WORDS;
    /** Whether the bot is able to be NSFW */
    private boolean NSFW;
    /** Whether the bot is able to respond to actions not directed to it */
    private boolean ACTIVE_PARTICIPATE;

    /** Commands not allowed on the server channels.*/
    private TreeMap<TextChannel, ArrayList<Class<? extends Command>>>
            COMMANDS_DISABLED;
	/** List of {@code Game}s currently Running */
	private List<Game<? extends Player>> GAMES_RUNNING;

	/**
	 * Sets up a Weebot for the server.
	 * Stores server <b> name </b> and <b> Unique ID long </b>
	 * @param guild Guild (server) the bot is in.
	 */
	public Weebot(Guild guild) {
        this.GUILD_ID   = guild.getIdLong();
        this.BOT_ID     = guild.getId() + "W";
        this.NICKNAME   = "Weebot";
        this.CALLSIGN   = "<>";
        this.EXPLICIT   = false;
        this.NSFW       = false;
        this.BANNED_WORDS = new ArrayList<>();
        this.ACTIVE_PARTICIPATE = false;
        this.COMMANDS_DISABLED = new TreeMap<>();
        this.GAMES_RUNNING  = new ArrayList<>();
        //TODO this.BIRTHDAY   = new LocalDateTime();
    }

    /**
     * Create a Weebot with no guild or call sign.
     * <b>This is only called for the private-chat instance Weebot created
     * when a new Database is created.</b>
     */
	public Weebot() {
        this.GUILD_ID   = 0L;
        this.BOT_ID     = "0W";
        this.NICKNAME   = "Weebot";
        this.CALLSIGN   = "";
        this.EXPLICIT   = false;
        this.NSFW       = false;
        this.BANNED_WORDS = new ArrayList<>(
                Arrays.asList()
        );
        this.ACTIVE_PARTICIPATE = false;
        this.COMMANDS_DISABLED = new TreeMap<>();
        this.GAMES_RUNNING  = null;
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
		if (args.length == 0) return -1;
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
	 * @param event BetterEvent to read
	 */
	public void readEvent(BetterEvent event) {
		if (event instanceof BetterMessageEvent) {
			BetterMessageEvent messageEvent = (BetterMessageEvent) event;
			messageEvent.getArgs();
			switch (this.validateCallsign(messageEvent.getArgs())) {
				case 1:
					this.runCommand(messageEvent,0);
					return;
				case 2:
					this.runCommand(messageEvent,1);
					return;
				default: break; //To allow for active participate later
			}
		}
	}

	/**
	 * Find and execute the command requested in a message.
	 * @param event The arguments of the command
	 * @param startIndex The index the commands begin at
	 */
	private void runCommand(BetterMessageEvent event, int startIndex) {
        //get the arg string without the callsign
        String command;
        if (startIndex == 0)
            command = event.getArgs()[0].substring(this.CALLSIGN.length());
        else
            command = event.getArgs()[1];

        for (Command c : Launcher.getCommands()) {
            if (c.isCommandFor(command)) {
                if (this.commandIsAllowed(c, event)) {
                    c.run(this, event);
                } else {
                    event.reply("This command is not allowed in this channel.");
                }
                return;
            }
        }

        //Command help = Launcher.getCommand(HelpCommand.class);
        //if (help != null) help.run(this, event);
        event.reply("Sorry, I don't recognize that command.");

	}

    /**
     * Checks if a command has been banned from a channel.
     * @param c The {@link Command} to check
     * @param e The {@link BetterMessageEvent} that called it.
     * @return
     */
	private boolean commandIsAllowed(Command c, BetterMessageEvent e) {
        try {
            //Get the channel the message was sent in
            ArrayList<Class<? extends Command>> commands =
                    this.COMMANDS_DISABLED.get(e.getTextChannel());

            for (Class com : commands) {
                if(c.getClass().equals(com)) {
                    return false;
                }
            }
        } catch (NullPointerException exc) {
            //If no list of banned commands was found
            return true;
        }
        return true;
    }

	/**
	 * Takes in a {@code Message} and calls the appropriate private method
	 * @param message JDA message to read
	 */
	public void read(Message message) {

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
	 */
	private void changeNickName(TextChannel channel, String[] command) {
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
	 * List current Weebot settings.
	 * @param channel Text channel to send to
	 */
	private void listServerSettings(TextChannel channel) {
		String out = "```Wanna learn about me?";

		out += "\n\n";
		out += "I live here: " + Launcher.getGuild(this.GUILD_ID).getName();
		out += "\n";
		out += "I now go by: " + this.NICKNAME;
		out += "\n";
		out += "Call me with: " + this.CALLSIGN;
		out += "\n";
		out += "I am " + (this.EXPLICIT ? "" : "not ") + "explicit";
		out += "\n";
		out += "I " + (this.NSFW ? "am " : "not ") + "NSFW";
		out += "\n";
		out += "I " + (this.ACTIVE_PARTICIPATE ? "" : "don't ")
						+ "join in on some conversations where I'n not called.";
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

	public String getCallsign() { return this.CALLSIGN; }

	/**
	 * @return String ID of the bot (1234W)
	 */
	public String getBotId() {
		return this.BOT_ID;
	}

    /**
     * @return The ID of the guild the bot is linked to.
     */
	public long getGuildID() {
		return this.GUILD_ID;
	}

    /**
     * The name of the bot's Guild (Unless private bot)
     * @return Name of the Guild
     */
	public String getGuildName() {
		return this.GUILD_ID != 0L ?
                Launcher.getGuild(this.GUILD_ID).getName()
                : "PRIVATE BOT";
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

	@Override
	public String toString() {
		String out = "";
		out += this.getBotId() + "\n\t";
		out += this.getGuildName() + "\n\t";
		out += this.getNickname() + "\n";
		return out;
	}

    public String getNICKNAME() {
        return NICKNAME;
    }

    public void setNICKNAME(String NICKNAME) {
        this.NICKNAME = NICKNAME;
    }

    public String getCALLSIGN() {
        return CALLSIGN;
    }

    public void setCALLSIGN(String CALLSIGN) {
        this.CALLSIGN = CALLSIGN;
    }

    public boolean isEXPLICIT() {
        return EXPLICIT;
    }

    public void setEXPLICIT(boolean EXPLICIT) {
        this.EXPLICIT = EXPLICIT;
    }

    public List<String> getBANNED_WORDS() {
        return BANNED_WORDS;
    }

    public void setBANNED_WORDS(List<String> BANNED_WORDS) {
        this.BANNED_WORDS = BANNED_WORDS;
    }

    public boolean isNSFW() {
        return NSFW;
    }

    public void setNSFW(boolean NSFW) {
        this.NSFW = NSFW;
    }

    public boolean isACTIVE_PARTICIPATE() {
        return ACTIVE_PARTICIPATE;
    }

    public void setACTIVE_PARTICIPATE(boolean ACTIVE_PARTICIPATE) {
        this.ACTIVE_PARTICIPATE = ACTIVE_PARTICIPATE;
    }

    public List<Game<? extends Player>> getGAMES_RUNNING() {
        return GAMES_RUNNING;
    }

    public void setGAMES_RUNNING(List<Game<? extends Player>> GAMES_RUNNING) {
        this.GAMES_RUNNING = GAMES_RUNNING;
    }
}
