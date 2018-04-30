/**
 *
 */

package com.ampro.main.database;

import com.ampro.main.bot.Weebot;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.User;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A database object with all Weebot objects created
 */
public class Database {

	/** Name of the Database */
	private final String NAME;

    /** All Weebots currently in circulation */
    private List<Weebot> WEEBOTS;

    /**
     * Build a {@code Databse}.
     *
     * @param name Name of the database.
     */
    public Database(String name) {
    	this.NAME = name;
    	this.WEEBOTS = new ArrayList<>();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        new Database("").save();
    }

    /**
     *
     */
    public void save() throws FileNotFoundException, IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("\temp.ser"));
    }

    /**
     * Saves an object to the {@code Database}.
	     *
     * @param o The object to save
     */
    public void save(Object o) {

    }

    /**
     * Loads ./;l
     * @param o
     */
    public void load(Object o) {

    }

    /**
     * Adds a new {@code Weebot} to the {@code Database} with a link to an
     * {@code ISnowflake} ID of either {@code Guild} or {@code User}.
     * Does nothing if the link already exists.
     *
     * @param link The User or Guild to connect the bot to.
     * @param bot The bot
     */
    public void addBot(ISnowflake link, Weebot bot) {
        if (link instanceof User || link instanceof Guild)
            this.WEEBOTS.add(bot);
    }

    /** Get the name of the Database. */
    public String getName() {
    	return this.NAME;
    }

}
