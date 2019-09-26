/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.bot.Credentials.Tokens
import com.ampro.weebot.bot.commands.*
import com.ampro.weebot.bots
import com.ampro.weebot.logger
import com.serebit.strife.*
import com.serebit.strife.data.*
import com.serebit.strife.entities.*

object WeebotInfo {
    const val version = "4.0.0"
    const val jonoGitLab = "https://gitlab.com/JonoAugustine"
    const val defaultPrefix = ">"
}

suspend fun initWeebot(weebot: Boolean? = null) {
    logger.info("Weebot Init")
    logger.info("\t\tPre-Existing bots: ${bots.size}")

    bot(if (weebot == true) Tokens.weebot else Tokens.tobeew) {
        logToConsole = weebot?.not() ?: true
        wCom(Help)
        wCom(About)
        wCom(Settings)
        wCom(Settings.Prefix)

        onReady {
            context.updatePresence(OnlineStatus.ONLINE,
                Activity.Type.Watching to "the world end")
        }

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
fun wEmbed(context: BotClient,
           run: EmbedBuilder.() -> Unit): EmbedBuilder = embed {
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
