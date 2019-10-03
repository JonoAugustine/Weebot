/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.args
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.wEmbed
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.*
import com.serebit.strife.events.MessageCreateEvent

object Settings : Command {

    override val name = "Settings" with listOf("set")

    override var enabled = true

    override val help by lazy {
        EmbedBuilder.FieldBuilder(
            "Settings", """
        TODO
    """.trimIndent()
        )
    }

    override val predicate: suspend MessageCreateEvent.(BotBuilder) -> Boolean
        get() = { message.guild != null }

    override val action: suspend BotBuilder.(MessageCreateEvent, Weebot) -> Unit
        get() = { e, w ->
            e.message.reply(wEmbed(e.context) {
                title("${e.message.guild!!.name} Settings")
                inlineField("Prefix") { w.prefix }
            })
        }

    object Prefix : Command {
        override val name = "Prefix" with listOf("pref")
        override var enabled = true
        override val help get() = TODO()

        override val predicate: suspend MessageCreateEvent.(BotBuilder) -> Boolean
            get() = {
                message.guild != null && message.args.size == 2
            }

        override val action: suspend BotBuilder.(MessageCreateEvent, Weebot) -> Unit
            get() = { e, w ->
                w.prefix = e.message.args[1]
                e.message.reply("Prefix set to ${w.prefix}")
            }
    }
}

