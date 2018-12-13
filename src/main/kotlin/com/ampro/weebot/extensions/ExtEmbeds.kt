/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.ampro.weebot.main.WAITER
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.menu.*
import com.jagrosh.jdautilities.menu.Paginator.Builder
import kotlinx.coroutines.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.requests.RestAction
import java.awt.Color
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES


const val EMBED_MAX_TITLE = 256
const val EMBED_MAX_DESCRIPTION = 2048
const val EMBED_MAX_FIELDS = 25
const val EMBED_MAX_FIELD_NAME = 256
const val EMBED_MAX_FIELD_VAL = 1024
const val EMBED_MAX_FOOTER_TEXT = 2048
const val EMBED_MAX_AUTHOR = 256

const val weebotAvatarUrl = "https://images-ext-2.discordapp" +
        ".net/external/jd498W5p3OMMOdHS2F7HqFm0g0d9Lk0yPjJ0bzsguk0/https/cdn." +
        "discordapp.com/avatars/437851896263213056/c00b298498bc546de4ad5512f53fc7d6.png"

val STD_GREEN = Color(0x31FF00)

/**
 * @return EmbedBuilder with the standard green, Author set to "Weebot"
 * and footer
 */
val strdEmbedBuilder: EmbedBuilder
    get() = EmbedBuilder().setColor(STD_GREEN)
        .setAuthor("Weebot", null, weebotAvatarUrl)
        .setFooter("Run by Weebot", weebotAvatarUrl)
        .setTimestamp(Instant.now())

/**
 * Makes a standard format EmbedBuilder with standard color and author,
 * the given title, title URL, and description.
 * @param title The title of the Embed
 * @param titleLink The site to link to in the Title
 * @param description The description that appears under the title
 * @return A Weebot-standard EmbedBuilder
 */
fun makeEmbedBuilder(title: String, titleLink: String, description: String)
        = strdEmbedBuilder.setTitle(title, titleLink)
            .setDescription(description)!!

val strdPaginator: Paginator.Builder
    get() = Builder().setColor(STD_GREEN).setEventWaiter(WAITER)
        .waitOnSinglePage(false).useNumberedItems(false).showPageNumbers(true)
        .wrapPageEnds(true).setTimeout(3, MINUTES)
        .setFinalAction { m ->
            try {
                m.clearReactions().queue()
            } catch (ex: Exception) {
            }
        }

val strdOrderedMenu: OrderedMenu.Builder
    get() = OrderedMenu.Builder().setEventWaiter(WAITER)
        .useCancelButton(true).setTimeout(3, MINUTES)


/**
 * A Paginator based on JAgrosh's [Paginator] that allows users to react with
 * a listed item like the [OrderedMenu].
 *
 * @author Jagrosh, Jonathan Augustine
 * @since 2.0
 */
class SelectablePaginator(users: Set<User> = emptySet(), roles: Set<Role> = emptySet(),
                          timeout: Long = 3L, unit: TimeUnit = MINUTES,
                          val title: String, val description: String,
                          /** The items to be listed with an [Emoji] and consumer */
                          val items: List<Triple<Emoji, String, (Emoji, Message) ->Unit>>,
                          val columns: Int = 1, val itemsPerPage: Int,
                          val waitOnSinglePage: Boolean = false,
                          val bulkSkipNumber: Int = 0,
                          val wrapPageEnds: Boolean = true,
                          val thumbnail: String = "",
                          val color: Color = STD_GREEN,
                          val finalAction: (Message) -> Unit)
    : Menu(WAITER, users, roles, timeout, unit) {

    companion object {
        val BIG_LEFT = Rewind
        val LEFT = ArrowBackward
        val STOP = Stop
        val RIGHT = ArrowForward
        val BIG_RIGHT = FastForward
    }

    private val pages: Int = Math.ceil(items.size.toDouble() / itemsPerPage).toInt()

    private val baseEmbed: EmbedBuilder get() = strdEmbedBuilder.apply {
        if (title.isNotBlank()) setTitle(title)
        if (description.isNotBlank()) setDescription(description)
        if (thumbnail.isNotBlank()) setThumbnail(thumbnail)
        setColor(color)
    }


    /**
     * Begins pagination on page 1 as a new [Message][net.dv8tion.jda.core.entities.Message]
     * in the provided [MessageChannel][net.dv8tion.jda.core.entities.MessageChannel].
     *
     *
     * Starting on another page is available via [ ][Paginator.paginate].
     *
     * @param  channel
     * The MessageChannel to send the new Message to
     */
    override fun display(channel: MessageChannel) = paginate(channel, 1)

    /**
     * Begins pagination on page 1 displaying this Pagination by editing the provided
     * [Message][net.dv8tion.jda.core.entities.Message].
     *
     *
     * Starting on another page is available via
     * [Paginator#paginate(Message, int)][Paginator.paginate].
     *
     * @param  message
     * The Message to display the Menu in
     */
    override fun display(message: Message) = paginate(message, 1)

    /**
     * Begins pagination as a new [Message][net.dv8tion.jda.core.entities.Message]
     * in the provided [MessageChannel][net.dv8tion.jda.core.entities.MessageChannel], starting
     * on whatever page number is provided.
     *
     * @param  channel
     * The MessageChannel to send the new Message to
     * @param  pageNum
     * The page number to begin on
     */
    fun paginate(channel: MessageChannel, pageNum: Int) {
        var page = pageNum
        if (page < 1) page = 1
        else if (page > pages) page = pages
        val msg = renderPage(page)
        initialize(channel.sendMessage(msg), page)
    }

    /**
     * Begins pagination displaying this Pagination by editing the provided
     * [Message][net.dv8tion.jda.core.entities.Message], starting on whatever
     * page number is provided.
     *
     * @param  message
     * The MessageChannel to send the new Message to
     * @param  pageNum
     * The page number to begin on
     */
    fun paginate(message: Message, pageNum: Int) {
        var page = pageNum
        if (page < 1) page = 1
        else if (page > pages) page = pages
        val msg = renderPage(page)
        initialize(message.editMessage(msg), page)
    }

    private fun initialize(action: RestAction<Message>, pageNum: Int) = runBlocking {
        action.queue { m ->
            when {
                pages > 1 -> {
                    if (bulkSkipNumber > 1) m.reactWith(BIG_LEFT)
                    m.reactWith(LEFT, STOP)
                    //items.forEach { m.reactWith(it.first) }
                    val start = (pageNum - 1) * itemsPerPage
                    val end = when {
                        items.size < pageNum * itemsPerPage -> items.size
                        else -> pageNum * itemsPerPage
                    }
                    (start until end).forEach { m.reactWith(items[it].first) }
                    runBlocking {  delay(1_000); pagination(m, pageNum) }
                    if (bulkSkipNumber > 1) m.reactWith(RIGHT)
                    m.reactWith(if (bulkSkipNumber > 1) BIG_RIGHT else RIGHT)
                    launch {
                        delay(500)
                        pagination(m, pageNum)
                    }
                    return@queue
                }
                else -> {
                    val start = (pageNum - 1) * itemsPerPage
                    val end = when {
                        items.size < pageNum * itemsPerPage -> items.size
                        else -> pageNum * itemsPerPage
                    }
                    (start until end).forEach { m.reactWith(items[it].first) }
                    runBlocking {  delay(1_000); pagination(m, pageNum) }
                    launch {
                        delay(500)
                        pagination(m, pageNum)
                    }
                }
            }
        }
    }

    private fun pagination(message: Message, pageNum: Int) {
        waiter.waitForEvent(MessageReactionAddEvent::class.java,
            { event -> checkReaction(event, message.idLong) }, // Check Reaction
            { event -> handleMessageReactionAddAction(event, message, pageNum) },
            timeout, unit, { finalAction(message) })
    }

    // Private method that checks MessageReactionAddEvents
    private fun checkReaction(event: MessageReactionAddEvent, messageId: Long): Boolean {
        if (event.messageIdLong != messageId) return false
        val emoji = event.reactionEmote.toEmoji()
        return when (emoji) {
            // LEFT, STOP, RIGHT, BIG_LEFT, BIG_RIGHT all fall-through to
            // return if the User is valid or not. If none trip, this defaults
            // and returns false.
            LEFT, STOP, RIGHT -> isValidUser(event.user, event.guild)
            BIG_LEFT, BIG_RIGHT -> bulkSkipNumber > 1 && isValidUser(event.user, event.guild)
            else -> if (items.indexOfFirst { it.first == emoji } != 0) {
                isValidUser(event.user,event.guild)
            } else false
        }
    }

    // Private method that handles MessageReactionAddEvents
    private fun handleMessageReactionAddAction(event: MessageReactionAddEvent,
                                               message: Message, pageNum: Int) {
        var newPageNum = pageNum
        val emoji = event.reaction.reactionEmote.toEmoji()
        when (emoji) {
            LEFT -> {
                if (newPageNum == 1 && wrapPageEnds) newPageNum = pages + 1
                if (newPageNum > 1) newPageNum--
            }
            RIGHT -> {
                if (newPageNum == pages && wrapPageEnds) newPageNum = 0
                if (newPageNum < pages) newPageNum++
            }
            BIG_LEFT -> if (newPageNum > 1 || wrapPageEnds) {
                var i = 1
                while ((newPageNum > 1 || wrapPageEnds) && i < bulkSkipNumber) {
                    if (newPageNum == 1 && wrapPageEnds) newPageNum = pages + 1
                    newPageNum--
                    i++
                }
            }
            BIG_RIGHT -> if (newPageNum < pages || wrapPageEnds) {
                var i = 1
                while ((newPageNum < pages || wrapPageEnds) && i < bulkSkipNumber) {
                    if (newPageNum == pages && wrapPageEnds) newPageNum = 0
                    newPageNum++
                    i++
                }
            }
            else -> {
                items.find { emoji == it.first }?.apply { third(first, message) }
                return
            }
        }

        try {
            event.reaction.removeReaction(event.user).queue()
        } catch (ignored: PermissionException) {}

        val n = newPageNum
        message.editMessage(renderPage(newPageNum)).queue { m ->
            val start = (newPageNum - 1) * itemsPerPage
            val end = when {
                items.size < newPageNum * itemsPerPage -> items.size
                else -> newPageNum * itemsPerPage
            }
            (start until end).forEach { m.reactWith(items[it].first) }
            runBlocking {  delay(1_000); pagination(m, n) }
        }
    }

    private fun renderPage(pageNum: Int): Message {
        val mbuilder = MessageBuilder()
        val ebuilder = this.baseEmbed
        val sb = StringBuilder()

        val start = (pageNum - 1) * itemsPerPage
        val end = when {
            items.size < pageNum * itemsPerPage -> items.size
            else -> pageNum * itemsPerPage
        }
        if (columns == 1) {
            (start until end).forEach { i ->
                sb.append("\n${items[i].first} ${items[i].second}")
            }
            ebuilder.setDescription(sb.toString())
        } else {
            val per = Math.ceil((end - start).toDouble() / columns).toInt()
            (0 until columns).forEach { k ->
                var i = start + k * per
                while (i < end && i < start + (k + 1) * per) {
                    sb.append("\n${items[i].first} ${items[i].second}")
                    i++
                }
                ebuilder.addField("", sb.toString(), true)
            }
        }

        ebuilder.setFooter("Page $pageNum/$pages", null)
        mbuilder.setEmbed(ebuilder.build())
        return mbuilder.build()
    }

}
