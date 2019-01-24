/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.utilitycommands

import com.ampro.weebot.*
import com.ampro.weebot.commands.*
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.AlarmClock
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_SHARD
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.IOException
import java.net.*
import java.net.URLEncoder.*


/** An instantiable representation of a User's OutHouse. */
class OutHouse(user: User, var remainingMin: Long, val message: String,
               val forward: Boolean) : IPassive {

    var lastTime: OffsetDateTime? = NOW()
    val userId = user.idLong

    override fun dead() = this.remainingMin <= 1

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

        if (author `is` userId) {
            val mem = guild.getMember(author)
            event.channel.sendMessage("*Welcome back ${mem.effectiveName}*").queue()
            this.remainingMin = 0
            return
        } else if (mess.mentionedUsers.any { it.idLong == userId }) {
            //Respond as bot
            val sb = StringBuilder().append("*Sorry, ")
                .append(getUser(userId)?.asMention ?: "that user")
            if (message.isNotBlank()) sb.append(" is out $message. ")
            else sb.append(" is currently unavailable. ")
            sb.append("Please try mentioning them again after ")
                .append((remainingMin * 60L).formatTime()).append(". Thank you.*")
            event.channel.sendMessage(sb.toString()).queue()

            //Forward Message
            if (forward) {
                val m = event.message.contentDisplay
                val a = "${author.name} (${guild.getMember(author).effectiveName})"
                val e = strdEmbedBuilder.setAuthor(a)
                    .setTitle("Message from ${guild.name}").setDescription(m).build()
                getUser(userId)?.openPrivateChannel()?.queue {
                    it.sendMessage(e).queue()
                }
            }
            return
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
class CmdOutHouse : WeebotCommand("outhouse", "OutHouse" ,arrayOf("ohc"), CAT_UTIL,
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
        val bot = if (event.isFromType(PRIVATE)) DAO.GLOBAL_WEEBOT
        else getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author, event.creationTime)

        DAO.GLOBAL_WEEBOT.getUserPassive<OutHouse>(event.author)?.apply {
            event.reply("*You're already in the outhouse* ${
            (remainingMin * 60L).formatTime()}")
        } ?: run {
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
            if (DAO.GLOBAL_WEEBOT.addUserPassive(event.author, oh)) {
                event.reply("I will hold down the fort while you're away! :guardsman:"
                        + " see you in ${(min * 60L).formatTime()}")
                return@run oh
            }
            else {
                event.reply("Sorry, You have already reached the maximum number of Passives")
                return@run null
            }
        }
    }

}


/**
 * A Command to set a Reminder for up to 30 Days.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdReminder : WeebotCommand("reminder", null, arrayOf("rc", "rem", "remindme"),
    CAT_UTIL, "Set a Reminder from 1 minute to 30 days.", cooldown = 5) {

    /**
     * A [Reminder] is similar to the [OutHouse] in the way it tracks time
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class Reminder(val userID: Long, val channel: Long?, var minutes: Long,
                   val message: String) {

        companion object { private val REM_ID_GEN = IdGenerator(7, "REM:") }

        val id = REM_ID_GEN.next()
        var lastTime: OffsetDateTime? = NOW()
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
            getUser(userID)?.apply {
                val m = MessageBuilder(makeEmbedBuilder("Weebot Reminder", null,message)
                    .build()).append(this).build()
                if (channel == null) {
                    openPrivateChannel()?.queue { it.sendMessage(m).queue() }
                } else {
                    JDA_SHARD_MNGR.getTextChannelById(channel).sendMessage(m).queue()
                }
            }
        }

        /**@return the remaining time formatted*/
        fun formatTime() = (minutes * 60).formatTime()

        fun selectableEmbed(author: User) { }//todo }

        override fun toString() = "$id = ${formatTime()} $message"

    }

    val remJobMap = ConcurrentHashMap<Long, Job>()

    companion object {
        val DAYS_30_MIN = 43200

        /**
         * Launches a [Job] that moniters a [List] of [Reminder]s
         *
         * @return The [Job] launched
         */
        fun remWatchJob(list: MutableList<Reminder>) = GlobalScope.launch(CACHED_POOL) {
            while (list.isNotEmpty()) {
                list.forEach { it.update() }
                list.filter { it.isDone() }.forEach { it.send() }
                delay(1_000L)
                list.removeIf { it.isDone() }
            }
        }
    }

    fun init() {
        DAO.GLOBAL_WEEBOT.getReminders()
            .filter { it.value.filterNotNull().isNotEmpty() }
            .forEach {
            remJobMap.putIfAbsent(it.key, remWatchJob(it.value))
        }
        /** Cleaner Job */ GlobalScope.launch(CACHED_POOL) {
            while (ON) {
                delay(60 * 60 * 1_000)
                synchronized(remJobMap) { remJobMap.removeIf { _, v -> !v.isActive } }
            }
        }
    }

    private fun sendReminderList(event: CommandEvent) {
        val rems = DAO.GLOBAL_WEEBOT.getReminders(event.author)
            .apply { removeIf { it == null } }
        if (rems.isEmpty()) return event.respondThenDeleteBoth("No reminders.")
        SelectablePaginator(baseEmbed = makeEmbedBuilder(
            event.author.name + "'s Reminders",null,"")// "Choose a reminder to edit it.")
            .build(), itemsPerPage = -1,
            items = rems.mapNotNull {
                "${it.id} : ${it.formatTime()}\n${it.message}" to { _: Int, _: Message ->
                    it.selectableEmbed(event.author)
                }
            }, exitAction = { it.delete().queue() },
            timeoutAction = { it.clearReactions().queue() }
        ).displayInPrivate(event.author)
    }

    //[-private] [Xm] [Yh] [Zd] [Reminder Message]
    //TODO send texts
    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        val bot = if (event.isFromType(PRIVATE)) DAO.GLOBAL_WEEBOT
        else getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author, event.creationTime)

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

                /**Minutes*/
                val min: Long = if (d == 0 && h == 0 && m == 0) 60L
                else (d * 24L * 60L) + (h * 60L) + m
                if (min > DAYS_30_MIN) {
                    event.reactError()
                    event.reply("*The Maximum reminder time is 30 days.*")
                    return
                }

                val message = if (args.size - 1 >= messageIndex) {
                    args.subList(messageIndex, args.size).joinToString(" ")
                } else ""

                val r = Reminder(event.author.idLong,
                    if (private) null else event.textChannel?.idLong, min, message)

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
            .setAliases(aliases)
            .build()
    }
}


/**
 *
 *
 * @author Jonathan Augustine
 * @since 2.2.1
 */
class CmdTranslate : WeebotCommand("Translate", null, arrayOf("gtc", "trans"),
    CAT_UTIL, "Translate a sentence to any language! (well not *any* language...)",
    cooldown = 45, cooldownScope = USER_SHARD) {

    /**
     * Langages that Google translate cares about
     * @param code the code for that language
     */
    private enum class Language(val code: String) {
        Afrikaans("af"), Irish("ga"), Albanian("sq"), Italian("it"), Arabic("ar"),
        Japanese("ja"), Azerbaijani("az"), Kannada("kn"), Basque("eu"), Korean("ko"),
        Bengali("bn"), Latin("la"), Belarusian("be"), Latvian("lv"), Bulgarian("bg"),
        Lithuanian("lt"), Catalan("ca"), Macedonian("mk"), ChineseSimplified("zh-CN"),
        Malay("ms"), ChineseTraditional("zh-TW"), Maltese("mt"), Croatian("hr"),
        Norwegian("no"), Czech("cs"), Persian("fa"), Danish("da"), Polish("pl"),
        Dutch("nl"), Portuguese("pt"), English("en"), Romanian("ro"), Esperanto("eo"),
        Russian("ru"), Estonian("et"), Serbian("sr"), Filipino("tl"), Slovak("sk"),
        Finnish("fi"), Slovenian("sl"), French("fr"), Spanish("es"), Galician("gl"),
        Swahili("sw"), Georgian("ka"), Swedish("sv"), German("de"), Tamil("ta"),
        Greek("el"), Telugu("te"), Gujarati("gu"), Thai("th"), HaitianCreole("ht"),
        Turkish("tr"), Hebrew("iw"), Ukrainian("uk"), Hindi("hi"), Urdu("ur"),
        Hungarian("hu"), Vietnamese("vi"), Icelandic("is"), Welsh("cy"), Indonesian("id"),
        Yiddish("yi");
        companion object {
            fun from(string: String) = values().firstOrNull {
                it.code.equals(string, true) || it.name.equals(string, true)
            }
        }
    }

    private val baseUrl = "https://script.google.com/macros/s/AKfycbyDk3bTHxqZGZ2F6xWmzPmP0EE7ZQORTDIFlf_eNXeWI0GeGX6E/exec"

    @Suppress("UNCHECKED_CAST")
    override fun execute(event: WeebotCommandEvent) {
        if (event.argList.isEmpty() || event.argList.size < 2) return
        //[-s sourceLang] <targetLang> <the stuff to translate>
        val args = event.argList.toMutableList()
        val source: Language?
        val target: Language
        val sourceText: String
        if (args[0].matches(REG_HYPHEN + "(s(ource)?|f(rom)?)")) {
            if (args.size < 4) return event.respondThenDeleteBoth(
                "You must specify a target language and text ``-s [sourceLang] " + "<targetLang> <stuff to translate>``",
                20)
            source = Language.from(args[1]) ?: return event.respondThenDeleteBoth(
                "Unavailable language (${args[1]})").also { event.reactError() }
            target = Language.from(args[2]) ?: return event.respondThenDeleteBoth(
                "Unavailable language (${args[2]})").also { event.reactError() }
            sourceText = args.subList(3).joinToString(" ")
        } else {
            source = null
            target = Language.from(args[0]) ?: return event.respondThenDeleteBoth(
                "Unavailable language (${args[0]})").also { event.reactError() }
            sourceText = args.subList(1).joinToString(" ")
        }
        try {
            makeEmbedBuilder("Translator", null, """
                        Translating ${source?.name ?: "auto"}
                        ```
                        $sourceText
                        ```
                        to ${target.name}
                        ```
                        ${translate(source, target, sourceText)}
                        ```
                    """.trimIndent()).build().send(event.channel)
        } catch (e: IOException) {
            MLOG.elog("Failed to receive translation: " + """
                {
                    from: ${source?.name ?: "auto"}
                    to: ${target.name}
                    text: $sourceText
                }
            """.trimIndent())
            event.respondThenDeleteBoth(GENERIC_ERR_MSG, 20)
        }

    }

    @Throws(IOException::class)
    private fun translate(from: Language?, langTo: Language, text: String): String {
        // INSERT YOU URL HERE
        val urlStr = "$baseUrl?q=${encode(text,"UTF-8")}&target=${langTo.code}&source=${
        from?.code?:""}"
        val url = URL(urlStr)
        val response = StringBuilder()
        val con = url.openConnection() as HttpURLConnection
        con.setRequestProperty("User-Agent", "Mozilla/5.0")
        val `in` = BufferedReader(InputStreamReader(con.inputStream))
        var inputLine: String? = `in`.readLine()
        while (inputLine != null) {
            response.append(inputLine)
            inputLine = `in`.readLine()
        }
        `in`.close()
        return response.toString()
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Translator", false)
            .setAliases(aliases).setDescription("""
                ``[-s sourceLang] <targetLang> <the stuff to translate or whatever>``
                ``<targetLang>`` can be either the ID or the name
                If ``[-s sourceLang]`` is not set, it will be auto-detected
            """.trimIndent())
            .addField("Available Language (Name:ID)", """
                ```css
                ${Language.values().joinToString { "${it.name}:${it.code}" }}
                ```""".trimIndent()
            ).build()
    }

}
