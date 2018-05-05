/**
 * @author Jonathan Augustine
 * @copyright Aquatic Mastery Productions
 */

package com.ampro.main;

import com.ampro.main.bot.Weebot;
import com.ampro.main.database.Database;
import com.ampro.main.database.DatabaseManager;
import com.ampro.main.jda.JDABuilder;
import com.ampro.main.listener.EventDispatcher;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Runner/Main class of the Weebot network.
 * Builds single JDA connection.
 *
 * @author Jonathan Augustine
 *
 */
public class Launcher {

	private static JDA JDA_CLIENT;

	/** The database */
	private static Database DATABASE;

	/**
	 * Put bot online, setup listeners, and get full list of servers (Guilds)
	 * @param args
	 * @throws LoginException
	 * @throws RateLimitedException
	 * @throws InterruptedException
	 */
   public static void main(final String[] args) {
		Launcher.jdaLogIn();
		Launcher.setUpDatabase();
		Collection c = DATABASE.getWeebots().values();
		Iterator it = c.iterator();
		System.out.println("Printing bots from database---------");
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		System.out.println("-------------------------------DONE");
		Launcher.startSaveTimer(1);
		//while (Launcher.JDA_.getStatus() == null)
		Launcher.addListeners();
	}

   /**
    * Initiates {@code Launcher} data and connects to Weebot API.
    * Builds Asynchronously in a separate thread to let main thread work on
    * other setup processes.
    * @throws LoginException
    * @throws InterruptedException
    */
   private static void jdaLogIn() {
	   try {
		   //Connect to API
		   JDABuilder builder = new JDABuilder(AccountType.BOT)
				   .setToken("NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk");
		   Launcher.JDA_CLIENT = builder.buildBlocking(Status.CONNECTED);
	   } catch (LoginException e) {
			e.printStackTrace();
	   } catch (IllegalArgumentException e) {
		   e.printStackTrace();
	   } catch (InterruptedException e) {
			e.printStackTrace();
	   }
   }

	/**
	 * Adds event listeners to the JDA.
	 */
	private static void addListeners() {
   		Launcher.JDA_CLIENT.addEventListener(new EventDispatcher());
   }

	/**
	 * Attempts to load a database from file. <br>
	 * If a database could not be loaded, a new one is created. <br>
	 * Is called only once during setup.
	 */
	private static void setUpDatabase() {
		Database db = DatabaseManager.load();
		Launcher.DATABASE = db == null ? new Database() : db;
		if (db == null) {
			System.err.println("Database not found, creating new database.");
			System.err.println("Loading known Guilds");
			List<Guild> guilds = Launcher.JDA_CLIENT.getGuilds();
			for (Guild g : guilds) {
				Launcher.DATABASE.addBot(new Weebot(g));
			}
			DatabaseManager.save(Launcher.DATABASE);
			return;
		}
		DatabaseManager.backUp(Launcher.DATABASE);
	}

	/**
	 * Starts a thread that saves a database backup each interval.
	 * <br> Listens for a shutdown event to save the the main file
	 * @param min The delay in minuets between saves.
	 */
	private static synchronized void startSaveTimer(double min) {
	   Thread saveTimer = new Thread( () -> {
			   try {
				   while(true) {
				   	   if (Launcher.JDA_CLIENT.getStatus().equals(Status.SHUTTING_DOWN)) {
					        //On a proper shutdown
					        System.err.println("Sutdown signal received. Saving database.");
				   			DatabaseManager.backUp(Launcher.DATABASE);
				   			DatabaseManager.save(Launcher.DATABASE);
				   			break;
				       }
					   System.out.println("Backing up database.");
					   DatabaseManager.backUp(Launcher.DATABASE);
					   Thread.sleep(Math.round((1000 * 60) * min));
				   }
			   } catch (InterruptedException e) {
				   e.printStackTrace();
			   }
		   }
	   );
	   saveTimer.start();
   }

	/**
	 * @return The database.
	 */
	public static Database getDatabase() {
		synchronized (Launcher.DATABASE) {
			return Launcher.DATABASE;
		}
	}

	/**
	 * Check if user ID matches a Developer ID.
	 * @param id long ID to check
	 * @return true if the user ID is a dev.
	 */
	public static boolean checkDevID(long id) {
		return Launcher.DATABASE.getDevelopers().contains(id);
	}

	/**
	 * Get a guild matching the ID given.
	 * @param id long ID
	 * @return requested Guild <br> null if not found.
	 */
	public static Guild getGuild(long id) {
		Iterator<Guild> it = Launcher.JDA_CLIENT.getGuilds().iterator();
		while (it.hasNext()) {
			Guild curr = it.next();
			if (curr.getIdLong() == id) {
				return curr;
			}
		}
		return null;
	}

	/** Get the JDA */
	public static JDA getJda() {
		return Launcher.JDA_CLIENT;
	}

}
