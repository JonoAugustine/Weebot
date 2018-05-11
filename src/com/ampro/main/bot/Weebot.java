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

package com.ampro.main.bot;

import com.ampro.main.Launcher;
import com.ampro.main.bot.commands.Command;
import com.ampro.main.game.Game;
import com.ampro.main.game.Player;
import com.ampro.main.listener.events.BetterEvent;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.*;

//import org.joda.time.LocalDateTime;

/**
 * A representation of a Weebot entity linked to a Guild.<br>
 *     Contains a reference to the Guild hosting the bot and
 *     the settings applied to the bot by said Guild. <br><br>
 *     Each Weebot is assigned an ID String consisting of the
 *     hosting Guild's unique ID + "W" (e.g. "1234W") <br><br>
 *
 * Development plans TODO: <br>
 *      Add a {@code Refresh} method to call on startup that updates and settings
 *      (like {@link Weebot#nickname}) that were changed during downtime.
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
    //TODO private LocalDateTime BIRTHDAY;

    /** Bot's nickname in hosting guild */
    private String nickname;
    /** Guils's command string to call the bot */
    private String callsign;
    /** Whether the bot is able to use explicit language */
    private boolean explicit;
    /** Words banned on the server */
    private final List<String> BANNED_WORDS;
    /** Whether the bot is able to be NSFW */
    private boolean NSFW;
    /** Whether the bot is able to respond to actions not directed to it */
    private boolean ACTIVE_PARTICIPATE;

    /** Commands not allowed on the server channels.*/
    private final TreeMap<TextChannel, ArrayList<Class<? extends Command>>> COMMANDS_DISABLED;
    /** List of {@code Game}s currently Running */
    private final List<Game<? extends Player>> GAMES_RUNNING;

    /**
     * Sets up a Weebot for the server.
     * Stores server <b> name </b> and <b> Unique ID long </b>
     * @param guild Guild (server) the bot is in.
     */
    public Weebot(Guild guild) {
        this.GUILD_ID = guild.getIdLong();
        this.BOT_ID = guild.getId() + "W";
        this.nickname = "Weebot";
        this.callsign = "<>";
        this.explicit = false;
        this.NSFW = false;
        this.BANNED_WORDS = new ArrayList<>();
        this.ACTIVE_PARTICIPATE = false;
        this.COMMANDS_DISABLED = new TreeMap<>();
        this.GAMES_RUNNING = new ArrayList<>();
        //TODO this.BIRTHDAY   = new LocalDateTime();
    }

    /**
     * Create a Weebot with no guild or call sign.
     * <b>This is only called for the private-chat instance Weebot created
     * when a new Database is created.</b>
     */
    public Weebot() {
        this.GUILD_ID = 0L;
        this.BOT_ID = "0W";
        this.nickname = "Weebot";
        this.callsign = "";
        this.explicit = false;
        this.NSFW = false;
        this.BANNED_WORDS = new ArrayList<>();
        this.ACTIVE_PARTICIPATE = false;
        this.COMMANDS_DISABLED = new TreeMap<>();
        this.GAMES_RUNNING = null;
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
     * Take in a {@code com.ampro.listener.events.BetterEvent}
     * and calls the appropriate command.
     * @param event BetterEvent to read
     */
    public void readEvent(BetterEvent event) {
        if(event instanceof BetterMessageEvent) {
            BetterMessageEvent messageEvent = (BetterMessageEvent) event;
            messageEvent.getArgs();
            switch (this.validateCallsign(messageEvent.getArgs())) {
                case 1:
                    this.runCommand(messageEvent, 0);
                    return;
                case 2:
                    this.runCommand(messageEvent, 1);
                    return;
                default:
                    break; //To allow for active participate later
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

        //Command help = Launcher.getCommand(HelpCommand.class);
        //if (help != null) help.run(this, event);
        event.reply("Sorry, I don't recognize that command.");

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
     * Spams "SPAM ATTACK" for the number listed after spam. Default is 5.
     * @param channel TextChannel to send spam to
     * @param command Command used to invode call to this method
     */
    private void spam(TextChannel channel, String[] command) {
        int loop;
        try {
            loop = Integer.parseInt(command[1]);
            if(loop > 10) {
                channel.sendMessage("That's a bit much...").queue();
                return;
            }
        } catch (IndexOutOfBoundsException e) {
            loop = 5;
        } catch (NumberFormatException e2) {
            System.err.println("Failed to parse int from: " + command[1]);
            loop = 5;
        }
        for (int i = 0; i < loop; i++) {
            channel.sendMessage("SPAM ATTACK").queue();
        }
    }

    /** Ping-Pong */
    private void pong(TextChannel channel) {
        channel.sendMessage("Pong!").queue();
    }

    /**
     * @param w2 Weebot to compare to.
     * @return -1 if the Guild/Server ID is less than parameter's
     * 			0 if equal to parameter's
     * 			1 if greater than parameter's
     */
    @Override
    public int compareTo(Weebot w2) {
        return (int) (this.getGuildID() - w2.getGuildID());
    }

    @Override
    public String toString() {
        String out = "";
        out += this.getBotId() + "\n\t";
        out += this.getGuildName() + "\n\t";
        out += this.getNickname() + "\n";
        return out;
    }

    /** @return The ID of the guild the bot is linked to.*/
    public final long getGuildID() {
        return this.GUILD_ID;
    }

    /** @return Name of the Guild or "PRIVATE BOT" if the private message bot.*/
    public final String getGuildName() {
        return this.GUILD_ID != 0L ? Launcher.getGuild(this.GUILD_ID).getName() : "PRIVATE BOT";
    }

    /** @return String ID of the bot (1234W) */
    public final String getBotId() {
        return this.BOT_ID;
    }

    public final String getNickname() {
        return this.nickname;
    }

    /**
     * Set the bot's internal copy of its effective name in its Guild.
     * @param newName The new {@link Weebot#nickname} for the bot.
     * @return The previous value of {@link Weebot#nickname}.
     */
    public final String setNickname(String newName) {
        String old = this.nickname;
        this.nickname = newName;
        return old;
    }

    /** @return The String to call the bot. */
    public final String getCallsign() {
        return this.callsign;
    }

    /**
     * Set the String used to call the bot.
     * @param newPrefix The new String used to call the bot.
     * @return The previous value of {@link Weebot#callsign}.
     */
    public final String setCallsign(String newPrefix) {
        String old = this.callsign;
        this.callsign = newPrefix;
        return old;
    }

    /** @return {@code true} if the bot is set to be explicit. */
    public final boolean isExplicit() {
        return this.explicit;
    }

    /**
     * Set whether the bot is allowed to be explicit.
     * @param explicit Is the bot allowed to be explicit.
     * @return The previous value of {@link Weebot#explicit}.
     */
    public final boolean setExplicit(boolean explicit) {
        boolean old = this.explicit;
        this.explicit = explicit;
        return old;
    }

    /** @return A List of the Guild's banned words */
    public final List<String> getBannedWords() {
        return this.BANNED_WORDS;
    }

    /**
     * Add words to the bot's banned word list.
     * @param words Words to ban.
     */
    public final void addBannedWords(String... words) {
        this.BANNED_WORDS.addAll(Arrays.asList(words));
    }

    /**
     * Add words to the bot's banned word list.
     * @param words Words to ban.
     */
    public final void addBannedWords(Collection<String> words) {
        this.BANNED_WORDS.addAll(words);
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
     * @return A list of {@link com.ampro.main.game.Game} instances currently
     *              being run by the bot.
     */
    public List<Game<? extends Player>> getGAMES_RUNNING() {
        return this.GAMES_RUNNING;
    }

    /**
     * Add a running {@link Game} to the bot.
     * @param game The {@link Game} to add.
     */
    public final void addRunningGame(Game<? extends Player> game) {
        this.GAMES_RUNNING.add(game);
    }

}
