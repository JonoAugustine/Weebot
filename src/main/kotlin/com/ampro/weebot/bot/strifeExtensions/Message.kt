/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.strifeExtensions

import com.serebit.strife.BotClient
import com.serebit.strife.StrifeInfo
import com.serebit.strife.data.Color
import com.serebit.strife.entities.*

val Message.args get() = content.split(' ')


/**
 * Get an [EmbedBuilder] with the [author][EmbedBuilder.author],
 * [footer][EmbedBuilder.footer], [color][EmbedBuilder.color] setup.
 *
 * @param builder Additional scope to modify the embed
 * @return The new [EmbedBuilder].
 */
fun wEmbed(context: BotClient, builder: EmbedBuilder.() -> Unit) = embed {
    color = Color.GREEN
    author {
        name = context.selfUser.username
        imgUrl = context.selfUser.avatar.uri
    }
    footer {
        text = "Run by ${context.selfUser.username} with Strife"
        imgUrl = StrifeInfo.logoUri
    }
}.apply(builder)

suspend fun Message.sendWEmbed(builder: EmbedBuilder.() -> Unit): Message? =
    channel.sendWEmbed(builder)

suspend fun TextChannel.sendWEmbed(builder: EmbedBuilder.() -> Unit) =
    send(wEmbed(context, builder))
