/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.*
import com.ampro.weebot.commands.`fun`.games.cardgame.CmdCardsAgainstHumanity
import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.commands.miscellaneous.PingCommand
import com.ampro.weebot.commands.moderation.*
import com.ampro.weebot.commands.progammer.CmdRegexTest
import com.ampro.weebot.commands.utilitycommands.*


/* *******************************
 *       Developer Commands      *
 ********************************/

val CMD_SHUTDOWN        = CmdShutdown()
val CMD_GUILDLIST       = CmdGuildList()
val CMD_STATS           = CmdStatsView()
val CMD_PASSIVES        = CmdViewPassivs() //Not Yet Public
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
val CMD_NOTE            = CmdNotePad()

val CMD_VCGEN           = CmdVoiceChannelGenerator()
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

/* *******************************
 *              Fun              *
 *********************************/

val CMD_CATFACT         = CmdCatFact()
val CMD_HELLOTHERE      = CmdHelloThere()
val CMD_THIS            = CmdThis()
val CMD_REDDICORD       = CmdReddicord()
val CMD_CAH             = CmdCardsAgainstHumanity()



val commands = listOf(CMD_SHUTDOWN, CMD_GUILDLIST, CMD_STATS, CMD_PASSIVES,
        CMD_PING, CMD_SUGG, CMD_INVITEBOT, CMD_ABOUT,
        CMD_SETTINGS, CMD_PURGE, CMD_SELFDESTRUCT,
        CMD_REGEX,
        CMD_OHC, CMD_REM, CMD_NOTE,
        CMD_VCR, CMD_VCGEN,
        CMD_CAH,
        CMD_THIS, CMD_HELLOTHERE, CMD_CATFACT, CMD_REDDICORD)
