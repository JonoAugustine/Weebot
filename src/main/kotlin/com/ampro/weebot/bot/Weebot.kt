package com.ampro.weebot.bot

import com.ampro.weebot.Launcher
import com.ampro.weebot.commands.Command
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.games.Game
import com.ampro.weebot.commands.games.Player
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand.CardsAgainstHumanity.CAHDeck
import com.ampro.weebot.commands.management.AutoAdminCommand.AutoAdmin
import com.ampro.weebot.commands.properties.Restriction
import com.ampro.weebot.commands.util.NotePadCommand
import com.ampro.weebot.listener.events.BetterEvent
import com.ampro.weebot.listener.events.BetterMessageEvent
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList


/**
 * A representation of a Weebot entity linked to a Guild.<br></br>
 * Contains a reference to the Guild hosting the bot and
 * the settings applied to the bot by said Guild. <br></br><br></br>
 * Each Weebot is assigned an ID String consisting of the
 * hosting Guild's unique ID + "W" (e.g. "1234W") <br></br><br></br>
 *
 * @author Jonathan Augsutine
 */
open class Weebot : Comparable<Weebot> {

    /** Hosting guild  */
    /** @return The ID of the guild the bot is linked to. */
    val guildID: Long
    /** The Bot's ID string ending in "W".  */
    /** @return String ID of the bot (1234W)
     */
    val botId: String
    /** The date the bot was added to the Database  */
    val birthday: OffsetDateTime

    /** Bot's nickname in hosting guild  */
    var nickname: String = "Weebot"
    /** Guild's command string to call the bot  */
    var callsign: String = "<>"
    /** Whether the bot is able to use explicit language  */
    var explicit: Boolean = false
    /** Whether the bot is able to be nsfw  */
    var nsfw: Boolean = false
    /** Whether the bot is able to respond to actions not directed to it  */
    var activeParticipate: Boolean = false

    @Transient
    private var locked: Boolean = false

    /** Commands mapped to s [Restriction]  */
    /** @return [Command] classes mapped to [restrictions][Restriction].
     */
    val commandRestrictions: ConcurrentHashMap<Class<out Command>, Restriction>

    /** List of `Game`s currently Running  */
    @Transient
    lateinit var runningGames: ArrayList<Game<out Player>>

    /**A Map of custom Cards Against Humanity card lists mapped to "deck name" Strings. */
    /**@return HashMap of Cards Against Humanity card lists mapped to Deck-Name Strings
     */
    @get:Synchronized
    val customCahDecks: ConcurrentHashMap<String, CAHDeck>?

    /** [IPassive] objects, cleared on exit  */
    @get:Synchronized
    val passives: ArrayList<IPassive> = ArrayList()

    /** Map of "NotePads"
     * @return [TreeMap&lt;String,NotePad&gt;][TreeMap]
     */
    @get:Synchronized
    val notePads: ArrayList<NotePadCommand.NotePad>

    /** How much the bot can spam.  */
    private var spamLimit: Int = 0

    /** @return The Guild hosting the bot */
    val guild: Guild?
        get() = Launcher.getGuild(this.guildID)

    /** @return Name of the Guild or "PRIVATE BOT" if the private message bot. */
    val guildName: String
        get() = if (this.guildID != 0L) Launcher.getGuild(
                this.guildID)!!.name else "PRIVATE BOT"

    /** Get the bot's AutoAdmin  */
    /** Set the bot's AutoAdmin  */
    var autoAdmin: AutoAdmin
        @Synchronized get() = this.passives.find { it is AutoAdmin } as AutoAdmin
        @Synchronized set(autoAdmin) {
            this.passives.removeIf { it is AutoAdmin }
            this.passives.add(0, autoAdmin)
        }

    /**
     * Sets up a Weebot for the server.
     * Stores server ** name ** and ** Unique ID long **
     * @param guild Guild (server) the bot is in.
     */
    constructor(guild: Guild) {
        this.guildID = guild.idLong
        this.botId = guild.id + "W"
        this.nickname = "Weebot"
        this.birthday = OffsetDateTime.now()
        this.callsign = "<>"
        this.explicit = false
        this.nsfw = false
        this.activeParticipate = false
        this.commandRestrictions = ConcurrentHashMap()
        this.runningGames = ArrayList()
        this.notePads = ArrayList()
        this.spamLimit = 5
        this.customCahDecks = ConcurrentHashMap()
    }

    /**
     * Create a Weebot with no guild or call sign.
     * **This is only called for the private-chat instance Weebot created
     * when a new Database is created.**
     */
    constructor() {
        this.guildID = 0L
        this.botId = "0W"
        this.birthday = OffsetDateTime.now()
        this.nickname = "Weebot"
        this.callsign = ""
        this.explicit = false
        this.nsfw = false
        this.activeParticipate = false
        this.commandRestrictions = ConcurrentHashMap()
        //this.runningGames = null
        this.notePads = ArrayList()
        this.spamLimit = 5
        customCahDecks = null
    }

    /**
     * Check if the message is valid for this bot.
     * @param args split Sring[] arguments parsed from message stripped content
     * @return `1` if the message begins with the right `callsign`
     * <br></br> `2` if the message begins with the right `nickname` <br></br>
     * `0` otherwise
     */
    fun validateCallsign(vararg args: String): Int {
        if (args.isEmpty()) return -1
        //Don't take commands with a space between the call sign and the command
        //It would just make life less easy
        if (args[0].startsWith(callsign) && args[0].length > callsign.length) {
            return 1
        } else if (args[0].equals("@" + this.nickname, ignoreCase = true)) return 2
        return 0
    }

    /**
     * Take in a [BetterEvent] and calls the appropriate command.
     * @param event BetterEvent to read
     */
    fun readEvent(event: BetterEvent) {
        if (event is BetterMessageEvent) {
            if (!locked) {
                when (this.validateCallsign(*event.args)) {
                    1 -> this.runCommand(event)
                    2 -> this.runCommand(event)
                    else -> {}
                }
                this.submitToPassives(event)
            }
        }
    }

    /**
     * Find and execute the command requested in a message.
     *
     * @param event The arguments of the command
     */
    private fun runCommand(event: BetterMessageEvent) {
        //get the arg string without the callsign
        val call = Command.cleanArgs(this, event)[0]
        for (c in Launcher.getCommands()) {
            if (c.isCommandFor(call)) {
                if (this.commandIsAllowed(c, event)) {
                    c.run(this, event)
                } else {
                    event.reply("This command is not allowed in this channel.")
                }
                return
            }
        }
    }

    /**
     * Checks if a command has been banned from a channel.
     * @param cmd The [Command] to check
     * @param event The [BetterMessageEvent] that called it.
     * @return `true` if the command is not banned in the event's chat.
     */
    private fun commandIsAllowed(cmd: Command, event: BetterMessageEvent): Boolean {
        //Admin is admin
        if (event.member.hasPermission(Permission.ADMINISTRATOR)) return true
        val restriction = commandRestrictions[cmd.javaClass]
        //If there is no restriction data mapped, then it's allowed
        if (restriction == null || !restriction.restricted()) return true
        //If the channel isn't allowed then immediately return false
        if (!restriction.isAllowed(event.textChannel)) return false

        if (restriction.isAllowed(event.author)) return true
        //For the roles, if any role is allowed, we will consider it an override
        //of any blocked role.
        for (role in event.member.roles) {
            if (restriction.isAllowed(role)) return true
        }
        return false
    }

    /**
     * Submit the event to any running [IPassive] implementations on this bot.
     * @param event The event to distribute.
     */
    private fun submitToPassives(event: BetterMessageEvent) {
        this.passives.forEach { p -> p.accept(this, event) }
        this.passives.removeIf { it.dead() }
    }

    /**
     * Update the bot's mutable settings that may have changed during downtime like
     * NickName. And initialize transient variables.
     */
    open fun startup() {
        if (this.guildID > 0) {
            //Ignores the private bot
            this.nickname = Launcher.getGuild(this.guildID)!!.getMember(
                    Launcher.getJda().selfUser).effectiveName
        }
        this.runningGames = ArrayList()
    }

    /**@return The bot's self use from [JDA.getSelfUser].
     */
    fun asUser() = Launcher.getJda().selfUser!!

    override fun toString(): String {
        var out = ""
        out += this.botId + "\n\t"
        out += this.guildName + "\n\t"
        out += this.nickname + "\n"
        return out
    }

    /**
     * @param other Weebot to compare to.
     * @return  negative int if the Guild/Server ID is less than parameter's
     * 0 if equal to parameter's
     * positive int if greater than parameter's
     */
    override fun compareTo(other: Weebot): Int {
        return (this.guildID - other.guildID).toInt()
    }

    /** Lock access to the Weebot from the Discord  */
    fun lock() {
        this.locked = true
    }

    /** Unlock access to the Weebot from the Discord  */
    fun unlock() {
        this.locked = false
    }

    /**
     * Set the bot's internal copy of its effective name in its Guild.
     * @param newName The new [Weebot.nickname] for the bot.
     * @return The previous value of [Weebot.nickname].
     */
    @Synchronized
    fun setNickname(newName: String): String? {
        val old = this.nickname
        this.nickname = newName
        return old
    }

    /**
     * Set the String used to call the bot.
     * @param newPrefix The new String used to call the bot.
     * @return The previous value of [Weebot.callsign].
     */
    @Synchronized
    fun setCallsign(newPrefix: String): String? {
        val old = this.callsign
        this.callsign = newPrefix
        return old
    }

    /** @return `true` if the bot is set to be explicit.
     */
    @Synchronized
    fun isExplicit(): Boolean {
        return this.explicit
    }

    /**
     * Set whether the bot is allowed to be explicit.
     * @param explicit Is the bot allowed to be explicit.
     * @return The previous value of [Weebot.explicit].
     */
    @Synchronized
    fun setExplicit(explicit: Boolean): Boolean {
        val old = this.explicit
        this.explicit = explicit
        return old
    }

    /** @return `true` if the bot is allowed to be not-safe-for-work.
     */
    @Synchronized
    fun isNSFW(): Boolean {
        return this.nsfw
    }

    /**
     * Set whether the bot is allowed to be not-safe-for-work.
     * @param NSFW `true` if the bot can be nsfw
     * @return The previous value of [Weebot.nsfw].
     */
    fun setNSFW(NSFW: Boolean): Boolean {
        val old = this.nsfw
        this.nsfw = NSFW
        return old
    }

    /**
     * @return `true` if the bot is able to respond to messages not
     * directed to it
     */
    @Synchronized
    fun canParticipate(): Boolean {
        return this.activeParticipate
    }

    /**
     *
     * @param activeParticipate `true` if the bot can join conversations.
     * @return The previous value of [Weebot.activeParticipate]
     */
    @Synchronized
    fun setActiveParticipate(activeParticipate: Boolean): Boolean {
        val old = this.activeParticipate
        this.activeParticipate = activeParticipate
        return old
    }

    /**
     * Get the [Restriction] mapped to the Command. This will never
     * return null, if a Command has no restriction mapped, one is added then
     * returned.
     * @param cmd The command's class
     * @return A non-null Restriction mapped to the Command class.
     */
    @Synchronized
    fun getCmdRestriction(cmd: Command): Restriction {
        val r = commandRestrictions.putIfAbsent(cmd.javaClass, Restriction())
        return r ?: Restriction()
    }

    /**
     * Get the [Restriction] mapped to the Command. This will never
     * return null, if a Command has no restriction mapped, one is added then
     * returned.
     * @param cmd The command's class
     * @return A non-null Restriction mapped to the Command class.
     */
    @Synchronized
    fun getCmdRestriction(cmd: Class<out Command>): Restriction {
        val r = commandRestrictions.putIfAbsent(cmd, Restriction())
        return r ?: Restriction()
    }

    /**
     * Add a running [Game] to the bot.
     * @param game The [Game] to add.
     */
    @Synchronized
    fun addRunningGame(game: Game<out Player>) = this.runningGames.add(game)

    /**
     * @param deckname Name of the "deck" requested.
     * @return Cards Against Humanity Deck mapped to the name. Null if not found.
     */
    @Synchronized
    fun getCustomCahDeck(deckname: String): CAHDeck? = this.customCahDecks!![deckname]

    /**
     * Make a new custom [Cards Against Humanity Deck][CAHDeck].
     * @param deck The Deck to add.
     * @return false if the deck already exists.
     */
    @Synchronized
    fun addCustomCahDeck(deck: CAHDeck): Boolean {
        return customCahDecks!!.putIfAbsent(deck.name, deck) == null
    }

    /**
     * Remove a custome [CAHDeck].
     * @param deckname The name of the deck.
     * @return false if the deck does not exist.
     */
    @Synchronized
    fun removeCustomCahDeck(deckname: String): Boolean {
        return this.customCahDecks!!.remove(deckname) == null
    }

    @Synchronized
    fun addPassive(passive: IPassive) = this.passives.add(passive)

    /** @return The number of messages the bot can spam at once.
     */
    @Synchronized
    fun getSpamLimit(): Int {
        return this.spamLimit
    }

    /**
     * Set the number of time the bot can spam at once.
     * @param limit The new limit
     * @return The old limit.
     */
    @Synchronized
    fun setSpamLimit(limit: Int): Int {
        val old = this.spamLimit
        this.spamLimit = limit
        return old
    }

}
