/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.stats

import com.ampro.weebot.bot.commands.*
import org.bson.codecs.pojo.annotations.BsonId

interface Statistic

/** Used to hold  */
data class BotStatCollection(
    @BsonId val command: String,
    val stats: MutableList<BotStatistic> = mutableListOf()
)

/**
 * Used to track usage stats of commands.
 *
 * @property guildSize The size of the guild the command was used in. -1 if
 * a DmChannel.
 * @property command The command's [display name][Name.display].
 * @property invokation The call used to invoke the command.
 */
data class BotStatistic(
    val guildSize: Int = -1,
    val command: String,
    val invokation: String = command
) : Statistic

data class SiteStatistic(
    val temp: Nothing
) : Statistic
