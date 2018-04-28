/**
 * 
 */

package Main.Bot.Games.CardGame;

import java.util.ArrayList;

import Main.Bot.Games.Player;
import Main.Bot.Games.CardGame.Card;
import net.dv8tion.jda.core.entities.User;

/**
 * A game of Cards Against Humanity.
 * 
 */
public class CardsAgainstHumanity 
        extends CardGame<CardsAgainstHumanity.CAHPlayer
                        , CardsAgainstHumanity.CAHCard> {
    
    protected static class CAHPlayer extends Player {

        //Current held cards
        CAHCard[] hand;
        //Card pairs from rounds won
        ArrayList<CAHCard> trophies;

        

    }

    protected static class CAHCard extends Card {

        //The winning card (Black cards only)
        CAHCard winningCard;
        //White vs Black card
        enum CARDTYPE { BLACK, WHITE; }
        CARDTYPE type;
        //What the card says
        String cardText;

        CAHCard(CARDTYPE type, String text) {
            this.cardText = text;
            this.type = type;
        }

    }

    //Cards delt to players
    private ArrayList<CAHCard> DECK_WHITE;
    //Cards pulled by the Tsar
    private ArrayList<CAHCard> DECK_BLACK;

    /**
     * Adds User to the game.
     * Gives the user a Player to interact with the game
     */
    public int joinGame(User user) {

        

        int retu = super.joinGame(player);

        return retu;
    }

    @Override
    protected int dealCards(CAHPlayer player) {

    }
}