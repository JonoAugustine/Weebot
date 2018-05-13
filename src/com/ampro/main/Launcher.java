/**
 * @author Jonathan Augustine
 * @copyright Aquatic Mastery Productions
 */

package com.ampro.main;

import com.ampro.main.bot.Weebot;
import com.ampro.main.bot.commands.*;
import com.ampro.main.bot.commands.MiscCommands.PingCommand;
import com.ampro.main.bot.commands.MiscCommands.SelfDestructMessageCommand;
import com.ampro.main.bot.commands.MiscCommands.SpamCommand;
import com.ampro.main.database.Database;
import com.ampro.main.database.DatabaseManager;
import com.ampro.main.jda.JDABuilder;
import com.ampro.main.listener.EventDispatcher;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.commons.io.FileUtils;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Runner/Main class of the Weebot network.
 * Builds single JDA connection, instances of all {@link Command Commands}, and
 * {@link Database}.
 *
 * @author Jonathan Augustine
 */
public class Launcher {

	private static JDA JDA_CLIENT;
	private static Thread saveTimer;

	private static final ArrayList<Command> COMMANDS =
			new ArrayList<Command>(Arrays.asList(
					new HelpCommand(), new ShutdownCommand(),
					new ManageSettingsCommand(), new ListGuildsCommand(),
                    new PingCommand(), new SpamCommand(), new NotePadCommand(),
                    new SelfDestructMessageCommand()
			));

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
		//Launcher.jdaDevLogIn();
		Launcher.setUpDatabase();
		Launcher.setUpDirStructure();

		Collection c = DATABASE.getWeebots().values();
		Iterator it = c.iterator();
		System.out.println("Printing bots from database---------");
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		System.out.println("-------------------------------DONE");

		Launcher.startSaveTimer(.5);
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
       } catch (InterruptedException e) {
	       e.printStackTrace();
       }
   }

	/**
	 * Initiates {@code Launcher} data and connects to Weebot TestBuild API.
	 * Builds Asynchronously in a separate thread to let main thread work on
	 * other setup processes.
	 * @throws LoginException
	 * @throws InterruptedException
	 */
	private static void jdaDevLogIn() {
		try {
			//Connect to API
			JDABuilder builder = new JDABuilder(AccountType.BOT)
					.setToken("NDQ0MzIzNzMyMDEwMzAzNDg4.DdaQyQ.ztloAQmeuUffaC-DC9zE-LFwPq4");
			Launcher.JDA_CLIENT = builder.buildBlocking(Status.CONNECTED);
		} catch (LoginException e) {
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
		System.err.println("[Launcher-setUpDatabase]\n\tSetting up Database.");
		Database db = DatabaseManager.load();
		Launcher.DATABASE = db == null ? new Database() : db;
		if (db == null) {
			System.err.println("\tDatabase not found, creating new database.");
			System.err.println("\tLoading known Guilds");
			List<Guild> guilds = Launcher.JDA_CLIENT.getGuilds();
			for (Guild g : guilds) {
				Launcher.DATABASE.addBot(new Weebot(g));
			}
			DatabaseManager.save(Launcher.DATABASE);
			System.err.println("\tDatabase created and saved to file.");
			return;
		} else {
			System.err.println("\tDatabase located.");
			System.err.println("\tUpdating registered Guilds.");
			Launcher.updateGuilds();
		}
		System.err.println("\tBacking up database.");
		DatabaseManager.backUp(Launcher.DATABASE);
	}

	private static void setUpDirStructure() {
        new File("/temp/out").mkdirs();
        new File("/temp/in").mkdirs();
    }

    /**
     * Clears the temp folders.
     */
    private static void clearTempDirs() {
        try {
            FileUtils.cleanDirectory(new File("/temp"));
        } catch (IOException e) {
            System.err.println("Failed clear temp dir.");
        }
    }

	/**
	 * Update the Weebots in the database after downtime.
	 * <b>This is only called once on startup</b>
	 */
	private static void updateGuilds() {
		List<Guild> guilds = Launcher.JDA_CLIENT.getGuilds();
		for (Guild g : guilds) {
			Launcher.DATABASE.addBot(new Weebot(g));
		}
	}

	/**
	 * Starts a thread that saves a database backup each interval.
	 * <br> Listens for a shutdown event to save the the main file
	 * @param min The delay in minuets between saves.
	 */
	private static synchronized void startSaveTimer(double min) {
	   Launcher.saveTimer = new Thread( () -> {
			   try {
				   while(true) {
				       if (Launcher.getJda().getStatus() != Status.CONNECTED)
				           continue;
					   DatabaseManager.backUp(Launcher.DATABASE);
					   Thread.sleep(Math.round((1000 * 60) * min));
				   }
			   } catch (InterruptedException e) {
				   e.printStackTrace();
			   }
		   }
	   );
	   saveTimer.setName("Save Timer");
	   Launcher.saveTimer.start();
   }

	/**
	 * Begin the shutdown sequence. Backup and save database.
	 */
	public static void shutdown() {
		Launcher.JDA_CLIENT.shutdown();

		Launcher.saveTimer.interrupt();
		System.err.println("Shutdown signal received. Saving database.");
		DatabaseManager.backUp(Launcher.DATABASE);
		DatabaseManager.save(Launcher.DATABASE);

		System.err.println("Clearing temp directories.");
		Launcher.clearTempDirs();

		System.out.println("Successfully shutdown.");

		Collection c = DATABASE.getWeebots().values();
		Iterator it = c.iterator();
		System.out.println("Printing bots from database---------");
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		System.out.println("-------------------------------DONE");

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
		Iterable<Guild> it = Launcher.JDA_CLIENT.getGuilds();
		for (Guild g : it)
			if (g.getIdLong() == id)
				return g;

		return null;
	}

	public static ArrayList<Command> getCommands() {
		return Launcher.COMMANDS;
	}

	/**
	 * Get a {@link Command} from the available list.
	 * @param cl The class of the {@link Command}.
	 * @return  The {@link Command} that was requested.
	 *          null if not found.
	 */
	public static Command getCommand(Class<? extends Command> cl) {
		for (Command c : Launcher.COMMANDS) {
			if (c.getClass().equals(cl)) {
				return c;
			}
		}
		return null;
	}

	/** Get the JDA */
	public static JDA getJda() {
		return Launcher.JDA_CLIENT;
	}

}
