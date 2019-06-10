/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

@file:Suppress("SENSELESS_COMPARISON")

package com.ampro.weebot

import com.ampro.weebot.commands.CMD_REM
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.`fun`.games.Game
import com.ampro.weebot.commands.`fun`.games.Player
import com.ampro.weebot.commands.`fun`.games.cardgame.CahGuildInfo
import com.ampro.weebot.commands.moderation.ModerationData
import com.ampro.weebot.commands.utility.CmdReminder.Companion.remWatchJob
import com.ampro.weebot.commands.utility.CmdReminder.Reminder
import com.ampro.weebot.commands.utility.NotePad
import com.ampro.weebot.database.data
import com.ampro.weebot.database.getGuild
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.util.NOW
import com.google.gson.annotations.SerializedName
import com.jagrosh.jdautilities.command.GuildSettingsProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * A store of settings for a Weebot. This class exists to make compliance with
 * JDA-Utilities a little easier.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class WeebotSettings(val guildID: Long) {

    /** Bot's nickname in hosting guild  */
    val nickname: String
        get() = getGuild(guildID)?.selfMember?.nickname ?: "Weebot"

    /** Guild's command string to call the bot.*/
    @SerializedName("prefixs")
    var prefixes = mutableListOf<String>()

    /** The [TextChannel] to send logs to */
    var logchannel: Long = -1

    /** Allows Weebot to track usage for stats */
    var trackingEnabled: Boolean = false

    /**
     * @author Jonathan Augustine
     * @since 2.1
     */
    class CommandRestriction {
        /** True if the Command is blocked from the entire Guild */
        var guildWide = false
        val lockedTo = mutableListOf<Long>()
        val blockedFrom = mutableListOf<Long>()
        /** GuildWide Enable */
        fun open() {
            guildWide = false
            lockedTo.clear()
            blockedFrom.clear()
        }
        /** GuildWide Block */
        fun close() {
            guildWide = true
            lockedTo.clear()
            blockedFrom.clear()
        }
        /** Lock to [channels] */
        fun lockTo(channels: Iterable<TextChannel>) {
            val ids = channels.map { it.idLong }
            lockedTo.addAll(ids)
            blockedFrom.removeIf { ids.contains(it) }
        }
        /** Block from [channels] */
        fun blockFrom(channels: Iterable<TextChannel>) {
            val ids = channels.map { it.idLong }
            blockedFrom.addAll(ids)
            lockedTo.removeIf { ids.contains(it) }
        }
        fun allows(textChannel: TextChannel) = when {
            guildWide -> false
            lockedTo.isNotEmpty() -> lockedTo.contains(textChannel.idLong)
            blockedFrom.isNotEmpty() -> !blockedFrom.contains(textChannel.idLong)
            else -> true
        }
    }

    /** [TextChannel.getIdLong] -> [Pair]<[MutableList]<Class<[WeebotCommand]>>
     *     Pair<lockedTo, BlockedFrom>
     */
    var cmdRestrictions: ConcurrentHashMap<String, CommandRestriction> =
        ConcurrentHashMap()
        get() {
            if (field == null) field = ConcurrentHashMap()
            return field
        }

    fun isAllowed(cmd: WeebotCommand, textChannel: TextChannel) =
        cmdRestrictions[cmd.permaID]?.allows(textChannel) != false

    /**
     * Sneds a log message to the log channel if it is set
     */
    fun sendLog(embed: MessageEmbed, consumer: (Message) -> Unit = {}) {
        getGuild(guildID)?.getTextChannelById(logchannel)?.sendMessage(embed)
            ?.queue { consumer(it) }
    }

    /**
     * Sneds a log message to the log channel if it is set
     */
    fun sendLog(message: String, mentions: Iterable<IMentionable> = emptyList(),
                consumer: (Message) -> Unit = {}) {
        getGuild(guildID)?.getTextChannelById(logchannel)?.apply {
            val msg = MessageBuilder(
                makeEmbedBuilder("${SELF.name} Log",null,message).build())
                .also { mentions.forEach { m -> it.append(m) } }
                .build()
            sendMessage(msg).queue { consumer(it) }
        }
    }

    fun sendLog(message: Message, consumer: (Message) -> Unit = {}) {
        getGuild(guildID)?.getTextChannelById(logchannel)?.sendMessage(message)
            ?.queue { consumer(it) }
    }

}

/**
 * A representation of a Weebot entity linked to a Guild.
 *
 * Contains a reference to the Guild hosting the bot and
 * the settings applied to the bot by said Guild.
 *
 * @param guildID The ID of the host guild.
 * @param _id The internal ID of the bot. Defaults to the [guildID].
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
//@Serializable
open class Weebot(val guildID: Long, val _id: String = guildID.toString())
    : Comparable<Weebot> {

    /** @param guild The host guild */
    constructor(guild: Guild) : this(guild.idLong)

    /*************************************************
     *                  BackEnd Info                 *
     *************************************************/

    /** The date the bot was added to the Database. */
    val initDate: OffsetDateTime = OffsetDateTime.now()

    /** The date the bot was last used  */
    var leaveDate: OffsetDateTime? = null

    /** Whether the bot can accept commands or not */
    var locked: Boolean = false

    /*************************************************
     *               State & User Data               *
     *************************************************/

    /** The [GuildSettingsProvider] for the Weebot */
    val settings: WeebotSettings = WeebotSettings(guildID)

    /** [IPassive] objects, cleared on exit  */
    @get:Synchronized
    val passives: ArrayList<IPassive> = ArrayList()

    @Suppress("SENSELESS_COMPARISON")
    @get:Synchronized
    @Transient
    var games: MutableList<Game<out Player>> = mutableListOf()
        get() {
            if (field == null) field = mutableListOf()
            return field
        }

    @Suppress("SENSELESS_COMPARISON")
    @get:Synchronized
    var moderationData: ModerationData = ModerationData(NOW())
        get() {
            if (field == null) field = ModerationData(NOW())
            return field
        }

    /** [NotePad]s */
    @get:Synchronized
    val notePads = mutableListOf<NotePad>()

    @get:Synchronized
    var cahGuildInfo: CahGuildInfo = CahGuildInfo(this.guildID)
        get() {
            if (field == null) field = CahGuildInfo(this.guildID)
            return field
        }

    /*************************************************
        *               INIT                       *
     *************************************************/

    init {

    }

    /**
     * @return The first [IPassive] of the given class or null
     */
    inline fun <reified C:IPassive> getPassive()
            = passives.firstOrNull {  it::class == C::class } as C?

    /**
     * @param predicate The conditions on which to return
     * @return The first [IPassive] of the given class or null
     */
    inline fun <reified C:IPassive> getPassive(predicate: (C) -> Boolean)
            = passives.firstOrNull { it::class == C::class && predicate(it as C) } as C?

    /** @return never-null list of all [IPassive]s of the type [C] */
    inline fun <reified C:IPassive> getPassives()
            = passives.filter {  it::class == C::class } as MutableList<*>

    /**
     * @param predicate The conditions on which to return
     * @return The all [IPassive]s of the type [C] held by this [Weebot] that match
     * the given [predicate]
     */
    inline fun <reified C:IPassive> getPassives(predicate: (C) -> Boolean)
            = passives.filter { it::class == C::class && predicate(it as C) }
            as MutableList<*>

    fun add(passive: IPassive) = this.passives.add(passive)

    /**
     * Removes dead [IPassive.dead] then
     * takes in an event and distributes it to the bot's [IPassive]s.
     *
     * @param event The event to distribute
     */
    open fun feedPassives(event: Event)
            = passives.apply { removeIf(IPassive::dead) }.forEach{
        GlobalScope.launch(CACHED_POOL) { it.accept(this@Weebot, event) }
    }

    /** Get all games of type [G] currently running */
    inline fun <reified G:Game<out Player>> getRunningGames(): List<G>
            = games.filter { it is G && it.isRunning }
        .mapTo(mutableListOf()) {it as G}

    fun add(game: Game<out Player>) = this.games.add(game)

    /**
     * Any startup settings or states that must be reloaded before launch.
     */
    open fun startUp() {}

    override fun compareTo(other: Weebot) = (this.guildID - other.guildID).toInt()

}


/**
 * The Global Weebot, with information on all users. Talks to people in private chats.
 *
 * @author Jonathan Augusitine
 * @since 1.0
 */
object GlobalWeebot : Weebot(-1L, "GLOBAL") {

    init {
        this.settings.prefixes = mutableListOf("", "w!", "\\", "!")
    }

    /** A list of user IDs that have enabled personal tracking */
    val trackedUsers = ArrayList<Long>(1000)

    val PASSIVE_MAX = 10
    val PASSIVE_MAX_PREM = 50

    private val userPassives  = ConcurrentHashMap<Long, MutableList<IPassive>>()

    /**
     * @return The list of [IPassive]s linked to this user. If one does not exist,
     *          it is created, added to the map and returned
     */
    fun getUserPassiveList(user: User) = userPassives.getOrPut(user.idLong) {
        mutableListOf()
    }!!

    /**
     * Adds a [IPassive] to the user's lists of global passives if the user
     * has not reached the max number of global passives.
     *
     * @return true if the passive was added, false if the max had been reached
     */
    fun addUserPassive(user: User, iPassive: IPassive): Boolean {
        val list = userPassives.getOrPut(user.idLong) { mutableListOf()}
        return if (user.data?.status?.on == true) {
            when {
                list.size >= PASSIVE_MAX_PREM -> false
                else -> { list.add(iPassive); true }
            }
        } else {
            when {
                list.size >= PASSIVE_MAX -> false
                else -> { list.add(iPassive); true }
            }
        }
    }

    inline fun <reified T:IPassive> getUserPassive(user: User)
            = getUserPassiveList(user).firstOrNull { it::class == T::class } as T?

    inline fun <reified T:IPassive> getUserPassives(user: User): MutableList<T>
            = getUserPassiveList(user).filter { it::class == T::class }
        .mapTo(mutableListOf()) { it as T }

    override fun feedPassives(event: Event) {
        userPassives.values.forEach { it ->
            GlobalScope.launch {
                it.removeIf(IPassive::dead)
                it.forEach { it.accept(this@GlobalWeebot, event) }
            }
        }
    }

    val REM_MAX = 10
    val REM_MAX_PREM = 50

    private val userReminders = ConcurrentHashMap<Long, MutableList<Reminder>>()

    /**
     * @return The list of [Reminder]s linked to this user
     */
    fun getReminders(user: User) = userReminders.getOrPut(user.idLong)
    { mutableListOf() }!!

    fun getReminders() = userReminders.toMap()

    fun addReminder(user: User, reminder: Reminder) = synchronized(userReminders) {
        val list = userReminders.getOrPut(user.idLong) { mutableListOf() }
        val added = if (user.data?.status?.on == true) when {
            list.size >= REM_MAX_PREM -> false
            else -> { list.add(reminder); true }
        } else when {
            list.size >= REM_MAX -> false
            else -> { list.add(reminder); true }
        }

        if (added) { CMD_REM.remJobMap.putIfAbsent(user.idLong, remWatchJob(list)) }
        return@synchronized added
    }

    override fun startUp() {
        CMD_REM.init()
    }

}
