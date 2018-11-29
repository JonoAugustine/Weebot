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
val CMD_GUILDLIST       = GuildlistCommand(WAITER)
val CMD_PING            = PingCommand() //Public
val CMD_SUGG            = CmdSendSuggestion() //Public


/* *******************************
 *       Utility Commands      *
 ********************************/
val CMD_INVITEBOT = CmdInviteLink()

val CMD_ABOUT           = CmdAbout(STD_GREEN, "Weebot is a bot", arrayOf(
        "Voice Channel Roles"
))

/* *******************************
 *         Admin Commands        *
 ********************************/

val CMD_SETTINGS        = CmdSettings()

val CMD_VCR             = CmdVoiceChannelRole()

val commands = listOf(CMD_SHUTDOWN, CMD_GUILDLIST, CMD_PING, CMD_SUGG, CMD_INVITEBOT,
        CMD_ABOUT, CMD_SETTINGS, CMD_VCR)
