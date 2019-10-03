/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.StrifeExtensions.args
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.WeebotInfo
import com.ampro.weebot.bot.memory
import com.ampro.weebot.util.and
import com.serebit.strife.BotBuilder
import com.serebit.strife.events.MessageCreateEvent
import com.serebit.strife.onMessageCreate
import org.joda.time.DateTime
import kotlin.time.seconds

typealias Prefix = String

/**
 * TODO
 *
 * @property name
 * @property aliases
 * @property children
 * @property enabled
 * @property guildOnly
 * @property rateLimit Number of seconds to wait after use
 * @property predicate
 * @property action
 */
abstract class Command(
    val name: String,
    val aliases: List<String> = emptyList(),
    val children: List<Command> = emptyList(),
    var enabled: Boolean = true,
    val rateLimit: Int = 0,
    val guildOnly: Boolean,
    val predicate: suspend MessageCreateEvent.(Weebot) -> Boolean = { true },
    val action: suspend MessageCreateEvent.(Weebot) -> Unit
) {

    protected val rateLimitMap = mutableMapOf<Long, DateTime>()

    open fun preCheck(event: MessageCreateEvent): Boolean {
        val m = event.message
        val a = m.author ?: return false
        // 1 steps
        return when {
            a.isBot -> false
            a.id in WeebotInfo.devIDs -> true
            guildOnly && m.guild == null -> false
            rateLimitMap[a.id]?.isAfterNow == true -> false
            else -> true
        }
    }

    /**
     * Runs the command or child command
     *
     * @param event
     * @param bot
     * @param arg The invoking arg
     */
    open suspend fun run(event: MessageCreateEvent, bot: Weebot, arg: Int = 0) {
        // check child commands if there could be additional commands
        if (event.message.args.size > arg + 1) {
            children
                .firstOrNull { "" and event.message.args[1] invokes it }
                ?.apply { return run(event, bot) }
        }

        // Run prechecks
        preCheck(event)
        // Run predicate
        if (!predicate(event, bot)) return
        // Run command
        action(event, bot)
        // Run post
        postRun(event)
    }

    open fun postRun(event: MessageCreateEvent) {
        if (rateLimit > 0) {
            rateLimitMap[event.message.author!!.id] =
                DateTime.now().plusSeconds(rateLimit)
        }
        TODO("Bot Statistics")
    }

}

private val _commands = mutableMapOf<String, Command>()
val commands get() = _commands.toMap()

/**
 * Prefix + Arg invokes command?
 *
 * @param command
 * @return
 */
private infix fun Pair<Prefix, String>.invokes(command: Command): Boolean {
    val names = buildString {
        append('(').append(command.name)
        append(command.aliases.joinToString("|"))
        append(')')
    }
    return "$first$second".matches(Regex("(?i)^${first}${names}\\s?.?"))
}

fun BotBuilder.commands() {
    onMessageCreate {
        memory(message.guild?.id ?: -1) {
            _commands.values
                .filter { it.enabled }
                .firstOrNull { prefix and message.content invokes it }
                ?.run(this@onMessageCreate, this)
        }
    }
}
