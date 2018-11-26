package com.ampro.weebot.contants

import com.ampro.weebot.JDA_CLIENT
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color


/**
 * @return EmbedBuilder with the standard green, Author set to "Weebot"
 * and footer
 */
val standardEmbedBuilder: EmbedBuilder
    get() = EmbedBuilder().setColor(Color(0x31FF00))
        .setAuthor("Weebot", null, JDA_CLIENT.selfUser.avatarUrl)
        .setFooter("Run by Weebot", JDA_CLIENT.selfUser.avatarUrl)

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
