/**
 *
 */

package com.ampro.main.game.cardgame;

import com.ampro.main.bot.Weebot;
import com.ampro.main.game.Game;
import com.ampro.main.game.Player;

/**
  *
  */
public abstract class CardGame<P extends Player, C extends Card>
                extends Game<P> {

    /**
     * Create a new CardGame.
     * @param bot Weebot hosting the game
     */
    protected CardGame(Weebot bot) {
        super(bot);
    }

    /**
     * Create a new CardGame and add with players.
     * @param bot Weebot hosting the game
     * @param p Players to add to the game
     */
    protected CardGame(Weebot bot, P...p) {
        super(bot, p);
    }

    /**
     * Adds {@code player} to game and deals them {@code Card}s.
     *
     * @param player Player to add
     * @return -1 if player could not be added
     *          0 if player is already in Game
     *          1 if player was added
     */
    @Override
    protected int joinGame(P player) {
        switch(super.joinGame(player)) {
            case -1:
                return -1;
            case 0:
                return 0;
            default:
                this.dealCards(player);
                return 1;
        }
    }

    protected abstract int dealCards(P player);

}
