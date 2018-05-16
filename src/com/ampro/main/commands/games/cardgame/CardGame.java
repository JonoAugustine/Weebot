/**
 *
 */

package com.ampro.main.commands.games.cardgame;

import com.ampro.main.commands.games.Game;
import com.ampro.main.commands.games.Player;
import com.ampro.main.entities.bot.Weebot;
import net.dv8tion.jda.core.entities.User;

/**
  *
  */
public abstract class CardGame<P extends Player, C extends Card>
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
     * Adds {@code player} to games and deals them {@code Card}s.
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
