/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.Extensions.RegexShorthand.ic
import com.ampro.weebot.args
import com.ampro.weebot.bot
import com.ampro.weebot.bot.wEmbed
import com.ampro.weebot.modify
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.*
import com.serebit.strife.onMessage

object Settings : Command {

    override val name = "Settings"

    override val help by lazy {
        EmbedBuilder.FieldBuilder("Settings", """
        TODO
    """.trimIndent())
    }

    override val matcherBase = "$ic$name".toRegex()

    override val install: BotBuilder.() -> Unit = {
        onMessage {
            val bot = message.guild?.bot ?: return@onMessage
            if (!message.content.calls(bot.prefix)) return@onMessage
            message.reply(wEmbed(context) {
                title("${message.guild!!.name} Settings")

            })
        }
    }

    object Prefix : Command {
        override val name = "Prefix"

        override val matcherBase = Regex("$ic$name\\s+.{1,3}")

        override val help by lazy {
            EmbedBuilder.FieldBuilder("Prefix", """
            TODO
        """.trimIndent())
        }

        override val install: BotBuilder.() -> Unit = {
            onMessage {
                val bot = message.guild?.bot ?: return@onMessage
                if (!message.content.calls(bot.prefix)) return@onMessage
                val np = message.args[1]
                bot.modify { prefix = np }
                message.reply(wEmbed(context) {
                    title("Prefix set to $np")
                })
                println(com.ampro.weebot.bot(bot.guildID).prefix)
            }
        }

    }
}
