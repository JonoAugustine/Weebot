/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.args
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.WeebotInfo
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.EmbedBuilder
import com.serebit.strife.entities.reply
import com.serebit.strife.events.MessageCreateEvent
import kotlin.system.exitProcess

object Shutdown : Command {

    override var enabled: Boolean = true
    override val name = "Shutdown" with listOf("kill")
    override val help: EmbedBuilder.FieldBuilder
        get() = TODO("not implemented")
    override val predicate: suspend MessageCreateEvent.(BotBuilder) -> Boolean
        get() = { message.author?.id in WeebotInfo.devIDs }
    override val action: suspend BotBuilder.(MessageCreateEvent, Weebot) -> Unit
        get() = { e, _ ->
            e.context.disconnect()
            exitProcess(0)
        }

}

/** Toggle a [Command.enabled] */
object ToggleEnable : Command {
    override val name = "toggle" with listOf("tog")
    override var enabled = true
    override val help: EmbedBuilder.FieldBuilder
        get() = TODO("not implemented")
    override val predicate: suspend MessageCreateEvent.(BotBuilder) -> Boolean
        get() = { message.args.size != 3 }

    override val action: suspend BotBuilder.(MessageCreateEvent, Weebot) -> Unit
        get() = { e, w ->
            commandAt(e.message.args[1])?.let {
                it.enabled = e.message.args[2].equals("on", true)
                e.message.reply(
                    "${if (it.enabled) "en" else "dis"}abled `${it.name}`"
                )
            } ?: e.message.reply("Not command `${e.message.args[1]}`.")
        }

}
