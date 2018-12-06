/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.developer.Suggestion.State
import com.ampro.weebot.commands.developer.Suggestion.State.UNREVIEWED
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.constants.*
import com.ampro.weebot.util.DD_MM_YYYY_HH_MM
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.TextChannel
import java.time.OffsetDateTime

/**
 * A suggestion. Basically a String wrapper with info about date and location.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class Suggestion(val suggestion: String) {

    enum class State {UNREVIEWED, ACCEPTED, COMPLETED, IGNORED}

    /** The date and time the suggestion was submitted */
    val submitTime: OffsetDateTime = OffsetDateTime.now()

    var state: State = UNREVIEWED

    /** The number of public upvotes a suggestion has */
    var upvotes: Int = 1
    /** The number of public downvotes a suggestion has */
    var downvotes: Int = 1

    override fun toString() = suggestion

}

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
 * Send a formatted list of [Suggestion]s in response to a non-dev [User] using
 * the sugg see command
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun sendSuggsPublic(event: CommandEvent, criteria: (Suggestion) -> Boolean) {
    val list = searchSuggs(criteria)

}

/**
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun sendSuggsDev(event: CommandEvent, criteria: (Suggestion) -> Boolean) {
    val list = searchSuggs(criteria)

}

/**
 * A way for anyone in a Guild hosting a Weebot to make suggestions to the
 * developers.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdSendSuggestion : WeebotCommand("suggest", arrayOf("suggestion", "sugg"),
    CAT_DEV, "[-g] <Your Suggestion here> | [-s] [page #]",
    "Submit an anonymous suggestion to the Weebot developers right from Discord!",
    HelpBiConsumerBuilder("Weebot Suggestions").setDescription(
        "Submit an anonymous suggestion to the Weebot developers right from " +
                "Discord!\nYou can use this command to report bugs, send " +
                "suggestions, or vote on suggestions that have been sent by others!" +
                "(don't worry, no one will know who sent each suggestion; 100% anon)"
    ).build(), cooldown = 60, children = arrayOf(CmdDevSuggestions())
){

    override fun execute(event: CommandEvent) {
        val sugg: String = event.args
        if (sugg.length < 3) {
            event.reply("*Sorry, your suggestion is a bit short " +
                    "-- can you include more detail? Thank you!*")
            return
        } else if (sugg.length > EMBED_MAX_DESCRIPTION) {
            event.reply("*Sorry, your suggestion is a too long (max=$EMBED_MAX_DESCRIPTION char)" +
                    "-- can you try and be more concise? Thank you!*")
            return
        }
        DAO.suggestions.add(Suggestion(sugg))
        event.reply("*Thank you for your suggestion! We're working hard to "
                        + "make Weebot as awesome as possible, and we "
                        + "will try our best to include your suggestion!*")
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
class CmdDevSuggestions : WeebotCommand("DevSuggestions", arrayOf("dev", "devsugg"),
        CAT_DEV, "sugg dev <args...>\n_ _ see [pageNum]\n_ rem <suggNum>\n_ _ state <suggNum> <newState>",
        "See user suggestions for weebot", ownerOnly = true, cooldown = 0 , hidden = true
) {

    /** How many suggestions per embed page */
    val PAGE_LENGTH: Int = 20

    override fun isAllowed(channel: TextChannel?)
            = super.isAllowed(channel) || OFFICIAL_CHATS.contains(channel?.idLong)

    /**
     * Send a link of submitted suggestions.
     *
     * @param page The page to view (starts at 1)
     * @param param The parameter to search by (State)
     * @param event The command event
     */
    fun sendSuggestions(page: Int = 0, event: CommandEvent, param: State? = null) {
        val e = strdEmbedBuilder.setTitle("Weebot Suggestions")
            .setDescription("A list of all user suggestions submitted.")

        val s = StringBuilder()
        for (k in page * PAGE_LENGTH until DAO.suggestions.size) {
            val sugg = DAO.suggestions[k]
            if (param != null && sugg.state == param) {
                s.append("${k + 1}) ").append(sugg.submitTime.format(DD_MM_YYYY_HH_MM))
                    .append(" | **${sugg.state}** |: $sugg\n")
            }
        }
        e.addField("Suggestions:", s.toString(), false)
        if (OFFICIAL_CHATS.contains(event.channel.idLong)) { event.reply(e.build()) }
        else { event.replyInDm(e.build()) }
    }

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
            sendSuggestions(0, event)
            return
        }

        when (args[0].toLowerCase()) {
            "see" -> {
                var state: State? = null
                val pn = try { args[1].toInt() - 1 }
                catch (e: NumberFormatException) {
                    state = try {
                        State.valueOf(args[1].toUpperCase())
                        sendSuggestions(0, event, state)
                        return
                    } catch (e: IllegalArgumentException) {
                        event.respondThenDelete(
                            "Invalid State!(UNREVIEWED, ACCEPTED, COMPLETED or IGNORED)")
                        return
                    }
                    return
                } catch (e: IndexOutOfBoundsException) { 0 }
                if (pn * PAGE_LENGTH > DAO.suggestions.size) {
                    event.respondThenDelete("There are only ${DAO.suggestions.size} pages")
                    return
                }
                state = try {
                    State.valueOf(args[1].toUpperCase())
                } catch (e: IllegalArgumentException) {
                    event.respondThenDelete(
                        "Invalid State!(UNREVIEWED, ACCEPTED, COMPLETED or IGNORED)")
                    return
                } catch (e: java.lang.IndexOutOfBoundsException) {
                    return
                }
                sendSuggestions(pn, event, state)
                return
            }
            "state" -> {
                val sn = try {
                    args[1].toInt() - 1
                } catch (e: Exception) {
                    event.respondThenDelete("Invalid suggestion id.")
                    return
                }
                if (sn > DAO.suggestions.size) {
                    event.respondThenDelete("Invalid suggestion id.")
                    return
                }
                val old = DAO.suggestions[sn].state
                DAO.suggestions[sn].state = try {
                    State.valueOf(args[2].toUpperCase())
                } catch (e: IllegalArgumentException) {
                    event.respondThenDelete(
                        "Invalid State!(UNREVIEWED, ACCEPTED, COMPLETED or IGNORED)")
                    return
                }
                event.reply(strdEmbedBuilder.setTitle(
                    "Changed Suggestion ${sn+1} from $old to ${DAO.suggestions[sn].state}")
                    .build())
            }
            "rem", "remove", "delete", "del" -> {
                val sn = try {
                    args[1].toInt() - 1
                } catch (e: Exception) {
                    event.respondThenDelete("Invalid suggestion id.")
                    return
                }
                if (sn > DAO.suggestions.size) {
                    event.respondThenDelete("Invalid suggestion id.")
                    return
                }
                val sugg: Suggestion = DAO.suggestions.removeAt(sn)
                event.reply(strdEmbedBuilder.setTitle("Suggestion ${sn+1} removed").build())
            }
            "share", "show" -> {
                val sn = try {
                    args[1].toInt() - 1
                } catch (e: Exception) {
                    event.respondThenDelete("Invalid suggestion id.")
                    return
                }
                if (sn > DAO.suggestions.size) {
                    event.respondThenDelete("Invalid suggestion id.")
                    return
                }
                val sugg = DAO.suggestions[sn]
                event.reply(strdEmbedBuilder.setTitle("Suggestion ${sn+1}")
                    .setDescription(sugg.suggestion)
                    .addField("State: ${sugg.state}", "", true)
                    .addField("Submited at", sugg.submitTime.format(DD_MM_YYYY_HH_MM), true)
                    .build())
            }
        }

    }
}
