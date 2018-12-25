/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.util.Emoji
import com.ampro.weebot.util.reactWith
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal val UP = Emoji.ArrowUp
internal val DOWN = Emoji.ArrowDown

/**
 * A Reddit clone that gives customizable up and downvote reactions to each message.
 * At the end of an allotted time span, the top Reddicorders are announced.
 * TODO
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class Reddicord(channel: TextChannel) : IPassive {
    var dead: Boolean = false
    override fun dead() = dead

    /** The Channel being Reddicorded */
    val channelID = channel.idLong

    /** */
    val scoreMap = ConcurrentHashMap<Long, AtomicInteger>()

    /**
     * React to messages with up/downvote emoji and tracks the score of each message
     *
     * @param bot
     * @param event
     */
    override fun accept(bot: Weebot, event: Event) {
        if (!check(event)) return
        when (event) {
            is GuildMessageReceivedEvent       -> {
                event.message.reactWith(UP, DOWN)
            }
            is GuildMessageReactionAddEvent    -> {
                if (event.member.user.isBot) return
                event.channel.getMessageById(event.messageId).queue( {
                    scoreMap.getOrPut(it.author.idLong) {AtomicInteger(1)}.incrementAndGet()
                }, {})
            }
            is GuildMessageReactionRemoveEvent -> {
                if (event.member.user.isBot) return
                event.channel.getMessageById(event.messageId).queue( {
                    scoreMap.getOrPut(it.author.idLong) {AtomicInteger(1)}.decrementAndGet()
                }, {})
            }
        }
    }

    /** @return true if the Event's [TextChannel] is the same as [channelID] */
    private fun check(event: Event) : Boolean = if (event is GenericGuildMessageEvent) {
        event.channel.idLong == channelID
    } else false


}

/**
 *
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdReddicord : WeebotCommand("Reddicord", arrayOf("reddiscord"), CAT_FUN,
        ""/*TODO*/, "Upvote and Downvote messages to gain points.",
        userPerms = arrayOf(MANAGE_CHANNEL, MANAGE_EMOTES, MESSAGE_ADD_REACTION),
        botPerms =  arrayOf(MANAGE_CHANNEL, MANAGE_EMOTES, MESSAGE_ADD_REACTION),
        guildOnly = true, cooldown = 10
) {
    override fun execute(event: CommandEvent) {
        //TODO Reddicord
    }
}
