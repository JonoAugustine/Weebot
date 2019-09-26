/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.*
import com.ampro.weebot.Extensions.RegexShorthand.ic
import com.ampro.weebot.bot.WeebotInfo
import com.ampro.weebot.bot.commands.Help.calls
import com.ampro.weebot.bot.wEmbed
import com.serebit.strife.BotBuilder
import com.serebit.strife.StrifeInfo
import com.serebit.strife.entities.EmbedBuilder.FieldBuilder
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title
import com.serebit.strife.onMessage

object Help : Command {

    override val name = "Help"

    override val matcherBase: Regex = "$ic$name(\\s+.+)?".toRegex()

    override val help: FieldBuilder = FieldBuilder(name, """
        Get a list of all my commands or extra information about a particular command
        Usage: `help <command_name>`
    """.trimIndent())

    override val install: BotBuilder.() -> Unit = {
        onMessage {
            val bot = message.guild?.bot ?: globalWeebot
            if (!message.content.calls(bot.prefix)) return@onMessage

            if (message.args.size == 1) {
                message.reply(wEmbed(context) {
                    title("${context.selfUser.username} Commands")
                    liveCommands.values.forEach { fields.add(it.help) }
                })
            } else {
                commandOf(message.args[1])?.let {
                    message.reply(wEmbed(context) {
                        title("${it.name} Help")
                        fields.add(it.help)
                    })
                } ?: message.reply("I do not have a command called ${message.args[1]}.")
            }

        }
    }
}

object About : Command {

    override val name = "About"

    override val matcherBase = "${ic}about".toRegex()

    override val help = FieldBuilder(name, """
        Get cool info about me and how I was made!
        Usage: `about`
    """.trimIndent())

    override val install: BotBuilder.() -> Unit = {
        onMessage {
            val bot = message.guild?.bot ?: globalWeebot
            if (!message.content.calls(bot.prefix)) return@onMessage

            message.reply(embed = wEmbed(context) {
                title(name)
                description = "I was made by [Jono](${WeebotInfo.jonoGitLab}) " +
                    "using [Kotlin](https://kotlinlang.org) and " +
                    "[Strife](${StrifeInfo.sourceUri}). I am currently running " +
                    "my version ${WeebotInfo.version} build. I am currently in " +
                    "the alpha stage of ${WeebotInfo.version}, so old features " +
                    "may not have been remade yet! Thank you for your patience."
            })
        }
    }

}
