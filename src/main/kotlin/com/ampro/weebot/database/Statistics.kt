/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.database

import com.ampro.weebot.GlobalWeebot
import com.ampro.weebot.Weebot
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.size
import com.ampro.weebot.extensions.trueSize
import com.jagrosh.jdautilities.command.Command
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.entities.User
import java.time.OffsetDateTime


fun track(
    command: WeebotCommand, weebot: Weebot, user: User, time: OffsetDateTime
) = when {
    weebot is GlobalWeebot -> user.data?.tracked == true
    !weebot.settings.trackingEnabled -> false
    else -> {
        getStatPlot(command.permaID).apply {
            points += StatisticPoint(weebot, user, time)
        }.let { runBlocking { it.save() } }
    }
}


class StatisticPlot(val _id: String) {
    val points = hashSetOf<StatisticPoint>()
    operator fun plusAssign(point: StatisticPoint) = points.plusAssign(point)
    fun summarize(): String = TODO()
}

/**
 * A class to track the bot's usage.
 *
 * @property time The
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class StatisticPoint(bot: Weebot, user: User, val time: OffsetDateTime) {

    val botState = BotState(bot)
    val userState = UserState(user)

    /**
     * A data class to hold tracked information about the state of a Weebot.
     *
     * @param settings The weebot's current settings
     * @param init The init time of the bot (for finding age)
     * @param passivesEnabled The number of [IPassive]s enabled
     * @param disabledCommands A list of [Command]s disabled by this bot
     * @param guildSize The size of the host giuld
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class BotState(weebot: Weebot) {
        val settings = weebot.settings
        val init = weebot.initDate
        val guildSize = getGuild(weebot.guildID)?.size ?: -1
        val percentHuman = run {
            val guild = getGuild(weebot.guildID)
            guild?.trueSize?.div(guild.size.toDouble()) ?: 1.0
        }
    }

    /**
     * State of [User] on tracking.
     *
     * @param user The user tracked.
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class UserState(user: User) {
        /** How many guilds does this user share with Weebot. */
        val mutualGuilds: Int = user.mutualGuilds.size
    }

    /**
     * A Unit of a [Command]'s usage, with information about the guild, invoking
     * user and corresponding Weebot.
     *
     * @param guildID
     * @param botState
     * @param userState The [UserState] of the invoker User
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    data class CommandUsageEvent(
        val guildID: Long,
        val botState: BotState,
        val userState: UserState,
        val time: OffsetDateTime?
    )

}
