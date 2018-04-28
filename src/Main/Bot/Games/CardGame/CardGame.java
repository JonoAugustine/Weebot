/**
 * 
 */

 package Main.Bot.Games.CardGame;

import java.util.ArrayList;

import Main.Bot.Games.Game;
import Main.Bot.Games.Player;
import Main.Bot.Games.CardGame.Card;

/**
  * 
  */
public abstract class CardGame<P extends Player, C extends Card> 
                extends Game<P> {

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