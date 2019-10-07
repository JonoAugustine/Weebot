/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands


import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.ampro.weebot.statistic
import com.ampro.weebot.util.Regecies
import com.ampro.weebot.util.matchesAny
import com.ampro.weebot.util.unit
import com.serebit.strife.entities.inlineField
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title
import com.serebit.strife.events.MessageCreateEvent
import com.serebit.strife.text.inlineCode
import kotlin.system.exitProcess

abstract class DeveloperCommand(
    name: String,
    aliases: List<String> = emptyList(),
    children: List<Command> = emptyList(),
    enabled: Boolean = true,
    details: String,
    params: List<Param> = emptyList(),
    predicate: suspend MessageCreateEvent.(Weebot) -> Boolean = { true },
    action: suspend MessageCreateEvent.(Weebot) -> Unit
) : Command(
    name,
    aliases,
    children,
    enabled,
    devOnly = true,
    details = details,
    params = params,
    predicate = predicate,
    action = action
)

object Shutdown : DeveloperCommand(
    "Shutdown",
    listOf("kill"),
    details = "Shutdown the bot.",
    action = {
        context.disconnect()
        exitProcess(0)
    }
)

/** Toggle a [Command.enabled] */
object ToggleEnable : DeveloperCommand(
    "toggle",
    listOf("tog"),
    details = "toggle the enable state of a command",
    params = listOfParams("command_name", "on/off"),
    action = {
        commands[message.args[1]]?.let {
            it.enabled = when {
                message.args[2].matchesAny(Regecies.on, Regecies.enable) -> true
                message.args[2].matchesAny(Regecies.off, Regecies.disable) -> {
                    false
                }
                else -> return@let message.reply("Invalid args")
            }
            message.reply(
                "${if (it.enabled) "en" else "dis"}abled `${it.name}`"
            )
        } ?: message.reply("No command found at `${message.args[1]}`.")
    }

)

object Statistics : DeveloperCommand(
    "statistics",
    listOf("stat", "stats"),
    details = "view statistics",
    params = listOfParams("command_name" to true),
    action = a@{
        val list = if (message.args.size > 1)
            commands[message.args[1]]?.name
                ?.let { listOf(statistic(it)) }
                ?: return@a message.reply("No command found").unit
        else commands.toList().distinctBy { it.second }
            .map { statistic(it.second.name) }
        message.sendWEmbed {
            title("Statistics")
            list.forEach { col ->
                inlineField(col.command) {
                    val ivks = col.stats
                        .map { it.invokation.toLowerCase() }
                        .distinct()
                    val count_ivks = ivks
                        .associateWith { s -> ivks.count { it == s } }
                    val size_ivk = ivks.associateWith { s ->
                        col.stats.filter { it.invokation.equals(s, true) }
                            .map { it.guildSize }
                            .average()
                    }
                    if (ivks.isNotEmpty()) buildString {
                        append("Invokation Data\n")
                        count_ivks.toList()
                            .sortedByDescending { it.second }
                            .forEach { (ivk, cnt) ->
                                append(ivk.inlineCode)
                                append(" used ").append(cnt)
                                append(" time with avg guild size of ")
                                append(size_ivk[ivk])
                            }
                    } else "No invokation data."
                }
            }
        }
    }
)
