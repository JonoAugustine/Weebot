/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.commands.moderation.CmdVoiceChannelRole
import com.ampro.weebot.main.WAITER
import com.ampro.weebot.main.constants.STD_GREEN


/* *******************************
 *       Developer Commands      *
 ********************************/

val CMD_SHUTDOWN        = CmdShutdown()
val COM_GUILDLIST       = GuildlistCommand(WAITER)
val COM_PING            = PingCommand() //Public
val CMD_SUGG            = CmdSendSuggestion() //Public


/* *******************************
 *       Utility Commands      *
 ********************************/

val CMD_ABOUT           = AboutCommand(STD_GREEN, "Weebot is a bot", arrayOf(
        "Voice Channel Roles"
))


val COM_VCR             = CmdVoiceChannelRole()
