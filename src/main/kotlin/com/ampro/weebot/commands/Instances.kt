/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.*
import com.ampro.weebot.commands.`fun`.games.cardgame.CmdCardsAgainstHumanity
import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.commands.miscellaneous.*
import com.ampro.weebot.commands.moderation.*
import com.ampro.weebot.commands.progammer.CmdEmbedMaker
import com.ampro.weebot.commands.progammer.CmdRegexTest
import com.ampro.weebot.commands.utilitycommands.*


/* *******************************
 *       Developer Commands      *
 ********************************/

val CMD_SHUTDOWN        = CmdShutdown()
val CMD_GUILDLIST       = CmdGuildList()
val CMD_STATS           = CmdStatsView()
val CMD_PING            = PingCommand() //Public
val CMD_SUGG            = CmdSuggestion() //Public

/* *******************************
 *         About Commands        *
 ********************************/

val CMD_ABOUT           = CmdAbout()
val CMD_WATCHADOIN      = CmdWatchaDoin()

/* *******************************
 *       Utility Commands      *
 ********************************/

val CMD_INVITEBOT       = CmdInviteLink()
val CMD_HELP            = CmdHelp()
val CMD_OHC             = CmdOutHouse()
val CMD_REM             = CmdReminder()
val CMD_NOTE            = CmdNotePad()

val CMD_VCGEN           = CmdVoiceChannelGenerator()
val CMD_SELFDESTRUCT    = CmdSelfDestruct()

/* *******************************
 *         Admin Commands        *
 ********************************/

val CMD_SETTINGS        = CmdSettings()
val CMD_WELCOME         = CmdWelcomeMsg()
val CMD_MODERATION      = CmdModeration()
val CMD_REPORT          = CmdReport()
val CMD_PURGE           = CmdPurge()
val CMD_CHATLOCK        = CmdChatLock()
val CMD_TASKS           = CmdTaskManager()
val CMD_VCR             = CmdVoiceChannelRole()

/* *******************************
 *        Programmer Stuff       *
 *********************************/

val CMD_REGEX           = CmdRegexTest()
val CMD_EMBED           = CmdEmbedMaker()

/* *******************************
 *              Fun              *
 *********************************/

//Random
val CMD_NAMEGEN         = CmdNameGenerator()
val CMD_ATGAL           = CmdApiToGetALife()

val CMD_HELLOTHERE      = CmdHelloThere()
val CMD_EMOJIFY         = CmdEmojify()
val CMD_THIS            = CmdThis()
//Structured
val CMD_REDDICORD       = CmdReddicord()
val CMD_REDDICORD_SCORE = SubCmdLeaderBoard("reddiscore", arrayOf("reddiscores"))
val CMD_CAH             = CmdCardsAgainstHumanity()



val COMMANDS = listOf(CMD_SHUTDOWN, CMD_GUILDLIST, CMD_STATS,
        CMD_PING, CMD_SUGG, CMD_INVITEBOT, CMD_ABOUT, CMD_WATCHADOIN, CMD_WELCOME,
        CMD_SETTINGS, CMD_PURGE, CMD_MODERATION, CMD_REPORT, CMD_TASKS, CMD_CHATLOCK,
        CMD_SELFDESTRUCT,
        CMD_REGEX, CMD_EMBED,
        CMD_OHC, CMD_REM, CMD_NOTE,
        CMD_VCR, CMD_VCGEN,
        CMD_CAH,
        CMD_NAMEGEN, CMD_ATGAL,
        CMD_THIS, CMD_HELLOTHERE, CMD_EMOJIFY,
        CMD_REDDICORD, CMD_REDDICORD_SCORE)
