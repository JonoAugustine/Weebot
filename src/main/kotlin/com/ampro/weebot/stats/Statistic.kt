/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.stats

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
 * @property invokation The call used to invoke the command.
 */
data class BotStatistic(
    val invokation: String,
    val guildSize: Int = -1
)
