/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.utilitycommands

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_UTIL
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.getUser
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.NOW
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.temporal.ChronoUnit

/** An instantiable representation of a User's OutHouse. */
class OutHouse(user: User, minutes: Long, val message: String = "", val forward: Boolean)
    : IPassive {

    val startTime = NOW()
    var remainingMin = minutes
    val userId = user.idLong

    override fun dead() = this.remainingMin <= 1

    fun formatTime(): String {
        val d    = remainingMin / 60 / 24
        val h   = (remainingMin / 60 ) - 24 * d
        val m = remainingMin - (h * 60) - (d * 60 * 24)
        return "${if(d > 0) "$d day(s), " else ""}${if(h > 0) "$h hour(s), " else ""
        }${if(m > 0) "$m minute(s) " else ""}"
    }

    override fun accept(bot: Weebot, event: Event) {
        if (event !is GuildMessageReceivedEvent) return
        if (event.isWebhookMessage || event.author.isBot) return

        remainingMin -= ChronoUnit.MINUTES.between(startTime, NOW())
        if (remainingMin <= 1) return

        val guild = event.guild
        val mess = event.message
        val author = event.author

        if (event.channel.type != PRIVATE) {
            if (author `is` userId) {
                val mem = guild.getMember(author)
                event.channel.sendMessage("*Welcome back ${mem.effectiveName}*").queue()
                this.remainingMin = 0
                return
            } else if (mess.isMentioned(guild.getMemberById(userId).user)) {
                //Respond as bot
                val sb = StringBuilder().append("*Sorry, ")
                    .append(guild.getMemberById(userId).effectiveName)
                if (message.isNotBlank()) sb.append(" is out $message. ")
                else sb.append(" is currently unavailable. ")
                sb.append("Please try mentioning them again after ")
                    .append(formatTime()).append(". Thank you.*")
                event.channel.sendMessage(sb.toString()).queue()

                //Forward Message
                if (forward) {
                    val m = event.message.contentDisplay
                    val a = "${author.name} (${guild.getMember(author).effectiveName})"
                    val e = strdEmbedBuilder.setAuthor(a)
                        .setTitle("Message from ${guild.name}")
                        .setDescription(m).build()
                    getUser(userId)?.openPrivateChannel()?.queue {
                        it.sendMessage(e).queue()
                    }
                }
                return
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
class CmdOutHouse : WeebotCommand("OutHouse", arrayOf("ohc"), CAT_UTIL,
    "Have the bot respond to anyone who mentions you for the given time.",
    "[time] [activity]", cooldown = 30) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("OutHouse Command")
            .setDescription("Have the bot respond to mentions for you while you're away.")
            .appendDesc("\nYou can also forward any message that mentions you to a ")
            .appendDesc("private channel.")
            .addField("Arguments", "[Zd] [Xh] [Ym] [-f] [afk-message]" +
                    "\ndays, hours, minutes. -f enables message forwarding to private " +
                    "chat.")
            .addField("Alias", "outhouse")
            .build()
    }

    override fun execute(event: CommandEvent) {
        //ohc [hours] [message here]

        val pas = DAO.GLOBAL_WEEBOT.getUesrPassiveList(event.author)
            .firstOrNull { it is OutHouse } ?: run {
            //Add new OH
            val args = event.splitArgs()

            //check days
            val d = args.firstOrNull{ it.matches("\\d+[Dd]".toRegex()) }
                ?.removeAll("[^\\d]+")?.toInt() ?: 0
            val h = args.firstOrNull{ it.matches("\\d+[Hh]".toRegex()) }
                ?.removeAll("[^\\d]+")?.toInt() ?: 0
            val m = args.firstOrNull{ it.matches("\\d+[Mm]".toRegex()) }
                ?.removeAll("[^\\d]+")?.toInt() ?: 0

            val min = if(d == 0 && h == 0 && m == 0) { 60 }
            else (d * 24 * 60) + (h * 60) + m

            //check for forwarding
            val forward = args.contains("(?i)-f(orward(ing)?)?".toRegex())

            var messageIndex = 0
            if (d > 0) messageIndex++
            if (h > 0) messageIndex++
            if (m > 0) messageIndex++
            if (forward) messageIndex++

            val message = if (args.size - 1 >= messageIndex) {
                args.subList(messageIndex, args.size).joinToString(" ")
            } else ""

            val oh = OutHouse(event.author, min.toLong(), message, forward)
            if (DAO.GLOBAL_WEEBOT.addUserPassive(event.author, oh))
                event.reply("I will hold down the fort while you're away! :guardsman:")
            else event.reply("Sorry, You have already reached the maximum number of Passives")
            return
        }
        //Already in OH
        pas as OutHouse
        event.reply("*You're already in the outhouse* ${pas.formatTime()}")
    }

}

class CmdReminder() //TODO
