package com.ampro.weebot.database.constants

import com.ampro.weebot.main.WAITER
import com.jagrosh.jdautilities.menu.Paginator
import com.jagrosh.jdautilities.menu.Paginator.Builder
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.exceptions.PermissionException
import java.awt.Color
import java.time.Instant
import java.util.concurrent.TimeUnit


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
        .setTimeout(3, TimeUnit.MINUTES).setFinalAction { m ->
            try {
                m.clearReactions().queue()
            } catch (ex: PermissionException) {}
        }
