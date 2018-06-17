/**
 *
 */

package com.ampro.weebot.commands.games;

import com.ampro.weebot.bot.Weebot;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basis of a Weebot Game.
 * Must be connected to a single type of Player.
 *
 * @param <P> A class that extends {@code Player}
 */
public abstract class Game<P extends Player> {

    /** Don't let some things be modified while the game is running */
    public static class ModificationWhileRunningException extends Exception {
        private static final long serialVersionUID = 1549072265432776147L;
        /** Parameterless constructor */
        public ModificationWhileRunningException() {}
        /** Constructor with message */
        public ModificationWhileRunningException(String err) { super(err); }

    }

    /** The ID of the hosting bot */
    protected final String HOST_ID;
    /** User ID of the User who started the game.*/
    protected final long AUTHOR_ID;
    //Keep a list of all the Players
    protected final ConcurrentHashMap<Long, P> PLAYERS;
    /** Is the game currently running? */
    protected boolean RUNNING;

    /** Create a game.
     *  Has empty Players list.
     * Starts not Running.
     * @param bot Weebot hosting the game
     */
    protected Game(Weebot bot, User author) {
        this.HOST_ID = bot.getBotId();
        this.RUNNING = false;
        this.PLAYERS = new ConcurrentHashMap<>();
        this.AUTHOR_ID = author.getIdLong();
        this.addUser(author);
    }

    /**
     * Create a game with an initial set of {@code Player}s
     * @param bot Weebot hosting game
     * @param players {@code Players} to add to the game
     */
    protected Game(Weebot bot, User author, P... players) {
        this.HOST_ID = bot.getBotId();
        this.RUNNING = false;
        this.PLAYERS = new ConcurrentHashMap<>();
        for (P p : players) {
            this.PLAYERS.putIfAbsent(p.getUser().getIdLong(), p);
        }
        this.AUTHOR_ID = author.getIdLong();
        this.addUser(author);
    }

    //Some Very important but vague methods to implement in child.
    protected abstract boolean startGame();
    protected abstract boolean endGame();

    /**
     * Add a user to the game, wrapping the {@link User} in a new {@link Player}
     * implementation.
     * @param user The user to add.
     * @return false if the user could not be added.
     */
    protected abstract boolean addUser(User user);

    /**
     * Add {@code Player} to the {@code Game}.
     * This method only attempts to add the player, any more action
     * should be implemented in a class-specific implementation.
     * @param player The player to add
     * @return  false if player is already in the Game. <br>
     *          true if player was added to the Game.
     */
    protected boolean joinGame(P player) {
        return this.PLAYERS.putIfAbsent(player.getUser().getIdLong(), player) == null
                   ? true : false;
    }

    /** Is the game ongoing? */
    public boolean isRunning() {
        return this.RUNNING;
    }

    /** Get an Iterable Arraylist of the players */
    public ArrayList<P> playerIterable() {
        return new ArrayList<>(this.PLAYERS.values());
    }

    public String getHOST_ID() {
        return HOST_ID;
    }

    public long getAUTHOR_ID() {
        return AUTHOR_ID;
    }

    public ConcurrentHashMap<Long, P> getPlayers() { return this.PLAYERS; }

    /**
     * Get a player.
     * @param user The user who's player to get.
     * @return The player or null if no player is found.
     */
    public P getPlayer(User user) {
        return this.PLAYERS.get(user.getIdLong());
    }

}
