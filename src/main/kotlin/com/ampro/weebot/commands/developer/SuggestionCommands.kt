/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.commands.WeebotCommand
import com.ampro.weebot.commands.developer.Suggestion.State
import com.ampro.weebot.commands.developer.Suggestion.State.UNREVIEWED
import com.ampro.weebot.commands.respondThenDelete
import com.ampro.weebot.commands.splitArgs
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.constants.BOT_DEV_CHAT
import com.ampro.weebot.database.constants.EMBED_MAX_DESCRIPTION
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.util.DD_MM_YYYY_HH_MM
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.TextChannel
import java.time.OffsetDateTime

/**
 * A way for anyone in a Guild hosting a Weebot to make suggestions to the
 * developers.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdSendSuggestion : WeebotCommand("suggest", arrayOf("suggestion", "sugg"),
    CAT_DEV, "<Your Suggestion here>",
    "Submit an anonymous suggestion to the Weebot developers right from Discord!",
    cooldown = 60, children = arrayOf(CmdDevSuggestions())
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
        "See user suggestions for weebot",
        guildOnly = false, ownerOnly = true, cooldown = 0 , hidden = true
) {

    /** How many suggestions per embed page */
    val PAGE_LENGTH: Int = 20

    override fun isAllowed(channel: TextChannel?): Boolean {
        return super.isAllowed(channel) || channel?.idLong == BOT_DEV_CHAT
    }

    /**
     * Send a link of submitted suggestions.
     *
     * @param page The page to view (starts at 1)
     * @param event The command event
     */
    fun sendSuggestions(page: Int = 0, event: CommandEvent) {
        val e = strdEmbedBuilder.setTitle("Weebot Suggestions")
            .setDescription("A list of all user suggestions submitted.")

        val s = StringBuilder()
        for (i in page * PAGE_LENGTH until DAO.suggestions.size) {
            val sugg = DAO.suggestions[i]
            s.append("${i + 1}) ")
                .append(sugg.submitTime.format(DD_MM_YYYY_HH_MM))
                .append(" | **${sugg.state}** |: $sugg\n")
        }
        e.addField("Suggestions:", s.toString(), false)

        if (event.channel.idLong == BOT_DEV_CHAT) { event.reply(e.build()) }
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
                val pn = try { args[1].toInt() - 1 }
                catch (e: NumberFormatException) {
                    event.respondThenDelete("Invalid page number.")
                    return
                } catch (e: IndexOutOfBoundsException) { 0 }
                if (pn * PAGE_LENGTH > DAO.suggestions.size) {
                    event.respondThenDelete("There are only ${DAO.suggestions.size} pages")
                    return
                }
                sendSuggestions(pn, event)
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

    override fun toString() = suggestion

}
