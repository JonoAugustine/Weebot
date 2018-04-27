/**
 * 
 */
import java.nio.channels.Channel;
import java.util.Timer;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.TreeMap;

/**
 * Listener for Guilds. <br>
 * Holds a TreeMap of all Servers currently using Weebot.
 * 
 * @author sword
 *
 */
public class GuildListener extends ListenerAdapter {
	
	private TreeMap<Long, Weebot> SERVERS;

	@Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		System.out.println(event.getGuild().getIdLong());
		
        Message message = event.getMessage();
        
        
        
	}
}
