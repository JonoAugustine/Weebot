/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.*
import com.ampro.weebot.bot.strifeExtensions.`is`
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.ampro.weebot.util.contains
import com.ampro.weebot.util.removeAll
import com.serebit.strife.entities.mentions
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title
import com.serebit.strife.events.Event
import com.serebit.strife.events.MessageCreateEvent
import com.serebit.strife.getUser
import com.serebit.strife.text.boldItalic
import com.serebit.strife.text.italic
import com.soywiz.klock.*
import kotlin.math.absoluteValue


/** An instantiable representation of a User's OutHouse. */
class OutHouse(
    val userID: Long,
    val note: String,
    val forward: Boolean,
    minutes: Int
) : Passive {

    val range: DateTimeRange

    init {
        val now = DateTime.now()
        range = now until now + minutes.minutes
    }

    override var active: Boolean
        get() = DateTime.now() in range
        set(value) {}

    override suspend fun predicate(event: Event, bot: Weebot): Boolean =
        event is MessageCreateEvent &&
            event.message.author?.isHumanUser == true &&
            event.message.guild != null

    override suspend fun consume(event: Event, bot: Weebot) {
        event as MessageCreateEvent

        val guild = event.message.guild!!
        val mess = event.message
        val author = event.message.author!!

        if (author `is` userID) {
            guild.getMember(userID)?.run {
                event.channel.send(
                    "Welcome back ${nickname ?: author.username}".italic
                )
            }
        } else if (mess.mentions(userID)) {
            //Respond as bot
            event.channel.send(
                buildString {
                    append("Sorry, ")
                    append(
                        guild.getMember(userID)
                            ?.run { nickname ?: user.username }
                            ?: "that user"
                    )
                    if (note.isNotBlank()) append(" is out $note. ")
                    else append(" is currently unavailable. ")
                    append("Please try mentioning them again after ")
                    append(
                        DateTime.now().until(range.to).duration.toTimeString(3)
                    )
                    append(". Thank you.")
                }.italic
            )

            //Forward Message
            if (forward) {
                val m = guild.getMember(event.message.author!!.id)
                event.context.getUser(userID)?.createDmChannel()?.sendWEmbed {
                    title(
                        buildString {
                            append("Message From ")
                            append(event.message.author!!.username.boldItalic)
                            m?.run {
                                append(" (a.k.a ")
                                append(
                                    (m.nickname ?: m.user.username).boldItalic
                                )
                                append(")")
                            }
                            append(" in ").append(guild.name.boldItalic)
                        }
                    )
                    description = "\"${event.message.content}\""
                }
            }
        }
    }

}

/**
 * Have the bot respond to your metions while you're AFK but shown as online.
 * Can also forward messages to a private channel.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
object OutHouseCmd : Command(
    "OutHouse",
    listOf("ohc"),
    rateLimit = 60,
    details = buildString {
        append("Have the bot respond to anyone who mentions you for the given ")
        append("time. You can have all mentions of you forwarded to a DM ")
        append("with the `-f` option.")
    },
    params = listOfParams("Wd", "Xh", "-f" to true, "activity" to true),
    action = {
        //ohc [hours] [note here]
        message.author?.getAll<OutHouse>()
            ?.takeIf { it.isNotEmpty() }
            ?.apply {
                message.reply("You're already in the outhouse".italic)
            } ?: run {
            //Add new OH
            val args = message.args

            //check days
            val d = args.firstOrNull { it.matches("\\d+[Dd]".toRegex()) }
                ?.removeAll("[^\\d]+")?.toInt()?.absoluteValue ?: 0
            val h = args.firstOrNull { it.matches("\\d+[Hh]".toRegex()) }
                ?.removeAll("[^\\d]+")?.toInt()?.absoluteValue ?: 0

            val min = when {
                d == 0 && h == 0 -> 60
                else -> (d * 24 * 60) + (h * 60)
            }

            //check for forwarding
            val forward = args.contains("(?i)-f(orward(ing)?)?".toRegex())

            var messageIndex = 0
            if (d > 0) messageIndex++
            if (h > 0) messageIndex++
            if (forward) messageIndex++

            val message = if (args.size - 1 >= messageIndex) {
                args.subList(messageIndex, args.size).joinToString(" ")
            } else ""

            val oh = OutHouse(this.message.author!!.id, message, forward, min)
            addPassive(0, oh)
            this.message.reply(
                "I will hold down the fort while you're away! :guardsman:"
                    + " see you in ${oh.range.duration.toTimeString()}"
            )
        }
    }
)
