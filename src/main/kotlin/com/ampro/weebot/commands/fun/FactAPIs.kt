/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.WeebotCommand
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.util.DIR_HOME
import com.ampro.weebot.util.Emoji.PoutingCat
import com.ampro.weebot.util.loadJson
import com.google.gson.annotations.SerializedName
import com.jagrosh.jdautilities.command.CommandEvent
import java.io.File
import kotlin.random.Random


/**
 *
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdCatFact : WeebotCommand("CatFact", arrayOf("cat"), CAT_FUN, "",
    "Get a random fact about Cats.", cooldown = 10) {

    /**
     * A JSON wrapper class for [https://catfact.ninja] fact
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    data class KatFact(@SerializedName("fact") val fact: String = "",
                       @SerializedName("length") val length: Int = 0)

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Cat Facts")
            .setDescription("Get a random fact about cats.\n").build()
    }

    private lateinit var katFacts: List<KatFact>

    fun loadFacts() = loadJson<List<KatFact>>(File(DIR_HOME, "res/catfact.json"))

    override fun execute(event: CommandEvent) {
        when {
            (!this::katFacts.isInitialized || katFacts.isEmpty()) -> {
                katFacts = loadFacts() ?: emptyList()
            }
            katFacts.isEmpty() -> {
                event.reply("*Sorry, we're all out of cat facts.* $PoutingCat")
                return
            }
            else -> {
                val fact = katFacts[Random(1).nextInt(0, katFacts.size)]
                event.reply(
                    strdEmbedBuilder.setTitle("Cat Fact:")
                        .setDescription(fact.fact)
                        .setThumbnail("https://purr.objects-us-west-1.dream.io/i/43878072_9b440a24af_z.jpg")
                        //TODO make the cat image random
                        .build()
                )
            }
        }
    }

}
