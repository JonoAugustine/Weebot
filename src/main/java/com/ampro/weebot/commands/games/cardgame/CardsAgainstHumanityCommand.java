package com.ampro.weebot.commands.games.cardgame;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.games.Game;
import com.ampro.weebot.commands.games.Player;
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand.CardsAgainstHumanity.*;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import javax.naming.InvalidNameException;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Run Cards Against Humanity games, and make custom cards. <br>
 * Users can:<br>
 *     start a game of CAH <br>
 *     make custom Card Decks <br>
 *     make custom White Cards <br>
 *     make custom Black Cards <br>
 *     play against the bot
 *
 */
public class CardsAgainstHumanityCommand extends Command {

    /**
     * A game of Cards Against Humanity.
     *
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

            final Member member;

            /**
             * Currently held cards
             * This is visible to the {@link Player} in their privateChannel
             * <emp>Do not expose this to the guild channel, since that
             * would show the hand to all members and that's dumb.</emp>
             */
            WhiteCard[] hand;
            WhiteCard[] playedCards;
            /** {@link BlackCard BlackCards} won */
            final List<BlackCard> cardsWon;

            Message handMessage;

            /** Make new Player */
            CAHPlayer(Member member) {
                super(member.getUser());
                this.member = member;
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
                    this.handMessage.editMessage(newMmessage).queue(message -> this
                            .handMessage = message);
                } catch (Exception e) {
                    this.privateMessage(newMmessage, m -> this.handMessage = m);
                }
            }

        }

        static final class scoreComparator implements Comparator<CAHPlayer> {
            @Override
            public int compare(CAHPlayer p1, CAHPlayer p2) {
                return p2.cardsWon.size() - p1.cardsWon.size();
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
            public CAHDeck(String name, Member author)
                    throws InvalidNameException {
                try {
                    Integer.parseInt(name);
                    throw new InvalidNameException("Name cannot be a number.");
                } catch (NumberFormatException e) {}
                this.name = String.join("_", name.split("\\s+", -1));
                this.authorID = author.getUser().getIdLong();
                this.whiteCards = new ArrayList<>();
                this.blackCards = new ArrayList<>();
                this.roleLocks  = new ArrayList<>();
            }

            public CAHDeck() {
                this.name = "default";
                this.authorID = 0;
                this.whiteCards = new ArrayList<>();
                this.blackCards = new ArrayList<>();
                this.roleLocks  = null;
            }

            /** @return A File of the Deck's black and white cards. */
            private File toFile() {
                File file = new File(Launcher.TEMP_OUT,
                                     this.name.replace("_", " ") + " Deck.txt"
                );
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("White Cards\n\n");
                    int i = 0;
                    for (WhiteCard wc : whiteCards) {
                        sb.append(++i + ".) " + wc + "\n\n");
                    }
                    i = 0;
                    sb.append("\n\nBlack Cards\n\n");
                    for (BlackCard bc : blackCards) {
                        sb.append(++i + ".) (Pick ").append(bc.blanks).append(") ")
                          .append(bc).append("\n\n");
                    }
                    bw.write(sb.toString());
                    return file;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            /**
             * Add a new {@link WhiteCard} to the deck.
             * @param card The card to add.
             * @return False if the card already exists in the deck.
             */
            public boolean addWhiteCard(WhiteCard card) {
                if (!this.whiteCards.contains(card))
                    return this.whiteCards.add(card);
                return false;
            }

            /**
             * Add a {@link BlackCard} to the deck.
             * @param card The card to add.
             * @return false if the deck already contains the card.
             */
            public boolean addBlackCard(BlackCard card) {
                if (!this.blackCards.contains(card))
                    return this.blackCards.add(card);
                return false;
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
                } else if (member.hasPermission(Permission.ADMINISTRATOR)) {
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

            /** The file of the standard white cards */
            static final transient File WC_STD =
                    new File("res/CAH/CAH_WHITE_CARDS.card");

            /** Content of the card */
            final String cardText;

            /**
             * Make a new White Card.
             * @param cardText The text of the card
             */
            WhiteCard(String cardText) { this.cardText = cardText; }

            @Override
            public String toString() { return this.cardText; }

            @Override
            public boolean equals(Object o) {
                try {
                    return ((WhiteCard) o).cardText == this.cardText;
                } catch (ClassCastException e) {
                    return false;
                }
            }

        }

        public static final class BlackCard extends Card {

            static final class TooFewBlanksException extends InvalidCardException {
                TooFewBlanksException() { super(); }
            }
            static final class TooManyBlanksException extends InvalidCardException {
                TooManyBlanksException() { super(); }
            }

            /** The file of the standard black cards */
            static final transient File BC_STD =
                    new File("res/CAH/CAH_BLACK_CARDS.card");

            /**The maximum number of white cards needed to answer this card: {@value}*/
            private static final int MAX_BLANKS = 5;
            /**The minimum number of white cards needed to answer this card: {@value}*/
            private static final int MIN_BLANKS = 1;

            /** Content of the card */
            final String cardText;
            /** The number of cards needed to fill in the card.*/
            final int blanks;
            /** The winning {@link WhiteCard} of the round */
            WhiteCard[] winningCards;

            BlackCard(String text, int blanks)
                    throws TooFewBlanksException, TooManyBlanksException {
                this.cardText = text;
                if (blanks > MAX_BLANKS)
                    throw new TooManyBlanksException();
                else if (blanks < MIN_BLANKS)
                    throw new TooFewBlanksException();
                this.blanks = blanks;
            }

            @Override
            public String toString() { return this.cardText; }

        }

        public enum GAME_STATE {
            /** Players are choosing white cards */
            CHOOSING,
            /** Czar is reading played white cards */
            READING
        }

        private static final transient int MIN_PLAYERS = 3;
        /** The standard deck loaded from files in src/CAH */
        private static final transient CAHDeck STD_DECK = loadStandardDeck();

        /** The hosting channel */
        private final TextChannel channel;
        private GAME_STATE STATE;
        /** The Deck of Cards playing with */
        private final CAHDeck deck;
        //How many cards per handis?
        private final int HAND_SIZE;

        /** The current Czar */
        CAHPlayer czar;
        Iterator<CAHPlayer> czarIterator;
        /** We use this to standardize the order in which players are accesesed */
        List<CAHPlayer> playerList;
        BlackCard blackCard;
        int round;
        /** The winners of the game */
        private final ArrayList<CAHPlayer> winners;

        /**
         * Initialize a new CardsAgainstHumanity game.
         * @param bot Weebot hosting the game
         * @param channel TextChannel to play the game in
         * @param handSize Number of cards each play holds
         */
        public CardsAgainstHumanity(Weebot bot, TextChannel channel, Member author,
                                    int handSize, CAHDeck...decks) {
            super(bot, author.getUser());
            this.channel = channel;
            this.HAND_SIZE = handSize;
            this.PLAYERS.putIfAbsent(AUTHOR_ID, new CAHPlayer(author));
            this.playerList = new ArrayList<>(PLAYERS.values());
            this.winners = new ArrayList<>();
            if (decks == null || decks.length == 0) {
                this.deck = STD_DECK;
                return;
            } else {
                this.deck = new CAHDeck();
                for (CAHDeck d : decks) {
                    this.deck.blackCards.addAll(d.blackCards);
                    this.deck.whiteCards.addAll(d.whiteCards);
                }
            }
        }

        /**
         * Initialize a new CardsAgainstHumanity game.
         * @param bot Weebot hosting the game
         * @param channel TextChannel to play the game in
         * @param handSize Number of cards each play holds
         */
        public CardsAgainstHumanity(Weebot bot, TextChannel channel, Member author,
                                    int handSize) {
            super(bot, author.getUser());
            this.channel = channel;
            this.HAND_SIZE = handSize;
            this.PLAYERS.putIfAbsent(AUTHOR_ID, new CAHPlayer(author));
            this.playerList = new ArrayList<>(PLAYERS.values());
            this.winners = new ArrayList<>();
            this.deck = STD_DECK;
        }

        /**
         * Add a new player by Member. If the game is running, deals cards. if not, wont.
         * @param member
         * @return false if the player is already in the game.
         */
        public final boolean addUser(Member member) {
            CAHPlayer p = new CAHPlayer(member);
            if (this.RUNNING) {
                if (this.joinGame(p)) {
                    this.playerList.add(p);
                    return true;
                } else {
                    return false;
                }
            } else {
                if (this.PLAYERS.putIfAbsent(member.getUser().getIdLong(), p) == null) {
                    this.playerList.add(p);
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Override
        protected final boolean addUser(User user) {return false;}

        /**
         * Deal random {@link WhiteCard whitecards} to the player until
         * @param player The player to deal cards to.
         * @return
         */
        @Override
        protected boolean dealCards(CAHPlayer player) {
            boolean delt = false;
            int rand;
            for (int i = 0; i < player.hand.length; i++) {
                if (player.hand[i] == null) {
                    rand = ThreadLocalRandom.current()
                                            .nextInt(deck.whiteCards.size());
                    player.hand[i] = deck.whiteCards.get(rand);
                    delt = true;
                }
            }
            return delt;
        }

        /**
         * Send all players their hands and the current black card.
         */
        protected void sendHands() {
            PLAYERS.values().forEach( p -> {
                if (!p.getUser().isBot()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Cards against ").append(Launcher.getGuild(
                            Long.parseLong(this.HOST_ID.replace("W", ""))).getName()).append("```Black Card (Pick ").append(this.blackCard.blanks)
                      .append("):\n").append(this.blackCard).append("\n\n\nYour deck:\n" +
                                                                            "\n");
                    for (int i = 0; i < p.hand.length; i++) {
                        sb.append((i + 1) + ".) " + p.hand[i] + "\n\n");
                    }
                    p.updateHandMessage(sb.append(" ```").toString());
                }
            });
        }

        /**
         * Put the cards in the player's {@link CAHPlayer#playedCards}.
         * @param player
         * @param cards card indices
         * @return  0 if the cards were played <br>
         *         -1 if the player has already played their cards<br>
         *         -2 if the player is the czar
         */
        protected int playCards(CAHPlayer player, int[] cards) {
            if (player.playedCards != null) {
                return -1;
            } else if (player == this.czar) {
                return -2;
            }
            player.playedCards = new WhiteCard[cards.length];
            int i = 0;
            for (Integer c : cards) {
                player.playedCards[i++] = player.hand[c];
                player.hand[c] = null;
            }
            return 0;
        }

        /**
         * Check if all players have played their cards. If so, change
         * {@link GAME_STATE GAME STATE} to {@link GAME_STATE#READING READING}.
         * @return true if all players have played their cards.
         */
        private boolean allCardsPlayed() {
            for (CAHPlayer p : this.PLAYERS.values()) {
                if (p.playedCards == null && p != czar) {
                    return false;
                }
            }
            //If all the playedCards are not null, we are done and the game state
            //progresses to READING
            this.STATE = GAME_STATE.READING;
            return true;
        }

        /** @return An embed of the black card and the played cards */
        private MessageEmbed playedCardsEmbed() {
            if (this.STATE != GAME_STATE.READING) return null;
            // Create the EmbedBuilder instance
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0x20F457));

            //Add embed author:
            //1. Arg: name as string
            //2. Arg: url as string (can be null)
            //3. Arg: icon url as string (can be null)
            eb.setAuthor("Cards against " + channel.getGuild().getName(),
                         null, channel.getGuild().getIconUrl());

            //Set the title:
            //1. Arg: title as string
            //2. Arg: URL as string or could also be null
            eb.setTitle("Czar:  " + czar.member.getEffectiveName(), null);

            eb.setDescription("All the players have played their cards" +
                                      " -- Czar, please choose your victor.");

            //Add fields to embed:
            //1. Arg: title as string
            //2. Arg: text as string
            //3. Arg: inline mode true / false
            eb.addField("Black Card:", this.blackCard.cardText, false);

            Collections.shuffle(this.playerList);
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (CAHPlayer p : this.playerList) {
                //Skip the Czar but still count up so the indexing doesn't go wack
                if (p == czar) {
                    i++; continue;
                }
                int k = 1;
                sb.setLength(0);
                for (WhiteCard wc : p.playedCards) {
                    if (p.playedCards.length > 1)
                        sb.append((k++) + ".) ");
                    sb.append(wc + "\n");
                }
                eb.addField("Player " + i++, sb.toString() , false);
            }

            //Add spacer like field
            //Arg: inline mode true / false
            eb.addBlankField(false);

            eb.addField("How to Play:",
                        "Select the best card (or set of cards) with 'cah pick #'.",
                        false);

            //1. Arg: text as string
            //2. icon url as string (can be null)
            eb.setFooter("Run by Weebot",
                         Launcher.getJda().getSelfUser().getAvatarUrl()
            );

            //Arg: image url as string
            eb.setThumbnail(czar.member.getUser().getAvatarUrl());

            return eb.build();

        }

        /** @return A round-start black card*/
        private MessageEmbed blackCardEmbed() {
            // Create the EmbedBuilder instance
            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(new Color(0x20F457));

            //Add embed author:
            //1. Arg: name as string
            //2. Arg: url as string (can be null)
            //3. Arg: icon url as string (can be null)
            eb.setAuthor("Cards against " + channel.getGuild().getName()
                                 + " | round " + round, null,
                         channel.getGuild().getIconUrl());

            //Set the title:
            //1. Arg: title as string
            //2. Arg: URL as string or could also be null
            eb.setTitle("Czar:  " + czar.member.getEffectiveName(), null);

            //Add fields to embed:
            //1. Arg: title as string
            //2. Arg: text as string
            //3. Arg: inline mode true / false
            eb.addField("Black Card (Pick "+ this.blackCard.blanks +"):",
                        this.blackCard.cardText,false);

            //Add spacer like field
            //Arg: inline mode true / false
            eb.addBlankField(false);

            eb.addField("How to Play:",
                        "Select your cards from the private chat I sent you.",
                        false);

            //1. Arg: text as string
            //2. icon url as string (can be null)
            eb.setFooter("Run by Weebot",
                         Launcher.getJda().getSelfUser().getAvatarUrl()
            );

            return eb.build();
        }

        private MessageEmbed winnerEmbed(CAHPlayer winner) {
            // Create the EmbedBuilder instance
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0x20F457));

            //Add embed author:
            //1. Arg: name as string
            //2. Arg: url as string (can be null)
            //3. Arg: icon url as string (can be null)
            eb.setAuthor("Cards against " + channel.getGuild().getName(),
                         null, channel.getGuild().getIconUrl());

            //Set the title:
            //1. Arg: title as string
            //2. Arg: URL as string or could also be null
            eb.setTitle(winner.member.getEffectiveName()
                                + " wins this round!!!:tada: :tada:",
                        null);

            this.playerList.sort(new scoreComparator());
            StringBuilder sb = new StringBuilder();
            for (CAHPlayer p : this.playerList) {
                sb.append("*").append(p.member.getEffectiveName())
                  .append("* : ").append(p.cardsWon.size()).append("\n");
            }

            eb.addField("Leader Board:", sb.toString(), false);

            //eb.addBlankField(false);

            //1. Arg: text as string
            //2. icon url as string (can be null)
            eb.setFooter("Run by Weebot",
                         Launcher.getJda().getSelfUser().getAvatarUrl()
            );

            return eb.build();
        }

        private MessageEmbed endGameEmbed() {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0x20F457));

            eb.setAuthor("Cards against " + channel.getGuild().getName(),
                         null, channel.getGuild().getIconUrl());

            eb.setTitle("Match Over! Here's how it went: ", null);

            this.playerList.sort(new scoreComparator());
            StringBuilder sb = new StringBuilder();
            for (CAHPlayer p : this.playerList) {
                sb.append("*").append(p.member.getEffectiveName())
                  .append("* : ").append(p.cardsWon.size()).append("\n");
            }

            eb.addField("Leader Board:", sb.toString(), false);

            eb.addBlankField(false);

            //1. Arg: text as string
            //2. icon url as string (can be null)
            eb.setFooter("Run by Weebot",
                         Launcher.getJda().getSelfUser().getAvatarUrl()
            );

            //Set image:
            //Arg: image url as string
            if (playerList.get(0).cardsWon.size() != playerList.get(1).cardsWon.size())
                eb.setImage(this.playerList.get(0).member.getUser().getAvatarUrl());

            return eb.build();
        }

        /**
         * Setup the game and players for the next round. <br>
         *     1.) nullify {@link CAHPlayer#playedCards played cards}
         *          and deal new cards.
         *     2.) Set new random black card.
         *     3.) Set next Czar
         *     4.) Set game state to {@link GAME_STATE#CHOOSING CHOOSING}
         */
        protected void setupNextRound() {
            //Deal new cards
            for (CAHPlayer p : PLAYERS.values()) {
                p.playedCards = null;
                if(!this.dealCards(p)) {
                    System.err.println("Not delt!");
                    return;
                }
            }

            //Set the new black card
            int rand;
            BlackCard t;
            do {
                rand = ThreadLocalRandom.current().nextInt(deck.blackCards.size());
                t = this.deck.blackCards.get(rand);
            } while (t.equals(this.blackCard));

            blackCard = t;

            //Set next czar
            CAHPlayer temp;
            do {
                if(this.czarIterator.hasNext())
                    temp = czarIterator.next();
                else {
                    //Reset the iterator if we reached the end of the list
                    this.czarIterator = PLAYERS.values().iterator();
                    temp = czarIterator.next();
                }
                //Don't let the bot be czar or a repeat czar
            } while (czar.getUser().isBot() && temp != czar);
            czar = temp;

            //Game state
            this.STATE = GAME_STATE.CHOOSING;
            round++;

        }

        /**
         * Start the game of CAH.
         * @return {@code false} if there are less than 3 players.
         */
        @Override
        public boolean startGame() {
            if(this.PLAYERS.size() < MIN_PLAYERS) {
                return false;
            }
            //Deal cards
            for (CAHPlayer p : this.PLAYERS.values())
                if(!this.dealCards(p)) {
                    System.err.println("Not delt!");
                    return false;
                }
            this.czarIterator = this.PLAYERS.values().iterator();
            do {
                //Set a random Czar
                this.czar = playerList.get(new Random().nextInt(playerList.size()));
            } while (czar.getUser().isBot());
            //Set the first black card
            int rand = ThreadLocalRandom.current().nextInt(deck.blackCards.size());
            blackCard = this.deck.blackCards.get(rand);

            //Send card hands
            sendHands();
            //Game state
            this.RUNNING = true;
            this.STATE = GAME_STATE.CHOOSING;
            return true;
        }

        /**
         * End the game, decide a winner.
         */
        protected boolean endGame() {
            ArrayList<CAHPlayer> players = this.playerIterable();
            CAHPlayer winner = players.get(0);
            for (CAHPlayer p : players) {
                if (p.cardsWon.size() > winner.cardsWon.size())
                    winner = p;
            }
            for (CAHPlayer p : players) {
                if (winner.cardsWon.size() == p.cardsWon.size())
                    this.winners.add(p);
            }

            return true;
        }

        protected final boolean isCzar(Member member) {
            return member == this.czar.member;
        }

    }

    public CardsAgainstHumanityCommand() {
        super(
                "CardsAgainstHumanity",
                new ArrayList<>(Arrays.asList("cah")),
                "Start a game of CardsAgainstHumanity or make custom cards.",
                "cah <command> [arguments]",
                true,
                false,
                0,
                false,
                false
        );
    }

    private static final String NO_GAME_FOUND =
            "There is no Cards Against Humanity game setup or running! Use ```"
            + "cah setup [hand_size] [deck_name] [deck2_name]...```to setup a new game.";

    private enum ACTION {
        /**Setup a new game*/
        SETUP,
        /**Start the game*/
        START,
        /** Join the game */
        JOIN,
        /** Player lease the game */
        LEAVE,
        /** Invite the bot to play */
        ADDBOT,
        /** End the game */
        END,
        /** Play cards */
        PLAY,
        /** Resend the player their hand */
        SENDHAND,
        /** Czar Choose a winning card */
        PICK,
        /** View list of all decks */
        VIEWALLDECKS,
        /** View all cards in deck */
        VIEWDECK,
        /** Get deck in a file */
        DECKFILE,
        /** Make a custom deck */
        MAKEDECK,
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
        StringBuilder sb = new StringBuilder();
        String[] args = this.cleanArgs(bot, event);
        if(args.length < 2) {
            event.reply(
                    "What do you want me to do? ```help cah``` for a list of commands");
            return;
        }
        TextChannel channel = event.getTextChannel();
        ACTION action = this.parseAction(args[1]);
        if(action == null) {
            sb.append("Sorry, '" + args[1] + "' is not an available command.")
                    .append("```help cah``` for a list of commands.");
            event.reply(sb.toString());
            return;
        }
        //Attempt to find a running game in this channel
        CardsAgainstHumanity game = null;
        if (action != ACTION.DECKFILE && action != (ACTION.MAKEBLACKCARD)
                && action != (ACTION.MAKEWHITECARD) && action != (ACTION.MAKEDECK)
                && action != ACTION.VIEWALLDECKS && action != ACTION.VIEWDECK) {
            for (Game g : bot.getRunningGames()) {
                if(g instanceof CardsAgainstHumanity) {
                    if(((CardsAgainstHumanity) g).channel.getIdLong() == channel.getIdLong()) {
                        game = (CardsAgainstHumanity) g;
                        break;
                    }
                }
            }
        }
        //Some cases later will need this
        CAHDeck cahDeck;
        CAHPlayer botPlayer;
        /*
        cah remove <deck_num> TODO maybe only on empty decks?
        cah remove <deck_num> <card_num>
         */

        switch (action) {
            case SETUP: //cah setup [hand_size] [deck_name] [deck2_name]...
                if(game == null) {
                    int hs = 5;
                    int deckIndex = 2;
                    try {
                        hs = Integer.parseInt(args[2]);
                        deckIndex++;
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    }
                    List<CAHDeck> decks = new ArrayList<>();
                    StringBuilder badDeckNames = new StringBuilder();
                    synchronized (bot) {
                        CAHDeck d;
                        for (int i = deckIndex; i < args.length; i++) {
                            d = bot.getCustomCahDeck(args[i]);
                            if(d != null) {
                                decks.add(d);
                            } else {
                                badDeckNames.append(d.name + ", ");
                            }
                        }
                    }
                    cahDeck = new CAHDeck();
                    for (CAHDeck d : decks) {
                        for (WhiteCard wc : d.whiteCards) {
                            cahDeck.addWhiteCard(wc);
                        }
                        for (BlackCard bc : d.blackCards) {
                            cahDeck.addBlackCard(bc);
                        }
                    }
                    game = new CardsAgainstHumanity(bot, channel, event.getMember(), hs,
                                                    cahDeck
                    );
                    bot.addRunningGame(game);
                    event.reply("A new game of *Cards Against "
                                + event.getGuild().getName()
                                + "* has been setup!" + "\nUse ```cah join```\nto join "
                                + "the game."
                                + " and ```cah start```\n to start the game.");
                } else {
                    event.reply(
                            "There is already a game of Cards Against Humanity " + "being played in this text channel. Please end that " + "game before starting a new one, or setup a new game " + "in a different text channel.");
                }
                return;
            case START:
                if(game != null) {
                    if (game.isRunning()) {
                        event.reply("There is already a game of Cards Against Humanity"
                                    + " running.", m -> {
                            try { Thread.sleep(5 * 1000); }
                            catch (InterruptedException e) {}
                            m.delete().queue();
                            event.deleteMessage();
                        });
                        return;
                    }
                    if(game.startGame()) {
                        event.reply(game.blackCardEmbed());
                        botPlayer = game.getPlayer(bot.asUser());
                        if (botPlayer != null) {
                            int[] cards = new int[game.blackCard.blanks];
                            for (int i = 0; i < cards.length; i++) {
                                cards[i] = new Random().nextInt(5);
                            }
                            game.playCards(botPlayer, cards);
                        }
                    } else {
                        event.reply("You need at least **3** players to start a game of"
                                    + " Cards Against Humanity.");
                    }
                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case JOIN:
                if(game != null) {
                    if(game.addUser(event.getMember())) {
                        event.reply("*You've been added to the game!*");
                    } else {
                        event.reply("***You're already in the game.***", m -> {
                            try {
                                Thread.sleep(5 * 1000);
                            } catch (InterruptedException e) {
                            }
                            m.delete().queue();
                            event.deleteMessage();
                        });
                    }
                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case ADDBOT:
                if (game != null) {
                    Member botMember =
                            event.getGuild().getMember(Launcher.getJda().getSelfUser());
                    if (game.addUser(botMember)) {
                        event.reply("*knocks door down* The robots have come to play.");
                        if (game.STATE == GAME_STATE.CHOOSING) {
                            int[] cards = new int[game.blackCard.blanks];
                            for (int i = 0; i < cards.length; i++) {
                                cards[i] = new Random().nextInt(5);
                            }
                            game.playCards(game.getPlayer(bot.asUser()), cards);
                        }
                    } else {
                        event.reply("***I am already in the game*** :wink:", m -> {
                            try {
                                Thread.sleep(5 * 1000);
                            } catch (InterruptedException e) {
                            }
                            m.delete().queue();
                            event.deleteMessage();
                        });
                    }
                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case LEAVE:
                if (game != null) {
                    if (game.getPlayers().remove(event.getAuthor().getIdLong()) != null) {
                        event.reply(event.getMember().getEffectiveName() + "has been "
                                    + "removed from the game; thanks for playing!");
                        return;
                    }
                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case PLAY: //cah play <card1_number> [card2_num]...[card5_num]
                if(game != null) {
                    if(game.STATE != GAME_STATE.CHOOSING) {
                        //If we are at the reading period, dont let people play cards.
                        //Respond then delete both messages to clean clutter
                        event.reply("Please wait for the next round to play more cards.",
                                    m -> {
                                        try {
                                            Thread.sleep(5 * 1000);
                                        } catch (InterruptedException e) {
                                        }
                                        m.delete().queue();
                                        event.deleteMessage();
                                    }
                        );
                        return;
                    }
                    //Check for minimum number of cards
                    if(args.length >= 2 + game.blackCard.blanks) {
                        int[] cards = new int[game.blackCard.blanks];
                        try {
                            for (int i = 2; i < args.length; i++) {
                                cards[i - 2] = Integer.parseInt(args[i]) - 1;
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            event.reply("Sorry, I had some trouble reading your request" +
                                                ".");
                            return;
                        } catch (IndexOutOfBoundsException e) {
                            event.reply(
                                    "Too many cards. Choose " + game.blackCard.blanks
                                            + " cards.", m -> {
                                        try { Thread.sleep(5 * 1000); }
                                        catch (InterruptedException e2) {}
                                        m.delete().queue();
                                        event.deleteMessage();
                                    });
                            return;
                        }
                        switch (game.playCards(game.getPlayer(event.getAuthor()), cards)) {
                            case 0:
                                event.reply("*Personally, I hope you win.*", m -> {
                                    try {
                                        Thread.sleep(1 * 1000);
                                    } catch (InterruptedException e) {
                                    }
                                    m.delete().queue();
                                    event.deleteMessage();
                                });
                                break;
                            case -1:
                                event.reply("*You've already played your cards.*", m -> {
                                    try {
                                        Thread.sleep(5 * 1000);
                                    } catch (InterruptedException e) {
                                    }
                                    m.delete().queue();
                                    event.deleteMessage();
                                });
                                return;
                            case -2:
                                event.reply("*The Card Czar can't play a card.*", m -> {
                                    try {
                                        Thread.sleep(5 * 1000);
                                    } catch (InterruptedException e) {
                                    }
                                    m.delete().queue();
                                    event.deleteMessage();
                                });
                                return;
                        }
                    } else {
                        event.reply("Not enough cards. Choose " + game.blackCard.blanks
                                            + " cards.", m -> {
                            try { Thread.sleep(5 * 1000); }
                            catch (InterruptedException e2){}
                            m.delete().queue();
                            event.deleteMessage();
                        });
                        return;
                    }
                    if(game.allCardsPlayed()) {
                        event.reply(game.playedCardsEmbed());
                    }
                    return;
                } else {
                    event.reply(NO_GAME_FOUND);
                }
            case PICK: //cah pick <card_set_num>
                if(game != null) {
                    if(game.STATE != GAME_STATE.READING) {
                        //If the players are still choosing their cards we don't want to
                        //let the czar pick anything, so reply and delete the messages to
                        //clear clutter
                        event.reply("*Players are still choosing thier cards, please "
                                            + "wait for them to finish*.",
                                    m -> {
                                        try {
                                            Thread.sleep(5 * 1000);
                                        } catch (InterruptedException e) {
                                        }
                                        m.delete().queue();
                                        event.deleteMessage();
                                    }
                        );
                    }
                    if (!game.isCzar(event.getMember())) {
                        event.reply("Only the Card Czar, **"
                                    + game.czar.member.getEffectiveName()
                                    + "** can pick a winner.", m -> {
                            try { Thread.sleep(2 * 1000); }
                            catch (InterruptedException e) {}
                            m.delete().queue();
                            event.deleteMessage();
                        });
                        return;
                    }
                    sb.setLength(0);
                    //Pick the winner by index (Since we shuffled the player list
                    //when we sent the list of played cards, then iterated through
                    //the shuffled list, we can use the index of the shuffled list
                    //to get the chosen winner.
                    CAHPlayer winner;
                    try {
                        winner = game.playerList.get(Integer.parseInt(args[2]) - 1);
                    } catch (NumberFormatException e) {
                        event.reply("I couldn't understand '" + args[2] + "'.");
                        return;
                    } catch (IndexOutOfBoundsException e) {
                        event.reply("Please choose one of the listed players by " +
                                            "their number");
                        return;
                    }

                    if (winner == game.czar) {
                        event.reply("*Please select one of the players listed*",
                                    m -> {
                                    try { Thread.sleep(5 * 1000); }
                                    catch (InterruptedException e) {}
                                    m.delete().queue();
                                    event.deleteMessage();
                        });
                        return;
                    }

                    //Give the won cards to the winner
                    game.blackCard.winningCards = winner.playedCards;
                    winner.cardsWon.add(game.blackCard);

                    event.reply(game.winnerEmbed(winner));

                    //Setup next round
                    game.setupNextRound();

                    event.reply(game.blackCardEmbed());

                    //Send new card hands
                    game.sendHands();

                    //If the bot is playing
                    botPlayer = game.getPlayer(bot.asUser());
                    if (botPlayer != null) {
                        int[] cards = new int[game.blackCard.blanks];
                        for (int i = 0; i < cards.length; i++) {
                            cards[i] = new Random().nextInt(5);
                        }
                        game.playCards(botPlayer, cards);
                    }
                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case SENDHAND:
                if (game != null) {
                    CAHPlayer p = game.getPlayer(event.getAuthor());
                    if (p != null) {
                        sb.setLength(0);
                        sb.append("Cards against ").append(Launcher.getGuild(
                                Long.parseLong(game.getHOST_ID().replace("W", ""))).getName())
                          .append("```Black Card:\n").append(game.blackCard).append
                                ("\n\n\nYour deck:\n\n");
                        for (int i = 0; i < p.hand.length; i++) {
                            sb.append((i + 1) + ".) " + p.hand[i] + "\n\n");
                        }
                        p.privateMessage(sb.append(" ```").toString());
                        event.deleteMessage();
                    } else {
                        event.reply("You are not in the game. Use ```cah join``` to " +
                                            "join.");
                    }
                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case END:
                if(game != null) {
                    game.endGame();
                    synchronized (bot) {
                        if(!bot.getRunningGames().remove(game)) {
                            System.err.println("Err encountered while removing game.");
                            return;
                        }
                    }
                    if (!game.isRunning()) {
                        event.reply("*Game cancelled.*");
                        return;
                    }
                    event.reply(game.endGameEmbed());

                } else {
                    event.reply(NO_GAME_FOUND);
                }
                return;
            case VIEWALLDECKS: //cah alldecks
                sb.setLength(0);
                Collection<CAHDeck> decks;
                synchronized (bot) {
                    decks = bot.getCustomCahDecks().values();
                }
                if (decks.isEmpty()) {
                    sb.append("There are no custom decks in this server.")
                      .append(" You can make a new deck using")
                      .append("```cah mkdk <deck name>```");
                    event.reply(sb.toString());
                    return;
                }
                sb.append("*Cards against ").append(event.getGuild().getName())
                  .append(" Custom Decks*```");
                decks.forEach( deck -> {
                    if (deck.isAllowed(event.getMember()))
                        sb.append("> " + deck.name).append("\n\n");
                });
                sb.append(" ```");
                event.reply(sb.toString());
                return;
            case VIEWDECK: //cah viewdeck <deck_name>
                sb.setLength(0);
                if (args.length != 3) {
                    sb.append("Please use the format:```cah viewdeck <deck_name>```");
                    event.reply(sb.toString());
                    return;
                }
                synchronized (bot) {
                    cahDeck = bot.getCustomCahDeck(args[2]);
                }
                if (cahDeck == null) {
                    sb.append("There is no deck named '*").append(args[2])
                      .append("*'. Use```cah alldecks```to see all available decks.");
                    event.reply(sb.toString());
                } else if (!cahDeck.isAllowed(event.getMember())) {
                    sb.append("You do not have permission to view deck '*")
                      .append(args[2])
                      .append("*'. Use```cah alldecks```to see all available decks.");
                    event.reply(sb.toString());
                }
                sb.append("```").append(cahDeck.name.replace("_", " "))
                  .append(" Deck:\n\n\n").append("White Cards:\n\n");
                int i = 0;
                for (WhiteCard wc : cahDeck.whiteCards) {
                    sb.append(++i + ".) " + wc + "\n\n");
                }
                i = 0;
                sb.append("\n\nBlack Cards\n\n");
                for (BlackCard bc : cahDeck.blackCards) {
                    sb.append(++i + ".) ").append(bc).append("\n\n");
                }
                sb.append("```");
                event.privateReply(sb.toString());
                event.deleteMessage();
                return;
            case MAKEDECK: //cah mkdk <deck_name>
                sb.setLength(0);
                CAHDeck deck;
                try {
                    deck = new CAHDeck(String.join(" ", Arrays.copyOfRange(args, 2, args.length)),
                                       event.getMember());
                } catch (InvalidNameException e) {
                    event.reply("Sorry, deck names cannot be a number.");
                    return;
                } catch (IndexOutOfBoundsException e) {
                    sb.append("Please use the format```cah mkdk <deck name>```")
                      .append("to make a new deck.");
                    event.reply(sb.toString());
                    return;
                }
                if (deck.name.isEmpty()) {
                    sb.append("The deck's name cannot be empty!");
                    event.reply(sb.toString());
                    return;
                }
                synchronized (bot) {
                    if (!bot.addCustomCahDeck(deck)) {
                        sb.append("A deck with the name *").append(deck.name)
                          .append("* already exists in this server.");
                        event.reply(sb.toString());
                        return;
                    }
                }
                sb.append("*").append(deck.name).append("* was created!\n\n")
                  .append(" Use```cah mkwc <deck_name> <card text>```")
                  .append("to make new white (played) cards and add them to the deck.")
                  .append("\n\nOr use ```cah mkbc <deck_name> <blanks> <card text>```")
                  .append("to make new black (read) cards and add them to the deck.");
                event.reply(sb.toString());
                return;
            case MAKEBLACKCARD: //cah mkbc <deck_name> <blanks> <card text>
                sb.setLength(0);
                if (args.length < 5) {
                    sb.append("Please provide a deck name, number of blanks,")
                      .append(" and card text in this format:")
                      .append("```cah mkbc <deck_nam> <blanks> <card text>```");
                    event.reply(sb.toString());
                    return;
                }
                synchronized (bot) {
                    cahDeck = bot.getCustomCahDeck(args[2]);
                }
                if (cahDeck == null) {
                    sb.append("The deck '*").append(args[2]).append("*' could not be")
                      .append("found.");
                    event.reply(sb.toString());
                } else if (!cahDeck.isAllowed(event.getMember())) {
                    sb.append("You do not have permission to modify deck '*")
                      .append(cahDeck.name).append("*'.");
                    event.reply(sb.toString());
                }
                int blanks;
                try {
                    blanks = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sb.append("Please provide a deck name, number of blanks,")
                      .append(" and card text in this format:")
                      .append("```cah mkbc <deck_nam> <blanks> <card text>```");
                    event.reply(sb.toString());
                    return;
                }

                String txt = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                BlackCard blackCard;
                try {
                    blackCard = new BlackCard(txt, blanks);
                } catch (BlackCard.TooFewBlanksException e) {
                    sb.append("*The maximum number of blanks is ")
                      .append(BlackCard.MAX_BLANKS).append("*.");
                    event.reply(sb.toString());
                    return;
                } catch (BlackCard.TooManyBlanksException e) {
                    sb.append("*The minimum number of blanks is ")
                      .append(BlackCard.MIN_BLANKS).append("*.");
                    event.reply(sb.toString());
                    return;
                }

                if (cahDeck.addBlackCard(blackCard)) {
                    sb.append("The Black Card was added to deck *")
                      .append(cahDeck.name).append("*");
                    event.reply(sb.toString());
                } else {
                    event.reply("This card already exists.");
                }
                return;
            case MAKEWHITECARD: //cah mkwc <deck_name> <card text>
                sb.setLength(0);
                if (args.length < 4) {
                    sb.append("Please provide a deck name and card text in this format:")
                      .append("```cah mkwc <deck_nam> <card text>```");
                    event.reply(sb.toString());
                    return;
                }
                cahDeck = null;
                synchronized (bot) {
                    cahDeck = bot.getCustomCahDeck(args[2]);
                }
                if (cahDeck == null) {
                    sb.append("The deck '*").append(args[2]).append("*' could not be")
                      .append("found.");
                    event.reply(sb.toString());
                } else if (!cahDeck.isAllowed(event.getMember())) {
                    sb.append("You do not have permission to modify deck '*")
                      .append(cahDeck.name).append("*'.");
                    event.reply(sb.toString());
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                WhiteCard card = new WhiteCard(text);
                if (!cahDeck.addWhiteCard(card)) {
                    event.reply("This card already exists.");
                    return;
                }
                sb.append("The White Card was added to deck *").append(cahDeck.name)
                  .append("*");
                event.reply(sb.toString());
                return;
            case DECKFILE: //cah deckfile [deck_name]
                try {
                    synchronized (bot) {
                        cahDeck = bot.getCustomCahDeck(args[2]);
                    }
                    if (cahDeck == null) {
                        if (args[2].equalsIgnoreCase("default")
                                || args[2].equalsIgnoreCase("standard")) {
                            cahDeck = CardsAgainstHumanity.STD_DECK;
                        } else {
                            event.reply("There is no deck by that name.");
                            return;
                        }
                    } else if (!cahDeck.isAllowed(event.getMember())) {
                        event.reply("You do not have permissions to view this deck.");
                        return;
                    }
                } catch (IndexOutOfBoundsException e) {
                    sb.append("Please use provide a deck name like this:")
                      .append("```cah deckfile <deck_name>```");
                    event.reply(sb.toString());
                    return;
                }
                File file = cahDeck.toFile();
                if(file != null) {
                    event.privateReply(file, file.getName(), f -> f.deleteOnExit());
                    event.deleteMessage();
                } else {
                    event.reply("Sorry, something went wrong...");
                }
                return;
        }

    }

    /**
     * Parse an {@link ACTION action} from a string.
     * @param arg The string to parse from.
     * @return The action parsed or null if no action was found.
     */
    private ACTION parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "setup":
                return ACTION.SETUP;
            case "start":
                return ACTION.START;
            case "join":
                return ACTION.JOIN;
            case "+bot":
            case "wbot":
            case "addbot":
            case "invitebot":
                return ACTION.ADDBOT;
            case "leave":
                return ACTION.LEAVE;
            case "end":
            case "stop":
                return ACTION.END;
            case "use":
            case "play":
                return ACTION.PLAY;
            case "myhand":
                return ACTION.SENDHAND;
            case "pick":
                return ACTION.PICK;
            case "alldecks":
                return ACTION.VIEWALLDECKS;
            case "viewdeck":
            case "seedeck":
            case "see":
                return ACTION.VIEWDECK;
            case "deckfile":
                return ACTION.DECKFILE;
            case "makedeck":
            case "mkdeck":
            case "mkdk":
                return ACTION.MAKEDECK;
            case "makeblackcard":
            case "makeblack":
            case "makebc":
            case "mkbc":
                return ACTION.MAKEBLACKCARD;
            case "makewhitecard":
            case "makewhite":
            case "makewc":
            case "mkwc":
                return ACTION.MAKEWHITECARD;
            default:
                return null;
        }
    }

    /**
     * Load the standard CAH cards from file.
     * @return The Standard deck of CAH cards.<br>null if an err occurs
     */
    private static final CAHDeck loadStandardDeck() {
        CAHDeck deck = new CAHDeck();

        //Load white cards
        Scanner scanner;
        try {
            Files.readAllLines(WhiteCard.WC_STD.toPath())
                 .iterator().forEachRemaining(
                    line -> deck.whiteCards.add(new WhiteCard(line))
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        //Load black cards
        try {
            Files.readAllLines(BlackCard.BC_STD.toPath()).iterator()
                 .forEachRemaining( line -> {
                     int blanks;
                     if (line.startsWith("(Pick 2)")) {
                         blanks = 2;
                         line = line.substring("(Pick 2)".length());
                     } else if (line.startsWith("(Pick 3)")) {
                         blanks = 3;
                         line = line.substring("(Pick 3)".length());
                     } else {
                         blanks = 1;
                     }
                     try {
                         deck.blackCards.add(new BlackCard(line, blanks));
                     } catch (Card.InvalidCardException e){
                         //This should not happen since we are loading from pre-made so
                         //ignore
                     }
                 });
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return deck;
    }

    @Override
    public String getHelp() {
        StringBuilder sb = new StringBuilder();

        sb.append("```Cards Against Humanity Command Help:\n\n")
          .append("<required> , [optional], /situational_required/\n\n");

        sb.append("setup [hand_size] [deck_name] [deck2_name]...\n")
          .append("cah join\n").append("cah start\n")
          .append("cah play <card1_number> /card2_num/.../card5_num/\n")
          .append("cah pick <card_set_num>\n").append("cah myhand\n")
          .append("cah addbot\n")
          .append("\ncah makedeck <deck_name>\n")
          .append("cah makewhitecard/mkwc <deck_name> <card text>\n")
          .append("cah makeblackcard/mkbc <deck_name> <blanks> <card text>\n")
          .append("cah alldecks \n").append("cah see <deck_name>")
          .append("cah deckfile [deck_name]\n")
          .append("cah remove <deck_num> **\n")
          .append("cah remove <deck_num> <card_num> **\n")
          .append("```");

        sb.append("\n(Commands marked '**' are still under construction.)"); //todo

        return sb.toString();

    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(new Color(0x31FF00));

        eb.setAuthor("Cards Against Weebot", null
                , Launcher.getJda().getSelfUser().getAvatarUrl());

        eb.setThumbnail(
                "https://cardsagainsthumanity.com/v8/images/social-3f4a4c57.png");

        eb.setTitle("Cards Against Humanity (cah)",
                "https://www.cardsagainsthumanity.com/");

        StringBuffer sb = new StringBuffer();
        sb.append("Play a game of Cards Against Humanity and ")
                .append("make custom decks with user-made cards.");
        eb.setDescription(sb.toString());
        sb.setLength(0);

        eb.addField("Guide",
                "<required> , [optional], /situational_required/",false);

        eb.addField("Game Setup",
                "cah setup [hand_size] [deck_name] [deck2_name]...",
                false);

        eb.addField("Join Game", "cah join", false);

        eb.addField("Start Game (Requires 3 players)",
                "cah start", false);

        eb.addField("Add Weebot to the Game",
                "cah +bot\n*Aliases*: addbot, invitebot",
                false);

        eb.addField("Play Your Card(s)",
                    "cah play <card1_number> /card2_num/.../card5_num/"
                    + "\n*Alias*: use", false);

        eb.addField("Card Czar Pick Winning Card(s)",
                "cah pick <card_set_num>", false);

        eb.addField("Re-send Your Hand of Cards", "cah myhand",
                false);

        eb.addField("Make a Custom Deck",
                "cah makedeck <deck_name>",
                false);

        eb.addField("Make a Custom White Card",
                "cah mkwc <deck_name> <card text>\n*Aliases*: " +
                        "makewhitecard, makewc", false);


        eb.addField("Make a Custom Black Card",
                "cah mkbc <deck_name> <card text>\n*Aliases*: " +
                        "makeblackcard, makebc", false);


        eb.addField("View all Custom Decks",
                    "cah alldecks", false);

        eb.addField("View a Custom Deck's Cards",
                "cah viewdeck <deck_name>\n*Alias*: seedeck", false);

        eb.addField("Get a Custom Deck as a Text File",
                "cah deckfile <deck_name>", false);

        eb.addField("Remove Custom Deck **",
                    "cah remove <deck_number>", false);

        eb.addField("Remove Custom Card **",
                "cah remove <deck_num> <card_number>", false);

        eb.addField("Under Construction ",
                     "Commands marked '**' are still under construction",
                false);

        eb.setFooter("Run by Weebot",
                Launcher.getJda().getSelfUser().getAvatarUrl());

        return eb.build();
    }

}
