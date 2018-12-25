/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.constants.REDDIT_ID
import com.ampro.weebot.database.constants.REDDIT_SEC
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.util.Emoji
import com.ampro.weebot.util.reactWith
import com.ampro.weebot.util.toEmoji
import com.jagrosh.jdautilities.command.CommandEvent
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import net.dean.jraw.ratelimit.FixedIntervalRefillStrategy
import net.dean.jraw.ratelimit.LeakyBucketRateLimiter
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.atomic.AtomicInteger


internal val UP = Emoji.ArrowUp
internal val DOWN = Emoji.ArrowDown

/* ***************
    Reddicord
 *****************/

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
                if (event.reactionEmote.toEmoji() == UP) {
                    event.channel.getMessageById(event.messageId).queue(incrementConsumer, {})
                } else if ()
            }
            is GuildMessageReactionRemoveEvent -> {
                if (event.member.user.isBot) return
                event.channel.getMessageById(event.messageId).queue( {
                    scoreMap.getOrPut(it.author.idLong) { AtomicInteger(getScore(it)) }
                        .decrementAndGet()
                }, {})
            }
        }
    }

    /**
     * Calculates the score of the [message]
     *
     * @param message
     * @return the score of the message
     */
    private fun getScore(message: Message) : Int {
        var up = message.reactions.firstOrNull { it.reactionEmote.toEmoji() == UP}?.count ?: 0
        var down = message.reactions.firstOrNull { it.reactionEmote.toEmoji() == DOWN}?.count ?: 0
        if (up == 1) up--
        if (down == 1) down--
        return up - down
    }

    private val incrementConsumer: (Message) -> Unit = {
        scoreMap.getOrPut(it.author.idLong) { AtomicInteger(1) }.incrementAndGet()
    }

    private val decrementConsumer: (Message) -> Unit = {
        scoreMap.getOrPut(it.author.idLong) { AtomicInteger(1) }.decrementAndGet()
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

/* ***************
    Reddit API
 *****************/

/** [RedditClient] used to make requests */
internal val redditClient = OAuthHelper.automatic(OkHttpNetworkAdapter(
        UserAgent("bot", "com.ampro.weebot", "v2.0", "Ventus_Aurelius")),
        Credentials.webapp(REDDIT_ID, REDDIT_SEC, "https://discord.gg/VdbNyxr")
).apply { rateLimiter = LeakyBucketRateLimiter(5, FixedIntervalRefillStrategy(60, MINUTES)) }

/**
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class RedditFeed: IPassive {
    var dead: Boolean = false
    override fun dead() = dead

    val feeds = listOf<Any>() //TODO

    override fun accept(bot: Weebot, event: Event) {
        TODO("not implemented")
    }

}

/**
 *
 * @since 1.0
 * @auhor Jonathan Augustine
 */
class CmdRedditFeed: WeebotCommand("RedditFeed", arrayOf(), CAT_FUN,
        ""/*TODO*/, "Get the best posts from your favorite subreddits.",
        guildOnly = true
) {
    override fun execute(event: CommandEvent) {
        TODO("not implemented")
    }

    init {
        //TODO HelpBiconsumer
    }

}
