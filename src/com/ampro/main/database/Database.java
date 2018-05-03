/**
 *
 */

package com.ampro.main.database;

import com.ampro.main.bot.Weebot;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * A database for storing all the information about the Weebot program
 * between downtimes.
 */
public class Database {

	/** Name of the Database */
	private final String NAME;

    /** All Weebots currently in circulation, mapped to their Guild's ID */
    private TreeMap<Long, Weebot> WEEBOTS;

    /** Array of registered developer Discord IDs */
    private static ArrayList<Long> DEV_IDS;

    /**
     * Build a {@code Databse}.
     *
     * @param name Name of the database.
     */
    public Database(String name) {
    	this.NAME = name;
    	this.WEEBOTS = new TreeMap<>();
    	this.DEV_IDS = new ArrayList<>();
    	this.DEV_IDS.add(139167730237571072L); //Jono
        this.DEV_IDS.add(186130584693637131L); //Dernst
    }

    /**
     * Adds a new {@code Weebot} to the {@code Database}.
     * Does nothing if the link already exists.
     *
     * @param bot The bot
     */
    public synchronized void addBot(Weebot bot) {
        this.WEEBOTS.putIfAbsent(bot.getGuildID(), bot);
    }

    /**
     * Remove a Weebot from the database.
     * @param bot Weebot to remove.
     */
    public synchronized void removeBot(Weebot bot) {
        this.WEEBOTS.remove(bot.getGuildID());
    }

    /**
     * Get the database's Weebots.
     * @return TreeMap of Weebot's mapped to their GuilID
     */
    public synchronized TreeMap getWeebots() {
        return this.WEEBOTS;
    }

    /**
     * Add a developer ID
     * @param id long user ID
     */
    public synchronized void addDeveloper(long id) {
        this.DEV_IDS.add(id);
    }

    /**
     * Removes a developer by ID.
     * @param id long user ID
     */
    public synchronized void removeDeveloper(long id) {
        this.DEV_IDS.remove(id);
    }

    /**
     * @return ArrayList of registered developers.
     */
    public synchronized ArrayList getDevelopers() {
        return this.DEV_IDS;
    }

    /** Get the name of the Database. */
    public String getName() {
    	return this.NAME;
    }

    /** */
    @Override
    public String toString() {
        return this.NAME;
    }
}
