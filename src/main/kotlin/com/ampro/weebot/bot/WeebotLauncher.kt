/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.bot.Credentials.Tokens
import com.ampro.weebot.bot.commands.*
import com.ampro.weebot.botCount
import com.ampro.weebot.delete
import com.ampro.weebot.logger
import com.ampro.weebot.save
import com.serebit.strife.BotClient
import com.serebit.strife.BotFeatureProvider
import com.serebit.strife.bot
import com.serebit.strife.data.Activity
import com.serebit.strife.data.OnlineStatus
import com.serebit.strife.onReady
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Initialize the Weebot. */
suspend fun initWeebot(weebot: Boolean? = null) {
    logger.info("Weebot Init")
    logger.info("\t\tPre-Existing bots: ${botCount()}")

    bot(if (weebot == true) Tokens.weebot else Tokens.tobeew) {
        logToConsole = weebot?.not() ?: true
        install(object : BotFeatureProvider<MemoryFeature> {
            override fun provide() = MemoryFeature
        })

        commands()

        cmd(Help)
        cmd(About)

        cmd(Settings)
        cmd(Settings.Prefix)

        cmd(OutHouseCmd)
        cmd(RegexTest)

        cmd(Shutdown)
        cmd(ToggleEnable)

        onReady {
            WeebotInfo.name = context.selfUser.username
            timer(context)
        }

    }
}

private val presences = mutableListOf(
    Activity.Type.Watching to "the world end",
    Activity.Type.Listening to "your thoughts",
    Activity.Type.Playing to "with knives"
)

val timer: suspend BotClient.() -> Unit = {
    while (true) {
        logger.trace("Updating presence")
        updatePresence(OnlineStatus.ONLINE, presences.random())
        delay(Random(69420).nextLong(60_000, 600_000))
    }
}
