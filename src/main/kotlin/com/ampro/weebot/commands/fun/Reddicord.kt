/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.main.SELF
import com.ampro.weebot.main.WAITER
import com.ampro.weebot.util.Emoji
import com.ampro.weebot.util.Emoji.*
import com.ampro.weebot.util.`is`
import com.ampro.weebot.util.reactWith
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.atomic.AtomicInteger


internal val UP = ArrowUp
internal val DOWN = ArrowDown
internal val CHECK = heavy_check_mark

/* ***************
    Reddicord
 *****************/

/**
 * A Reddit clone that gives customizable [Emoji.ArrowUp] [Emoji.ArrowDown]
 * reactions to each message.
 * At the end of an allotted time span, the top Reddicorders are announced.
 * TODO Asks if the user wants this message to be a submission before checking
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
            is GuildMessageReceivedEvent -> {
                event.message.reactWith(CHECK, X_Red)
                WAITER.waitForEvent(GuildMessageReactionAddEvent::class.java, { e ->
                    e.messageId == event.messageId && e.member.user.idLong == event.author.idLong
                            && (e.reactionEmote `is` CHECK || e.reactionEmote `is` X_Red)
                }, { when {
                    it.reactionEmote `is` CHECK -> event.message.clearReactions()
                        .queue { event.message.reactWith(UP, DOWN) }
                    it.reactionEmote `is` X_Red -> event.message.clearReactions()
                        .queueAfter(250, MILLISECONDS)
                }
                },1L, MINUTES) { event.message.clearReactions().queue() }
            }
            is GuildMessageReactionAddEvent -> when {
                event.reactionEmote `is` UP   -> event.channel.getMessageById(event.messageId)
                    .queue { message -> message.isPosted({ incrementConsumer }) }
                event.reactionEmote `is` DOWN -> event.channel.getMessageById(event.messageId)
                    .queue { message -> message.isPosted({ decrementConsumer }) }
            }
            is GuildMessageReactionRemoveEvent -> when {
                event.reactionEmote `is` DOWN   -> event.channel.getMessageById(event.messageId)
                        .queue { message -> message.isPosted({ incrementConsumer }) }
                event.reactionEmote `is` UP -> event.channel.getMessageById(event.messageId)
                    .queue { message -> message.isPosted({ decrementConsumer }) }
            }
        }
    }

    private val incrementConsumer: (Message) -> Unit = {
        scoreMap.getOrPut(it.author.idLong) { AtomicInteger(getScore(it) - 1) }.incrementAndGet()
        it.reactions.firstOrNull { r -> r.reactionEmote `is` DOWN }
            ?.removeReaction(it.author)?.queue()
    }

    private val decrementConsumer: (Message) -> Unit = {
        scoreMap.getOrPut(it.author.idLong) { AtomicInteger(getScore(it) + 1) }.decrementAndGet()
        it.reactions.firstOrNull { r -> r.reactionEmote `is` UP }
            ?.removeReaction(it.author)?.queue()
    }

    /**
     * Calculates the score of the [message]
     *
     * @param message
     * @return the score of the message
     */
    private fun getScore(message: Message) : Int {
        var up = message.reactions.firstOrNull { it.reactionEmote `is` UP}?.count ?: 0
        var down = message.reactions.firstOrNull { it.reactionEmote `is` DOWN}?.count ?: 0
        if (up == 1) up--
        if (down == 1) down--
        return up - down
    }

    /**
     * Checks if the [Message] was "posted" to Reddicord, then performs the given action
     *
     * @param success The action to perform if true
     * @param failure The action to perform if false
     */
    private fun Message.isPosted(success: (MutableList<User>) -> Unit,
                                 failure: (MutableList<User>) -> Unit = {}) {
        reactions.firstOrNull { it.reactionEmote `is` UP || it.reactionEmote `is` DOWN}
            ?.users?.queue {
            if (it.has { it.idLong == SELF.idLong }) success(it)
            else failure(it)
        }
    }

    /** @return true if the Event's [TextChannel] is the same as [channelID] */
    private fun check(event: Event) = when (event) {
        is GenericGuildMessageEvent -> if (event.channel.idLong == channelID) {
            when (event) {
                is GuildMessageReceivedEvent -> !event.author.isBot
                is GuildMessageReactionAddEvent -> !event.member.user.isBot
                is GuildMessageReactionRemoveEvent -> !event.member.user.isBot
                else -> false
            }
        } else false
        else -> false
    }

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
        //TODO Reddicord leaderboard,

        val bot = getWeebotOrNew(event.guild)
        val rCord = bot.getPassive(Reddicord::class)
        val args = event.splitArgs()

        when {
            args.isEmpty() -> {
                event.reply(strdEmbedBuilder.setTitle("Reddicord")
                    .setThumbnail("https://www.redditstatic.com/new-icon.png")
                    .setDescription("Reddiscord is currently ")
                    .appendDescription(if (rCord == null) "off" else "on")
                    .appendDescription(" in ${event.textChannel.asMention}.")
                    .appendDescription(" Use ``help reddicord`` for more ")
                    .appendDescription("info on usage.")
                    .build())
            }
            args[0].toLowerCase().matches(Regex("^(on|enable)$"))   -> {
                if (rCord == null) {
                    bot.passives.add(Reddicord(event.textChannel))
                    event.reply(strdEmbedBuilder.setTitle("Reddicord Activated!").apply {
                        descriptionBuilder.append("Each message in ${event.textChannel.asMention}")
                            .append(" will get a $CHECK and $X_Red reaction; if you ")
                            .append("want to post your message to Reddicord then react ")
                            .append("with $CHECK, otherwise click $X_Red or ignore it. ")
                            .append("Once a post is sent, anyone can react with ")
                            .append("${UP.unicode} or ${DOWN.unicode} to cast their ")
                            .append("vote! Each vote will add or detract to the author's ")
                            .append("ReddiScore.\n*Bring your bestest memes and posts and ")
                            .append("let the games begin!*")
                    }.build()) { it.reactWith(UP, DOWN) }
                } else {
                    event.respondThenDelete(
                            "Reddicord is already active in ${event.textChannel.asMention}")
                }
            }
            args[0].toLowerCase().matches(Regex("^(off|disable)$")) -> {
                if (rCord != null) {
                    bot.passives.remove(rCord)
                    event.reply(strdEmbedBuilder.setTitle("Reddicord has been deactivated.")
                        .setDescription("Use ``\\reddicord on`` to turn it back on at any time.")
                        .build())
                } else {
                    event.respondThenDelete(
                            "Reddicord is already off in ${event.textChannel.asMention}")
                }
            }
            else -> {
                event.respondThenDelete(
                        "Sorry, I had trouble understanding ${args[0]}. Try using " +
                                "``on`` or ``off``.", 30)
            }
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Reddicord") //TODO
            .build()
    }
}

/* ***************
    Reddit API
 *****************/

/** [RedditClient] used to make requests
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
*/
