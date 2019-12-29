/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.WeebotInfo
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.serebit.strife.StrifeInfo
import com.serebit.strife.entities.inlineField
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.thumbnail
import com.serebit.strife.entities.title
import com.soywiz.klock.DateFormat.Companion.FORMAT_DATE

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
                    .filter { it.enabled }
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
        message.delete()
    }
)

object About : Command(
    "About",
    children = listOf(Me),
    details = "Get cool info about me or you!",
    params = listOfParams("me" to true),
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
) {

    object Me : Command(
        "Me",
        details = "Get information about yourself.",
        action = {
            val user = message.author!!
            val member = message.guild?.getMember(user.id)
            val name = member?.nickname ?: user.username
            val created = user.createdAt
            val joined = member?.joinedAt
            val roles = member?.roles
            val isOwner = member?.guild?.getOwner()?.user?.id == user.id
            val presence = member?.presence
            message.sendWEmbed {
                title(name)
                thumbnail(user.username)
                description = buildString {
                    append("username: ").append(user.username).append('\n')
                    member?.nickname?.let {
                        append("nickname: ").append(it).append('\n')
                    }
                    append("ID: ").append(user.id)
                }
                inlineField("Joined Discord") { created.format(FORMAT_DATE) }
                joined?.let { tz ->
                    inlineField("Joined ${member.guild.name}") {
                        tz.format(FORMAT_DATE)
                    }
                    inlineField("Owner of ${member.guild.name}?") {
                        if (isOwner) "Yes" else "No"
                    }
                    presence?.let {
                        inlineField("Status") {
                            buildString {
                                it.clientStatus.run {
                                    append("Desktop: ").append(desktop)
                                    append('\n')
                                    append("Mobile: ").append(mobile)
                                    append('\n')
                                    append("Web: ").append(web)
                                    append('\n')
                                }
                                it.game?.run {
                                    append(type.name)
                                    append(" ").append(name)
                                    timespan?.start?.run {
                                        append(" since ")
                                        append(format(FORMAT_DATE))
                                        append('\n')
                                    }
                                    url?.run(this@buildString::append)
                                }
                            }
                        }
                    }
                    if (roles?.isNotEmpty() == true) {
                        inlineField("Roles") { roles.joinToString { it.name } }
                    }
                    color = member.highestRole?.color ?: color
                }
            }
        }
    )
}

object Ping : Command(
    "ping",
    listOf("pong", "latency"),
    details = "Check my response times",
    rateLimit = 30,
    action = {
        TODO()
    }
)
