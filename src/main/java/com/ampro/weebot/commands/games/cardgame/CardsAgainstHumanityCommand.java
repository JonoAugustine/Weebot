/**
 *
 */

package com.ampro.weebot.commands.games.cardgame;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.games.Game;
import com.ampro.weebot.commands.games.Player;
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand
        .CardsAgainstHumanity.*;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.*;

import javax.naming.InvalidNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Run Cards Against Humanity games, TODO and make custom cards. <br>
 * Users can:<br>
 *     start a game of CAH <br>
 *     make custom White Cards <br>
 *     make custom Black Cards <br>
 *     TODO play against the bot
 *
 */
public class CardsAgainstHumanityCommand extends Command {

    /**
     * A game of Cards Against Humanity.
     * TODO How to load the standard cards from file. Everytime or just on startup?
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
             * This is visible to the {@link Player} in their privateChannel
             * <emp>Do not expose this to the guild channel, since that
             * would show the hand to all members and that's dumb.</emp>
             */
            WhiteCard[] hand;
            /** {@link BlackCard BlackCards} won */
            final List<BlackCard> cardsWon;

            Message handMessage;

            /** Make new user */
            CAHPlayer(User user) {
                super(user);
                this.hand = new WhiteCard[CardsAgainstHumanity.this.HAND_SIZE];
                this.cardsWon = new ArrayList<>();
            }

            /**
             * Send a private message to the player, saving the {@link Message} as
             * {@link CAHPlayer#handMessage} for later editing.
             * @param message The message to send.
             */
            public final void privateMessage(String message) {
                super.privateMessage(message, m -> this.handMessage = m);
            }

            /**
             * Update the {@link CAHPlayer#handMessage}. If the message cannot be edited,
             * send a new one with the given message.
             * @param newMmessage The new Message to update or send.
             */
            public final void updateHandMessage(String newMmessage) {
                try {
                    this.handMessage.editMessage(newMmessage).queue(
                            message -> this.handMessage = message
                    );
                } catch (Exception e) {
                    this.privateMessage(newMmessage, m -> this.handMessage = m);
                }
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
            public CAHDeck(String name, User author) throws InvalidNameException {
                try {
                    Integer.parseInt(name);
                    throw new InvalidNameException("Name cannot be a number.");
                } catch (NumberFormatException e) {
                    if (name.equalsIgnoreCase("win") || name.equalsIgnoreCase("rounds"))
                        throw new InvalidNameException("Name cannot be reserved word");
                    this.name = String.join("_", name.split(" "));
                }
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
            WhiteCard(String cardText) { this.cardText = cardText; }

            @Override
            public String toString() { return this.cardText; }
        }

        public static final class BlackCard extends Card {

            /**The maximum number of white cards need to answer this card: {@value}*/
            private static final int MAX_BLANKS = 5;

            /** Content of the card */
            final String cardText;
            /** The number of cards needed to fill in the card.*/
            final int blanks;
            /** The winning {@link WhiteCard} of the round */
            WhiteCard winningCard;

            BlackCard(String text, int blanks) throws InvalidCardException {
                this.cardText = text;
                if (blanks > MAX_BLANKS)
                    throw new InvalidCardException("Too many blanks");
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

        /** The hosting channel */
        private final long channelID;
        /** End after certain # of wins or rounds */
        private WIN_CONDITION WIN_CONDITION;
        private GAME_STATE STATE;
        /** The Deck of Cards playing with */
        private final CAHDeck deck;
        //How many cards per handis?
        private final int HAND_SIZE;

        /** The current Czar */
        CAHPlayer czar;
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
            this.channelID = channel.getIdLong();
            this.HAND_SIZE = handSize;
            this.deck = null; //TODO
        }

        /**
         * Add a user to the game.
         * @param user The user to add.
         * @return false if the user is already in the game.
         */
        @Override
        public final boolean addUser(User user) {
            return this.PLAYERS.putIfAbsent(user.getIdLong(), new CAHPlayer(user)) == null;
        }

        /**
         * Deal random {@link WhiteCard whitecards} to the player until
         * @param player The player to deal cards to.
         * @return
         */
        @Override
        protected boolean dealCards(CAHPlayer player) {
            boolean delt = false;
            for (WhiteCard c : player.hand) {
                if (c == null) {
                    c = deck.whiteCards.get(ThreadLocalRandom
                                            .current().nextInt(deck.whiteCards.size()));
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
            //Deal cards
            for (CAHPlayer p : this.PLAYERS.values())
                this.dealCards(p);
            //Set Czar
            CAHPlayer[] ar = this.PLAYERS.values().toArray(new CAHPlayer[0]);
            this.czar = ar[ThreadLocalRandom.current().nextInt(0, ar.length + 1)];
            this.STATE = GAME_STATE.CHOOSING;
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
                "Start a game of CardsAgainstHumanity or make custom cards.",
                " ", //TODO ArgFormat
                true,
                false,
                0,
                false
        );
    }

    private enum ACTION {
        /**Setup a new game*/
        SETUP,
        /**Start the game*/
        START,
        /** End the game */
        END,
        /**Make a custom white card*/
        MAKEWHITECARD,
        /**Make a custom black card*/
        MAKEBLACKCARD
    }

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
        if (this.check(event))
            this.execute(bot, event);
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
        String[] args = this.cleanArgs(bot, event);
        String message = event.toString();
        TextChannel channel = event.getTextChannel();
        ACTION action = this.parseAction(args[1]);
        if (action == null) {
            event.reply("Sorry, '" + args[1] + "' is not an available command.");
            //TODO Better help
            return;
        }
        //Attempt to find a running game in this channel
        CardsAgainstHumanity game = null;
        for (Game g : bot.getRunningGames()) {
            if (g instanceof CardsAgainstHumanity) {
                if (((CardsAgainstHumanity) g).channelID == channel.getIdLong()) {
                    game = (CardsAgainstHumanity) g;
                    break;
                }
            }
        }

        /*
        cah setup [hand_size] [win_condition] [deck_name] [deck2_name]...
        cah join
        cah start
        cah play/use <card_number> [card2_num] [card3_num] [card4_num] [card5_num]

        cah make deck <deck_name>
        cah make <deck_num> <white[card]> [card text]
        cah make <deck_num> <black[card]> <blanks> [card text]

        cah showdeck <deck_num> //TODO Probably best in a private message
        cah deckfile <deck_num>

        cah remove <deck_num> TODO maybe only on empty decks?
        cah remove <deck_num> <card_num>
         */

        switch (action) {
            case SETUP:
                if (game == null) {
                    int hs = 5;
                    WIN_CONDITION win = null;
                    try {
                        hs = Integer.parseInt(args[2]);
                        win = this.parseWinCondition(args[3]);
                    }
                    catch (NumberFormatException e) {
                        win = this.parseWinCondition(args[2]);
                    } finally {
                        if (win == null)
                            win = WIN_CONDITION.WINS;
                    }
                    game = new CardsAgainstHumanity(bot, channel, event.getAuthor(), hs);

                } else {
                    event.reply("There is already a game of Cards Against Humanity "
                                + "being played in this text channel. Please end that "
                                + "game before starting a new one, or setup a new game "
                                + "in a different text channel.");
                }
                return;
            case START:
            case END:
            case MAKEBLACKCARD:
            case MAKEWHITECARD:
        }

    }

    /**
     * TODO Parse an {@link ACTION action} from a string.
     * @param arg The string to parse from.
     * @return The action parsed or null if no action was found.
     */
    private ACTION parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "setup":
                return ACTION.SETUP;
            case "start":
                return ACTION.START;
            case "end":
            case "stop":
                return ACTION.END;
            //TODO: Card and Deck making
            default:
                return null;
        }
    }

    /**
     * Parse a {@link WIN_CONDITION win condition} from a string.
     * @param arg The string to parse from.
     * @return The parsed win condition or null if one was not found.
     */
    private final WIN_CONDITION parseWinCondition(String arg) {
        switch (arg.toLowerCase()) {
            case "wins":
                return WIN_CONDITION.WINS;
            case "rounds":
                return WIN_CONDITION.ROUNDS;
            default:
                return null;
        }
    }

}
