/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.util.DD_MM_YYY_HH_MM
import com.ampro.weebot.util.IdGenerator
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

    companion object { val idGenerator = IdGenerator(7, "SUG_") }

}

object SuggestionCmd : Command {



    object Dev : Command {

    }
}
