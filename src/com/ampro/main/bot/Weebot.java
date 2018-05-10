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

import java.util.*;

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
 *      (like {@link Weebot#nickname}) that were changed during downtime.
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
    private String nickname;
    /** Guils's command string to call the bot */
    private String callsign;
    /** Whether the bot is able to use explicit language */
    private boolean explicit;
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
        this.nickname = "Weebot";
        this.callsign = "<>";
        this.explicit = false;
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
        this.nickname = "Weebot";
        this.callsign = "";
        this.explicit = false;
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
	 * @return {@code 1} if the message begins with the right {@code callsign}
	 * 			<br> {@code 2} if the message begins with the right {@code nickname} <br>
	 * 			{@code 0} otherwise
	 */
	private int validateCallsign(Message message) {
		String call = message.getContentStripped().split(" ")[0];
		//Don't take commands with a space between the call sign and the command
		//It would just make life less easy
		if (call.startsWith(this.callsign) && call.length() > this.callsign.length())
			return 1;
		else if (call.equals("@" + this.nickname))
			return 2;
		return 0;
	}

	/**
	 * Check if the message is valid for this bot.
	 * @param args split Sring[] arguments parsed from message stripped content
	 * @return {@code 1} if the message begins with the right {@code callsign}
	 * 			<br> {@code 2} if the message begins with the right {@code nickname} <br>
	 * 			{@code 0} otherwise
	 */
	private int validateCallsign(String...args) {
		if (args.length == 0) return -1;
		String call = args[0];
		//Don't take commands with a space between the call sign and the command
		//It would just make life less easy
		if (call.startsWith(this.callsign) && call.length() > this.callsign.length())
			return 1;
		else if (call.equals("@" + this.nickname))
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
            command = event.getArgs()[0].substring(this.callsign.length());
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
	 * Set or Get {@code explicit}.
	 * @param channel TextChannel called from
	 * @param command command invoked
	 */
	private void explicit(TextChannel channel, String[] command) {
	    //Only respond to commands with the appropriate number of args
        switch (command.length) {
            case 1:
                channel.sendMessage(
                        "I am " + (this.explicit ? "" : "not ") + "NSFW"
                ).queue();
                return;
            case 2:
                switch (command[1]) {
                    case "true":
                        this.explicit = true;
                        channel.sendMessage("I am now explicit" ).queue();
                        return;
                    case "false":
                        this.explicit = false;
                        channel.sendMessage("I am now clean" ).queue();
                        return;
                    default:
                        channel.sendMessage("Sorry, " + command[1]
                                + " is not an option. Please use the commands: "
                                + "```" + this.callsign + "explicit [true/on/false/off]```"
                                + "```" + this.callsign + "expl [true/on/false/off]```"
                                + "```" + this.callsign + "cuss [true/on/false/off]```"
                        ).queue();
                        return;
                }
            default:
                channel.sendMessage("Sorry, " + command[1]
                        + " is not an option. Please use the commands: "
                        + "```" + this.callsign + "explicit [true/on/false/off]```"
                        + "```" + this.callsign + "expl [true/on/false/off]```"
                        + "```" + this.callsign + "cuss [true/on/false/off]```"
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
                                + "```" + this.callsign +
                                "participate " +
                                "[true/on/false/off]```"
                                + "```" + this.callsign +
                                "activeparticipate [true/on/false/off]```"
                                + "```" + this.callsign + "interrupt" +
                                " " +
                                "[true/on/false/off]```"
                        ).queue();
                        return;
                }
            default:
                channel.sendMessage("Sorry, " + command[1]
                        + " is not an option. Please use the commands: "
                        + "```" + this.callsign + "participate " +
                        "[true/on/false/off]```"
                        + "```" + this.callsign + "activeparticipate" +
                        " " +
                        "[true/on/false/off]```"
                        + "```" + this.callsign + "interrupt " +
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
                                + "```" + this.callsign + "nsfw " +
                                "[true/on/false/off]```"
                        ).queue();
                        return;
                }
                break;
            default:
                channel.sendMessage("Sorry, " + String.join(" ", command[1])
                        + " is not an option. Please use the command: "
                        + "```" + this.callsign + "nsfw " +
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
			this.nickname = newName;
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
                channel.sendMessage("You can call me with " + this.callsign
                        + " or @" + this.nickname).queue();
                return;
            case 2:
                //Set a new callsign (if under 3 char)
                if (command[1].length() > 3) {
                    channel.sendMessage(
                            "Please keep the callsign under 4 characters."
                    ).queue();
                    return;
                } else {
                    this.callsign = command[1];
                    channel.sendMessage("You can now call me with ```" +
                            this.callsign
                            + "``` or ```@" + this.nickname + "```")
                            .queue();
                    return;
                }
            default:
                channel.sendMessage(
                        "Sorry, I can't understand that command."
                        + "\nYou can change my callsign with these commands:"
                        + "```" + this.callsign + "prefix " +
                                "new_prefix```"
                        + "```@" + this.nickname + " prefix " +
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
		out += "I now go by: " + this.nickname;
		out += "\n";
		out += "Call me with: " + this.callsign;
		out += "\n";
		out += "I am " + (this.explicit ? "" : "not ") + "explicit";
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

    /** @return The ID of the guild the bot is linked to.*/
    public final long getGuildID() {
        return this.GUILD_ID;
    }

    /** @return Name of the Guild or "PRIVATE BOT" if the private message bot.*/
    public final String getGuildName() {
        return this.GUILD_ID != 0L ?
                Launcher.getGuild(this.GUILD_ID).getName() : "PRIVATE BOT";
    }

    /** @return String ID of the bot (1234W) */
    public final String getBotId() {
        return this.BOT_ID;
    }

    public final String getNickname() {
        return this.nickname;
    }

    /**
     * Set the bot's internal copy of its effective name in its Guild.
     * @param newName The new {@link Weebot#nickname} for the bot.
     * @return The previous value of {@link Weebot#nickname}.
     */
    public final String setNickname(String newName) {
        String old = this.nickname;
        this.nickname = newName;
        return old;
    }

    /** @return The String to call the bot. */
    public final String getCallsign() { return this.callsign; }

    /**
     * Set the String used to call the bot.
     * @param newPrefix The new String used to call the bot.
     * @return The previous value of {@link Weebot#callsign}.
     */
    public final String setCallsign(String newPrefix) {
        String old = this.callsign;
        this.callsign = newPrefix;
        return old;
    }

    /** @return {@code true} if the bot is set to be explicit. */
    public final boolean isExplicit() {
        return this.explicit;
    }

    /**
     * Set whether the bot is allowed to be explicit.
     * @param explicit Is the bot allowed to be explicit.
     * @return The previous value of {@link Weebot#explicit}.
     */
    public final boolean setExplicit(boolean explicit) {
        boolean old = this.explicit;
        this.explicit = explicit;
        return old;
    }

    /** @return A List of the Guild's banned words */
    public final List<String> getBannedWords() {
        return this.BANNED_WORDS;
    }

    /**
     * Add words to the bot's banned word list.
     * @param words Words to ban.
     */
    public final void addBannedWords(String...words) {
        this.BANNED_WORDS.addAll(Arrays.asList(words));
    }

    /**
     * Add words to the bot's banned word list.
     * @param words Words to ban.
     */
    public final void addBannedWords(Collection<String> words) {
        this.BANNED_WORDS.addAll(words);
    }

    /** @return {@code true} if the bot is allowed to be not-safe-for-work.*/
    public boolean isNSFW() {
        return this.NSFW;
    }

    /**
     * Set whether the bot is allowed to be not-safe-for-work.
     * @param NSFW
     * @return The previous value of {@link Weebot#NSFW}.
     */
    public final boolean setNSFW(boolean NSFW) {
        boolean old = this.NSFW;
        this.NSFW = NSFW;
        return old;
    }

    /**
     * @return {@code true} if the bot is able to respond to messages not
     *              directed to it
     */
    public final boolean canParticipate() {
        return this.ACTIVE_PARTICIPATE;
    }

    /**
     *
     * @param ACTIVE_PARTICIPATE
     * @return The previous value of {@link Weebot#ACTIVE_PARTICIPATE}
     */
    public final boolean setACTIVE_PARTICIPATE(boolean ACTIVE_PARTICIPATE) {
        boolean old = this.ACTIVE_PARTICIPATE;
        this.ACTIVE_PARTICIPATE = ACTIVE_PARTICIPATE;
        return old;
    }

    /**
     * @return A list of {@link com.ampro.main.game.Game} instances currently
     *              being run by the bot.
     */
    public List<Game<? extends Player>> getGAMES_RUNNING() {
        return this.GAMES_RUNNING;
    }

    /**
     * Add a running {@link Game} to the bot.
     * @param game The {@link Game} to add.
     */
    public final void addRunningGame(Game game) {
        this.GAMES_RUNNING.add(game);
    }

}
