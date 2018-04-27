/**
 * 
 */

import gnu.trove.impl.hash.TIntByteHash;
import javafx.collections.ListChangeListener.Change;
import jdk.nashorn.internal.ir.annotations.Ignore;
import net.dv8tion.jda.bot.JDABot;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

/**
 * 
 * @author sword
 *
 */
public class Weebot {
	
	//General Info/Settings [
	
	
	
	//]
	
	//Sever Specific Info [
	
	//The server
	private final Guild GUILD;
	//Name of the Server
	private final String SERVERNAME;
	//Server Unique ID
	private final Long	 SERVERID;
	
	//Bot communitcations
	private String NICKNAME;
	private String CALLSIGN;
	
	//Can this bot say explicit things? (false default)
	private boolean EXPLICIT;
	
	//Can this bot be used for NSFW? (false default)
	private boolean NSFW;
	
	//]
	
	/**
	 * Sets up a Weebot for the server.
	 * Stores server <b> name </b> and <b> Unique ID long </b>
	 * @param server Guild (server) the bot is in.
	 */
	public Weebot(Guild guild) {
		this.GUILD		= guild;
		this.SERVERNAME = guild.getName();
		this.SERVERID	= guild.getIdLong();
		this.NICKNAME	= "Weebot";
		this.CALLSIGN	= "++";
		this.EXPLICIT	= false;
		this.NSFW		= false;
	}
	
	/**
	 * Check if the message is valid for this bot.
	 * @param message
	 * @return {@code 1} if the message begins with the right {@code CALLSIGN}
	 * 			<br> {@code 2} if the message begins with the right {@code NICKNAME} <br>
	 * 			{@code 0} otherwise 
	 */
	private int validateCallsign(Message message) {
		//Dont take commands with a space between the callsign and the command
		//It would just make life less easy
		if (message.getContentRaw().startsWith(this.CALLSIGN + " "))
			return 0;
		if (message.getContentRaw().startsWith(this.CALLSIGN))
			return 1;
		else if (message.getMentionedMembers().get(0).getNickname().equals(this.NICKNAME))
			return 2;
		else
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
			text = message.getContentRaw().toLowerCase().substring(this.CALLSIGN.length());
		else
			//Cut the nickname from the next ( +1 to erase @ symbol )
			text = message.getContentRaw().toLowerCase().substring(this.NICKNAME.length() + 1);
			
		if(text.equals("ping"))
			this.pong(message.getTextChannel());
		if(text.startsWith("spam "))
			this.spam(message);
		if(text.startsWith("name "))
			this.changeNickName(message, "name".length());
		
		
	}
	
	/**
	 * Change the nickname of the bot for this server.
	 * @param message
	 */
	private void changeNickName(Message message, int command) {
		GuildController controller = this.GUILD.getController();
		String newName = message.getContentRaw().substring(command);
		//Change name on server
		try {
			controller.setNickname(this.GUILD.getSelfMember(), newName);
		} catch (InsufficientPermissionException e) {
			message.getTextChannel().sendMessage("I don't have permissions do that :(").queue();
		}
		//Change internal name
		this.NICKNAME = newName;
	}
	
	/**
	 * Spams "SPAM ATTACK" for the number listed after spam. Default is 5.
	 * @param channel Text channel to spam
	 */
	private void spam(Message message) {
		int loop;
		try {
			loop = Integer.parseInt(message.getContentRaw().substring(4));
		} catch (NumberFormatException e) {
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

}