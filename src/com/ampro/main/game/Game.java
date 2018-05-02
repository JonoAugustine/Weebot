/**
 *
 */

package com.ampro.main.game;

import com.ampro.main.comparators.Comparators;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Basis of a Weebot Game.
 * Must be connected to a single type of Player.
 *
 * @param <P> A class that extends {@code Player}
 */
public abstract class Game<P extends Player> {

    /** Don't let some things be modified while the game is running */
    protected static class ModificationWhileRunningException extends Exception {
        private static final long serialVersionUID = 1549072265432776147L;
        /** Parameterless constructor */
        public ModificationWhileRunningException() {}
        /** Constructor with message */
        public ModificationWhileRunningException(String err) { super(err); }
    }

    /** The ID of the hosting bot */
    protected String HOST_ID;

    /** Is the game currently running? */
    protected boolean RUNNING;
    //Keep a list of all the Players
    protected TreeMap<User, P> PLAYERS;

    /** Create a game.
     *  Has empty Players list.
     *  is not running.
     */
    public Game() {
        this.RUNNING = false;
        this.PLAYERS = new TreeMap<>(new Comparators.UserIdComparator());
    }

    /**
     * Create a game with an initial set of {@code Player}s
     * @param p Array of {@code Players}
     */
    public Game(P...p) {
        this.RUNNING = false;
        for (int i = 0; i < p.length; i++) {
            this.getPlayers().putIfAbsent(p[i].getUser(), p);
        }
    }

    //Some Very important but vague methods to implement in child.
    protected abstract int startGame();
    protected abstract int endGame();

    /**
     * Add {@code Player} to the {@code Game}.
     * This method only attempts to add the player, any more action
     * should be implemented in a class-specific implementation.
     * @param player The player to add
     * @return -1 if player could not be added. <br>
     *          0 if player is already in the Game. <br>
     *          1 if player was added to the Game.
     */
    protected int joinGame(P player) {
        if (false /** What should deny joining? */)
            return -1;
        if (this.PLAYERS.containsValue(player))
            return 0;
        else
            return this.PLAYERS.putIfAbsent(
                    player.getUser(), player) == null ? 1 : -1;
    }

    /** The current players */
    public TreeMap getPlayers() {
        return this.PLAYERS;
    }

    /** Is the game ongoing? */
    public boolean isRunning() {
        return this.RUNNING;
    }

    public ArrayList playerIterable() {
        ArrayList<P> retu = new ArrayList<>();
        for(Map.Entry<User, P> entry : this.PLAYERS.entrySet())
            retu.add(entry.getValue());
        return retu;
    }

}
