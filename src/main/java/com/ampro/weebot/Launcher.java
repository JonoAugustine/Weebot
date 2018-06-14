/**
 * @author Jonathan Augustine
 * @copyright Aquatic Mastery Productions
 */

package com.ampro.weebot;

import com.ampro.weebot.commands.ChatbotCommand;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.HelpCommand;
import com.ampro.weebot.commands.NotePadCommand;
import com.ampro.weebot.commands.developer.DatabaseFileCommand;
import com.ampro.weebot.commands.developer.ListGuildsCommand;
import com.ampro.weebot.commands.developer.ShutdownCommand;
import com.ampro.weebot.commands.developer.WeebotSuggestionCommand;
import com.ampro.weebot.commands.games.SecretePhraseCommand;
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand;
import com.ampro.weebot.commands.management.AutoAdminCommand;
import com.ampro.weebot.commands.management.ManageSettingsCommand;
import com.ampro.weebot.commands.miscellaneous.*;
import com.ampro.weebot.database.Database;
import com.ampro.weebot.database.DatabaseManager;
import com.ampro.weebot.entities.bot.GlobalWeebot;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.jda.JDABuilder;
import com.ampro.weebot.listener.EventDispatcher;
import com.ampro.weebot.util.Logger;
import com.ampro.weebot.util.io.FileManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ampro.weebot.database.DatabaseManager.DIR_DBS;
import static com.ampro.weebot.util.Logger.LOGS;
import static com.ampro.weebot.util.io.FileManager.*;

/**
 * Runner/Main class of the Weebot network.
 * Builds single JDA connection, instances of all {@link Command Commands}, and
 * {@link Database}.
 *
 * @author Jonathan Augustine
 */
public class Launcher {

	private static final String TOKEN_WBT
			= "NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk";
	private static final String TOKEN_TEST
            = "NDQ0MzIzNzMyMDEwMzAzNDg4.DdaQyQ.ztloAQmeuUffaC-DC9zE-LFwPq4";

	private static JDA JDA_CLIENT;
	private static Thread saveTimer;

	public static GlobalWeebot GLOBAL_WEEBOT;

	private static final ArrayList<Command> COMMANDS =
			new ArrayList<>(Arrays.asList(
					new HelpCommand(), new ShutdownCommand(), new DatabaseFileCommand(),
					new AutoAdminCommand(),new ManageSettingsCommand(),
					new ListGuildsCommand(), new PingCommand(), new SpamCommand(),
					new NotePadCommand(), new SelfDestructMessageCommand(),
					new SecretePhraseCommand(), new WeebotSuggestionCommand(),
					new CardsAgainstHumanityCommand(), new OutHouseCommand(),
                    new ChatbotCommand(), new CalculatorCommand(), new ReminderCommand()
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
   public static void main(final String[] args)
   throws LoginException, InterruptedException {
       System.err.println("[Launcher] Building Directories...");
       if(!Launcher.buildDirs()) {
           System.err.println("[Launcher]\tFAILED!");
           System.exit(-1);
       }
       System.err.println("[Launcher] Initializing Logger...");
       if(!Logger.init()) {
           System.err.println("[Launcher]\tLogger failed to initialize!");
       }
       //Debug
       //RestAction.setPassContext(true); // enable context by default
       //RestAction.DEFAULT_FAILURE = Throwable::printStackTrace;
       Launcher.jdaLogIn();
       //Launcher.jdaDevLogIn();
       Launcher.setUpDatabase();
       Launcher.updateWeebots();
       Launcher.startSaveTimer(.5);
       Launcher.addListeners();

       Logger.derr("[Launcher] Initialization Complete!\n\n");
   }

    /**
     * Build the all file directories used by Weebot.
     * @return {@code false} if any directory is not created (and did not already exist)
     */
    private static boolean buildDirs() {
        if (!DIR_DBS.exists())
            DIR_DBS.mkdirs();
        if (!LOGS.exists())
            LOGS.mkdirs();
        if (!TEMP_OUT.exists())
            TEMP_OUT.mkdirs();
        if (!TEMP_IN.exists())
            TEMP_IN.mkdirs();
        return DIR_HOME.exists() && TEMP_OUT.exists() && TEMP_IN.exists() && DIR_DBS
                .exists() && LOGS.exists();
    }

   /**
    * Initiates {@code Launcher} data and connects to Weebot API.
    * Builds Asynchronously in a separate thread to let main thread work on
    * other setup processes.
    * @throws LoginException
    * @throws InterruptedException
    */
   private static void jdaLogIn()
   throws LoginException, InterruptedException {
       //Connect to API
       Logger.derr("[Launcher] Logging in to Weebot JDA client...");
       JDABuilder builder = new JDABuilder(AccountType.BOT)
                   .setToken(TOKEN_WBT).setGame(Game.playing("@Weebot help"));
       JDA_CLIENT = builder.buildBlocking(Status.CONNECTED);
   }

	/**
	 * Initiates {@code Launcher} data and connects to Weebot TestBuild API.
	 * Builds Asynchronously in a separate thread to let main thread work on
	 * other setup processes.
	 * @throws LoginException
	 * @throws InterruptedException
	 */
	private static void jdaDevLogIn()
    throws LoginException, InterruptedException {
        //Connect to API
        Logger.derr("[Launcher] Logging in to TestBot JDA client...");
        JDABuilder builder = new JDABuilder(AccountType.BOT).setToken(TOKEN_TEST);
        Launcher.JDA_CLIENT = builder.buildBlocking(Status.CONNECTED);
	}

	/**
	 * Adds event listeners to the JDA.
	 */
	private static void addListeners() {
        Logger.derr("[Launcher] Adding Listeners to JDA Client...");
   		Launcher.JDA_CLIENT.addEventListener(new EventDispatcher(), GLOBAL_WEEBOT);
   }

	/**
	 * Attempts to load a database from file. <br>
	 * If a database could not be loaded, a new one is created. <br>
	 * Is called only once during setup.
	 */
	private static void setUpDatabase() {
	    String f = "[Launcher#setUpDatabase]";
        Logger.derr(f + " Setting up Database...");
		Logger.derr(f + "\tLoading database...");
		Database db = DatabaseManager.load();
		Launcher.DATABASE = db == null ? new Database() : db;
		if (db == null) {
            Logger.derr(f + "\t\tUnable to load database, creating new database.");
			Logger.derr(f + "\t\tLoading known Guilds");
			List<Guild> guilds = Launcher.JDA_CLIENT.getGuilds();
			for (Guild g : guilds) {
				Launcher.DATABASE.addBot(new Weebot(g));
			}
			DatabaseManager.save(Launcher.DATABASE);
			Logger.derr(f + "\tDatabase created and saved to file.");
		} else {
			Logger.derr(f + "\tDatabase located.");
            Logger.derr(f + "\tUpdating registered Guilds.");
			Launcher.updateGuilds();
		}
		Logger.derr(f + "\tBacking up database.");
		DatabaseManager.backUp(Launcher.DATABASE);
		Launcher.GLOBAL_WEEBOT = DATABASE.getGlobalWeebot() == null
                        ? new GlobalWeebot() : DATABASE.getGlobalWeebot();
	}

    /**
     * Update the Weebots in the database after downtime.
     * <b>This is only called once on startup</b>
     */
    private static void updateGuilds() {
        List<Guild> guilds = Launcher.JDA_CLIENT.getGuilds();
        guilds.forEach(g -> Launcher.DATABASE.addBot(new Weebot(g)));
    }

	/**
	 * Calls the update method for each Weebot to setup NickNames
	 * changed during downtime and initialize transient variables.
	 */
	private static void updateWeebots() {
        Logger.derr("[Launcher] Updating Weebots...");
	    DATABASE.getWeebots().forEach( (id, bot) -> bot.startup() );
		DATABASE.getGlobalWeebot().startup();
    }

	/**
	 * Starts a thread that saves a database backup each interval.
	 * <br> Listens for a shutdown event to save the the main file
	 * @param min The delay in minuets between saves.
	 */
	private static synchronized void startSaveTimer(double min) {
	   Launcher.saveTimer = new Thread( () -> {
		   int i = 1;
		   long time = Math.round((1000 * 60) * min);
		   try {
			   while (true) {
			       if (Launcher.JDA_CLIENT.getStatus()  == Status.SHUTDOWN)
			           break;
				   if(Launcher.getJda().getStatus() != Status.CONNECTED)
					   continue;
				   DatabaseManager.backUp(Launcher.DATABASE);
				   if(i % 10 == 0) {
                       System.err.println("Database back up: " + i);
                   }
                   i++;
                   Thread.sleep(time);
			   }
		   } catch (InterruptedException e) {
			   e.printStackTrace();
		   }
	   });
	   saveTimer.setName("Save Timer");
	   Launcher.saveTimer.start();
   }

	/** Begin the shutdown sequence. Backup and save database. */
	public static void shutdown() {
	    String f = "[Launcher#shutdown]";
        Logger.derr(f + " Shutdown signal received.");
        Logger.derr(f + "\tClearing registered event listeners...");
        for (Object o : Launcher.JDA_CLIENT.getRegisteredListeners())
		    JDA_CLIENT.removeEventListener(o);
        Logger.derr(f + "\tStopping save timer thread...");
        Launcher.saveTimer.interrupt();
        Logger.derr(f + "\tShutting down Global Weebot Reminder pools...");
		DATABASE.getGlobalWeebot().getReminderPools().forEach(
		        (id, pool) -> pool.shutdown()
        );
        Logger.derr(f + "\tBacking up database...");
		if (DatabaseManager.backUp(Launcher.DATABASE) < 1)
            Logger.derr(f + "\t\tFailed to backup database!");
		else {
            Logger.derr(f + "\tSaving Database...");
            switch (DatabaseManager.save(Launcher.DATABASE)) {
                case -1:
                    Logger.derr(f + "\t\tCould not save backup due to file exception.");
                    break;
                case -2:
                    Logger.derr(f + "\t\tCould not save backup due to corrupt Json.");
                    break;
                default:
                    Logger.derr(f + "\t\tDatabase saved.");
            }
        }

		Logger.derr(f + "\tClearing temp directories...");
		FileManager.clearTempDirs();

        Logger.derr(f + "Successfully shutdown.");

        JDA_CLIENT.shutdown();

	}

	/** @return The acting database. */
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
		return Database.getDevelopers().contains(id);
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

	/**@return EmbedBuilder with the standard green, Author set to "Weebot", and footer */
	public static EmbedBuilder getStandardEmbedBuilder() {
		return new EmbedBuilder()
				.setColor(new Color(0x31FF00))
				.setAuthor("Weebot", null, JDA_CLIENT.getSelfUser().getAvatarUrl())
				.setFooter("Run by Weebot", JDA_CLIENT.getSelfUser().getAvatarUrl());
	}

	/**
	 * Makes a standard format EmbedBuilder with standard color and author
	 * , the given title, title URL, and description.
	 * @param title The title of the Embed
	 * @param titleLink The site to link to in the Title
	 * @param description The description that appears under the title
	 * @return A Weebot-standard EmbedBuilder
	 */
	public static EmbedBuilder makeEmbedBuilder(String title,
                                                String titleLink,
                                                String description) {
		return getStandardEmbedBuilder()
				.setTitle(title, titleLink)
				.setDescription(description);

	}

	/**
	 * Makes a standard format EmbedBuilder with standard color and author
	 * , the given title, title URL, and description.
	 * @param title The title of the Embed
	 * @param titleURL The site to link to in the Title
	 * @param description The description that appears under the title
	 * @return A Weebot-standard EmbedBuilder
	 */
	public static EmbedBuilder makeEmbedBuilder(String title, String titleURL,
                                                StringBuilder description) {
		return makeEmbedBuilder(title, titleURL, description.toString());
	}


}
