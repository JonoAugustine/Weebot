/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.developer.TrackerInitPassive
import com.ampro.weebot.database.getGuild
import com.jagrosh.jdautilities.command.GuildSettingsProvider
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import java.time.OffsetDateTime

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
    var prefixs = mutableListOf<String>("\\", "w!")

    /** Whether the bot is able to use explicit language  */
    var explicit: Boolean = false
    /** Whether the bot is able to be nsfw */
    var nsfw: Boolean = false
    /** Whether the bot is able to respond to actions not directed to it */
    var enablePassives: Boolean = false

    /** The [TextChannel] to send logs to */
    var logchannel: TextChannel? = null

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
 * @author Jonathan Augustine
 * @since 1.0
 */
open class Weebot(/**The ID of the host guild.*/ val guildID: Long)
    : Comparable<Weebot> {

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
    @Transient
    val passives: ArrayList<IPassive> = ArrayList()

    /*************************************************
        *               INIT                       *
     *************************************************/

    init {

    }

    /**
     * Takes in an event and distributes it to the bot's [IPassive]s
     */
    fun feedPassives(event: Event) = passives.forEach{ it.accept(this, event) }

    /**
     * Any startup settings or states that must be reloaded before launch.
     */
    fun startup() {
    }

    override fun compareTo(other: Weebot): Int {
        TODO("not implemented")
    }

}
