/**
 *
 */

package com.ampro.main.game.cardgame;

import com.ampro.main.bot.Weebot;
import com.ampro.main.game.Player;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;

/**
 * A game of Cards Against Humanity.
 * @author Jonathan Augustine
 */
public class CardsAgainstHumanity
        extends CardGame<CardsAgainstHumanity.CAHPlayer
                        , CardsAgainstHumanity.CAHCard> {

    /**
     * The CardsAgainstHumanity Player. <br>
     * Holds a Hand of cards and a list of cards won.
     *
     */
    static class CAHPlayer extends Player {

        /**
         * Currently held cards
         * This is visible to the {2code Player} in their privateChannel
         * and to the bot. Do not expose this to the guild channel, since that
         * would show the hand to all members and that's dumb.
         */
        CAHCard[] HAND;
        //Card pairs from rounds won
        ArrayList<CAHCard> cardsWon;

        /** Make new user */
        CAHPlayer(User user) {
            super(user);
        }

    }

    static class CAHCard extends Card {

        //The winning card of the round
        CAHCard winningCard;
        //White vs Black card
        enum CARDTYPE { BLACK, WHITE
        }
        final CARDTYPE type;
        /** Content of the card */
        final String cardText;

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

    /** The hosting channel */
    private final Channel CHANNEL;

    //Cards delt to players
    private ArrayList<CAHCard> DECK_WHITE;
    //Cards pulled by the Tsar
    private ArrayList<CAHCard> DECK_BLACK;
    //How many cards per hand?
    private final int HAND_SIZE;
    //End after certain # of wins or rounds?
    public enum WIN_CONDITION {WINS, ROUNDS
    }
    private WIN_CONDITION WIN_CONDITION;
    private ArrayList<CAHPlayer> WINNERS;

    /**
     * Initialize a new CardsAgainstHumanity game.
     * @param bot Weebot hosting the game
     * @param channel TextChannel to play the game in
     * @param handSize Number of cards each play holds
     */
    public CardsAgainstHumanity(Weebot bot, TextChannel channel, int handSize) {
        super(bot);
        this.CHANNEL = channel;
        this.HAND_SIZE = handSize;
    }

    /**
     * Initialize a new CardsAgainstHumanity game.
     * @param bot Weebot hosting the game
     * @param channel TextChannel to play the game in
     * @param users Users to add to the game
     * @param handSize Number of cards each play holds
     */
    public CardsAgainstHumanity(Weebot bot, TextChannel channel, int handSize
                                , User...users) {
        super(bot);
        for (User u : users) {
            this.PLAYERS.putIfAbsent(u, new CAHPlayer(u));
        }
        this.CHANNEL = channel;
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
        ArrayList<CAHPlayer> players = this.playerIterable();
        CAHPlayer winner = this.PLAYERS.get(this.PLAYERS.firstKey());
        for (CAHPlayer p : players) {
            if (winner.cardsWon.size() < p.cardsWon.size())
                winner = p;
        }
        //Check for ties
        if (WIN_CONDITION == WIN_CONDITION.ROUNDS) {
            for (CAHPlayer p : players) {
                if (winner.cardsWon.size() == p.cardsWon.size())
                    this.WINNERS.add(p);
            }
        }
        return 0;
    }

    /**
     * Change the win condition.
     * @param wincondition Number of wins or Rounds completed
     * @return Previous win condition
     * @throws ModificationWhileRunningException if game is running
     */
    protected WIN_CONDITION setWinCondition(WIN_CONDITION wincondition)
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
