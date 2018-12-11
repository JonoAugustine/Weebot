/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.*
import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.commands.moderation.*
import com.ampro.weebot.commands.progammer.CmdEval
import com.ampro.weebot.commands.progammer.CmdRegexTest
import com.ampro.weebot.commands.utilitycommands.CmdOutHouse
import com.ampro.weebot.commands.utilitycommands.CmdReminder
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.main.WAITER


/* *******************************
 *       Developer Commands      *
 ********************************/

val CMD_SHUTDOWN        = CmdShutdown()
val CMD_GUILDLIST       = GuildlistCommand(WAITER)
val CMD_PING            = PingCommand() //Public
val CMD_SUGG            = CmdSuggestion() //Public


/* *******************************
 *       Utility Commands      *
 ********************************/

val CMD_INVITEBOT       = CmdInviteLink()
val CMD_HELP            = CmdHelp()
val CMD_ABOUT           = CmdAbout()
val CMD_OHC             = CmdOutHouse()
val CMD_REM             = CmdReminder()

val CMD_SELFDESTRUCT    = CmdSelfDestruct()

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
val CMD_EVAL            = CmdEval()


/* *******************************
 *              Fun              *
 *********************************/

val CMD_CATFACT         = CmdCatFact()
val CMD_HELLOTHERE      = CmdHelloThere()
val CMD_THIS            = CmdThis()
//val CMD_CAH             = CmdCardsAgainstHumanity()



val commands = listOf<WeebotCommand>(CMD_SHUTDOWN, CMD_GUILDLIST, CMD_PING,
        CMD_SUGG, CMD_INVITEBOT, CMD_ABOUT,
        CMD_SETTINGS, CMD_PURGE, CMD_SELFDESTRUCT,
        CMD_REGEX, CMD_EVAL,
        CMD_VCR, CMD_OHC, CMD_REM,
  //      CMD_CAH,
        CMD_THIS, CMD_HELLOTHERE, CMD_CATFACT)
