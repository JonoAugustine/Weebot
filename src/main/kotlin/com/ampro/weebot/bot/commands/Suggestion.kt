/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.commands.SuggestionCmd.add
import com.ampro.weebot.bot.commands.SuggestionCmd.see
import com.ampro.weebot.bot.commands.SuggestionCmd.vote
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.util.DD_MM_YYY_HH_MM
import com.ampro.weebot.util.IdGenerator
import com.ampro.weebot.util.Regecies
import com.serebit.strife.entities.reply
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz

/**
 * A suggestion. Basically a String wrapper with info about date and location.
 *
 * @property suggestion The content of the suggestion.
 * @property submitTime The date and time the suggestion was submitted
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
data class Suggestion(
    val suggestion: String,
    val submitTime: DateTimeTz = DateTime.nowLocal()
) {

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
            fun read(string: String): State = string.toUpperCase().run {
                when {
                    matches(Regex("^(UNREVIEW(E?D?)?)$")) -> UNREVIEWED
                    matches(Regex("^(ACCEPT(E?D?)?)$")) -> ACCEPTED
                    matches(Regex("^(COMPLET(E?D?)?)$")) -> COMPLETED
                    matches(Regex("^(IGNOR(E?D?)?)$")) -> IGNORED
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    val _id = idGenerator.next()

    var state: State = State.UNREVIEWED

    /** A list of user IDs who have upvoted (to prevent double votes) */
    private var votes = mutableSetOf<Long>()

    val score get() = votes.size

    /**
     * Add a user's vote.
     * @return `true` if the userID was added, `false` if they already voted.
     */
    fun addVote(userID: Long): Boolean = votes.add(userID)

    override fun toString() = suggestion

    /**
     * ```
     * ID
     * suggestion
     * Score: score (state)
     * ```
     */
    fun toStringPub() = buildString {
        append(_id).append("\n")
            .append(suggestion)
            .append("\nScore: ").append(score)
            .append('(').append(state).append(')')
    }

    /**
     * ```
     * ID (state, score, submitTime, suggestion)
     * ```
     */
    fun toStringDev() = buildString {
        append(_id).append('(')
        append(state).append(", ")
        append(score).append(", ")
        append(submitTime.DD_MM_YYY_HH_MM)
        append(')')
    }

    companion object {
        val idGenerator = IdGenerator(7, "SUG_")
    }

}

object SuggestionCmd : Command(
    "Suggestion",
    listOf("sugg"),
    listOf(Dev),
    rateLimit = 90,
    details = buildString {
        append("Submit a suggestion to my developers for new features!\n")
        append("actions: a(dd), s(ee), v(ote)\n")
        append("value: add=new_suggestion, see=review_state (")
        append("unreviewed, accepted, completed)")
        append("vote=suggestion_ID")
    },
    params = listOfParams("action", "value" to true),
    predicate = {
        val args = message.args
        when {
            args[1].matches(add) -> if (args.size < 6) {
                message.reply(
                    "Suggestion is too short, please include more detail."
                )
                false
            } else true
            args[1].matches(vote) -> if (args.size != 3) {
                message.reply("Please provide a suggestion ID to vote for.")
                false
            } else true
            else -> args[1].matches(see)
        }
    },
    action = {
         TODO()
    }
) {

    object Dev : DeveloperCommand(
        "dev",
        params = listOfParams("id"),
        details = "view and edit a command",
        action = {
            TODO()
        }
    )

    private val add = Regex("${Regecies.ic}ad?")
    private val see =  Regex("${Regecies.ic}se?")
    private val vote =  Regex("${Regecies.ic}v(ote)?")


}
