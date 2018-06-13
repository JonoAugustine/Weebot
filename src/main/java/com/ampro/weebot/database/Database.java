/**
 *
 */

package com.ampro.weebot.database;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.developer.WeebotSuggestionCommand.Suggestion;
import com.ampro.weebot.entities.bot.GlobalWeebot;
import com.ampro.weebot.entities.bot.Weebot;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A database for storing all the information about the Weebot program
 * between downtime.
 * TODO: add error log
 */
public class Database {

    /** Array of registered developer Discord IDs */
    private static final List<Long> DEV_IDS = new ArrayList<>(Arrays.asList(
            139167730237571072L /*JONO*/, 186130584693637131L /*DERNST*/
    ));

    /**
     * Map of all suggestions given through
     * {@link com.ampro.weebot.commands.developer.WeebotSuggestionCommand}
     */
    private final ConcurrentHashMap<OffsetDateTime, Suggestion> SUGGESTIONS;

    private final GlobalWeebot GLOBAL_WEEBOT;

    /** All Weebots currently in circulation, mapped to their Guild's ID */
    private final ConcurrentHashMap<Long, Weebot> WEEBOTS;

    private final List<Long> PREMIUM_USERS;

    /** Build an empty {@code Database}.*/
    public Database() {
        GLOBAL_WEEBOT = new GlobalWeebot();
        WEEBOTS = new ConcurrentHashMap<>();
        WEEBOTS.putIfAbsent(0L, new Weebot());
        SUGGESTIONS = new ConcurrentHashMap<>();
        PREMIUM_USERS = new ArrayList<>();
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
    public synchronized ConcurrentHashMap<Long, Weebot> getWeebots() {
        return this.WEEBOTS;
    }

    public synchronized GlobalWeebot getGlobalWeebot() { return GLOBAL_WEEBOT; }

    /**
     * @return ArrayList of registered developers.
     */
    public static synchronized List<Long> getDevelopers() {
        return DEV_IDS;
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

    public synchronized boolean isPremium(User user) {
        return PREMIUM_USERS.contains(user.getIdLong());
    }

    public synchronized boolean isPremium(Long userId) {
        return PREMIUM_USERS.contains(userId);
    }

    public synchronized List<Long> premiumUsers() {
        return Collections.unmodifiableList(PREMIUM_USERS);
    }

    public synchronized boolean addPremiumUser(User user) {
        return PREMIUM_USERS.add(user.getIdLong());
    }

    public synchronized boolean removePremiumUser(User user) {
        return PREMIUM_USERS.remove(user.getIdLong());
    }

    public synchronized void addSuggestion(Suggestion suggestion) {
        SUGGESTIONS.putIfAbsent(suggestion.getSubmitTime(), suggestion);
    }

    public ConcurrentHashMap<OffsetDateTime, Suggestion> getSuggestions() {
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
