/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.extensions.MentionType.CHANNEL
import com.ampro.weebot.main.SELF
import com.ampro.weebot.main.WAITER
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger


internal val UP = ArrowUp
internal val DOWN = ArrowDown
internal val CHECK = heavy_check_mark
internal val REDDIT_ICON = "https://www.redditstatic.com/new-icon.png"

/* ***************
    Reddicord
 *****************/

/**
 * A Reddit clone that gives customizable [Emoji.ArrowUp] [Emoji.ArrowDown]
 * reactions to each message.
 * At the end of an allotted time span, the top Reddicorders are announced.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class Reddicord(channels: MutableList<TextChannel> = mutableListOf()) : IPassive {
    var dead: Boolean = false
    override fun dead() = dead

    /** Enabled Channels (empty = all channels) */
    val channelIDs = MutableList(channels.size) {channels[it].idLong}

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
                    e.messageId == event.messageId
                            && e.member.user.idLong == event.author.idLong
                            && (e.reactionEmote `is` CHECK || e.reactionEmote `is` X_Red)
                }, {
                    when {
                        it.reactionEmote `is` CHECK -> event.message.clearReactions().queueAfter(
                            250, MILLISECONDS) { event.message.reactWith(UP, DOWN) }
                        it.reactionEmote `is` X_Red -> event.message.clearReactions().queueAfter(
                            250, MILLISECONDS)
                    }
                }, 30L, SECONDS) { event.message.clearReactions().queue() }
            }
            is GuildMessageReactionAddEvent -> when {
                event.reactionEmote `is` UP -> event.channel.getMessageById(
                    event.messageId).queue { message ->
                    message.ifPosted({
                        if (message.author.idLong != event.member.user.idLong)
                            incrementConsumer(message, event.user)
                        else message.removeUserReaction(message.author, UP)
                    })
                }
                event.reactionEmote `is` DOWN -> event.channel.getMessageById(
                    event.messageId).queue { message ->
                    message.ifPosted({
                        if (message.author.idLong != event.member.user.idLong)
                            decrementConsumer(message, event.user)
                        else message.removeUserReaction(message.author, DOWN)
                    })
                }
            }
            is GuildMessageReactionRemoveEvent -> when {
                event.reactionEmote `is` DOWN -> event.channel.getMessageById(
                    event.messageId).queue { message ->
                    message.ifPosted({
                        if (message.author.idLong != event.member.user.idLong)
                            incrementConsumer(message, event.user)
                        else message.removeUserReaction(message.author, DOWN)
                    })
                }
                event.reactionEmote `is` UP -> event.channel.getMessageById(
                    event.messageId).queue { message ->
                    message.ifPosted({
                        if (message.author.idLong != event.member.user.idLong)
                            decrementConsumer(message, event.user)
                        else message.removeUserReaction(message.author, UP)
                    })
                }
            }
        }
    }

    private fun incrementConsumer(message: Message, user: User) {
        scoreMap.getOrPut(message.author.idLong) { AtomicInteger(getScore(message)) }
            .incrementAndGet()
        message.removeUserReaction(user, DOWN)
    }

    private fun decrementConsumer(message: Message, user: User) {
        scoreMap.getOrPut(message.author.idLong) { AtomicInteger(getScore(message)) }
            .decrementAndGet()
        message.removeUserReaction(user, UP)
    }

    /**
     * Calculates the score of the [message]
     *
     * @param message
     * @return the score of the message
     */
    private fun getScore(message: Message) : Int {
        val up = message.reactions.firstOrNull { it.reactionEmote `is` UP}?.count ?: 0
        val down = message.reactions.firstOrNull { it.reactionEmote `is` DOWN}?.count ?: 0
        //if (up == 1) up--
        //if (down == 1) down--
        return up - down
    }

    /**
     * Checks if the [Message] was "posted" to Reddicord, then performs the given action
     *
     * @param success The action to perform if true
     * @param failure The action to perform if false
     */
    private fun Message.ifPosted(success: (MutableList<User>) -> Unit,
                                 failure: (MutableList<User>) -> Unit = {}) {
        val k = reactions.firstOrNull {
            it.reactionEmote `is` UP || it.reactionEmote `is` DOWN
        }
        k?.users?.queue {
            it.joinToString(", ") { it.name }
            if (it.has { it.idLong == SELF.idLong }) success(it)
            else failure(it)
        }
    }

    /** @return true if the Event's [TextChannel] is the same as [channelID] */
    private fun check(event: Event) = when (event) {
        is GenericGuildMessageEvent -> if (channelIDs.isEmpty()
                || channelIDs.contains(event.channel.idLong)) {
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
class CmdReddicord : WeebotCommand("Reddicord", arrayOf("reddiscord", "redditcord",
    "reddiscore", "reddiscores"), CAT_FUN, "[on/off/scores]",
    "Upvote and Downvote messages to gain points.",
    userPerms = arrayOf(MANAGE_CHANNEL, MANAGE_EMOTES, MESSAGE_ADD_REACTION),
    botPerms =  arrayOf(MANAGE_CHANNEL, MANAGE_EMOTES, MESSAGE_ADD_REACTION),
    guildOnly = true, children = arrayOf(CmdReddicord.CmdLeaderBoard())
) {

    /**
     * A command to view the guild [Reddicord] leaderboard
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class CmdLeaderBoard : WeebotCommand("leaderboard",
        arrayOf("ranks", "lb", "scores", "reddiscore"),
        CAT_FUN, "[@/member @/member2...]", "See the Reddicord leaderboard.",
        guildOnly = true, ownerOnly = false, cooldown = 180,
        cooldownScope = CooldownScope.USER_CHANNEL) {

        override fun execute(event: CommandEvent) {
            val bot = getWeebotOrNew(event.guild)
            val rCord = bot.getPassive<Reddicord>()
            val mentions = event.message.mentionedUsers

            when {
                rCord == null -> {
                    event.respondThenDelete(makeEmbedBuilder("Reddicord has not been activated",
                        description = "To activate Reddicord, use ``reddicord on`` or try "
                                + "``help reddicord`` for more info.").build(), 60)
                }
                rCord.scoreMap.isEmpty() -> {
                    event.respondThenDelete(makeEmbedBuilder(
                        "Reddicord has been activated but no one has any points yet!",
                        description = "To get started, post a meme in a Reddicord chat.")
                        .build(), 60)
                }
                mentions.size == 1 -> event.reply(strdEmbedBuilder.setTitle(
                    "${mentions[0].name}'s ReddiScore: ${
                    rCord.scoreMap[mentions[0].idLong]?.get() ?: 0}")
                    .setThumbnail(mentions[0].avatarUrl)
                    .setDescription("Keep posting in Reddiscore channels to increase")
                    .appendDescription(" your score!").build())
                rCord.scoreMap.size > 0 -> {
                    val scoreMap = if (mentions.isEmpty()) rCord.scoreMap else {
                        rCord.scoreMap.filterKeys { k -> mentions.has { it.idLong == k } }
                    }.toSortedMap()
                    strdPaginator.setText("Reddicord Leaderboard").setItemsPerPage(10)
                        .useNumberedItems(true).apply {
                            scoreMap.forEach { id, score ->
                                val name = event.guild.getMemberById(id)?.effectiveName
                                        ?: "Uknown User"
                                addItems("$name: $score")
                            }
                        }.build().display(event.textChannel)
                }
            }
        }

    }

    override fun execute(event: CommandEvent) {

        if (children[0].isCommandFor(event.getInvocation())) {
            children[0].run(event)
            return
        }

        val bot = getWeebotOrNew(event.guild)
        val rCord = bot.getPassive<Reddicord>()
        val args = event.splitArgs()
        val mentionedChannels = event.message.mentionedChannels

        when {
            args.isEmpty() -> {
                event.reply(strdEmbedBuilder.setTitle("Reddicord")
                    .setThumbnail(REDDIT_ICON)
                    .setDescription("Reddiscord is currently ")
                    .appendDescription(when (rCord) {
                        null -> "off."
                        else -> "on in ${rCord.channelIDs.joinToString(", ") {
                            it.asMention(CHANNEL)
                        }}."
                    })
                    .appendDescription(" Use ``help reddicord`` for more ")
                    .appendDescription("info on usage.")
                    .build())
            }
            args[0].toLowerCase().matches(Regex("^(on|enable)$"))   -> {
                if (rCord == null) {
                    bot.passives.add(Reddicord(mentionedChannels))
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
                        setThumbnail(REDDIT_ICON)
                    }.build()) { it.reactWith(UP, DOWN) }
                } else {
                    event.respondThenDelete(
                            "Reddicord is already active in ${event.textChannel.asMention}")
                }
            }
            args[0].toLowerCase().matches(Regex("^(off|disable)$")) -> {
                if (rCord != null) {
                    if (mentionedChannels.isNotEmpty()) {
                        rCord.channelIDs.removeAll { id ->
                            mentionedChannels.has { it.idLong == id }
                        }
                        event.reply(strdEmbedBuilder.setTitle(
                            "Reddicord has been deactivated in ${mentionedChannels
                                .joinToString(", ") { it.name }}").setDescription(
                            "Use ``reddicord on <channels>`` to turn it back on at any time.")
                            .build())
                    } else {
                        bot.passives.remove(rCord)
                        event.reply(strdEmbedBuilder.setTitle(
                            "Reddicord has been deactivated.").setDescription(
                            "Use ``reddicord on`` to turn it back on at any time.").build())
                    }
                } else {
                    event.respondThenDelete(
                            "Reddicord is already off in ${event.textChannel.asMention}")
                }
            }
            else -> event.respondThenDelete("Sorry, I had trouble understanding " +
                    "${args[0]}. Try using ``on`` or ``off``.", 30)
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
