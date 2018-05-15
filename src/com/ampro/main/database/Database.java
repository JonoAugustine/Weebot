/**
 *
 */

package com.ampro.main.database;

import com.ampro.main.Launcher;
import com.ampro.main.commands.MiscCommands.WeebotSuggestionCommand.Suggestion;
import com.ampro.main.entities.bot.Weebot;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * A database for storing all the information about the Weebot program
 * between downtime.
 */
public class Database {

    /** All Weebots currently in circulation, mapped to their Guild's ID */
    private final TreeMap<Long, Weebot> WEEBOTS;

    /** Array of registered developer Discord IDs */
    private final ArrayList<Long> DEV_IDS;

    private final TreeMap<OffsetDateTime, Suggestion> SUGGESTIONS;

    /** Build an empty {@code Database}.*/
    public Database() {
        WEEBOTS = new TreeMap<>();
        WEEBOTS.putIfAbsent(0L, new Weebot());
    	DEV_IDS = new ArrayList<>();
    	DEV_IDS.add(139167730237571072L); //Jono
        DEV_IDS.add(186130584693637131L); //Dernst
        SUGGESTIONS = new TreeMap<>();
    }

    /**
     * Adds a new {@code Weebot} to the {@code Database}.
     * Does nothing if the link already exists.
     *
     * @param bot The bot
     */
    public synchronized void addBot(Weebot bot) {
        if (!this.WEEBOTS.containsValue(bot))
            this.WEEBOTS.putIfAbsent(bot.getGuildID(), bot);
    }

    /**
     * Retrieve a Weebot form the database by it's hosting guild's ID.
     * @param id Hosting guild ID
     * @return The weebot associated with the given guild ID. Null if not found.
     */
    public synchronized Weebot getBot(long id) {
        return this.WEEBOTS.get(id);
    }

    /**
     * Remove a Weebot from the database.
     * @param bot Weebot to remove.
     * @return The removed Weebot.
     */
    public synchronized Weebot removeBot(Weebot bot) {
        return this.WEEBOTS.remove(bot.getGuildID());
    }

    /**
     * Remove a bot from the database.
     * @param id The hosting Guild ID
     * @return The removed Weebot.
     */
    public synchronized Weebot removeBot(long id) {
        return this.WEEBOTS.remove(id);
    }

    /**
     * Get the database's Weebots.
     * @return TreeMap of Weebot's mapped to their Guild ID
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
     * @return The removed id
     */
    public synchronized long removeDeveloper(long id) {
        return this.DEV_IDS.remove(DEV_IDS.indexOf(id));
    }

    public synchronized void addSuggestion(Suggestion suggestion) {
        SUGGESTIONS.putIfAbsent(suggestion.getSubmitTime(), suggestion);
    }

    public TreeMap<OffsetDateTime, Suggestion> getSuggestions() {
        return SUGGESTIONS;
    }

    public synchronized Suggestion removeSuggestion(Suggestion suggestion) {
        return SUGGESTIONS.remove(suggestion.getSubmitTime());
    }

    public synchronized ArrayList<Suggestion> clearUserSuggestions(User user) {
        ArrayList<Suggestion> out = new ArrayList<>();
        for (Suggestion s : SUGGESTIONS.values()) {
            if (Launcher.getJda().getUserById(s.getAuthorID()).getIdLong()
                    == user.getIdLong()) {
                out.add(SUGGESTIONS.remove(s.getSubmitTime()));
            }
        }
        return out;
    }

    /**
     * @return ArrayList of registered developers.
     */
    public synchronized ArrayList<Long> getDevelopers() {
        return this.DEV_IDS;
    }

    @Override
    public String toString() {
        String out = "";
        out += "[";

        for(Map.Entry<Long, Weebot> entry : this.WEEBOTS.entrySet()) {
            long key = entry.getKey();
            Weebot bot = entry.getValue();

            out.concat("[" + key + "," + bot.getNickname() + "]");
        }

        out += "]";

        return out;
    }
}
