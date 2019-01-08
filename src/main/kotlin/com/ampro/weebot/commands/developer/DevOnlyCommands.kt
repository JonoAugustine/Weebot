/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

/**
 * This file holds [WeebotCommand]s for Shutdown, GuildList, and Ping
 */

import com.ampro.weebot.*
import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.PHONE_JONO
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.X_Red
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.reflect.KClass


/**
 * Shuts down the Weebots.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdShutdown : WeebotCommand("shutdown", arrayOf("tite", "killbot", "devkill"),
    CAT_DEV, "", "Shutdown the weebot", hidden = true, ownerOnly = true
) {

    override fun execute(event: CommandEvent) = runBlocking {
        event.reactWarning()
        event.reply("Shutting down all Weebots...")
        sendSMS(PHONE_JONO, "WEEBOT: Shutting Down")
        delay(2_000)
        shutdown(event.author)
    }
}

class CmdStatsView : WeebotCommand("stats", arrayOf("viewstats", "statsview",
    "statview", "viewstat"), CAT_DEV, "[commandName...]", "View Weebot statistics",
    hidden = true, ownerOnly = true) {
    override fun execute(event: CommandEvent) { GlobalScope.launch(CACHED_POOL) {
        val stats = STAT.commandUsage
        val bots = JDA_SHARD_MNGR.guilds.map { getWeebotOrNew(it) }
        val enabled: List<Boolean> = bots.map { it.settings.trackingEnabled }
        if (stats.isEmpty()) {
            if (enabled.none { it }) {
                event.reply("No Weebots have Tracking Enabled.")
            } else event.reply("No Statistics available.")
            return@launch
        }

        val restrictions = HashMap<KClass<out WeebotCommand>, AtomicInteger>()

        bots.map { it.settings.commandRestrictions.filter { it.value.guildWide }
            .forEach { restrictions.getOrPut(it.key) {AtomicInteger(0)}.incrementAndGet() }
        }

        if (restrictions.isNotEmpty()) {
            val rList = restrictions.map { it.key to it.value.get() }
                .sortedBy { it.second }.map { "**${it.first.simpleName}:** ${it.second}" }

            strdPaginator.useNumberedItems(true).setText("Blocked Command Stats")
                .setUsers(event.author).setItemsPerPage(6).apply {
                    rList.forEach { addItems(it) }
                }.build().display(event.channel)
        }

        val size = enabled.filter { it }.size
        val perc = ceil((size / enabled.size.toDouble()) * 100)

        strdPaginator.useNumberedItems(true).setText("Usage Stats from $size ($perc)")
            .setUsers(event.author).setItemsPerPage(6).apply {
                stats.map {
                    """${it.key}:
                    ${it.value.summarize()}
                """.trimIndent()
                }.forEach { addItems(it) }
            }.build().display(event.channel)
    }}
}

/**
 *
 * @author John Grosh (jagrosh)
 * @since 2.0
 */
class CmdGuildList : WeebotCommand("guildlist",
    arrayOf("guilds", "serverlist", "servers"), CAT_DEV, "[-s <[joindate][size]>]",
    "Gets a paginated list of the guilds the bot is on.",
        HelpBiConsumerBuilder("Guild List")
            .setDescription("Gets a paginated list of the guilds the bot is on.").build(),
        ownerOnly = true, userPerms = arrayOf(MESSAGE_EMBED_LINKS), hidden = true,
        botPerms = arrayOf(MESSAGE_EMBED_LINKS, MESSAGE_ADD_REACTION)
) {

    val REG_DATE = Regex("(?i)-(d+a*t*e*)")

    override fun execute(event: CommandEvent) {
        val gs = JDA_SHARD_MNGR.guilds
        val sortByDate = event.args.matches(REG_DATE)

        SelectablePaginator(setOf(event.author),
            title = "All guilds housing ${SELF.name} (Total: ${gs.size})",
            description = "``Name ~ member count (percentage non-bots) on shardID``",
            items = gs.sortedByDescending { if (sortByDate)
                it.selfMember.joinDate.until(NOW(), ChronoUnit.MINUTES) * -1
            else it.trueSize.toLong() }.map {
                "**${it.name}** ~ ${it.size} (${((
                        it.trueSize/it.size.toDouble())*100).roundToInt()}%)" +
                        " on ${getGuildShard(it)?.shardId ?: "N/A"}" to {
                        _: Int, _: Message -> it.infoEmbed(event).display(event.channel)
                }
            }, itemsPerPage = 10)
            .display(event.channel)

    }

    private fun getGuildShard(g: Guild): JDA.ShardInfo?
        = JDA_SHARD_MNGR.shards.find { s -> s.guilds.has { it.id == g.id } }?.shardInfo

}

fun Guild.infoEmbed(event: CommandEvent): SelectableEmbed {
    val gAge = ChronoUnit.SECONDS.between(creationTime, event.creationTime)
    val wAge = ChronoUnit.SECONDS.between(selfMember.joinDate, event.creationTime)
    return SelectableEmbed(event.author, true, makeEmbedBuilder("Guild: $name", null, """
        **Weebot Join Date:**   ${selfMember.joinDate.format(DD_MM_YYYY_HH_MM)}
        **Weebot Age:** ${wAge.formatTime()}
        **Guild Age:** ${gAge.formatTime()}
        **Size:**     $size
        **TrueSize**: $trueSize (**Bot Count**: ${size - trueSize})
        **Roles:**          ${roles.size}
        **Text Channels:**  ${textChannels.size}
        **Voice Channels:** ${voiceChannels.size}
        **Custom Emotes:**  ${emotes.size}
    """.trimIndent()).setThumbnail(iconUrl).addField("Options", """
        $X_Red Remove Weebot from this guild""".trimIndent(), false)
        .setColor(if (roles.isNotEmpty()) this.roles[0].color else STD_GREEN).build(),
        listOf(X_Red to { _: Message, _: User ->
            event.reply("Are you sure? (yes/no) (30 sec timeout)")
            WAITER.waitForEvent(MessageReceivedEvent::class.java,
                { e -> e.isValidUser(event.guild, setOf(event.author))}, {
                    if (it.message.contentDisplay.matches(REG_YES)) {
                        event.reply("Reason? (send ``null`` if nothing)")
                        WAITER.waitForEvent(MessageReceivedEvent::class.java,
                            { e -> e.isValidUser(event.guild, setOf(event.author))}, {
                                if (!it.message.contentDisplay.matches(Regex("(?i)null"))) {
                                    val g = getGuild(this.idLong)//update guild info
                                    if (g != null) {
                                        getWeebotOrNew(g).settings.sendLog(it.message)
                                        g.leave().queue {
                                            event.reply("*Weebot left ${g.name}*")
                                        }
                                    }
                                }
                            }, 2, MINUTES) {
                            event.reply("*Removal cancelled (timed out)*")
                        }
                    } else if (it.message.contentDisplay.matches(REG_NO)) {
                        event.reply("*Removal cancelled*")
                    }
                }, 30, SECONDS) { event.reply("*Removal cancelled (timed out)*") }
        })) { it.clearReactions().queueAfter(250, MILLISECONDS) }
}
