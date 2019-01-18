/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.WAITER
import com.ampro.weebot.commands.CAT_MOD
import com.ampro.weebot.commands.moderation.ModerationData.ModAction.*
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.Warning
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_GUILD
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.Permission.KICK_MEMBERS
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.Int.Companion.MAX_VALUE


data class ModerationData(val initDate: OffsetDateTime) {
    enum class ModAction { KICK, BAN, NOTIFY_USER, NOTIFY_ADMINS }
    var reportLimit: Int = -1
    var reportLimitActions: List<ModAction> = listOf(NOTIFY_USER, NOTIFY_ADMINS)
    var hierarchicalReports: Boolean = false
    /**User idLong -> List<ReportString>*/
    val reports: ConcurrentHashMap<Long, MutableList<String>> = ConcurrentHashMap()

    fun asEmbed(guild: Guild) : EmbedBuilder = makeEmbedBuilder("Moderation Settings",
        null, "Moderation Settings for **${guild.name}**")
        .addField("Report Settings", """
            Reports Limit: ${if (reportLimit == -1) "*not set*" else "$reportLimit"}
            Report Limit Actions: ${reportLimitActions.joinToString()}
            ${if (hierarchicalReports) "Hierarchical Reports" else "Anyone can report anyone"}
        """.trimIndent(), true)
}

/**
 * TODO add more moderation commands
 *
 * @author Jonathan Augustine
 * @since 2.1.1
 */
class CmdModeration : WeebotCommand("mod", "Moderation", emptyArray(), CAT_MOD, "",
    "View and edit Moderation settings like max user-reports.", guildOnly = true,
    userPerms = arrayOf(ADMINISTRATOR)) {

    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        val g = event.guild
        val bot = getWeebotOrNew(g)
        val moderationData = bot.moderationData ?: let {
            bot.moderationData = ModerationData(event.creationTime)
            return@let bot.moderationData!!
        }

        if (args.isEmpty()) return event.reply(moderationData.asEmbed(g).build())
        else STAT.track(this, bot, event.author, event.creationTime)

        when {
            args[0].matches(REG_HYPHEN + "rl") && args.size >= 2  -> {
                try {
                    val rl = args[1].toInt()
                    when (rl) {
                        in 1..MAX_VALUE -> {
                            moderationData.reportLimit = rl
                            event.reply("Report Limit set to ``$rl``")
                        }
                        -1 -> {
                            moderationData.reportLimit = -1
                            event.reply("Report Limit disabled")
                        }
                        else -> event.reply("*Please use a number between " +
                                "``1`` and ``$MAX_VALUE`` or ``-1`` to disable*")
                    }
                } catch (e: NumberFormatException) {
                    return event.reply("*Please use a number between " +
                            "``1`` and ``$MAX_VALUE`` or ``-1`` to disable*")
                }
            }
            args[0].matches(REG_HYPHEN + "rla") && args.size >= 2 -> {
                try {
                    val list = args.subList(1, args.size).map { valueOf(it.toUpperCase()) }
                    moderationData.reportLimitActions = list
                    event.reply("Report Limit Actions set to: ${list.joinToString()}")
                } catch (e: IllegalArgumentException) {
                    event.reply("*Please use choose from: ${values().joinToString()}*")
                }
            }
            args[0].matches(REG_HYPHEN + "hr") && args.size >= 2 -> {
                when {
                    args[1].matches(REG_ON) -> {
                        moderationData.hierarchicalReports = true
                        event.reply("Members can now report anyone who does not outrank them")
                    }
                    args[1].matches(REG_OFF) -> {
                        moderationData.hierarchicalReports = false
                        event.reply("Anyone can now report anyone (but admins)")
                    }
                    else -> return event.respondThenDelete("Please say ``on`` or ``off``", 10)
                }
            }
        }

    }

    init {
        val modActions = values().joinToString(""){ "[${it.name.toLowerCase()}]" }
        helpBiConsumer = HelpBiConsumerBuilder("Moderation",
            "Commands for server member moderation.")
            .addField("Report Settings", """
                Set Report limit: ``-rl <1 to $MAX_VALUE> (-1 to disable)``
                Set Report Limit Actions: ``-rla <$modActions>``
                Hierarchical Reporting: ``-hr on/off``
            """.trimIndent())
            .build()
    }
}

class CmdReport : WeebotCommand("report", null, arrayOf("reports"), CAT_MOD,
    "<@User> [@users...] [reason]", "Report users to the server moderators.",
    cooldown = 20, cooldownScope = USER_GUILD, guildOnly = true) {

    fun name(guild: Guild, id: Long)
            = guild.getMemberById(id)?.effectiveName ?: getUser(id)?.name ?: "Unknown User"

    override fun execute(event: CommandEvent) {
        val gld = event.guild
        val bot = getWeebotOrNew(gld)
        if (event.args.isNullOrBlank()) return
        val args = event.splitArgs()
        val mentions = event.message.mentionedMembers.filterNot {
            it.user `is` event.author || it.user.isBot
        }.toMutableList()
        val modData = bot.moderationData

        //if See (dont track see)
        if (args[0].matches(REG_HYPHEN + "(se*|v(iew)?)")) {
            if (!(event.member hasPerm KICK_MEMBERS)) {
                return event.respondThenDelete("You must have the Kick Member Permission")
            }
            val reports = (if (mentions.isEmpty()) modData.reports
            else modData.reports.filter {
                mentions.map { m -> m.user.idLong }.contains(it.key)
            }).filter { it.value.isNotEmpty() }

            if (reports.isNotEmpty()) {
                SelectablePaginator(setOf(event.author), emptySet(),
                    (reports.size % 10) + 3L, title = "${gld.name} User Reports",
                    items = reports.map { r ->
                        val n = name(gld, r.key)
                        n to { _: Int, _: Message ->
                            SelectablePaginator(setOf(event.author), title = "$n Reports",
                                description = "Select a report to remove it.",
                                items = r.value.map { s ->
                                    s to { i: Int, m: Message ->
                                        event.reply(
                                            "Remove report? (``yes/no``)\n*$s*") { qm ->
                                            WAITER.waitForEvent(
                                                MessageReceivedEvent::class.java, {
                                                    it.isValidUser(gld, channel = event.channel)
                                                            && it.message.contentDisplay.matchesAny(
                                                        REG_YES, REG_NO)
                                                }, {
                                                    val display = it.message.contentDisplay
                                                    if (display.matches(REG_YES)) {
                                                        r.value.removeAt(i)
                                                        event.reply("Removed") {
                                                            it.delete()
                                                                .queueAfter(5, SECONDS)
                                                            qm.delete()
                                                                .queueAfter(5, SECONDS)
                                                        }
                                                    } else if (display.matches(REG_NO)) {
                                                        event.reply("Cancelled") {
                                                            it.delete()
                                                                .queueAfter(5, SECONDS)
                                                            qm.delete()
                                                                .queueAfter(5, SECONDS)
                                                        }
                                                    }
                                                }, 2L, MINUTES, {
                                                    event.reply("Cancelled") {
                                                        it.delete().queueAfter(5, SECONDS)
                                                        qm.delete().queueAfter(5, SECONDS)
                                                    }
                                                })
                                        }
                                    }
                                }).display(event.channel)
                        }
                    }).display(event.channel)
            } else event.respondThenDelete("There are no user reports to see.")
        } else {
            //Report actions
            STAT.track(this, bot, event.author, event.creationTime)
            if (mentions.isEmpty())
                return event.respondThenDelete("No user was mentioned in the report.")

            val reas = event.args.split(Regex("\\s+"))
                .filterNot { it.matchesAny(userMentionRegex) }.joinToString(" ")
            if (reas.isBlank()) return event.respondThenDelete("No reason given.")

            if (modData.hierarchicalReports)
                mentions.removeAll{ it outRanks event.member || it hasPerm ADMINISTRATOR }

            mentions.forEach { member ->
                val reps = modData.reports.getOrPut(member.user.idLong){mutableListOf()}
                reps.add(reas)
                if (modData.reportLimit == -1 || reps.size < modData.reportLimit)
                    return@forEach
                modData.reportLimitActions.forEach { action ->
                    when (action) {
                        KICK -> {
                            event.guild.controller.kick(member)
                                .reason("Hit the maximum community reports").queue({
                                bot.settings.sendLog("""
                                        User kicked: ${member.user.asMention}
                                        Reached max community reports (${modData.reportLimit})
                                        """.trimIndent())
                            }, {
                                bot.settings.sendLog("""
                                        Unable to kick: ${member.user.asMention}
                                        Reached max community reports (${modData.reportLimit})
                                        """.trimIndent())
                            })
                        }
                        BAN -> {
                            event.guild.controller.ban(member, 1,
                                "Hit the maximum community reports").queue({
                                bot.settings.sendLog("""
                                        User banned: ${member.user.asMention}
                                        Reached max community reports (${modData.reportLimit})
                                        """.trimIndent())
                            }, {
                                bot.settings.sendLog("""
                                        Unable to ban: ${member.user.asMention}
                                        Reached max community reports (${modData.reportLimit})
                                        """.trimIndent())
                            })
                        }
                        NOTIFY_USER -> member.user.openPrivateChannel().queue {
                            it.sendMessage(makeEmbedBuilder(
                                "You hit the Report Limit! $Warning",null,
                                "You have hit the maximum number of Reports " +
                                        "in **${gld.name}**").setColor(STD_RED).build())
                                .queueIgnore()
                        }
                        NOTIFY_ADMINS -> {
                            val admins = gld.members.filter {
                                (it.isOwner /**|| it hasPerm ADMINISTRATOR*/)
                                        && !it.user.isBot
                            }
                            bot.settings.sendLog("""
                                    User reached maximum community reports!!
                                    $Warning User: ${member.asMention}
                                """.trimIndent(), admins)
                        }
                    }
                }
            }
            val rep = """Reported: *${mentions.joinToString(", ") { it.effectiveName }}*
                | Reason: $reas""".trimMargin()
            event.replyWarning(rep)
            bot.settings.sendLog(
                "${event.member.effectiveName} (ID: ${event.author.id}) $rep")
        }

    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("User Reports",
            "Allow members to help moderate by reporting users.")
            .addField("Report User", "``<@User> [@Users...] <reason>``", true)
            .addField("See Reports", "``report -s``", true)
            .build()
    }

}
