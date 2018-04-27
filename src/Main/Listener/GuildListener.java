package Main.Listener;
/**
 * 
 */

import Main.Launcher;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Listener for Guilds. <br>
 * Holds a TreeMap of all Servers currently using Weebot.
 * 
 * @author sword
 *
 */
public class GuildListener extends ListenerAdapter {
		
	public GuildListener() {}
	
	@Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		
		//Ignore ourself
		if (event.getAuthor().getName() == "Weebot") return;
				
        //Message message = event.getMessage();
        
        //Get the proper bot
        Launcher.getGuilds().get(event.getGuild()).read(event.getMessage());
        
	}
	
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		Launcher.updateServers(event.getGuild());		
	}
}
