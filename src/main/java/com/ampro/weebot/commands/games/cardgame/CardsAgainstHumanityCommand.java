/**
 *
 */

package com.ampro.weebot.commands.games.cardgame;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.games.Player;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


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
            extends CardGame<CardsAgainstHumanity.CAHPlayer> {

        /**
         * The Cards Against Humanity Player. <br>
         * Has a hand (Array) of {@link WhiteCard White Cards}
         * and won {@link BlackCard Black Cards}.
         */
        final class CAHPlayer extends Player {

            /**
             * Currently held cards
             * This is visible to the {2code Player} in their privateChannel
             * and to the bot. Do not expose this to the guild channel, since that
             * would show the hand to all members and that's dumb.
             */
            WhiteCard[] hand;
            /** {@link BlackCard BlackCards} won */
            final List<BlackCard> cardsWon;

            /** Make new user */
            CAHPlayer(User user) {
                super(user);
                this.hand = new WhiteCard[CardsAgainstHumanity.this.HAND_SIZE];
                this.cardsWon = new ArrayList<>();
            }

        }

        /**
         * A deck of {@link BlackCard Black} and {@link WhiteCard White} cards.
         * <br> Can be locked to any number of Roles.
         */
        public static final class CAHDeck {
            final String name;
            final List<WhiteCard> whiteCards;
            final List<BlackCard> blackCards;
            final long authorID;
            final List<Long> roleLocks;

            /**
             * Make a new Cards Against Humanity Deck
             * @param name Name of the deck
             * @param author The author of the Deck
             */
            public CAHDeck(String name, User author) {
                this.name = name;
                this.authorID = author.getIdLong();
                this.whiteCards = new ArrayList<>();
                this.blackCards = new ArrayList<>();
                this.roleLocks  = new ArrayList<>();
            }

            /**
             * Add a new {@link WhiteCard} to the deck.
             * @param card The card to add.
             * @return False if the card already exists in the deck.
             */
            public boolean addWhiteCard(WhiteCard card) {
                return this.whiteCards.add(card);
            }

            /**
             * Add a {@link BlackCard} to the deck.
             * @param card The card to add.
             * @return false if the deck already contains the card.
             */
            public boolean addBlackCard(BlackCard card) {
                return this.blackCards.add(card);
            }

            /**
             * Add a {@link Role} lock to the Deck to allow this role to use this deck.
             * @param role The Role to add permission to.
             * @return False if the role is already allowed.
             */
            public boolean addRoleLock(Role role) {
                return this.roleLocks.add(role.getIdLong());
            }

            /**
             * Remove a Role's permissions to use this deck.
             * @param role The role to remove access to.
             * @return true if the Role was removed.
             */
            public boolean removeRoleLock(Role role) {
                return this.roleLocks.remove(role.getIdLong());
            }

            public List<WhiteCard> getWhiteCards() {
                return this.whiteCards;
            }

            public List<BlackCard> getBlackCards() {
                return this.blackCards;
            }

            public final String getName() { return this.name; }

            /**
             * Check if a member is allowed to use this deck.
             * @param member The member.
             * @return true if the member is the author or has a role in
             * {@link CAHDeck#roleLocks}.
             */
            public boolean isAllowed(Member member) {
                if (this.roleLocks.isEmpty()) {
                    return true;
                } else if (member.getUser().getIdLong() ==  this.authorID) {
                    return true;
                }
                for (Role r : member.getRoles()) {
                    if (this.roleLocks.contains(r.getIdLong()))
                        return true;
                }
                return false;
            }

        }

        public static final class WhiteCard extends Card {
            /** Content of the card */
            final String cardText;

            /**
             * Make a new White Card.
             * @param cardText The text of the card
             */
            WhiteCard(String cardText) {
                this.cardText = cardText;
            }

        }

        public static final class BlackCard extends Card {

            /** Content of the card */
            final String cardText;
            /** The winning {@link WhiteCard} of the round */
            WhiteCard winningCard;
            /** The number of cards needed to fill in the card.*/
            final int blanks;

            BlackCard(String text, int blanks) {
                this.cardText = text;
                this.blanks = blanks;
            }

            void setWinningCard(WhiteCard winningCard) {
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

        Random random = new Random();
        /** The hosting channel */
        private final Channel CHANNEL;
        /** The Deck of Cards playing with */
        private final CAHDeck deck;
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
            this.PLAYERS.putIfAbsent(AUTHOR_ID, new CAHPlayer(author));
            this.CHANNEL = channel;
            this.HAND_SIZE = handSize;
            this.deck = null; //TODO
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
            this.deck = null; //TODO
        }

        @Override
        protected boolean dealCards(CAHPlayer player) {
            boolean delt = false;
            for (WhiteCard c : player.hand) {
                if (c == null) {
                    c = deck.whiteCards.get(random.nextInt(deck.whiteCards.size()));
                    delt = true;
                }
            }
            return delt;
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
