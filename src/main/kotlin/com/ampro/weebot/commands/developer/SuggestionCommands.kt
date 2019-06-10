/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.commands.CAT_GEN
import com.ampro.weebot.commands.developer.Suggestion.State
import com.ampro.weebot.commands.developer.Suggestion.State.COMPLETED
import com.ampro.weebot.commands.developer.Suggestion.State.Companion.read
import com.ampro.weebot.commands.developer.Suggestion.State.IGNORED
import com.ampro.weebot.commands.developer.Suggestion.State.UNREVIEWED
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.constants.OFFICIAL_CHATS
import com.ampro.weebot.database.deleteSuggestions
import com.ampro.weebot.database.getSuggestion
import com.ampro.weebot.database.getSuggestions
import com.ampro.weebot.database.save
import com.ampro.weebot.database.track
import com.ampro.weebot.extensions.EMBED_MAX_DESCRIPTION
import com.ampro.weebot.extensions.EMBED_MAX_FIELD_VAL
import com.ampro.weebot.extensions.REG_MENTION_USER
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.containsAny
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.extensions.matchesAny
import com.ampro.weebot.extensions.removeAll
import com.ampro.weebot.extensions.respondThenDeleteBoth
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.extensions.strdPaginator
import com.ampro.weebot.extensions.subList
import com.ampro.weebot.util.DD_MM_YYYY_HH_MM
import com.ampro.weebot.util.IdGenerator
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.entities.TextChannel
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit.MINUTES

val seeRegi = Regex("^(-([sS])+.*)$")
val giveRegi = Regex("^(-([gG])+.*)$")
val voteRegi = Regex("^(-([vV])+.*)$")
val keywordRegi = Regex("^(-([kK])+.*)$")
val reviewRegi = Regex("^(-([rR])+.*)$")
val pageRegi = Regex("^(-([pP])+.*)$")

/**
 * A suggestion. Basically a String wrapper with info about date and location.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class Suggestion(val suggestion: String) {

    enum class State {
        UNREVIEWED, ACCEPTED, COMPLETED, IGNORED;

        companion object {
            /**
             * A replacement for Enum::valueOf that is more lenient in parsing
             *
             * @param string the string to parse
             * @return The parsed [State]
             * @throws IllegalArgumentException if the [string] could not be parsed
             */
            fun read(s: String) = when {
                s.matches(Regex("(?i)^(UNREVIEW(E?D?)?)$")) -> UNREVIEWED
                s.matches(Regex("(?i)^(ACCEPT(E?D?)?)$")) -> ACCEPTED
                s.matches(Regex("(?i)^(COMPLET(E?D?)?)$")) -> COMPLETED
                s.matches(Regex("(?i)^(IGNOR(E?D?)?)$")) -> IGNORED
                else -> throw IllegalArgumentException()
            }
        }
    }

    companion object {
        val idGenerator = IdGenerator(7, "SUG")
    }

    val _id = idGenerator.next()

    /** The date and time the suggestion was submitted */
    val submitTime: OffsetDateTime = OffsetDateTime.now()

    var state: State = UNREVIEWED

    /** A list of user IDs who have upvoted (to prevent double votes) */
    var votes = mutableListOf<Long>()

    val score get() = votes.size

    override fun toString() = suggestion

    fun toStringPub() = "id : $_id:\n*$suggestion*\n**score: $score | | | state: ${
    state.toString().toLowerCase()}**\n\n"

    fun toStringDev() = "id : $_id | ${submitTime.format(
        DD_MM_YYYY_HH_MM)} | **$state** | " +
        "$score\n$suggestion\n\n"
}

/**
 * Get a [Suggestion] from the [DAO] or send a message that no sugg was found
 * and return null
 */
private suspend fun getSuggById(id: String, event: CommandEvent) =
    getSuggestion(id) ?: run {
        event.respondThenDeleteBoth("*No suggestion matched the ID provided.*", 30)
        null
    }

/**
 * A way for anyone in a Guild hosting a Weebot to make suggestions to the
 * developers.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
open class CmdSuggestion : WeebotCommand("suggest", "SUGG", "Suggest",
    arrayOf("suggestion", "sugg"), CAT_GEN,
    "Submit and Vote for anonymous suggestions for the Weebot devs.",
    HelpBiConsumerBuilder("Weebot Suggestions", false).setDescription(
        "Submit an anonymous suggestion to the Weebot developers right from " +
            "Discord!\nYou can use this command to report bugs, send " +
            "suggestions, or vote on suggestions that have been sent by others!" +
            "\n(don't worry, no one will know who sent each suggestion; 100% anon)"
    ).addField("To submit", "-g[ive] <Your Suggestion Here>", true)
        .addField("To see suggesitons (order after \"*-s*\" doesn't matter)",
            "-s [-k <keyword> [key2...]] [-r <reviewState> [reviewState2...]]"
                + " [-p <pagenum>]"
                + "\n Review states: accepted/unreviewed/completed/ignored", true)
        .addField("To vote for a suggestion", "-v[ote] <suggestionID>", true)
        .build(), cooldown = 60,
    children = arrayOf(CmdDevSuggestions(), CmdSeeSuggestions())
) {

    // \sugg [-g[ive]] <suggestion>
    // \sugg -v(ote) <suggID>
    override fun execute(event: WeebotCommandEvent) {
        val args = event.splitArgs()
        val message = event.args.replace(REG_MENTION_USER, "@/ User")

        when {
            //Ignore empty COMMANDS
            args.isEmpty() -> return
            //When submitting
            args[0].matches(giveRegi) || !args[0].matchesAny(voteRegi, seeRegi) -> {
                track(this, event.guild.bot, event.author, event.creationTime)
                when {
                    args.size < 4 -> {
                        event.reply(
                            "*Sorry, your suggestion is a bit short "
                                + "-- can you include more detail? Thank you!*")
                        return
                    }
                    message.length > EMBED_MAX_DESCRIPTION -> {
                        return event.reply(
                            "*Sorry, your suggestion is a too long " +
                                "(max=$EMBED_MAX_FIELD_VAL char) -- can you try " +
                                "and be more concise? Thank you!*")
                    }
                    else -> {
                        runBlocking { Suggestion(message.removeAll(giveRegi)).save() }
                        event.reply(
                            "*Thank you for your suggestion! We're working hard to"
                                + " make Weebot as awesome as possible, and we "
                                + "will try our best to include your suggestion!*")
                        event.reactSuccess()
                        return
                    }
                }
            }
            //When voting
            args[0].matches(voteRegi) -> {
                if (args.size < 2) {
                    event.respondThenDeleteBoth("*No suggestion ID was provided.*", 30)
                    return
                }
                track(this, event.guild.bot, event.author, event.creationTime)
                val sugg = runBlocking { getSuggById(args[1], event) } ?: return
                if (!sugg.votes.contains(event.author.idLong)) {
                    sugg.votes.add(event.author.idLong)
                    event.reactSuccess()
                    event.reply("*You voted for suggestion ${sugg._id}! " +
                        "Bringing the score up to ${sugg.score}!*")
                } else {
                    event.reactError()
                    event.reply("*You have already voted for this suggestion!*")
                }
            }
        } //end when

    }

}

/** How many suggestions per embed page */
const val PAGE_LENGTH = 6

/**
 * Get a list of [Suggestion]s that match the given [criteria]
 *
 * @param criteria The search parameters
 * @return A [List] of [Suggestion]s that match the given [criteria] (never null)
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
suspend fun searchSuggs(criteria: (Suggestion) -> Boolean) =
    getSuggestions().filter { criteria(it) }

/**
 * Send a formatted list of [Suggestion]s in response to a non-dev User using
 * the sugg see command
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
suspend fun sendSuggsPublic(
    page: Int, event: CommandEvent, criteria: (Suggestion) -> Boolean
) {
    val list = searchSuggs(criteria).sortedByDescending {
        when (it.state) {
            COMPLETED -> -1
            IGNORED -> -2
            else -> it.score
        }
    }

    val e = strdEmbedBuilder.setTitle("Weebot Suggestions")
    if (list.isEmpty()) {
        event.reply(
            e.setDescription("*No Suggestions found which match the search criteria*")
                .build()
        )
        return
    }

    strdPaginator.apply { list.forEach { this.addItems(it.toStringPub()) } }
        .setText("Weebot Suggestions").setItemsPerPage(PAGE_LENGTH)
        .build().paginate(event.channel, page)

}

/**
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
suspend fun sendSuggsDev(page: Int, event: CommandEvent,
                         criteria: (Suggestion) -> Boolean) {
    val list = searchSuggs(criteria).sortedByDescending {
        when (it.state) {
            COMPLETED -> -1
            IGNORED -> 2
            else -> it.score
        }
    }
    val e = strdEmbedBuilder.setTitle("Weebot Suggestions")
    if (list.isEmpty()) {
        event.reply(e.setDescription(
            "*No Suggestions found which match the search criteria*").build())
        return
    }

    if (OFFICIAL_CHATS.contains(event.channel.idLong)) {
        strdPaginator.setTimeout(5, MINUTES).setItemsPerPage(PAGE_LENGTH)
            .apply { list.forEach { this.addItems(it.toStringDev()) } }
            .setText("Weebot Suggestions").build().paginate(event.channel, page)
    } else {
        event.author.openPrivateChannel().queue { ch ->
            strdPaginator.setTimeout(5, MINUTES).setItemsPerPage(PAGE_LENGTH)
                .apply { list.forEach { this.addItems(it.toStringDev()) } }
                .setText("Weebot Suggestions").build().paginate(ch, page)
        }
    }
}

/**
 * Child command of [CmdSuggestion] used to reduce the cooldown for seeing suggestions
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdSeeSuggestions : WeebotCommand("-see", "SUGGSEE", null, arrayOf("-s", "see"),
    CAT_GEN, "", cooldown = 5) {

    // \sugg -s(ee) [-k <keyword>] [-r <accepted/unreviewed/completed/ignored>] [pagenum]
    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        val keyWords = mutableListOf<String>()
        val reviewStates = mutableListOf<State>()
        var pagenum = 1

        var loopState = -1 //0 = key, 1 = review, 2 = pagenum
        args.forEach {
            when {
                it.matches(keywordRegi) -> {
                    loopState = 0
                    return@forEach
                }
                it.matches(reviewRegi) -> {
                    loopState = 1
                    return@forEach
                }
                it.matches(pageRegi) -> {
                    loopState = 2
                    return@forEach
                }
            }
            when (loopState) {
                0 -> keyWords.add(it)
                1 -> {
                    if (reviewStates.size < 4) {
                        //Stop checking for states after the max num
                        try {
                            val s = read(it)
                            if (!reviewStates.contains(s)) reviewStates.add(s)
                        } catch (e: Exception) {
                        }
                    }
                }
                2 -> pagenum = try {
                    it.toInt()
                } catch (e: Exception) {
                    1
                }
            }
        }

        GlobalScope.launch {
            sendSuggsPublic(pagenum - 1, event) { suggestion ->
                //possible Failure point
                runBlocking {
                    !awaitAll(async {
                        if (keyWords.isNotEmpty()) {
                            suggestion.suggestion.containsAny(keyWords)
                        } else true
                    }, async {
                        if (reviewStates.isNotEmpty()) {
                            reviewStates.contains(suggestion.state)
                        } else true
                    }).contains(false)
                }
            }
        }
    }
}

/**
 * A way for devs to see and modify sent suggestions.
 *
 * sugg dev <args...>
 *     _ _ see [pageNum]
 *     _ _ rem <suggNum>
 *     _ _ state <newState>
 *     _ _ share <suggNum>
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdDevSuggestions : WeebotCommand(
    "dev", "SUGGDEV", null, emptyArray(), CAT_DEV, "",
    HelpBiConsumerBuilder("Weebot Suggestions", false)
        .setDescription("Commands for devs to control suggestions").build(),
    ownerOnly = true, cooldown = 0, hidden = true
) {

    override fun isAllowed(channel: TextChannel) = super.isAllowed(
        channel) || OFFICIAL_CHATS.contains(channel.idLong)

    /**
     * sugg dev <args...>
     *     _ _ see [pageNum]
     *     _ _ rem <suggNum>
     *     _ _ state <newState> <suggNums...>
     */
    override fun execute(event: WeebotCommandEvent) {
        val args = event.argList

        //sugg dev
        if (args.isEmpty()) {
            GlobalScope.launch { sendSuggsDev(0, event) { true } }
            return
        } else if (args.size == 1) {
            return try {
                runBlocking { sendSuggsDev(args[0].toInt() - 1, event) { true } }
            } catch (e: NumberFormatException) {
            }
        }

        when (args[0].toLowerCase()) {
            "state" -> {
                //_ _ state <newState> <suggNums...>
                val suggs = runBlocking { getSuggestions(event.argList.subList(2)) }
                if (suggs.isEmpty()) return
                val oldStates = List(suggs.size) { suggs[it].state }
                val newState = try {
                    read(args[1])
                } catch (e: IllegalArgumentException) {
                    return event.respondThenDeleteBoth(
                        "Invalid State! ${State.values().joinToString { it.name }}")
                }
                suggs.forEach { it.state = newState }
                event.reply(makeEmbedBuilder("Sugg State Change", null, suggs.mapIndexed
                { i, s -> "${s._id} | ${oldStates[i]} -> $newState" }.joinToString("\n"))
                    .build())
            }
            "rem", "remove", "delete", "del" -> {
                GlobalScope.launch {
                    if (deleteSuggestions(args.subList(1))) {
                        event.reply("Suggestions Removed")
                    } else {
                        event.reply("Failed to remove suggestions")
                    }
                }
            }
            else -> GlobalScope.launch {
                try {
                    sendSuggsDev(args[0].toInt() - 1, event) { true }
                } catch (e: NumberFormatException) {
                }
            }
        } //end When

    }
}

