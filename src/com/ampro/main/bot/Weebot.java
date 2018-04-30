/**
 *
 */

package com.ampro.main.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ampro.main.Launcher;
import com.ampro.main.game.Game;
import com.ampro.main.game.Player;
import com.ampro.main.game.cardgame.CardsAgainstHumanity;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.managers.GuildController;

/**
 * A Weebot.
 * <br></br>
 * ? Should User relation to Weebot be gloabal or local
 * ? This would likelly invole a User wrapper class to keep track of the relationship
 * ? Though that would probably be needed anyway if we are to implement the
 * ? User-bot good/bad relationship meter thing (which I really wannt to)
 *
 * @author sword
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
	private final long	 SERVERID;

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
	private boolean ALWAYSLISTEN;

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
		this.SERVERID	= guild.getIdLong();
		this.NICKNAME	= "Weebot";
		this.CALLSIGN	= "<>";
		this.ALWAYSLISTEN = true;
		this.EXPLICIT	= false;
		this.NSFW		= false;
		this.SELF		= guild.getSelfMember();
		this.GAMES_RUNNING = new ArrayList<>();
		this.GAMES_DISABLED = new ArrayList<>();
	}

	/**
	 * Check if the message is valid for this bot.
	 * @param message
	 * @return {@code 1} if the message begins with the right {@code CALLSIGN}
	 * 			<br> {@code 2} if the message begins with the right {@code NICKNAME} <br>
	 * 			{@code 0} otherwise
	 */
	private int validateCallsign(Message message) {
		String call = message.getContentDisplay().split(" ")[0];
		//Don't take commands with a space between the call sign and the command
		//It would just make life less easy
		if (call.length() > this.CALLSIGN.length())
			return 1;
		else if (call.equals("@" + this.NICKNAME))
			return 2;
		return 0;
	}

	/**
	 * Takes in a {@code Message} and calls the appropriate private method
	 * @param message
	 */
	public void read(Message message) {
		String text;
		//Is this a valid call?
		int valid = this.validateCallsign(message);
		if (valid == 0) return;
		else if (valid == 1)
			//Cut the callsign from the text (makes handling it easier)
			text = message.getContentRaw().toLowerCase().substring(this.CALLSIGN.length()).trim();
		else
			//Cut the nickname from the next ( +1 to erase @ symbol )
			text = message.getContentDisplay().toLowerCase().substring(this.NICKNAME.length() + 1).trim();

		//Don't respond
		if (text.trim().isEmpty()) {
			return;
		}

		//TODO Redo the if-else with a string split(" ") and switch-case

		//Actual responses and actions

		if(text.equals("ping"))
			this.pong(message.getTextChannel());

		else if(text.startsWith("spam ") || text.equals("spam"))
			this.spam(message);

		else if(text.startsWith("name "))
			this.changeNickName(message, 7);

		else if(text.equals("settings") || text.trim().equals("setting"))
			this.listServerSettings(message.getTextChannel());

		else if(text.startsWith("callsign") || text.startsWith("prefix"))
			this.changeCallsign(message.getTextChannel(), text);

		else if(text.startsWith("nsfw"))
			this.nsfw(message.getTextChannel(), text);

		else if(text.startsWith("expl") || text.startsWith("explicit"))
			this.explicit(message.getTextChannel(), text);

		else if(text.startsWith("alwayslisten") || text.startsWith("listen"))
			this.alwaysListen(message.getTextChannel(), text);

		//Move to Developer Reader
		else if(Launcher.checkDevID(message.getAuthor().getIdLong()) &&
				 text.startsWith("dev ") || text.trim().equals("dev")) {

			this.devRead(message, text.trim().substring(3));
		}
		//Cannot Understand command
		else
			message.getTextChannel()
				.sendMessage("Sorry, I don't recognize that command...").queue();

	}

	/**
	 * Set or Get {@code EXPLICIT}.
	 * @param channel Text channel
	 * @param text Command
	 */
	private void explicit(TextChannel channel, String text) {
		String content;
		try {
			content = text.trim().split(" ")[1];
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("I am " + (this.EXPLICIT ? "" : "not ") + "explicit").queue();
			return;
		}
		switch(content.trim()) {
		case "true":
			this.EXPLICIT = true;
			break;
		case "false":
			this.EXPLICIT = false;
			break;
		case "?":
			channel.sendMessage("I am " + (this.EXPLICIT ? "" : "not ") + "explicit").queue();
			return;
		default:
			channel.sendMessage("Sorry, " + content
					+ " is not a command. Please use 'true' or 'false' ").queue();
			return;
		}
		channel.sendMessage("I am now " + (this.EXPLICIT ? "" : "not ") + "explicit").queue();
	}

	/**
	 * Set or Get {@code ALWAYSLISTEN}.
	 * @param channel Text channel
	 * @param text Command
	 */
	private void alwaysListen(TextChannel channel, String text) {
		String content;
		try {
			content = text.trim().split(" ")[1];
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("I " + (this.ALWAYSLISTEN ? "" : "do not ")
							+ "always listen for a call").queue();
			return;
		}
		switch(content.trim()) {
		case "true":
			this.ALWAYSLISTEN = true;
			break;
		case "false":
			this.ALWAYSLISTEN = false;
			break;
		case "?":
			channel.sendMessage("I " + (this.ALWAYSLISTEN ? "" : "do not ")
					+ "always listen for a call").queue();
			return;
		default:
			channel.sendMessage("Sorry, " + content
					+ " is not a command. Please use 'true' or 'false' ").queue();
			return;
		}
		channel.sendMessage("I will " + (this.ALWAYSLISTEN ? "" : "not ")
				+ "always listen for a call").queue();
	}

	/**
	 * Sets the bot's NSFW setting according to message containing true or false. <br>
	 * Or sends whether or not the Bot is NSFW.
	 * @param channel
	 * @param text
	 */
	private void nsfw(TextChannel channel, String text) {
		//Question or Statement
		String content;
		try {
			content = text.trim().split(" ")[1];
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("I am " + (this.NSFW ? "" : "not ") + "NSFW").queue();
			return;
		}
		if (content.isEmpty()) {
			String out = "```";
			out += "I am " + (this.NSFW ? "" : "not ") + "NSFW```";
			channel.sendMessage(out).queue();
		} else {
			switch(content.trim()) {
			case "true":
				this.NSFW = true;
				break;
			case "false":
				this.NSFW = false;
				break;
			case "?":
				channel.sendMessage("I am " + (this.NSFW ? "" : "not ") + "NSFW").queue();
				return;
			default:
				channel.sendMessage("Sorry, " + content
						+ " is not a command. Please enter 'true' or 'false' ").queue();
				return;
			}
			channel.sendMessage("I am now " + (this.NSFW ? "" : "not ") + "NSFW" ).queue();
		}
	}

	/**
	 * Change the nickname of the bot for this server.
	 * @param message
	 * @param command The length of the command used to call this
	 */
	private void changeNickName(Message message, int command) {
		GuildController controller = new GuildController(this.GUILD);
		String newName = message.getContentRaw().substring(command).trim();
		try {
			//Change name on server
			controller.setNickname(this.SELF, newName).queue();
			//Change internal name
			this.NICKNAME = newName;
			if (newName != "Weebot")
				message.getTextChannel()
					.sendMessage("Hmm... " + newName
							+ "... I like the sound of that!")
					.queue();
			else
				message.getTextChannel()
				.sendMessage("Hmm... Weebot... I like the sound of th-- wait!")
				.queue();
		} catch (InsufficientPermissionException e) {
			message.getTextChannel()
				.sendMessage("I don't have permissions do that :(").queue();
		}
	}

	/**
	 * Change or send the callsign (limited to 3 char).
	 * @param channel
	 * @param text
	 */
	private void changeCallsign(TextChannel channel, String text) {
		String newCall;
		try {
			newCall = text.trim().split(" ")[1];
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("You can call me with " + this.CALLSIGN
									+ " or @" + this.NICKNAME).queue();
			return;
		}
		//Limit to 3 characters
		if (newCall.length() > 3) {
			channel.sendMessage("Please keep the callsign under 4 characters.").queue();
			return;
		}

		this.CALLSIGN = newCall;

		channel.sendMessage("You can now call me with " + this.CALLSIGN
				+ " or @" + this.NICKNAME).queue();

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
		out += "I " + (this.ALWAYSLISTEN ? "" : "don't ")
						+ "always listen for a call \n\tRespond to empty callsign or mentions)";
		//out += "\n";
		//out += "\n";
		//out += "\n";
		out += "```";

		channel.sendMessage(out).queue();
	}

	/**
	 * Spams "SPAM ATTACK" for the number listed after spam. Default is 5.
	 * @param channel Text channel to spam
	 */
	private void spam(Message message) {
		int loop;
		try {
			loop = Integer.parseInt(message.getContentRaw().substring(7));
			if (loop > 10) {
				message.getTextChannel().sendMessage("That's a bit much...").queue();
				return;
			}
		} catch (NumberFormatException e) {
			System.err.println("spam: Could not parse int from "
						+ message.getContentRaw().substring(7));
			loop = 5;
		}
    	for (int i = 0; i < loop; i++) {
    		message.getTextChannel().sendMessage("SPAM ATTACK").queue();
    		try {
    		    Thread.sleep(1000);
    		} catch(InterruptedException ex) {
    			Thread.currentThread().interrupt();
    		}
    	}
    }

	private void pong(TextChannel channel) {
         channel.sendMessage("Pong!").queue();
	}

	public String getNickname() {
		return this.NICKNAME;
	}

	public long getGuildID() {
		return this.SERVERID;
	}

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
	 * @param message
	 * @param text
	 */
	private void devRead(Message message, String text) {
		if(text.trim().equals("listguilds"))
			listGuilds(message.getTextChannel());
		else if(text.trim().equals("dev") || text.trim().equals("devhelp"))
			this.devHelp(message.getTextChannel());

		else
			message.getTextChannel()
				.sendMessage("Don't recognize: " + message.getContentRaw());
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
		return (int) (this.SERVERID - w2.getGuildID());
	}

}
