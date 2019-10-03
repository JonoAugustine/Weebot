/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.bot.Credentials.Tokens
import com.ampro.weebot.bot.commands.*
import com.ampro.weebot.botCount
import com.ampro.weebot.logger
import com.serebit.strife.*
import com.serebit.strife.data.Activity
import com.serebit.strife.data.Color
import com.serebit.strife.data.OnlineStatus
import com.serebit.strife.entities.EmbedBuilder
import com.serebit.strife.entities.author
import com.serebit.strife.entities.embed
import com.serebit.strife.entities.footer
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

        wCom(Help)
        wCom(About)

        wCom(Settings)
        wCom(Settings.Prefix)

        wCom(Shutdown)
        wCom(ToggleEnable)

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
        updatePresence(OnlineStatus.ONLINE, presences.random())
        delay(Random(69420).nextLong(60_000, 600_000))
    }
}

/**
 * Get an [EmbedBuilder] with the [author][EmbedBuilder.author],
 * [footer][EmbedBuilder.footer], [color][EmbedBuilder.color] setup.
 *
 * @param context The bot client context to use for names and avatars
 * @param run Additional scope to modify the embed
 * @return The new [EmbedBuilder].
 */
fun wEmbed(
    context: BotClient,
    run: EmbedBuilder.() -> Unit
): EmbedBuilder = embed {
    color = Color.GREEN
    author {
        name = context.selfUser.username
        imgUrl = context.selfUser.avatar.uri
    }
    footer {
        text = "Run by ${context.selfUser.username} with Strife"
        imgUrl = StrifeInfo.logoUri
    }
}.apply(run)
