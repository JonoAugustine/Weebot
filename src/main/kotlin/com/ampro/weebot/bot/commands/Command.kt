/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.EmbedBuilder

interface Command {

    val name: String

    val matcherBase: Regex

    val help: EmbedBuilder.FieldBuilder

    val install: BotBuilder.() -> Unit

    fun String.calls(prefix: String): Boolean =
        trim().takeIf { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.matches(matcherBase) == true

}

fun BotBuilder.wCom(command: Command) = command.also {
    it.install(this)
    _liveCommands[command.name.toLowerCase()] = command
}

private val _liveCommands: MutableMap<String, Command> = mutableMapOf()

val liveCommands: Map<String, Command> get() = _liveCommands.toMap()

fun commandOf(name: String) = _liveCommands[name.toLowerCase()]
