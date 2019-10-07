/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.bot.Credentials.Tokens
import com.ampro.weebot.bot.commands.*
import com.ampro.weebot.botCount
import com.ampro.weebot.logger
import com.serebit.strife.bot
import com.serebit.strife.data.Activity
import com.serebit.strife.data.OnlineStatus
import com.serebit.strife.onReady
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.timer
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

/** Initialize the Weebot. */
@ExperimentalTime
suspend fun initWeebot(weebot: Boolean? = null) {
    logger.info("Weebot Init")
    logger.info("\t\tPre-Existing bots: ${botCount()}")

    bot(if (weebot == true) Tokens.weebot else Tokens.tobeew) {
        logToConsole = weebot?.not() ?: true

        cmd(Help)
        cmd(About)
        cmd(SuggestionCmd)

        cmd(Settings)
        cmd(Settings.Prefix)

        //cmd(GateKeeperCmd)
        cmd(OutHouseCmd)
        cmd(RegexTest)

        cmd(Shutdown)
        cmd(Statistics)
        cmd(ToggleEnable)

        onReady {
            WeebotInfo.name = context.selfUser.username
            timer(
                "presence",
                true,
                30.seconds.toLongMilliseconds(),
                10.minutes.toLongMilliseconds()
            ) {
                logger.trace("Updating presence")
                runBlocking {
                    context.updatePresence(
                        OnlineStatus.ONLINE, presences.random()
                    )
                }
            }
        }

    }
}

private val presences = mutableListOf(
    Activity.Type.Watching to "the world end",
    Activity.Type.Listening to "your thoughts",
    Activity.Type.Playing to "with knives"
)
