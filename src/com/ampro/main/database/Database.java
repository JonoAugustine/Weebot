/**
 *
 */

package com.ampro.main.database;

import com.ampro.main.bot.Weebot;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * A database for storing all the information about the Weebot program
 * between downtime.
 */
public class Database {

    /** All Weebots currently in circulation, mapped to their Guild's ID */
    private static TreeMap<Long, Weebot> WEEBOTS;

    /** Array of registered developer Discord IDs */
    private static ArrayList<Long> DEV_IDS;

    /** Build an empty {@code Database}.*/
    public Database() {
        Database.WEEBOTS = new TreeMap<>();
        Database.WEEBOTS.putIfAbsent(0L, new Weebot());
    	Database.DEV_IDS = new ArrayList<>();
    	Database.DEV_IDS.add(139167730237571072L); //Jono
        Database.DEV_IDS.add(186130584693637131L); //Dernst
    }

    /**
     * Adds a new {@code Weebot} to the {@code Database}.
     * Does nothing if the link already exists.
     *
     * @param bot The bot
     */
    public synchronized void addBot(Weebot bot) {
        if (!Database.WEEBOTS.containsValue(bot))
            Database.WEEBOTS.putIfAbsent(bot.getGuildID(), bot);
    }

    /**
     * Remove a Weebot from the database.
     * @param bot Weebot to remove.
     * @return The removed Weebot.
     */
    public synchronized Weebot removeBot(Weebot bot) {
        return Database.WEEBOTS.remove(bot.getGuildID());
    }

    /**
     * Remove a bot from the database.
     * @param long The hosting Guild ID
     * @return The removed Weebot.
     */
    public synchronized Weebot removeBot(long guildId) {
        return Database.WEEBOTS.remove(guildId);
    }

    /**
     * Get the database's Weebots.
     * @return TreeMap of Weebot's mapped to their Guild ID
     */
    public synchronized TreeMap getWeebots() {
        return Database.WEEBOTS;
    }

    /**
     * Add a developer ID
     * @param id long user ID
     */
    public synchronized void addDeveloper(long id) {
        Database.DEV_IDS.add(id);
    }

    /**
     * Removes a developer by ID.
     * @param id long user ID
     * @return The removed id
     */
    public synchronized long removeDeveloper(long id) {
        return Database.DEV_IDS.remove(DEV_IDS.indexOf(id));
    }

    /**
     * @return ArrayList of registered developers.
     */
    public synchronized ArrayList getDevelopers() {
        return Database.DEV_IDS;
    }

    /** */
    @Override
    public String toString() {
       	return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
