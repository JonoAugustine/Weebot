/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.CmdCatFact
import com.ampro.weebot.commands.`fun`.reactions.CmdHelloThere
import com.ampro.weebot.commands.`fun`.reactions.CmdThis
import com.ampro.weebot.commands.developer.CmdSendSuggestion
import com.ampro.weebot.commands.developer.CmdShutdown
import com.ampro.weebot.commands.developer.GuildlistCommand
import com.ampro.weebot.commands.developer.PingCommand
import com.ampro.weebot.commands.moderation.CmdPurge
import com.ampro.weebot.commands.moderation.CmdSettings
import com.ampro.weebot.commands.moderation.CmdVoiceChannelRole
import com.ampro.weebot.commands.progammer.CmdRegexTest
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

val CMD_INVITEBOT       = CmdInviteLink()
val CMD_HELP            = CmdHelp()
val CMD_ABOUT           = CmdAbout()

/* *******************************
 *         Admin Commands        *
 ********************************/

val CMD_SETTINGS        = CmdSettings()
val CMD_PURGE           = CmdPurge()
val CMD_VCR             = CmdVoiceChannelRole()

/* *******************************
 *        Programmer Stuff       *
 *********************************/

val CMD_REGEX           = CmdRegexTest()


/* *******************************
 *              Fun              *
 *********************************/

val CMD_CATFACT         = CmdCatFact()
val CMD_HELLOTHERE      = CmdHelloThere()
val CMD_THIS            = CmdThis()


val commands = listOf<WeebotCommand>(CMD_SHUTDOWN, CMD_GUILDLIST, CMD_PING,
        CMD_SUGG, CMD_INVITEBOT, CMD_ABOUT,// CMD_HELP,
        CMD_SETTINGS, CMD_PURGE,
        CMD_REGEX,
        CMD_VCR,
        CMD_THIS, CMD_HELLOTHERE)
