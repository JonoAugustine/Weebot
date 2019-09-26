/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.stats

import kotlinx.serialization.Serializable

@Serializable
interface Statistic

@Serializable
data class BotStatistic(
    val size: Int,
    val commandName: String,
    val userRank: Pair<Int, Int>
) : Statistic

@Serializable
data class SiteStatistic(
    val temp: Nothing
) : Statistic
