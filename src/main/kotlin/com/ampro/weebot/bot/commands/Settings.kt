/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.Extensions.RegexShorthand.ic
import com.ampro.weebot.args
import com.ampro.weebot.bot.memory
import com.ampro.weebot.bot.wEmbed
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.EmbedBuilder
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title
import com.serebit.strife.onMessage

object Settings : Command {

    override val name = "Settings"

    override val help by lazy {
        EmbedBuilder.FieldBuilder(
            "Settings", """
        TODO
    """.trimIndent()
        )
    }

    override val matcherBase = "$ic$name".toRegex()

    override val install: BotBuilder.() -> Unit = {
        onMessage {
            memory(message.guild?.id) {
                if (!message.content.calls(prefix)) return@memory
                message.reply(wEmbed(context) {
                    title("${message.guild!!.name} Settings")
                })
            }
        }
    }

    object Prefix : Command {
        override val name = "Prefix"

        override val matcherBase = Regex("$ic$name\\s+.{1,3}")

        override val help by lazy {
            EmbedBuilder.FieldBuilder("Prefix", "TODO")
        }

        override val install: BotBuilder.() -> Unit = {
            onMessage {
                memory(message.guild?.id) {
                    if (!message.content.calls(prefix)) return@memory
                    prefix = message.args[1]
                    message.reply("Prefix set to $prefix")
                }
            }
        }

    }
}
