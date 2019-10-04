/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.WeebotInfo
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.serebit.strife.StrifeInfo
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title

object Help : Command(
    name = "Help",
    rateLimit = 60,
    params = listOfParams("command_name" to true),
    details = "Get a list of all my commands or extra information about a particular command",
    action = {
        if (message.args.size == 1) {
            message.author?.createDmChannel()?.sendWEmbed {
                title("${context.selfUser.username} Commands")
                commands.values
                    .distinct()
                    .filterNot {
                        it.devOnly && message.author?.id !in WeebotInfo.devIDs
                    }
                    .forEach { fields.add(it.help) }
            }
        } else {
            commands[message.args[1]]?.let {
                if (it.devOnly && message.author?.id !in WeebotInfo.devIDs)
                    return@let null
                message.author?.createDmChannel()?.sendWEmbed {
                    title("${it.name} Help")
                    fields.add(it.help)
                }
            } ?: message.reply(
                "I do not have a command called ${message.args[1]}."
            )
        }
    }
)

object About : Command(
    "About",
    details = "Get cool info about me and how I was made!",
    action = {
        message.sendWEmbed {
            title("About ${WeebotInfo.name}")
            description = "I was made by [Jono](${WeebotInfo.jonoGitLab}) " +
                "using [Kotlin](https://kotlinlang.org) and " +
                "[Strife](${StrifeInfo.sourceUri}). I am currently running " +
                "my version ${WeebotInfo.version} build. I am currently in " +
                "the alpha stage of ${WeebotInfo.version}, so old features " +
                "may not have been remade yet. Thank you for your patience!."
        }
    }
)
