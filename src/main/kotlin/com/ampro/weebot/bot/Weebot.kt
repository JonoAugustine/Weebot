package com.ampro.weebot.bot

import kotlinx.serialization.Serializable

/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */


@Serializable
data class Weebot(
    val guildID: Long,
    var prefix: String = WeebotInfo.defaultPrefix
)
