/**
 *
 */

package com.ampro.weebot.commands.games.cardgame;

import com.ampro.weebot.commands.games.Game;
import com.ampro.weebot.commands.games.Player;
import com.ampro.weebot.bot.Weebot;
import net.dv8tion.jda.core.entities.User;

/**
  *
  */
public abstract class CardGame<P extends Player>
                extends Game<P> {

    /**
     * Create a new CardGame.
     * @param bot Weebot hosting the games
     */
    CardGame(Weebot bot, User author) {
        super(bot, author);
    }

    /**
     * Create a new CardGame and add with players.
     * @param bot Weebot hosting the games
     * @param p Players to add to the games
     */
    protected CardGame(Weebot bot, User author, P...p) {
        super(bot, author, p);
    }

    /**
     * Adds {@code player} to games.
     * If the game is running they are dealt {@code Card cards}.
     */
    @Override
    protected boolean joinGame(P player) {
        if (super.joinGame(player)) {
            if (this.RUNNING) {
                this.dealCards(player);
            }
            return true;
        } else
            return false;
    }

    /**
     * Deal cards to a player
     * @param player The player to deal cards to.
     * @return False if the player already has a full hand.
     */
    protected abstract boolean dealCards(P player);

}
