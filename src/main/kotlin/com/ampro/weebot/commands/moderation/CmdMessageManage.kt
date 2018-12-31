/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.commands.*
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.jagrosh.jdautilities.command.Command.CooldownScope.*
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.core.Permission.MANAGE_CHANNEL
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.MessageHistory
import java.util.concurrent.TimeUnit.SECONDS

/**
 * A File contating [WeebotCommand]s regarding [Message] Management (deletion, etc(?))
 */


/**
 * Automatically deletes the marked message after a given time or 30 seconds by default
 *
 * Formatted: \<deleteme> [-t time] [message]
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdSelfDestruct : WeebotCommand("selfdestruct",
    arrayOf("deleteme","cleanthis","deletethis","covertracks","whome?","podh","sdc"),
    CAT_MOD, "[-t delaySeconds] [message]",
    "Deletes the marked message after the given amount of time (30 sec by default)",
    HelpBiConsumerBuilder("SelfDestruct Message")
        .setDescription("Deletes the marked message after the given amount of" +
                " time (30 sec by " + "default)")
        .addField("Arguments", "[-t delaySeconds] [message]")
        .addField("Aliases", "deleteme, cleanthis, deletethis, covertracks, whome?, podh,sdc")
        .build(), true, cooldown = 0, botPerms = arrayOf(MESSAGE_MANAGE)
) {
    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
        val args = event.splitArgs()
        val delay = try {
            if (args[0].matches(Regex("^-[t|T]$"))) {
                args[1].toLong()
            } else { 30L }
        } catch (e: Exception) { 30L }
        event.reactWarning()
        event.message.delete().queueAfter(delay, SECONDS)
    }
}

/**
 * A [WeebotCommand] to delete a number (2-1,000) messages from a given text channel.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdPurge : WeebotCommand("purge", arrayOf("prune", "clean", "clear"), CAT_MOD,
    "<number>", "Delete multiple messages, 2-1,000.", guildOnly = true,
    botPerms = arrayOf(MESSAGE_MANAGE), userPerms = arrayOf(MESSAGE_MANAGE),
    cooldown = 3, cooldownScope = GUILD
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Chat Purge", false).setDescription(
            "Delete 2 to 1,000 messages from the chat.\n``purge <2...1,000>``"
        ).setAliases(this.aliases).build()
    }

    // \purge #
    override fun execute(event: CommandEvent)  {
        var toDelete = try {
            Integer.parseInt(event.splitArgs()[0])
        } catch (e: Exception) { return }
        val td = toDelete

        if (toDelete !in 2..1_000) {
            event.respondThenDelete("You must choose a number between 2 and 1,000.")
            return
        }
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
        val list = mutableListOf<Int>()
        while (toDelete > 0) {
            if (toDelete >= 100) {
                list.add(100)
                toDelete -= 100
            } else {
                list.add(toDelete)
                toDelete = 0
            }
        }

        GlobalScope.launch {
            list.forEach { toDel ->
                MessageHistory(event.textChannel).retrievePast(toDel).queue {
                    event.textChannel.deleteMessages(it).queue()
                }
                delay(1_000)
            }
            event.respondThenDelete("*${event.author.name} cleared $td messages from ${
            event.textChannel.name}*", 5)
        }

    }

}

/**
 * A [WeebotCommand] to lock all messages from being sent in a chat. This can be
 * achieved in 2 ways: Locking out roles (that Weebot has power over) OR
 * deleting any message that is sent.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdChatLock : WeebotCommand("chatlock",
    arrayOf("lockchat", "pausechat", "holdchat", "blockchat"), CAT_UNDER_CONSTRUCTION,
    "<[-s #] [-m #] [-h #]> [from @/Roles @/Members]",
    "Block messages to the TextChannel for a given time.",
    guildOnly = true, botPerms = arrayOf(MESSAGE_MANAGE, MANAGE_CHANNEL),
    userPerms = arrayOf(MANAGE_CHANNEL), cooldown = 20, cooldownScope = USER_CHANNEL
) {
    override fun execute(event: CommandEvent?) {
            //TODO Chatlock
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder().build()
    }
}
