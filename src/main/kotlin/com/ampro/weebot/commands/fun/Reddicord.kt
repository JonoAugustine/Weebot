/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.CMD_CLIENT
import com.ampro.weebot.SELF
import com.ampro.weebot.WAITER
import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.database.track
import com.ampro.weebot.extensions.MentionType.CHANNEL
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.`is`
import com.ampro.weebot.extensions.asMention
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.delete
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.extensions.parseMessageLink
import com.ampro.weebot.extensions.respondThenDelete
import com.ampro.weebot.extensions.respondThenDeleteBoth
import com.ampro.weebot.extensions.splitBy
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.extensions.strdPaginator
import com.ampro.weebot.util.Emoji
import com.ampro.weebot.util.Emoji.ArrowDown
import com.ampro.weebot.util.Emoji.ArrowUp
import com.ampro.weebot.util.Emoji.X_Red
import com.ampro.weebot.util.Emoji.heavy_check_mark
import com.ampro.weebot.util.NOW
import com.ampro.weebot.util.`is`
import com.ampro.weebot.util.reactWith
import com.ampro.weebot.util.removeUserReaction
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_CHANNEL
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.Permission.MANAGE_CHANNEL
import net.dv8tion.jda.core.Permission.MANAGE_EMOTES
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.time.temporal.ChronoUnit.HOURS
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

private val UP = ArrowUp
private val DOWN = ArrowDown
private val CHECK = heavy_check_mark
private const val REDDIT_ICON = "https://www.redditstatic.com/new-icon.png"

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
    private var dead: Boolean = false
    override fun dead() = dead

    /** Enabled Channels (empty = all channels) */
    val channelIDs = MutableList(channels.size) { channels[it].idLong }

    /** [User.getIdLong] -> [AtomicInteger] */
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
    private fun getScore(message: Message): Int {
        val up = message.reactions.firstOrNull { it.reactionEmote `is` UP }?.count ?: 0
        val down = message.reactions.firstOrNull { it.reactionEmote `is` DOWN }?.count
            ?: 0
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
            if (it.any { it.idLong == SELF.idLong }) success(it)
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
class CmdReddicord : WeebotCommand(
    "reddicord", "REDDIT", "Redicord", arrayOf("reddiscord", "redditcord", "rcord"),
    CAT_FUN, "Upvote and Downvote messages to gain points.",
    userPerms = arrayOf(MANAGE_CHANNEL, MANAGE_EMOTES, MESSAGE_ADD_REACTION),
    botPerms = arrayOf(MANAGE_CHANNEL, MANAGE_EMOTES, MESSAGE_ADD_REACTION),
    guildOnly = true, children = arrayOf(SubCmdReset(),
        SubCmdLeaderBoard("leaderboard", arrayOf("lb", "scores", "reddiscore",
            "reddiscores", "score")), SubCmdManualSubmit())
) {

    private class SubCmdReset : WeebotCommand("reset", "REDDITRESET", null,
        arrayOf("clear", "clearscores"), CAT_FUN, "Set everyone's score to 0.",
        guildOnly = true, cooldown = 360, userPerms = arrayOf(ADMINISTRATOR)) {
        override fun execute(event: WeebotCommandEvent) {
            val bot = event.guild.bot
            bot.getPassive<Reddicord>()?.also { rCord ->
                if (rCord.scoreMap.isNotEmpty()) {
                    rCord.scoreMap.replaceAll { _, _ -> AtomicInteger(0) }
                }
                event.reply("Scores reset to ``0``")
            } ?: event.respondThenDeleteBoth(makeEmbedBuilder("Reddicord has not been " +
                "activated", description = "To activate Reddicord, use ``reddicord " +
                "on`` or try ``help reddicord`` for more info.").build(), 60)
        }
    }

    private class SubCmdManualSubmit : WeebotCommand("submit", "REDDITMANUAL", null,
        arrayOf("-s", "-sub", "sub", "s", "p", "post", "-p"), CAT_FUN,
        "Manually submit a \"post\"", guildOnly = true, cooldown = 360) {
        override fun execute(event: WeebotCommandEvent) {
            val bot = event.guild.bot
            if (event.argList.isEmpty()) return
            bot.getPassive<Reddicord>()?.also { r ->
                fun fail() = event.respondThenDeleteBoth("Invalid link")
                if (r.channelIDs.isNotEmpty()
                    && !r.channelIDs.contains(event.channel.idLong))
                    return event.respondThenDeleteBoth(
                        "Reddicord is not active in ${event.textChannel.asMention}.")
                // rcord post <link>
                event.argList[0].parseMessageLink()?.also { (g, c, m) ->
                    if (!(g == event.guild.idLong && c == event.channel.idLong))
                        return fail()
                    event.textChannel.getMessageById(m).queue {
                        if (!it.author.`is`(event.author))
                            return@queue event.respondThenDeleteBoth(
                                "You are not the author of this message.")
                        if (HOURS.between(it.creationTime, event.creationTime) > 1
                            && HOURS.between(CMD_CLIENT.initTime, NOW()) > 1)
                            return@queue event.respondThenDeleteBoth(
                                "Message is too old. Try reposting it.")
                        it.reactWith(UP, DOWN)
                        event.respondThenDeleteBoth("Posted!", 60)
                        track(this, bot, event.author, event.creationTime)
                    }
                } ?: return fail()
            } ?: return event.respondThenDeleteBoth("""
                There is no Reddicord active in this server.
                Use ``reddicord on`` to start""".trimIndent(), 15)
        }
    }

    override fun execute(event: WeebotCommandEvent) {
        val bot = event.guild.bot
        val rCord = bot.getPassive<Reddicord>()
        val mentionedChannels = event.message.mentionedChannels
        track(this, bot, event.author, event.creationTime)

        when {
            event.argList.isEmpty() -> {
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
            event.argList[0].toLowerCase().matches(Regex("^(on|enable)$")) -> {
                when {
                    rCord == null -> {
                        bot.passives.add(Reddicord(mentionedChannels))
                        event.reply(
                            strdEmbedBuilder.setTitle("Reddicord Activated!").apply {
                                descriptionBuilder.append(
                                    "Each message in ${event.textChannel.asMention}")
                                    .append(
                                        " will get a $CHECK and $X_Red reaction; if you ")
                                    .append(
                                        "want to post your message to Reddicord then react ")
                                    .append(
                                        "with $CHECK, otherwise click $X_Red or ignore it. ")
                                    .append("Once a post is sent, anyone can react with ")
                                    .append(
                                        "${UP.unicode} or ${DOWN.unicode} to cast their ")
                                    .append(
                                        "vote! Each vote will add or detract to the author's ")
                                    .append(
                                        "ReddiScore.\n*Bring your bestest memes and posts and ")
                                    .append("let the games begin!*")
                                setThumbnail(REDDIT_ICON)
                            }.build()) { it.reactWith(UP, DOWN) }
                    }
                    mentionedChannels.isNotEmpty() -> {
                        val validToInvalid = mentionedChannels.splitBy {
                            rCord.channelIDs.contains(it.idLong)
                        }
                        if (validToInvalid.first.isNotEmpty()) {
                            rCord.channelIDs.addAll(
                                validToInvalid.first.map { it.idLong })
                            event.reply("Added channels to Reddicord: "
                                + validToInvalid.first.map { it.asMention })
                        }
                        if (validToInvalid.second.isNotEmpty()) {
                            event.respondThenDelete("These channles are already " +
                                "enabled: " + validToInvalid.second.map { it.asMention })
                        }
                    }
                    else -> event.respondThenDeleteBoth(
                        "Reddicord is already active in ${event.textChannel.asMention}")
                }
            }
            event.argList[0].toLowerCase().matches(Regex("^(off|disable)$")) -> {
                when {
                    rCord == null -> event.respondThenDeleteBoth(
                        "Reddicord is already off in ${event.textChannel.asMention}")
                    mentionedChannels.isNotEmpty() -> {
                        rCord.channelIDs.removeAll { id ->
                            mentionedChannels.any { it.idLong == id }
                        }
                        event.reply(strdEmbedBuilder.setTitle(
                            "Reddicord has been deactivated in ${mentionedChannels.joinToString(
                                ", ") { it.name }}")
                            .setDescription(
                                "Use ``reddicord on <channels>`` to turn it back on at any time.").build())
                    }
                    else -> {
                        bot.passives.remove(rCord)
                        event.reply(strdEmbedBuilder.setTitle(
                            "Reddicord has been deactivated.").setDescription(
                            "Use ``reddicord on`` to turn it back on at any time.").build())
                    }
                }
            }
            else -> event.respondThenDeleteBoth("Sorry, I had trouble understanding " +
                "${event.argList[0]}. Try using ``on`` or ``off``.", 30)
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Reddicord", """
            Run your own Reddit clone in Discord's text channels! Memebers can submit,
            upvote, and downvote "posts" in the selected TextChannels.
        """.trimIndent())
            .setAliases(aliases)
            .addField("Enable/Disable", """``on/off [#textchannel...]``
                en/disables Reddicord entirely or in the mentioned TextChannels.
            """.trimIndent(), true)
            .addField("Manually Submit", """``post <messageLink>``
                There is a 1 hour window to manually post something.""".trimIndent(),
                true)
            .addField("Score Board", "``scores [@:mention...]``", true)
            .addField("Reset Scores", "``reset``", true)
            .build()
    }
}

/**
 * A command to view the guild [Reddicord] leaderboard.
 * This is modifiable so that it can be a top-level command `Reddiscore`
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class SubCmdLeaderBoard(name: String, alias: Array<String>) : WeebotCommand(
    name, "LEADERBORD", null, alias, CAT_FUN, "See the Reddicord leaderboard.",
    guildOnly = true, cooldown = 120, hidden = true, cooldownScope = USER_CHANNEL
) {

    override fun execute(event: WeebotCommandEvent) {
        val bot = event.guild.bot
        val rCord = bot.getPassive<Reddicord>()
        val mentions = event.message.mentionedUsers
        track(this, bot, event.author, event.creationTime)
        event.delete()

        fun getScore(userID: Long) = rCord?.scoreMap!![userID]?.get()

        when {
            rCord == null -> {
                event.respondThenDeleteBoth(
                    makeEmbedBuilder("Reddicord has not been activated",
                        description = "To activate Reddicord, use ``reddicord on`` or try "
                            + "``help reddicord`` for more info.").build(), 60)
            }
            rCord.scoreMap.isEmpty() -> {
                event.respondThenDeleteBoth(makeEmbedBuilder(
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
                val mapped: List<Pair<AtomicInteger, String>> = (if (mentions.isEmpty()) rCord.scoreMap
                else rCord.scoreMap.filterKeys { k ->
                    mentions.any { it.idLong == k }
                }).map {
                    val name = event.guild.getMemberById(it.key)
                        ?.effectiveName ?: "Uknown User"
                    it.value to "$name: ${getScore(it.key)}"
                }.sortedByDescending { it.first.get() }

                strdPaginator.setText("Reddicord Leaderboard").setItemsPerPage(10)
                    .useNumberedItems(true).apply {
                        mapped.forEach { addItems(it.second) }
                    }.build().display(event.textChannel)
            }
        }
    }

}

/* ***************
    TODO Reddit API
 *****************/
