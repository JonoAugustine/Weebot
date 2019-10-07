/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.Passive
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.add
import com.ampro.weebot.bot.getAll
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.ampro.weebot.util.Regecies
import com.ampro.weebot.util.matchesAny
import com.ampro.weebot.util.unit
import com.serebit.strife.entities.GuildTextChannel
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title
import com.serebit.strife.events.Event
import com.serebit.strife.events.MessageCreateEvent
import com.serebit.strife.events.MessageReactionAddEvent
import com.serebit.strife.events.MessageReactionRemoveEvent
import com.serebit.strife.text.italic

class Reddicord(var channelID: Long = -1) : Passive {

    private val posts = mutableMapOf<Long, MutableMap<Long, Int>>()

    override var active: Boolean = true

    override suspend fun consume(event: Event, bot: Weebot) {
        when (event) {
            is MessageCreateEvent -> TODO("Check Message")
            is MessageReactionAddEvent -> TODO("Modify Score")
            is MessageReactionRemoveEvent -> TODO("Modify Score")
        }
    }
}

object ReddicordCmd : Command(
    "Reddicord",
    listOf("rcord", "rcc"),
    listOf(LeaderBoard),
    details = TODO(),
    rateLimit = 30,
    guildOnly = true,
    params = listOfParams("on/off" to true, "channel" to true),
    predicate = {
        if (message.args.size > 1)
            message.args.size > 2 && message.args[1].matchesAny(
                Regecies.on,
                Regecies.off,
                Regecies.enable,
                Regecies.disable
            )
        else true
    },
    action = a@{
        if (message.args.size == 1) {
            message.sendWEmbed {
                val rd = message.guild!!.getAll<Reddicord>()?.firstOrNull()
                title(
                    "Reddicord is " + if (rd == null) "Disabled" else "Active"
                )
                description = "Use `reddicord on/off #channel` to (de)activate."
            }
        } else {
            if (message.args[1].matchesAny(Regecies.enable, Regecies.on)) {
                message.guild!!.getAll<Reddicord>()
                    ?.takeIf { it.isNotEmpty() }
                    ?.run {
                        return@a message.reply("Reddicord is already active")
                            .unit
                    }
                message.mentionedChannels.firstOrNull()
                    ?.let { it as GuildTextChannel }
                    ?.also { message.guild!!.add(Reddicord(it.id)) }
                    ?.run {
                        message.reply(
                            "Reddicord is now active in $asMention.".italic
                        )
                    } ?: message.reply("Please mention a text channel as well.")
            } else {
                message.guild!!.getAll<Reddicord>()
                    ?.firstOrNull()
                    ?.run {
                        active = false
                        message.reply("Reddicord deactivated")
                    }
            }
        }
    }
) {
    object LeaderBoard : Command(
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO(),
        TODO()
    )
}
