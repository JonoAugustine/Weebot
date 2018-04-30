/**
 * @author Jonathan Augustine
 * @copyright Aquatic Mastery Productions
 */


import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javax.security.auth.login.LoginException;

import com.AMPro.Weebot.main.Bot.Weebot;
import com.AMPro.Weebot.main.JDA.JDABuilder;
import com.AMPro.Weebot.main.Listener.GuildListener;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

public class Launcher {
	
	private static final class GuildComparator implements Comparator<Guild> {
		@Override
		public int compare(Guild g1, Guild g2) {
			return (int) (g1.getIdLong() - g2.getIdLong());
		}
	}
	
	private static JDA JDA;
	
	private static TreeMap<Guild, Weebot> GUILDS;
	
	private static final long[] DEV_IDS = new long[]{139167730237571072L};
		
	/**
	 * Put bot online, setup listeners, and get full list of servers (guilds)
	 * @param args
	 * @throws LoginException
	 * @throws RateLimitedException
	 * @throws InterruptedException
	 */
   public static void main(final String[] args)
            throws LoginException, RateLimitedException, InterruptedException
    {
	   Launcher.setUpLauncher();
       Launcher.JDA.addEventListener(new GuildListener());
       //Launcherjda.addEventListener(new PrivateListener()); TODO
    }
   
   /**
    * Initiates {@code Launcher} data and connects to Weebot API.
    * @throws LoginException
    * @throws InterruptedException
    */
   private static void setUpLauncher() throws LoginException, InterruptedException {
	   //Connect to API
	   Launcher.JDA = new JDABuilder(AccountType.BOT)
       		.setToken("NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk")
       		.buildBlocking();
       
	   //Get Guild list
	   Launcher.GUILDS = new TreeMap<Guild, Weebot>(new GuildComparator());
	   Launcher.updateServers();
	   
   }
	
   /**
    * Update local server list from online API.
    */
	private static void updateServers() {
		final List<Guild> g = Launcher.JDA.getGuilds();
		for (final Guild guild : g) {
			Launcher.GUILDS.putIfAbsent(guild, new Weebot(guild));
		}
	}

	/**
	 * Add a new {@code Guild} and {@code Weebot} to the map.
	 * @param guild
	 */
	public static void updateServers(final Guild guild) {
		Launcher.GUILDS.put(guild, new Weebot(guild));
	}
	
	/**
	 * Update and return the Guild map.
	 * @return Launcher guild TreeMap
	 */
	public static TreeMap<Guild, Weebot> getGuilds() {
		Launcher.updateServers();
		return Launcher.GUILDS;
	}
	
	/**
	 * Check if user ID matches a Developer ID.
	 * @param id long ID to check
	 * @return true if the user ID is a dev.
	 */
	public static boolean checkDevID(long id) {
		for (int i = 0; i < DEV_IDS.length; i++) {
			if (DEV_IDS[i] == id)
				return true;
		}
		return false;
	}
}
