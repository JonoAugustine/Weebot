/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.progammer

import com.ampro.weebot.commands.CAT_PROG
import com.ampro.weebot.commands.WeebotCommand
import com.jagrosh.jdautilities.command.CommandEvent

//TODO
/**
 * A [WeebotCommand] that allows an end user to send a [MessageEmbed].
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdEmbedMaker : WeebotCommand("embedmaker",
        arrayOf("embedbuilder", "embed", "makeembed", "sendembed"), CAT_PROG,
        ""/*TODO*/, "Sends a message embed with the provided content.",
        HelpBiConsumerBuilder("Embed Builder")
            .setDescription("Make a message embed (Like this message) straight from discord!\n\n")
            .appendDesc("")
            .addField("This is a Field Title", "This is a field content")
            .addField("This field is inline", "So it shares space with the next", true)
            .addField("This field is also inline", "It wont take a full line", true)
            .apply { embedBuilder.setFooter("This is the footer", "") }.build()
) {
    override fun execute(event: CommandEvent) {
        //-t [Title here] -d [description here] -f [Title Here] [Content Here] [inline]
        //-ft [footer content]
    }
}
