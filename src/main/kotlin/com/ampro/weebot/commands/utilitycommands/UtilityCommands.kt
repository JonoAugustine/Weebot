/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.utilitycommands

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_UTIL
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.DAO
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.database.getUser
import com.ampro.weebot.extensions.*
import com.ampro.weebot.main.JDA_SHARD_MNGR
import com.ampro.weebot.main.ON
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/** An instantiable representation of a User's OutHouse. */
class OutHouse(user: User, var remainingMin: Long, val message: String, val forward:Boolean)
    : IPassive {

    var lastTime = NOW()
    val userId = user.idLong

    override fun dead() = this.remainingMin <= 1

    fun formatTime(): String {
        val d    = remainingMin / 60 / 24
        val h   = (remainingMin / 60 ) - 24 * d
        val m = remainingMin - (h * 60) - (d * 60 * 24)
        return "${if(d > 0) "$d d, " else ""}${if(h > 0) "$h hr, " else ""
        }${if(m > 0) "$m min " else ""}"
    }

    override fun accept(bot: Weebot, event: Event) {
        if (event !is GuildMessageReceivedEvent) return
        if (event.isWebhookMessage || event.author.isBot) return

        val diff = ChronoUnit.MINUTES.between(lastTime, NOW())
        if (diff > 0) {
            remainingMin -= diff
            lastTime = NOW()
        }
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
    "[Zd] [Xh] [Ym] [-f] [activity]",
    "Have the bot respond to anyone who mentions you for the given time.",
    cooldown = 30) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("OutHouse Command")
            .setDescription("Have the bot respond to mentions for you while you're away.")
            .addToDesc("\nYou can also forward any message that mentions you to a ")
            .addToDesc("private channel.")
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


val remThreadPool = newFixedThreadPoolContext(1_000, "ReminderThreadPool")

/**
 * A Command to set a Reminder for up to 30 Days.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdReminder : WeebotCommand("Reminder", arrayOf("rc", "rem", "remindme"),
    CAT_UTIL, "[-private] [-t phoneNum] [Xm] [Yh] [Zd] [Reminder Message]",
    "Set a Reminder from 1 minute to 30 days.", cooldown = 5
) {

    /**
     * A [Reminder] is similar to the [OutHouse] in the way it tracks time
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class Reminder(val userID: Long, val channel: Long?, var minutes: Long,
                   val message: String) {

        companion object { val REM_ID_GEN = IdGenerator(7) }

        val id = REM_ID_GEN.next()
        var lastTime = NOW()
        /** Update the remaining time */
        fun update() {
            val diff = ChronoUnit.MINUTES.between(lastTime, NOW())
            if (diff > 0) {
                minutes -= diff
                lastTime = NOW()
            }
        }
        fun isDone() = minutes <= 0L

        /**
         * Sends the user their reminder
         */
        fun send() {
            val e = strdEmbedBuilder.setTitle("Weebot Reminder")
                .setDescription(getUser(userID)?.asMention ?: "")
                .appendDescription(message).build()
            if (channel == null) {
                getUser(userID)?.openPrivateChannel()?.queue { pc ->
                    pc.sendMessage(e).queue()
                }
            } else {
                JDA_SHARD_MNGR.getTextChannelById(channel).sendMessage(e).queue()
            }
        }

        fun formatTime(): String {
            val d    = minutes / 60 / 24
            val h   = (minutes / 60 ) - 24 * d
            val m = minutes - (h * 60) - (d * 60 * 24)
            return "${if(d > 0) "$d d, " else ""}${if(h > 0) "$h hr, " else ""
            }${if(m > 0) "$m min " else ""}"
        }

        override fun toString() = "$id = ${formatTime()} $message"
    }

    val remJobMap = ConcurrentHashMap<Long, Job>()

    val remCleaner: Job = GlobalScope.launch(remThreadPool) {
        while (ON) {
            delay(60 * 60 * 1_000)
            synchronized(remJobMap) { remJobMap.removeIf { _, v -> !v.isActive } }
        }
    }

    companion object {
        val DAYS_30_MIN = 43200

        /**
         * Launches a [Job] that moniters a [List] of [Reminder]s
         *
         * @return The [Job] launched
         */
        fun remWatchJob(list: MutableList<Reminder>) = GlobalScope.launch(remThreadPool) {
            while (list.isNotEmpty()) {
                list.forEach { it.update() }
                list.filter { it.isDone() }.forEach { it.send() }
                delay(1_000L)
                list.removeIf { it.isDone() }
            }
        }
    }

    fun init() {
        DAO.GLOBAL_WEEBOT.getReminders().forEach {
            remJobMap.putIfAbsent(it.key, remWatchJob(it.value))
        }
    }

    fun sendReminderList(event: CommandEvent) {
        event.replyInDm(strdEmbedBuilder.setTitle(event.author.name + "'s Reminders")
            .apply {
                setDescription("")
                DAO.GLOBAL_WEEBOT.getReminders(event.author).forEach {
                    appendDescription("${it.id} : ${it.formatTime()}\n${it.message}\n\n")
                }
            }.build())
    }

    //[-private] [Xm] [Yh] [Zd] [Reminder Message]
    //TODO send texts
    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()

        when {
            args.isEmpty() || args[0].matches(Regex("(?i)-s(e)*"))-> {
                sendReminderList(event)
                return
            }
            args[0].matches(Regex("(?i)-r(em(ove)?)?")) -> {
                if (args.size < 2) {
                    event.reactError()
                    event.reply("*You must specify one or more Reminders to remove*")
                    return
                }
                val rems = DAO.GLOBAL_WEEBOT.getReminders(event.author)
                synchronized(rems) {
                    for (i in 1 until args.size) {
                        rems.removeAll { it.id.equals(args[i], true) }
                    }
                    event.replyInDm("*Reminders removed.*")
                }
            }
            else -> {
                //check days
                val d = args.firstOrNull { it.matches("(?i)\\d+d(a?y?s?)?".toRegex()) }
                    ?.removeAll("[^\\d]+")?.toInt() ?: 0
                val h = args.firstOrNull { it.matches("(?i)\\d+ho?u?r?s?".toRegex()) }
                    ?.removeAll("[^\\d]+")?.toInt() ?: 0
                val m = args.firstOrNull { it.matches("(?i)\\d+m(in)?".toRegex()) }
                    ?.removeAll("[^\\d]+")?.toInt() ?: 0
                val private = args.firstOrNull {
                    it.matches(Regex("(?i)-p(riv(ate)?)?"))
                } != null

                var messageIndex = 0
                if (d > 0) messageIndex++
                if (h > 0) messageIndex++
                if (m > 0) messageIndex++
                if (private) messageIndex++

                val min = if (d == 0 && h == 0 && m == 0) {
                    60
                } else (d * 24 * 60) + (h * 60) + m
                if (min > DAYS_30_MIN) {
                    event.reactError()
                    event.reply("*The Maximum reminder time is 30 days.*")
                    return
                }

                val message = if (args.size - 1 >= messageIndex) {
                    args.subList(messageIndex, args.size).joinToString(" ")
                } else ""

                val r = Reminder(event.author.idLong,
                    if (private || event.isFromType(PRIVATE)) null
                    else event.channel.idLong, min.toLong(), message)

                if (DAO.GLOBAL_WEEBOT.addReminder(event.author, r)) {
                    event.reactSuccess()
                } else {
                    event.reactError()
                    event.reply("*You already have the maximum reminders set.*")
                }
            }
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Weebot Reminders $AlarmClock")
            .setDescription("Set a Reminder from 1 minute to 30 days.")
            .addField("Set a Reminder", "[-p] [Xmin] [Yhr] [Zd] [Reminder Message]\n" +
                    "-p (-private) sends the reminder as a private message", true)
            .addField("See Your Reminders", "-s (-see)", true)
            .addField("Remove a Reminder", "-r (-remove) <Reminder IDs...>", true)
            .build()
    }
}
