/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.WeebotInfo
import com.ampro.weebot.bot.memory
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.logger
import com.ampro.weebot.util.and
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.EmbedBuilder
import com.serebit.strife.events.MessageCreateEvent
import com.serebit.strife.onMessageCreate
import org.joda.time.DateTime

/** Alias for command prefixes. */
typealias Prefix = String

/**
 * TODO
 *
 * @property name
 * @property optional
 */
data class Param(
    val name: String,
    val optional: Boolean = false
) {
    override fun toString(): String = buildString {
        if (optional) append('[') else append('<')
        append(name)
        if (optional) append(']') else append('>')
    }
}

/**
 * TODO
 *
 * @param params
 * @return
 */
fun listOfParams(vararg params: Any): List<Param> = params.map {
    when (it) {
        is Pair<*, *> -> Param(it.first as String, it.second as Boolean)
        else -> Param(it as String)
    }
}

/**
 * TODO
 *
 * @property name
 * @property params
 */
data class Invokation(
    private val name: String,
    private val params: List<Param>
) {
    override fun toString(): String = buildString {
        append(name).append(" ")
        append(params.joinToString(" "))
    }
}

/**
 * TODO
 *
 * @param list
 */
infix fun String.with(list: List<Param>) = Invokation(this, list)

/**
 * TODO
 *
 * @property name
 * @property aliases
 * @property children
 * @property enabled
 * @property guildOnly
 * @property rateLimit Number of seconds to wait after use.
 * @property details Additional information on the command.
 * @property params
 * @property predicate
 * @property action
 */
abstract class Command(
    val name: String,
    val aliases: List<String> = emptyList(),
    val children: List<Command> = emptyList(),
    var enabled: Boolean = true,
    val rateLimit: Int = 0,
    val guildOnly: Boolean = false,
    val devOnly: Boolean = false,
    val details: String,
    val params: List<Param> = emptyList(),
    val predicate: suspend MessageCreateEvent.(Weebot) -> Boolean = { true },
    val action: suspend MessageCreateEvent.(Weebot) -> Unit
) {

    protected val rateLimitMap = mutableMapOf<Long, DateTime>()

    open suspend fun preCheck(
        event: MessageCreateEvent,
        bot: Weebot,
        arg: Int = 0
    ): Boolean {
        val m = event.message
        val a = m.author ?: return false
        return when {
            m.args.size - arg < params.filterNot { it.optional }.size -> false
            a.isBot -> false
            a.id in WeebotInfo.devIDs -> true
            devOnly && a.id !in WeebotInfo.devIDs -> false
            guildOnly && m.guild == null -> false
            rateLimitMap[a.id]?.isAfterNow == true -> false
            rateLimitMap[a.id]?.isBeforeNow == true -> {
                rateLimitMap.remove(a.id)
                true
            }
            else -> m.guild?.let {
                bot.cmdSettings[name]
                    ?.check(m.guild!!.getMember(a.id))
                    ?: true
            } ?: true
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
                ?.apply { return run(event, bot, arg + 1) }
        }

        logger.trace("Command $name invoked")
        // Run prechecks
        when {
            !preCheck(event, bot, arg) -> return
            // Run predicate
            !predicate(event, bot) -> return
            // Run command
            // Run post
            else -> {
                action(event, bot)
                // Run post
                postRun(event, arg)
            }
        }
    }

    open fun postRun(event: MessageCreateEvent, arg: Int = 0) {
        if (rateLimit > 0) {
            rateLimitMap[event.message.author!!.id] =
                DateTime.now().plusSeconds(rateLimit)
        }
        //TODO("Bot Statistics")
    }

    open val invokation: Invokation = name with params

    open val help: EmbedBuilder.FieldBuilder = EmbedBuilder.FieldBuilder(
        name, buildString {
            append('`').append(invokation).append("`\n")
            append(details).append("\nAliases: ")
            append(aliases.joinToString())
        }
    )

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
        if (command.aliases.isNotEmpty())
            append('|').append(command.aliases.joinToString("|"))
        append(')')
    }
    return "$first$second".matches(Regex("(?i)^${first}${names}(\\s+.*)?"))
}

fun BotBuilder.commands() {
    onMessageCreate {
        memory(message.guild?.id ?: -1) {
            _commands.values
                .distinct()
                .filter { it.enabled }
                .firstOrNull {
                    prefix and message.content.removePrefix(prefix) invokes it
                }
                ?.run(this@onMessageCreate, this)
        }
    }
}

fun <C : Command> cmd(command: C) {
    (command.aliases + command.name)
        .map { it.toLowerCase() }
        .forEach {
            require(!_commands.containsKey(it)) {
                "Command with name $it already registered."
            }
            _commands[it] = command
        }
    logger.trace("Registered command with name ${command.name}")
}
