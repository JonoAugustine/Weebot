package com.ampro.weebot.main.constants

import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color


const val weebotAvatarUrl = "https://images-ext-2.discordapp" +
        ".net/external/jd498W5p3OMMOdHS2F7HqFm0g0d9Lk0yPjJ0bzsguk0/https/cdn." +
        "discordapp.com/avatars/437851896263213056/c00b298498bc546de4ad5512f53fc7d6.png"


/**
 * @return EmbedBuilder with the standard green, Author set to "Weebot"
 * and footer
 */
val standardEmbedBuilder: EmbedBuilder
    get() = EmbedBuilder().setColor(Color(0x31FF00))
        .setAuthor("Weebot", null, weebotAvatarUrl)
        .setFooter("Run by Weebot", weebotAvatarUrl)

/**
 * Makes a standard format EmbedBuilder with standard color and author,
 * the given title, title URL, and description.
 * @param title The title of the Embed
 * @param titleLink The site to link to in the Title
 * @param description The description that appears under the title
 * @return A Weebot-standard EmbedBuilder
 */
fun makeEmbedBuilder(title: String, titleLink: String, description: String)
        = standardEmbedBuilder.setTitle(title, titleLink)
            .setDescription(description)!!
