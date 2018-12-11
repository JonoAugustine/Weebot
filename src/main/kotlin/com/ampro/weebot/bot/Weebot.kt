/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.commands.CMD_REM
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.utilitycommands.CmdReminder
import com.ampro.weebot.commands.utilitycommands.CmdReminder.Companion.remWatchJob
import com.ampro.weebot.commands.utilitycommands.CmdReminder.Reminder
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.getGuild
import com.ampro.weebot.util.elog
import com.jagrosh.jdautilities.command.GuildSettingsProvider
import kotlinx.coroutines.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A store of settings for a Weebot. This class exists to make compliance with
 * JDA-Utilities a little easier.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class WeebotSettings(val guildID: Long) : GuildSettingsProvider {

    /** Bot's nickname in hosting guild  */
    val nickname: String
        get() { return getGuild(guildID)?.selfMember?.nickname ?: "Weebot" }

    /** Guild's command string to call the bot.*/
    var prefixs = mutableListOf("\\", "w!")

    /** Whether the bot is able to use explicit language  */
    var explicit: Boolean = false
    /** Whether the bot is able to be nsfw */
    var nsfw: Boolean = false
    /** Whether the bot is able to respond to actions not directed to it */
    var enablePassives: Boolean = false

    /** The [TextChannel] to send logs to */
    var logchannel: Long = -1

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
    fun sendLog(message: String, consumer: (Message) -> Unit = {}) {
        getGuild(guildID)?.getTextChannelById(logchannel)?.sendMessage(message)
            ?.queue { consumer(it) }
    }

    /** Allows Weebot to track usage for stats */
    var trackingEnabled: Boolean = false

    override fun getPrefixes() = prefixs
}

/**
 * A representation of a Weebot entity linked to a Guild.<br></br>
 * Contains a reference to the Guild hosting the bot and
 * the settings applied to the bot by said Guild. <br></br><br></br>
 * Each Weebot is assigned an ID String consisting of the
 * hosting Guild's unique ID + "W" (e.g. "1234W") <br></br><br></br>
 *
 * @param guildID The ID of the host guild
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
open class Weebot(/**The ID of the host guild.*/ val guildID: Long)
    : Comparable<Weebot> {

    /** @param guild The host guild */
    constructor(guild: Guild) : this(guild.idLong)

    /*************************************************
     *                  BackEnd Info                 *
     *************************************************/

    /** The date the bot was added to the Database. */
    val initDate: OffsetDateTime = OffsetDateTime.now()

    /** Whether the bot can accept commands or not */
    @Transient
    private var locked: Boolean = false

    /*************************************************
     *                  Settings                     *
     *************************************************/

    /** The [GuildSettingsProvider] for the Weebot */
    val settings: WeebotSettings = WeebotSettings(guildID)

    /*************************************************
     *               State & User Data               *
     *************************************************/

    /** [IPassive] objects, cleared on exit  */
    @get:Synchronized
    val passives: ArrayList<IPassive> = ArrayList()

    /*************************************************
        *               INIT                       *
     *************************************************/

    init {

    }

    /**
     * @param klass The class of the [IPassive] wanted
     * @return The first [IPassive] of the given class or null
     */
    fun <C:IPassive> getPassive(klass: KClass<C>)
            = passives.firstOrNull { klass == it::class }

    /**
     * Removes dead [IPassive.dead] then
     * takes in an event and distributes it to the bot's [IPassive]s.
     *
     * @param event The event to distribute
     */
    open fun feedPassives(event: Event)
            = passives.apply { removeIf(IPassive::dead) }.forEach{ it.accept(this, event) }

    /**
     * Any startup settings or states that must be reloaded before launch.
     * TODO Startup
     */
    open fun startUp() {
    }

    override fun compareTo(other: Weebot) = (this.guildID - other.guildID).toInt()

}

/**
 * The Global Weebot, with information on all users. Talks to people in private chats.
 *
 * @author Jonathan Augusitine
 * @since 1.0
 */
class GlobalWeebot : Weebot(-1L) {

    init {
        this.settings.prefixs = mutableListOf("", "w!", "\\", "!")
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
    fun getUesrPassiveList(user: User)
            = userPassives.getOrPut(user.idLong) { mutableListOf()}


    /**
     * Adds a [IPassive] to the user's lists of global passives if the user
     * has not reached the max number of global passives.
     *
     * @return true if the passive was added, false if the max had been reached
     */
    fun addUserPassive(user: User, iPassive: IPassive): Boolean {
        val list = userPassives.getOrPut(user.idLong) { mutableListOf()}
        return if (DAO.isPremium(user)) {
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

    override fun feedPassives(event: Event) {
        userPassives.values.forEach { it ->
            GlobalScope.launch {
                it.apply { removeIf { it.dead() } }.forEach {
                    it.accept(this@GlobalWeebot, event)
                }
            }
        }
    }

    val REM_MAX = 10
    val REM_MAX_PREM = 50

    private val userReminders = ConcurrentHashMap<Long, MutableList<Reminder>>()

    /**
     * @return The list of [Reminder]s linked to this user
     */
    fun getReminders(user: User) = userReminders.getOrPut(user.idLong) { mutableListOf() }

    fun getReminders() = userReminders.toMap()

    fun addReminder(user: User, reminder: Reminder) = synchronized(userReminders) {
        val list = userReminders.getOrPut(user.idLong) { mutableListOf() }
        val added = if (DAO.isPremium(user)) {
            when {
                list.size >= REM_MAX_PREM -> false
                else -> { list.add(reminder); true }
            }
        } else {
            when {
                list.size >= REM_MAX -> false
                else -> { list.add(reminder); true }
            }
        }

        if (added) { CMD_REM.remJobMap.putIfAbsent(user.idLong, remWatchJob(list)) }
        return@synchronized added
    }

    override fun startUp() {
        CMD_REM.init()
    }

}
