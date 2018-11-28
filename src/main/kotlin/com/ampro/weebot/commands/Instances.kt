/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.commands.moderation.CmdVoiceChannelRole
import com.ampro.weebot.database.constants.STD_GREEN
import com.ampro.weebot.main.WAITER


/* *******************************
 *       Developer Commands      *
 ********************************/

val CMD_SHUTDOWN        = CmdShutdown()
val COM_GUILDLIST       = GuildlistCommand(WAITER)
val CDM_PING            = PingCommand() //Public
val CMD_SUGG            = CmdSendSuggestion() //Public


/* *******************************
 *       Utility Commands      *
 ********************************/
val CMD_INVITEBOT = CmdInviteLink()

val CMD_ABOUT           = CmdAbout(STD_GREEN, "Weebot is a bot", arrayOf(
        "Voice Channel Roles"
))


val CDM_VCR             = CmdVoiceChannelRole()
