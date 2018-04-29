/**
 * 
 */

package com.ampro.main.game;

import java.util.List;
import java.util.ArrayList;

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

    //Is the game still running?
    protected boolean RUNNING;
    //Keep a list of all the Players
    protected ArrayList<P> PLAYERS;

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
        if (this.PLAYERS.contains(player))
            return 0;
        else
            return this.PLAYERS.add(player) ? 1 : -1;
    }

}