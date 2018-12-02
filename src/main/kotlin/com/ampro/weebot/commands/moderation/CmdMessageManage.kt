/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.commands.*
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.MessageHistory

/**
 * A File contating [WeebotCommand]s regarding [Message] Management (deletion, etc(?))
 */

/**
 * A [WeebotCommand] to delete a number (2-1,000) messages from a given text channel.
 *
 * //TODO Allow more than 100 deletions (must be in 100 message chunks)
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdPurge : WeebotCommand("Purge", arrayOf("prune", "clean"), CAT_MOD,
    "<number>", "Delete multiple messages.", guildOnly = true,
    botPerms = arrayOf(MESSAGE_MANAGE), userPerms = arrayOf(MESSAGE_MANAGE),
    cooldown = 10, cooldownScope = CooldownScope.GUILD
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
                { m ->
                    try {
                        Thread.sleep(5 * 1_000)
                    } catch (e: InterruptedException) {}
                    m.delete().queue()
                }
            } else {
                event.reply("*You must choose a number between 2 and 100.*")
            }
        }
    }
}
