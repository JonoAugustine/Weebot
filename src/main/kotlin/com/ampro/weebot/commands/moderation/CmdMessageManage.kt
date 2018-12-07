/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.commands.*
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.splitArgs
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
class CmdSelfDestruct : WeebotCommand("SelfDestruct",
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
        val args = event.splitArgs()
        val delay = try {
            if (args[0].matches(Regex("^(-t|-T)$"))) {
                args[1].toLong()
            } else { 30L }
        } catch (e: Exception) { 30L }
        event.message.delete().queueAfter(delay, SECONDS)
    }
}

/**
 * A [WeebotCommand] to delete a number (2-1,000) messages from a given text channel.
 *
 * //TODO Allow more than 100 deletions (must be in 100 message chunks, 2 sec apart)
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdPurge : WeebotCommand("Purge", arrayOf("prune", "clean", "clear"), CAT_MOD,
    "<number>", "Delete multiple messages, 2-99.", guildOnly = true,
    botPerms = arrayOf(MESSAGE_MANAGE), userPerms = arrayOf(MESSAGE_MANAGE),
    cooldown = 3, cooldownScope = CooldownScope.GUILD
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder() //TODO
            .build()
    }

    // \purge #
    override fun execute(event: CommandEvent) {
        GlobalScope.launch {

            val toDelete = try {
                Integer.parseInt(event.splitArgs()[0])
            } catch (e: Exception) {
                return@launch
            }

            val mh = MessageHistory(event.textChannel)

            if (toDelete in 2..100) {
                mh.retrievePast(toDelete + 1).queue { messages ->
                    event.textChannel.deleteMessages(messages).queue()
                }
                event.reply(
                    "*${event.author.name} cleared $toDelete messages from ${event.textChannel.name}*")
                { m -> m.delete().queueAfter(5, SECONDS) }
            } else {
                event.reply("*You must choose a number between 2 and 99.*")
            }
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
    arrayOf("lockchat", "pausechat", "holdchat", "blockchat"), CAT_MOD,
    "[delete] or [limit] @/RoleOrMember",
    "Block messages to the TextChannel for a given time.",
    HelpBiConsumerBuilder().build(),
    true, false, botPerms = arrayOf(MESSAGE_MANAGE),
    userPerms = arrayOf(MANAGE_CHANNEL)
) {
    override fun execute(event: CommandEvent?) {
            //TODO Chatlock
    }
}
