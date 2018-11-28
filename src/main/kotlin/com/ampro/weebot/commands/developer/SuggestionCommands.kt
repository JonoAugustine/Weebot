/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.developer.Suggestion.State
import com.ampro.weebot.commands.developer.Suggestion.State.UNREVIEWED
import com.ampro.weebot.database.DAO
import com.ampro.weebot.main.constants.BOT_DEV_CHAT
import com.ampro.weebot.main.constants.strdEmbedBuilder
import com.ampro.weebot.util.standardDateTimeFormatter
import com.jagrosh.jdautilities.command.Command
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
class CmdSendSuggestion : Command() {
    init {
        name = "suggest"
        aliases = arrayOf("suggestion", "sugg")
        help = "Submit an anonymous suggestion to the Weebot developers" +
                " right from Discord!"
        arguments = "<Your Suggestion>"
        guildOnly = false
        cooldown = 60
        children = arrayOf(CmdDevSuggestions())
        category = CAT_DEV
    }

    override fun execute(event: CommandEvent) {
        val sugg: String = event.args
        if (sugg.length < 3) {
            event.reply("*Sorry, your suggestion is a bit short " +
                    "-- can you include more detail? Thank you!*")
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
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdDevSuggestions : Command() {

    /** How many suggestions per embed page */
    val PAGE_LENGTH: Int = 20

    init {
        name = "DevSuggestions"
        aliases = arrayOf("dev", "devsugg")
        help = "See user suggestions for weebot"
        arguments = "sugg dev <args...>\n_ _ see [pageNum]\n_ _ rem <suggNum>\n_ _ state <suggNum> <newState>"
        guildOnly = false
        ownerCommand = true
    }

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
        val pagenum = (page - 1) * PAGE_LENGTH //TODO

        val e = strdEmbedBuilder.setTitle("Weebot Suggestions")
            .setDescription("A list of all user suggestions submitted.")

        //For each User's suggestion list, make a field
        DAO.suggestions.forEachIndexed { i, sugg ->
            val s = StringBuilder()
            s.append("${i + 1}) ")
                .append(sugg.submitTime.format(standardDateTimeFormatter))
                .append(" | **${sugg.state}** |: $sugg\n")
            e.addField("Suggestions", s.toString(), false)
        }

        if (event.channel.idLong == BOT_DEV_CHAT) {
            event.reply(e.build())
        } else {
            event.replyInDm(e.build())
        }
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
                val pn = try {
                    args[1].toInt()
                } catch (e: NumberFormatException) {
                    event.deleteWithResponse("Invalid page number.")
                    return
                } catch (e: IndexOutOfBoundsException) {
                    1
                }
                sendSuggestions(pn, event)
                return
            }
            "state" -> {
                val sn = try {
                    args[1].toInt() - 1
                } catch (e: Exception) {
                    event.deleteWithResponse("Invalid suggestion id.")
                    return
                }
                if (sn > DAO.suggestions.size) {
                    event.deleteWithResponse("Invalid suggestion id.")
                    return
                }
                val old = DAO.suggestions[sn].state
                DAO.suggestions[sn].state = try {
                    State.valueOf(args[2].toUpperCase())
                } catch (e: IllegalArgumentException) {
                    event.deleteWithResponse(
                        "Invalid State!(UNREVIEWED, ACCEPTED, COMPLETED or IGNORED)")
                    return
                }
                event.reply(strdEmbedBuilder.setTitle(
                    "Changed Suggestion $sn from $old to ${DAO.suggestions[sn].state}")
                    .build())
            }
            "rem", "remove", "delete", "del" -> {
                val sn = try {
                    args[1].toInt() - 1
                } catch (e: Exception) {
                    event.deleteWithResponse("Invalid suggestion id.")
                    return
                }
                if (sn > DAO.suggestions.size) {
                    event.deleteWithResponse("Invalid suggestion id.")
                    return
                }
                val sugg: Suggestion = DAO.suggestions.removeAt(sn)
                event.reply(strdEmbedBuilder.setTitle("Suggestion $sn removed").build())
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
