/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.commands.developer.Suggestion.State
import com.ampro.weebot.commands.developer.Suggestion.State.*
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.constants.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.main.SELF
import com.ampro.weebot.util.DD_MM_YYYY_HH_MM
import com.ampro.weebot.util.IdGenerator
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
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

    enum class State { UNREVIEWED, ACCEPTED, COMPLETED, IGNORED;

        companion object {
            /**
             * A replacement for Enum::valueOf that is more lenient in parsing
             *
             * @param string the string to parse
             * @return The parsed [State]
             * @throws IllegalArgumentException if the [string] could not be parsed
             */
            fun read(string: String): State {
                val s = string.toUpperCase()
                return when {
                    s.matches(Regex("^(UNREVIEW(E?D?)?)$")) -> UNREVIEWED
                    s.matches(Regex("^(ACCEPT(E?D?)?)$")) -> ACCEPTED
                    s.matches(Regex("^(COMPLET(E?D?)?)$")) -> COMPLETED
                    s.matches(Regex("^(IGNOR(E?D?)?)$")) -> IGNORED
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    companion object { val idGenerator = IdGenerator() }

    val id = idGenerator.next()

    /** The date and time the suggestion was submitted */
    val submitTime: OffsetDateTime = OffsetDateTime.now()

    var state: State = UNREVIEWED

    /** A list of user IDs who have upvoted (to prevent double votes) */
    var votes = mutableListOf<Long>()

    val score get() = votes.size

    override fun toString() = suggestion

    fun toStringPub()
            = "id : $id:\n*$suggestion*\n**score: $score | | | state: ${
    state.toString().toLowerCase()}**\n\n"

    fun toStringDev()
            = "id : $id | ${submitTime.format(DD_MM_YYYY_HH_MM)} | **$state** | " +
            "$score\n$suggestion\n\n"
}

/**
 * Get a [Suggestion] from the [DAO] or send a message that no sugg was found
 * and return null
 */
fun getSuggById(id: String, event: CommandEvent): Suggestion? {
    return DAO.suggestions.find {
        it.id.equals(id.removeAll("[^a-zA-Z0-9]"), true)
    } ?: run {
        event.respondThenDelete("*No suggestion matched the ID provided.*", 30)
        return null
    }
}

/**
 * A way for anyone in a Guild hosting a Weebot to make suggestions to the
 * developers.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
open class CmdSuggestion : WeebotCommand("suggest",
    arrayOf("suggestion", "sugg"), CAT_DEV, "See help embed.",
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
    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        val message = event.args.replace(userMentionRegex, "@/ User")

        when {
            //Ignore empty commands
            args.isEmpty() -> return
            //When submitting
            args[0].matches(giveRegi) || !args[0].matchesAny(voteRegi, seeRegi) -> {
                when {
                    args.size < 4 -> {
                        event.reply(
                            "*Sorry, your suggestion is a bit short "
                                    + "-- can you include more detail? Thank you!*")
                        return
                    }
                    message.length > EMBED_MAX_DESCRIPTION -> {
                        event.reply(
                            "*Sorry, your suggestion is a too long  (max=$EMBED_MAX_FIELD_VAL char) -- can you try and be more concise? Thank you!*")
                        return
                    }
                    else -> {
                        DAO.suggestions.add(Suggestion(message.removeAll(giveRegi)))
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
                    event.respondThenDelete("*No suggestion ID was provided.*", 30)
                    return
                }
                val sugg: Suggestion = getSuggById(args[1], event) ?: return
                if (!sugg.votes.contains(event.author.idLong)) {
                    sugg.votes.add(event.author.idLong)
                    event.reactSuccess()
                    event.reply("*You voted for suggestion ${sugg.id}! " +
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
const val PAGE_LENGTH = 6.0

/**
 * Get a list of [Suggestion]s that match the given [criteria]
 *
 * @param criteria The search parameters
 * @return A [List] of [Suggestion]s that match the given [criteria] (never null)
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun searchSuggs(criteria: (Suggestion) -> Boolean)
        : List<Suggestion> = DAO.suggestions.filter { criteria(it) }

/**
 * Send a formatted list of [Suggestion]s in response to a non-dev User using
 * the sugg see command
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun sendSuggsPublic(page: Int, event: CommandEvent, criteria: (Suggestion) -> Boolean) {
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
        .setText("Weebot Suggestions")
        .build().paginate(event.channel, page)

}

/**
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun sendSuggsDev(page: Int, event: CommandEvent, criteria: (Suggestion) -> Boolean) {
    val list = searchSuggs(criteria)
    val e = strdEmbedBuilder.setTitle("Weebot Suggestions")
    if (list.isEmpty()) {
        event.reply(e.setDescription(
            "*No Suggestions found which match the search criteria*").build())
        return
    }

    if (OFFICIAL_CHATS.contains(event.channel.idLong)) {
        strdPaginator.setTimeout(5, MINUTES)
            .apply { list.forEach { this.addItems(it.toStringDev()) } }
            .setText("Weebot Suggestions").build().paginate(event.channel, page)
    } else {
        event.author.openPrivateChannel().queue { ch ->
            strdPaginator.setTimeout(5, MINUTES)
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
class CmdSeeSuggestions : WeebotCommand("see", arrayOf("-s"), CAT_DEV, "", "",
    cooldown = 5) {

    // \sugg -s(ee) [-k <keyword>] [-r <accepted/unreviewed/completed/ignored>] [pagenum]
    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        //When seeing

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
                            val s = State.read(it)
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
class CmdDevSuggestions : WeebotCommand("dev", emptyArray(), CAT_DEV, "",
    "", HelpBiConsumerBuilder("Weebot Suggestions", false).setDescription(
        "Commands for devs to control suggestions"
    ).addField("", "", false)
        .build(), ownerOnly = true, cooldown = 0, hidden = true
) {

    override fun isAllowed(channel: TextChannel)
            = super.isAllowed(channel) || OFFICIAL_CHATS.contains(channel.idLong)

    /**
     * sugg dev <args...>
     *     _ _ see [pageNum]
     *     _ _ rem <suggNum>
     *     _ _ state <suggNum> <newState>
     */
    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()

        //sugg dev
        if (args.isEmpty()) {
            sendSuggsDev(0, event) { true }
            return
        } else if (args.size < 2) {
            try {
                sendSuggsDev(args[0].toInt() - 1, event) { true }
                return
            } catch (e: NumberFormatException) {}
            return
        }

        when (args[0].toLowerCase()) {
            "state" -> {
                val sugg = getSuggById(args[1], event) ?: return
                val old = sugg.state
                sugg.state = try {
                    State.read(args[2])
                } catch (e: IllegalArgumentException) {
                    event.respondThenDelete(
                        "Invalid State! UNREVIEWED, ACCEPTED, COMPLETED or IGNORED"
                    )
                    return
                } catch (e: IndexOutOfBoundsException) {
                    event.reply(strdEmbedBuilder
                        .setTitle("Sugg <${sugg.id}< is currently ${sugg.state}")
                        .setDescription("${SELF.asMention} sugg dev state " +
                                "<ID> <newState>\nTo change the state." +
                                    "\nUNREVIEWED, ACCEPTED, COMPLETED or IGNORED")
                        .build())
                    return
                }
                event.reply(strdEmbedBuilder.setTitle(
                    "Changed Suggestion ${sugg.id} from $old to ${sugg.state}")
                    .build())
            }
            "rem", "remove", "delete", "del" -> {
                val remList = mutableListOf<String>()
                for (i in 1 until args.size) {
                    val sugg: Suggestion = getSuggById(args[i], event) ?: return
                    DAO.suggestions.remove(sugg)
                    remList.add(sugg.id)
                }
                event.reply(strdEmbedBuilder.setTitle("Suggestions Removed")
                    .setDescription(remList.joinToString(", "))
                    .build())
            }
            else -> try {
                sendSuggsDev(args[0].toInt() - 1, event) { true }
                return
            } catch (e: NumberFormatException) {}
        } //end When

    }
}

