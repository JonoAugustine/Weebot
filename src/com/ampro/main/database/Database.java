/**
 * 
 */

package com.ampro.main.database;

import java.util.Comparator;
import java.util.TreeMap;

import com.ampro.main.bot.Weebot;

import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

/**
 * A database object with all Weebot objects created
 */
public class Database {
   
    //All Weebots currently in circulation
    private TreeMap<Long, Weebot> WEEBOTS;

    /**
     * Adds a new {@code Weebot} to the {@code Database} with a link to an
     * {@code ISnowflake} ID of either {@code Guild} or {@code User}.
     * Does nothing if the link already exists.
     * @param link The User or Guild to connect the bot to.
     * @param bot The bot
     */
    public void addBot(ISnowflake link, Weebot bot) {
        if (link instanceof User || link instanceof Guild)
            this.WEEBOTS.put(link.getIdLong(), bot);
    }

}
