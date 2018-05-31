/**
 * Copyright 2018 Jonathan Augustine, Daniel Ernst
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.ampro.weebot.entities.bot;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.commands.NotePadCommand.NotePad;
import com.ampro.weebot.commands.games.Game;
import com.ampro.weebot.commands.games.Player;
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand
        .CardsAgainstHumanity.CAHDeck;
import com.ampro.weebot.commands.management.AutoAdminCommand.AutoAdmin;
import com.ampro.weebot.listener.events.BetterEvent;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A representation of a Weebot entity linked to a Guild.<br>
 *     Contains a reference to the Guild hosting the bot and
 *     the settings applied to the bot by said Guild. <br><br>
 *     Each Weebot is assigned an ID String consisting of the
 *     hosting Guild's unique ID + "W" (e.g. "1234W") <br><br>
 *
 * TODO: Scanning for banned words
 *
 * <br><br>
 * Development Questions TODO: <br>
 * ? Should User relation to Weebot be gloabal or local? This would likelly
 * invole a User wrapper class to keep track of the relationship Though that
 * would probably be needed anyway if we are to implement the User-bot good/bad
 * relationship meter thing (which I really want to)
 * <br>
 *
 *
 * @author Jonathan Augsutine
 *
 */
public class Weebot implements Comparable<Weebot> {

    /** Hosting guild */
    private final long GUILD_ID;
    /** The Bot's ID string ending in "W". */
    private final String BOT_ID;
    /** The date the bot was added to the Database */
    private final OffsetDateTime BIRTHDAY;

    /** Bot's nickname in hosting guild */
    private String nickname;
    /** Guild's command string to call the bot */
    private String callsign;
    /** Whether the bot is able to use explicit language */
    private boolean explicit;
    /** Whether the bot is able to be NSFW */
    private boolean NSFW;
    /** Whether the bot is able to respond to actions not directed to it */
    private boolean ACTIVE_PARTICIPATE;

    private transient boolean locked;

    /** Commands not allowed on the server channels.*/
    private final ConcurrentHashMap<TextChannel, ArrayList<Class<? extends Command>>>
            COMMANDS_DISABLED;
    /** List of {@code Game}s currently Running */
    private transient List<Game<? extends Player>> GAMES_RUNNING;

    /**A Map of custom Cards Against Humanity card lists mapped to "deck name" Strings.*/
    private final ConcurrentHashMap<String, CAHDeck> CUSTOM_CAH_DECKS;

    /** {@link IPassive} objects, cleared on exit */
    protected List<IPassive> passives;

    /** Map of "NotePads" */
    private final ArrayList<NotePad> NOTES;

    /** How much the bot can spam. */
    private int spamLimit;

    /**
     * Sets up a Weebot for the server.
     * Stores server <b> name </b> and <b> Unique ID long </b>
     * @param guild Guild (server) the bot is in.
     */
    public Weebot(Guild guild) {
        this.GUILD_ID = guild.getIdLong();
        this.BOT_ID = guild.getId() + "W";
        this.nickname = "Weebot";
        this.BIRTHDAY   = OffsetDateTime.now();
        this.callsign = "<>";
        this.explicit = false;
        this.NSFW = false;
        this.ACTIVE_PARTICIPATE = false;
        this.COMMANDS_DISABLED = new ConcurrentHashMap<>();
        this.GAMES_RUNNING = new ArrayList<>();
        this.NOTES  = new ArrayList<>();
        this.spamLimit = 5;
        this.passives = new ArrayList<>();
        this.CUSTOM_CAH_DECKS = new ConcurrentHashMap<>();
    }

    /**
     * Create a Weebot with no guild or call sign.
     * <b>This is only called for the private-chat instance Weebot created
     * when a new Database is created.</b>
     */
    public Weebot() {
        this.GUILD_ID = 0L;
        this.BOT_ID = "0W";
        this.BIRTHDAY   = OffsetDateTime.now();
        this.nickname = "Weebot";
        this.callsign = "";
        this.explicit = false;
        this.NSFW = false;
        this.ACTIVE_PARTICIPATE = false;
        this.COMMANDS_DISABLED = new ConcurrentHashMap<>();
        this.GAMES_RUNNING = null;
        this.NOTES  = new ArrayList<>();
        this.spamLimit = 5;
        passives = new ArrayList<>();
        CUSTOM_CAH_DECKS = null;
    }

    /**
     * Check if the message is valid for this bot.
     * @param args split Sring[] arguments parsed from message stripped content
     * @return {@code 1} if the message begins with the right {@code callsign}
     * 			<br> {@code 2} if the message begins with the right {@code nickname} <br>
     * 			{@code 0} otherwise
     */
    private int validateCallsign(String... args) {
        if(args.length == 0) return -1;
        //Don't take commands with a space between the call sign and the command
        //It would just make life less easy
        if(args[0].startsWith(this.callsign)
                        && args[0].length() > this.callsign.length()) {
            return 1;
        }
        else if(args[0].equalsIgnoreCase("@" + this.nickname))
            return 2;
        return 0;
    }

    /**
     * Take in a {@link BetterEvent} and calls the appropriate command.
     * @param event BetterEvent to read
     */
    public void readEvent(BetterEvent event) {
        if(event instanceof BetterMessageEvent) {
            BetterMessageEvent messageEvent = (BetterMessageEvent) event;
            this.submitToPassives(messageEvent);
            if (!locked) {
                switch (this.validateCallsign(messageEvent.getArgs())) {
                    case 1:
                        this.runCommand(messageEvent, 0);
                        break;
                    case 2:
                        this.runCommand(messageEvent, 1);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Find and execute the command requested in a message.
     * @param event The arguments of the command
     * @param startIndex The index the commands begin at
     */
    private void runCommand(BetterMessageEvent event, int startIndex) {
        //get the arg string without the callsign
        String command;
        if(startIndex == 0) command = event.getArgs()[0].substring(this.callsign.length());
        else command = event.getArgs()[1];

        for (Command c : Launcher.getCommands()) {
            if(c.isCommandFor(command)) {
                if(this.commandIsAllowed(c, event)) {
                    c.run(this, event);
                } else {
                    event.reply("This command is not allowed in this channel.");
                }
                return;
            }
        }
    }

    /**
     * Checks if a command has been banned from a channel.
     * @param c The {@link Command} to check
     * @param e The {@link BetterMessageEvent} that called it.
     * @return {@code true} if the command is not banned in the event's chat.
     */
    private boolean commandIsAllowed(Command c, BetterMessageEvent e) {
        try {
            //Get the channel the message was sent in
            ArrayList<Class<? extends Command>> commands = this.COMMANDS_DISABLED.get(e.getTextChannel());

            for (Class com : commands) {
                if(c.getClass().equals(com)) {
                    return false;
                }
            }
        } catch (NullPointerException exc) {
            //If no list of banned commands was found
            return true;
        }
        return true;
    }

    /**
     * Submit the event to any running {@link IPassive} implementations on this bot.
     * @param event The event to distribute.
     */
    private void submitToPassives(BetterMessageEvent event) {
        this.passives.forEach( p -> p.accept(event));
        this.passives.removeIf( IPassive::dead );
    }

    /**
     * Update the bot's mutable settings that may have changed during downtime like
     * NickName. And initialize transient variables.
     */
    public void updateBotOnStartup() {
        if (this.getGuildID() > 0) //Ignores the private bot
            this.nickname = Launcher.getGuild(this.GUILD_ID)
                                    .getMember(Launcher.getJda().getSelfUser())
                                    .getEffectiveName();
        if (this.GAMES_RUNNING == null)
            this.GAMES_RUNNING = new ArrayList<>();
        if (this.passives == null) {
            this.passives = new ArrayList<>();
        }
    }

    /**@return The bot's self use from {@link JDA#getSelfUser()}. */
    public final User asUser() {
        return Launcher.getJda().getSelfUser();
    }

    @Override
    public String toString() {
        String out = "";
        out += this.getBotId() + "\n\t";
        out += this.getGuildName() + "\n\t";
        out += this.getNickname() + "\n";
        return out;
    }

    /**
     * @param w2 Weebot to compare to.
     * @return  negative int if the Guild/Server ID is less than parameter's
     * 			0 if equal to parameter's
     * 			positive int if greater than parameter's
     */
    @Override
    public int compareTo(Weebot w2) {
        return (int) (this.getGuildID() - w2.getGuildID());
    }

    /** Lock access to the Weebot from the Discord */
    public void lock() { this.locked = true; }
    /** Unlock access to the Weebot from the Discord */
    public void unlock() { this.locked = false; }

    /** @return The ID of the guild the bot is linked to.*/
    public final long getGuildID() {
        return this.GUILD_ID;
    }

    /** @return The Guild hosting the bot */
    public final Guild getGuild() { return Launcher.getGuild(this.GUILD_ID); }

    /** @return Name of the Guild or "PRIVATE BOT" if the private message bot.*/
    public final String getGuildName() {
        return this.GUILD_ID != 0L ? Launcher.getGuild(this.GUILD_ID).getName() : "PRIVATE BOT";
    }

    /** @return String ID of the bot (1234W) */
    public final String getBotId() {
        return this.BOT_ID;
    }

    public final OffsetDateTime getBirthday() {
        return this.BIRTHDAY;
    }

    public final synchronized String getNickname() {
        return this.nickname;
    }

    /**
     * Set the bot's internal copy of its effective name in its Guild.
     * @param newName The new {@link Weebot#nickname} for the bot.
     * @return The previous value of {@link Weebot#nickname}.
     */
    public final synchronized String setNickname(String newName) {
        String old = this.nickname;
        this.nickname = newName;
        return old;
    }

    /** @return The String to call the bot. */
    public final synchronized String getCallsign() {
        return this.callsign;
    }

    /**
     * Set the String used to call the bot.
     * @param newPrefix The new String used to call the bot.
     * @return The previous value of {@link Weebot#callsign}.
     */
    public final synchronized String setCallsign(String newPrefix) {
        String old = this.callsign;
        this.callsign = newPrefix;
        return old;
    }

    /** @return {@code true} if the bot is set to be explicit. */
    public final synchronized boolean isExplicit() {
        return this.explicit;
    }

    /**
     * Set whether the bot is allowed to be explicit.
     * @param explicit Is the bot allowed to be explicit.
     * @return The previous value of {@link Weebot#explicit}.
     */
    public final synchronized boolean setExplicit(boolean explicit) {
        boolean old = this.explicit;
        this.explicit = explicit;
        return old;
    }

    /** Set the bot's AutoAdmin */
    public final synchronized void setAutoAdmin(AutoAdmin autoAdmin) {
        this.passives.removeIf( (IPassive p) -> p instanceof AutoAdmin);
        this.passives.add(0, autoAdmin);
    }

    /** Get the bot's AutoAdmin */
    public final synchronized AutoAdmin getAutoAdmin() {
        for (IPassive p : this.passives)
            if (p instanceof AutoAdmin) return (AutoAdmin) p;
        return null;
    }

    /** @return {@code true} if the bot is allowed to be not-safe-for-work.*/
    public boolean isNSFW() {
        return this.NSFW;
    }

    /**
     * Set whether the bot is allowed to be not-safe-for-work.
     * @param NSFW {@code true} if the bot can be NSFW
     * @return The previous value of {@link Weebot#NSFW}.
     */
    public final boolean setNSFW(boolean NSFW) {
        boolean old = this.NSFW;
        this.NSFW = NSFW;
        return old;
    }

    /**
     * @return {@code true} if the bot is able to respond to messages not
     *              directed to it
     */
    public final boolean canParticipate() {
        return this.ACTIVE_PARTICIPATE;
    }

    /**
     *
     * @param ACTIVE_PARTICIPATE {@code true} if the bot can join conversations.
     * @return The previous value of {@link Weebot#ACTIVE_PARTICIPATE}
     */
    public final boolean setACTIVE_PARTICIPATE(boolean ACTIVE_PARTICIPATE) {
        boolean old = this.ACTIVE_PARTICIPATE;
        this.ACTIVE_PARTICIPATE = ACTIVE_PARTICIPATE;
        return old;
    }

    /**
     * @return A list of {@link Game} instances currently
     *              being run by the bot.
     */
    public synchronized List<Game<? extends Player>> getRunningGames() {
        return this.GAMES_RUNNING;
    }

    /**
     * Add a running {@link Game} to the bot.
     * @param game The {@link Game} to add.
     */
    public final void addRunningGame(Game<? extends Player> game) {
        this.GAMES_RUNNING.add(game);
    }

    /**@return HashMap of Cards Against Humanity card lists mapped to Deck-Name Strings*/
    public final synchronized ConcurrentHashMap<String, CAHDeck> getCustomCahDecks() {
        return this.CUSTOM_CAH_DECKS;
    }

    /**
     * @param deckname Name of the "deck" requested.
     * @return Cards Against Humanity Deck mapped to the name. Null if not found.
     */
    public final synchronized CAHDeck getCustomCahDeck(String deckname) {
        return this.CUSTOM_CAH_DECKS.get(deckname);
    }

    /**
     * Make a new custom {@link CAHDeck Cards Against Humanity Deck}.
     * @param deck The Deck to add.
     * @return false if the deck already exists.
     */
    public final synchronized boolean addCustomCahDeck(CAHDeck deck) {
        return this.CUSTOM_CAH_DECKS.putIfAbsent(deck.getName(), deck) == null;
    }

    /**
     * Remove a custome {@link CAHDeck}.
     * @param deckname The name of the deck.
     * @return false if the deck does not exist.
     */
    public final boolean removeCustomCahDeck(String deckname) {
        return this.CUSTOM_CAH_DECKS.remove(deckname) == null;
    }

    public final boolean addPassive(IPassive passive) {
        return this.passives.add(passive);
    }

    public final synchronized List<IPassive> getPassives() {
        return this.passives;
    }

    /** @return {@link TreeMap TreeMap<String,NotePad>} */
    public ArrayList<NotePad> getNotePads() {
        return NOTES;
    }

    /** @return The number of messages the bot can spam at once. */
    public int getSpamLimit() { return this.spamLimit; }

    /**
     * Set the number of time the bot can spam at once.
     * @param limit The new limit
     * @return The old limit.
     */
    public int setSpamLimit(int limit) {
        int old = this.spamLimit;
        this.spamLimit = limit;
        return old;
    }

}
