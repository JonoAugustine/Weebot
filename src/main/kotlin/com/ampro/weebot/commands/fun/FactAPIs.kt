/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.util.ApiLinkResponse
import com.ampro.weebot.util.get
import com.google.gson.annotations.SerializedName
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.MessageEmbed


/**
 *
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdCatFact : WeebotCommand("CatFact", arrayOf("cat"), CAT_FUN, "",
    "Get a random fact about Cats and a cute picture.", cooldown = 10) {

    val FALLBACK_CAT_IMAGE = "https://www.readersdigest.ca/wp-content/uploads/sites/14/2011/01/4-ways-cheer-up-depressed-cat.jpg"
    val RAND_CAT_FACT  = "https://api-to.get-a.life/catfact"

    /**
     * A JSON wrapper class for [https://catfact.ninja] fact
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    inner class KatFact(@SerializedName("fact") val fact: String) {
        val embed: MessageEmbed get() {
            val url = "https://api-to.get-a.life/catimg".get<ApiLinkResponse>()
                .component1()?.link ?: FALLBACK_CAT_IMAGE
            return strdEmbedBuilder.setTitle("Kat Fact")
                .setDescription(fact)
                .setImage(url).build()
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Cat Facts")
            .setDescription("Get a random fact about cats.\n").build()
    }

    private val katFacts: List<KatFact> = mutableListOf()

    override fun execute(event: CommandEvent) {
        GlobalScope.launch {
            val catFact = RAND_CAT_FACT.get<KatFact>().component1()
            if (catFact != null) {
                event.reply(catFact.embed)
            } else {
                event.reply(
                    "*Sorry, Weebot tripped and got frustrated. " + "Please try again later.*")
            }
        }
    }

}
