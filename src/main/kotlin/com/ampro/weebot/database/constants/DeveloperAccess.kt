/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.database.constants

import com.ampro.weebot.database.getGuild
import net.dv8tion.jda.core.entities.User

/** The bot-dev chat in the NL Discord 439522471369244683*/
const val BOT_DEV_CHAT = 439522471369244683
const val NL_ONLY_CHAT = 198260878636875787
val OFFICIAL_CHATS     = listOf(BOT_DEV_CHAT, NL_ONLY_CHAT)

/** Array of registered developer Discord IDs  */
val DEV_IDS = mutableListOf(
        139167730237571072L /*JONO*/, 186130584693637131L /*DERNST*/
)

/**
 * Check if user ID matches a Developer ID.
 *
 * @param id long ID to check
 * @return true if the user ID is a dev.
 */
fun isDev(id: Long): Boolean = DEV_IDS.contains(id)

fun User.isDev() = isDev(this.idLong)

const val PHONE_JONO = "+12404236950"

const val NL_GUILD_ID = 139168039634468865
val NL_GUILD = getGuild(NL_GUILD_ID)
const val NL_SUBSCRIBER = "Neptune's Army"
