/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.database.getWeebotOrNew
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.Permission
import java.util.function.BiConsumer

/** @return the string arguments of the message split into a [List]. Does NOT have the
 * command call in it */
fun CommandEvent.splitArgs(): List<String> = this.args.split("\\s+")

/** @return The string used to invoke this command (i.e. the first string of the message */
fun CommandEvent.getInvocation(): String = this.message.contentStripped
    .removePrefix("w!").removePrefix("\\").split(" ")[0]

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete
 */
fun CommandEvent.deleteWithResponse(reason: String, delay: Int = 10) {
    this.reply("*$reason*") {
        GlobalScope.launch (Dispatchers.Default) {
            delay(delay * 1000L)
            event.message.delete().reason(reason).queue()
            it.delete().reason(reason).queue()
        }
    }
}

/**
 * A wrapper class for [Command] holding functions used by Weebots
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
abstract class WeebotCommand(name: String, aliases: Array<String>, category: Category,
                             arguments: String, help: String,
                             helpBiConsumer: BiConsumer<CommandEvent, Command>? = null,
                             guildOnly: Boolean = false, ownerOnly: Boolean = false,
                             hidden: Boolean = false, useTopicTags: Boolean = true,
                             children: Array<WeebotCommand>? = emptyArray(),
                             requiredRole: String? = null, cooldown: Int = 0,
                             cooldownScope: CooldownScope = USER,
                             userPerms: Array<Permission> = emptyArray(),
                             botPerms: Array<Permission> = emptyArray()
) : Command() {

    init {
        super.name      = name
        super.aliases   = aliases
        super.help      = help
        super.helpBiConsumer    = helpBiConsumer
        super.category  = category
        super.arguments = arguments
        super.guildOnly = guildOnly
        super.requiredRole  = requiredRole
        super.ownerCommand  = ownerCommand
        super.cooldown  = cooldown
        super.userPermissions   = userPerms
        super.botPermissions    = botPerms
        super.children  = children
    }

    /**
     * Send Tracking data to Dao.
     *
     * @param weebot
     * @param event
     */
    fun track(weebot: Weebot, event: CommandEvent) {
        val bot: Weebot = getWeebotOrNew(event.guild?.ownerIdLong ?: -1L)
        if (bot.settings.trackingEnabled) {
            //TODO()
        }
    }

}
