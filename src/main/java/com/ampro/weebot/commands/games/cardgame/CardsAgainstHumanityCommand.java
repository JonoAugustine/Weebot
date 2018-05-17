/**
 *
 */

package com.ampro.weebot.commands.games.cardgame;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.games.Player;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Run Cards Against Humanity games, TODO and make custom cards.
 */
public class CardsAgainstHumanityCommand extends Command {

    /**
     * A game of Cards Against Humanity.
     * TODO How to get a standard list of black and white cards w/o literally writing it
     * @author Jonathan Augustine
     */
    public static final class CardsAgainstHumanity
            extends CardGame<CardsAgainstHumanity.CAHPlayer
                            ,CardsAgainstHumanity.CAHCard> {

        /**
         * The CardsAgainstHumanity Player. <br>
         * Holds a Hand of cards and a list of cards won.
         *
         */
        final class CAHPlayer extends Player {

            /**
             * Currently held cards
             * This is visible to the {2code Player} in their privateChannel
             * and to the bot. Do not expose this to the guild channel, since that
             * would show the hand to all members and that's dumb.
             */
            CAHCard[] hand;
            //Card pairs from rounds won
            final List<CAHCard> cardsWon;

            /** Make new user */
            CAHPlayer(User user) {
                super(user);
                this.hand = new CAHCard[CardsAgainstHumanity.this.HAND_SIZE];
                this.cardsWon = new ArrayList<>();
            }

        }

        public static final class CAHCard extends Card {

            //White vs Black card
            enum CARDTYPE { BLACK, WHITE }
            /** White or Black card */
            final CARDTYPE type;

            /** Content of the card */
            final String cardText;

            /**
             * The winning {@link CARDTYPE#WHITE white card} of the round
             * (Applies to {@link CARDTYPE#BLACK black cards})
             */
            CAHCard winningCard;
            /**
             * The number of cards needed to fill in the card
             * (Applies to {@link CARDTYPE#BLACK black cards})
             */
            final int blanks;

            static final CAHCard makeBlackCard(String text, int blanks)
            throws InvalidCardException {
                if (blanks < 1) {
                    throw new InvalidCardException(
                            "Black Card needs at least one blank space."
                    );
                } else if (text.isEmpty()) {
                    throw new InvalidCardException("Card text cannot be empty!");
                } else {
                    return new CAHCard(CARDTYPE.BLACK, text, blanks);
                }
            }

            static final CAHCard makeWhiteCard(String text) throws InvalidCardException {
                if (text.isEmpty()) {
                    throw new InvalidCardException("Card text cannot be empty!");
                } else {
                    return new CAHCard(CARDTYPE.WHITE, text, -1);
                }
            }

            CAHCard(CARDTYPE type, String text, int blanks) {
                this.type = type;
                this.cardText = text;
                this.blanks = blanks;
            }

            void setWinningCard(CAHCard winningCard)
                    throws InvalidCardException {
                if (winningCard.type == CARDTYPE.BLACK)
                    throw new InvalidCardException(
                            "Winning Card cannot be of type CAHCard.CARDTYPE.BLACK"
                    );
                else
                    this.winningCard = winningCard;
            }

        }

        public enum WIN_CONDITION {WINS, ROUNDS}

        public enum GAME_STATE {
            /** Players are choosing white cards */
            CHOOSING,
            /** Czar is reading played white cards */
            READING

        }

        private static final int MIN_PLAYERS = 3;
        /** The hosting channel */
        private final Channel CHANNEL;
        //Cards delt to players
        private ArrayList<CAHCard> DECK_WHITE;
        //Cards pulled by the Tsar
        private ArrayList<CAHCard> DECK_BLACK;
        //How many cards per hand?
        private final int HAND_SIZE;
        //End after certain # of wins or rounds?
        private WIN_CONDITION WIN_CONDITION;
        /** The winners of the game */
        private ArrayList<CAHPlayer> WINNERS;

        /**
         * Initialize a new CardsAgainstHumanity game.
         * @param bot Weebot hosting the game
         * @param channel TextChannel to play the game in
         * @param handSize Number of cards each play holds
         */
        public CardsAgainstHumanity(Weebot bot, TextChannel channel, User author,
                                    int handSize) {
            super(bot, author);
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
        public CardsAgainstHumanity(Weebot bot, TextChannel channel, User author, int handSize
                , User...users) {
            super(bot, author);
            for (User u : users) {
                this.PLAYERS.putIfAbsent(u.getIdLong(), new CAHPlayer(u));
            }
            this.CHANNEL = channel;
            this.HAND_SIZE = handSize;
        }

        @Override
        protected boolean dealCards(CAHPlayer player) {
            boolean delt = false;
            for (CAHCard c : player.hand) {
                if (c == null) {
                    c = randomCard(CAHCard.CARDTYPE.WHITE);
                    delt = true;
                }
            }
            return delt;
        }

        /**
         * @return a random {@link CAHCard.CARDTYPE#WHITE white} or
         *         {@link CAHCard.CARDTYPE#BLACK black} card.
         */
        protected CAHCard randomCard(CAHCard.CARDTYPE type) {
            switch (type) {
                case BLACK:
                    return null;
                case WHITE:
                    return null;
                default:
                    return null;
            }
        }

        /**
         * Start the game of CAH.
         * @return {@code false} if there are less than 3 players.
         */
        @Override
        public boolean startGame() {
            //Well you can't play CAH with less than 3 people
            if (this.PLAYERS.size() < 3) {
                return false;
            }
            //More conditions for gamestart?
            this.RUNNING = true;
            //Oh yeah we need TODO the actual game
            return true;
        }

        /**
         * End the game, decide a winner.
         */
        protected boolean endGame() {
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
            return true;
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

    public CardsAgainstHumanityCommand() {
        super(
                "CardsAgainstHumanity",
                new ArrayList<>(Arrays.asList("cah")),
                "Start a game of CardsAgainstHumanity",
                " ",
                true,
                false,
                0,
                false
        );
    }

    private enum ACTION {START, MAKECARD}

    /**
     * Performs a check then runs the command.
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {

    }

    /**
     * Performs the action of the command.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {

    }

    /**
     * TODO Parse an {@link ACTION action} from a string.
     * @param arg The string to parse from.
     * @return The action parsed or null if no action was found.
     */
    private ACTION parseAction(String arg) {
        switch (arg) {
            case "start":
                return ACTION.START;
            default:
                return null;
        }
    }

}
