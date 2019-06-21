/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.CmdBiggifyEmoji
import com.ampro.weebot.commands.`fun`.CmdEmojify
import com.ampro.weebot.commands.`fun`.CmdHelloThere
import com.ampro.weebot.commands.`fun`.CmdReddicord
import com.ampro.weebot.commands.`fun`.CmdThis
import com.ampro.weebot.commands.`fun`.SubCmdLeaderBoard
import com.ampro.weebot.commands.`fun`.games.TempCmdCheckReadMe
import com.ampro.weebot.commands.`fun`.games.cardgame.CmdCardsAgainstHumanity
import com.ampro.weebot.commands.developer.CmdGuildList
import com.ampro.weebot.commands.developer.CmdShutdown
import com.ampro.weebot.commands.developer.CmdStatsView
import com.ampro.weebot.commands.developer.CmdSuggestion
import com.ampro.weebot.commands.miscellaneous.CmdApiToGetALife
import com.ampro.weebot.commands.miscellaneous.CmdInviteLink
import com.ampro.weebot.commands.miscellaneous.CmdNameGenerator
import com.ampro.weebot.commands.miscellaneous.PingCommand
import com.ampro.weebot.commands.moderation.CmdChatLock
import com.ampro.weebot.commands.moderation.CmdModeration
import com.ampro.weebot.commands.moderation.CmdMoveConversation
import com.ampro.weebot.commands.moderation.CmdPurge
import com.ampro.weebot.commands.moderation.CmdReport
import com.ampro.weebot.commands.moderation.CmdSelfDestruct
import com.ampro.weebot.commands.moderation.CmdSettings
import com.ampro.weebot.commands.moderation.CmdTaskManager
import com.ampro.weebot.commands.moderation.CmdVoiceChannelGenerator
import com.ampro.weebot.commands.moderation.CmdVoiceChannelRole
import com.ampro.weebot.commands.moderation.CmdWelcomeMsg
import com.ampro.weebot.commands.progammer.CmdEmbedMaker
import com.ampro.weebot.commands.progammer.CmdRegexTest
import com.ampro.weebot.commands.utility.CmdNotePad
import com.ampro.weebot.commands.utility.CmdOutHouse
import com.jagrosh.jdautilities.command.Command.Category


private const val GEN_FAILURE_MESSAGE = "You do not have access to this command."

val CAT_GEN     = Category("General", GEN_FAILURE_MESSAGE) {true}
val CAT_DEV     = Category("Developer", GEN_FAILURE_MESSAGE) { it.isOwner }
val CAT_MOD     = Category("Moderator", GEN_FAILURE_MESSAGE) {true}
val CAT_PROG    = Category("Programmer", GEN_FAILURE_MESSAGE) {true}
val CAT_GAME    = Category("Game", GEN_FAILURE_MESSAGE) {true}
val CAT_FUN     = Category("Fun", GEN_FAILURE_MESSAGE) {true}
val CAT_SOC     = Category("Social", GEN_FAILURE_MESSAGE) {true}
val CAT_UTIL    = Category("Utility", GEN_FAILURE_MESSAGE) {true}
val CAT_MISC    = Category("Miscellaneous", GEN_FAILURE_MESSAGE) { true }
val CAT_UNDER_CONSTRUCTION = Category("Under Construction",
        "This command is still under (re)construction") { it.isOwner }

val CATEGORIES = listOf(CAT_GEN, CAT_DEV, CAT_MOD, CAT_PROG, CAT_GAME, CAT_FUN,
        CAT_SOC, CAT_UTIL, CAT_MISC)

// TODO Change commands to objects

/* *******************************
 *       Developer Commands      *
 ********************************/

val CMD_SHUTDOWN        by lazy { CmdShutdown() }
val CMD_GUILDLIST       by lazy { CmdGuildList() }
val CMD_STATS           by lazy { CmdStatsView() }
val CMD_PING            by lazy { PingCommand() }//Public
val CMD_SUGG            by lazy { CmdSuggestion() }//Public

/* *******************************
 *         About Commands        *
 ********************************/

val CMD_ABOUT           by lazy { CmdAbout() }
val CMD_WATCHADOIN      by lazy { CmdWatchaDoin() }

/* *******************************
 *       Utility Commands      *
 ********************************/

val CMD_INVITEBOT       by lazy { CmdInviteLink() }
val CMD_HELP            by lazy { CmdHelp() }
val CMD_OHC             by lazy { CmdOutHouse() }
//val CMD_REM             by lazy { CmdReminder() }
val CMD_NOTE            by lazy { CmdNotePad() }
//val CMD_TRANSLATE       by lazy { CmdTranslate() }

val CMD_VCGEN           by lazy { CmdVoiceChannelGenerator() }
val CMD_SELFDESTRUCT    by lazy { CmdSelfDestruct() }

/* *******************************
 *         Admin Commands        *
 ********************************/

val CMD_SETTINGS        by lazy { CmdSettings() }
val CMD_WELCOME         by lazy { CmdWelcomeMsg() }
val CMD_MODERATION      by lazy { CmdModeration() }
val CMD_REPORT          by lazy { CmdReport() }
val CMD_PURGE           by lazy { CmdPurge() }
val CMD_CHATLOCK        by lazy { CmdChatLock() }
val CMD_MOVECONVO       by lazy { CmdMoveConversation() }
val CMD_TASKS           by lazy { CmdTaskManager() }
val CMD_VCR             by lazy { CmdVoiceChannelRole() }

/* *******************************
 *        Programmer Stuff       *
 *********************************/

val CMD_REGEX           by lazy { CmdRegexTest() }
val CMD_EMBED           by lazy { CmdEmbedMaker() }

/* *******************************
 *              Fun              *
 *********************************/

//Random
val CMD_NAMEGEN         by lazy { CmdNameGenerator() }
val CMD_ATGAL           by lazy { CmdApiToGetALife() }

val CMD_BIGGIFY         by lazy { CmdBiggifyEmoji() }
val CMD_HELLOTHERE      by lazy { CmdHelloThere() }
val CMD_EMOJIFY         by lazy { CmdEmojify() }
val CMD_THIS            by lazy { CmdThis() }
//Structured
val CMD_REDDICORD       by lazy { CmdReddicord() }
val CMD_REDDICORD_SCORE by lazy { SubCmdLeaderBoard("reddiscore", arrayOf("reddiscores"))}
val CMD_CAH             by lazy { CmdCardsAgainstHumanity() }



val COMMANDS = listOf(CMD_SHUTDOWN, CMD_GUILDLIST, CMD_STATS,TempCmdCheckReadMe(),
        CMD_PING, CMD_SUGG, CMD_INVITEBOT, CMD_ABOUT, CMD_WATCHADOIN, CMD_WELCOME,
        CMD_SETTINGS, CMD_PURGE, CMD_MODERATION, CMD_REPORT, CMD_TASKS, CMD_CHATLOCK,
        CMD_SELFDESTRUCT, CMD_MOVECONVO,
        CMD_REGEX, CMD_EMBED,
        CMD_OHC, CMD_NOTE, //CMD_TRANSLATE, CMD_REM,
        CMD_VCR, CMD_VCGEN,
        CMD_CAH,
        CMD_NAMEGEN, CMD_ATGAL,
        CMD_THIS, CMD_HELLOTHERE, CMD_EMOJIFY, CMD_BIGGIFY,
        CMD_REDDICORD, CMD_REDDICORD_SCORE)
