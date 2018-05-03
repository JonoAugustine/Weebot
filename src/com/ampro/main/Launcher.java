/**
 * @author Jonathan Augustine
 * @copyright Aquatic Mastery Productions
 */

package com.ampro.main;

import com.ampro.main.database.Database;
import com.ampro.main.database.DatabaseManager;
import com.ampro.main.jda.JDABuilder;
import com.ampro.main.listener.EventDispatcher;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

/**
 * Runner/Main class of the Weebot network.
 * Builds single JDA connection.
 * TODO: Connect to Database
 *
 * @author Jonathan Augustine
 *
 */
public class Launcher {

	private static JDA JDA;

	private static final long[] DEV_IDS = new long[]{139167730237571072L
												   , 186130584693637131L};

	/** The database */
	private static Database DATABASE;

	/**
	 * Put bot online, setup listeners, and get full list of servers (Guilds)
	 * @param args
	 * @throws LoginException
	 * @throws RateLimitedException
	 * @throws InterruptedException
	 */
   public static void main(final String[] args)
            throws LoginException, RateLimitedException, InterruptedException
    {
	    Launcher.setUpDatabase();
		Launcher.setUpJda();
    }

   /**
    * Initiates {@code Launcher} data and connects to Weebot API.
    * @throws LoginException
    * @throws InterruptedException
    */
   private static void setUpJda() throws LoginException, InterruptedException {
	   //Connect to API
	   Launcher.JDA = new JDABuilder(AccountType.BOT)
       		.setToken("NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk")
       		.buildBlocking();

	   Launcher.JDA.addEventListener(new EventDispatcher());

   }

	/**
	 * Attempts to load a database from file. <br>
	 * If a database could not be loaded, a new one is created.
	 */
	private static void setUpDatabase() {
		Database db = DatabaseManager.load();
		Launcher.DATABASE = db == null ? new Database("database") : db;
   }

	/**
	 * @return The database.
	 */
	public static Database getDatabase() {
		return Launcher.DATABASE;
	}

	/**
	 * Check if user ID matches a Developer ID.
	 * @param id long ID to check
	 * @return true if the user ID is a dev.
	 */
	public static boolean checkDevID(long id) {
		return Launcher.DATABASE.getDevelopers().contains(id);
	}

	/** Get the JDA */
	public static JDA getJDA() {
		return Launcher.JDA;
	}

}
