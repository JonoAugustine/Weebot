/**
 * 
 */

package com.ampro.main.game.cardgame;

import java.util.ArrayList;

import com.ampro.main.game.Player;
import com.ampro.main.game.cardgame.*;

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
        ArrayList<CAHCard> cardsWon;

        /** Make new user */
        CAHPlayer(User user) {
            super(user);
        }

    }

    protected static class CAHCard extends Card {

        //The winning card of the round
        CAHCard winningCard;
        //White vs Black card
        enum CARDTYPE { BLACK, WHITE; }
        CARDTYPE type;
        //What the card says
        String cardText;

        /** 
         * Make a new card. 
         * @param type Black or White card type
         * @param text The text of the card
         */
        CAHCard(CARDTYPE type, String text) {
            this.cardText = text;
            this.type = type;
        }

        void setWinningCard(CAHCard winningCard) throws InvalidCardException {
            if (winningCard.type == CARDTYPE.BLACK)
                throw new InvalidCardException(
                    "Winning Card cannot be of type CAHCard.CARDTYPE.BLACK"
                    );
            else 
                this.winningCard = winningCard;
        }

    }

    //Cards delt to players
    private ArrayList<CAHCard> DECK_WHITE;
    //Cards pulled by the Tsar
    private ArrayList<CAHCard> DECK_BLACK;
    //How many cards per hand?
    private final int HAND_SIZE;
    //End after certain # of wins or rounds?
    public enum WIN_CONDITION {WINS, ROUNDS;}
    private WIN_CONDITION WIN_CONDITION;
    private CAHPlayer WINNER;

    /**
     * Initialize a new CardsAgainstHumanity game.
     */
    public CardsAgainstHumanity(User host, int handSize) {
        this.HAND_SIZE = handSize;
    }

    

    /**
     * Adds User to the game.
     * Gives the user a Player wrapper to interact with the game.
     * Deals the player cards
     * @param user User
     * @return -1 if the user could not be added
     *          0 if the user is already in the game
     *          1 if the user was added
     */
    public int joinGame(User user) {

        CAHPlayer player = new CAHPlayer(user);

        switch(super.joinGame(player)) {
            case -1: return -1;
            case 0: return 0;
            default:
                return this.dealCards(player);
        }

    }

    @Override
    public int startGame() {
        //Well you can't play CAH with less than 3 people
        if (this.PLAYERS.size() < 3) {
            return -1;
        }
        //More conditions for gamestart?
        this.RUNNING = true;
        //Oh yeah we need TODO the actual game
        return 1;
    }

    protected int dealCards(CAHPlayer player) {
        return -1; //TODO
    }

    /**
     * End the game, decide a winner.
     */
    protected int endGame() {
        for (Player p : this.PLAYERS) {
            //TODO
        }
        return -1;
    }

 
    /**
     * Change the win condition.
     * @param wincondition Number of wins or Rounds completed
     * @return Previous win condition
     * @throws ModificationWhileRunningException if game is running
     */
    public WIN_CONDITION setWinCondition(WIN_CONDITION wincondition)
        throws ModificationWhileRunningException {

        WIN_CONDITION prev = this.WIN_CONDITION;
        if (this.RUNNING)
            throw new ModificationWhileRunningException(
                "Cannot change win condition after game has started"
            );
        else
            this.WIN_CONDITION = wincondition;
        return prev;
    }
}