/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.CACHED_POOL
import com.ampro.weebot.JDA_SHARD_MNGR
import com.ampro.weebot.SELF
import com.ampro.weebot.WAITER
import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.getGuild
import com.ampro.weebot.extensions.CLR_GREEN
import com.ampro.weebot.extensions.SelectableEmbed
import com.ampro.weebot.extensions.SelectablePaginator
import com.ampro.weebot.extensions.TODO
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.isValidUser
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.extensions.size
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.extensions.trueSize
import com.ampro.weebot.extensions.unit
import com.ampro.weebot.shutdown
import com.ampro.weebot.util.DD_MM_YYYY_HH_MM
import com.ampro.weebot.util.Emoji.X_Red
import com.ampro.weebot.util.NOW
import com.ampro.weebot.util.REG_NO
import com.ampro.weebot.util.REG_YES
import com.ampro.weebot.util.formatTime
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToInt


/**
 * This file holds [WeebotCommand]s for Shutdown, GuildList, and Ping
 */

/**
 * Shuts down the Weebots.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdShutdown : WeebotCommand(
    "shutdown", "SHUTDOWN", null,
    arrayOf("tite", "killbot", "devkill"),
    CAT_DEV, "Shutdown the weebot",
    hidden = true, ownerOnly = true
) {

    override fun execute(event: WeebotCommandEvent) = runBlocking {
        event.reactWarning()
        event.reply("Shutting down all Weebots...")
        //sendSMS(PHONE_JONO, "WEEBOT: Shutting Down")
        delay(2_000)
        shutdown(event.author)
    }
}

class CmdStatsView : WeebotCommand(
    "stats", "STATS", null,
    arrayOf("viewstats", "statsview", "statview", "viewstat"),
    CAT_DEV, "View Weebot statistics",
    hidden = true, ownerOnly = true,
    execution = {
        println(argList)
        // TODO List all commands or parse requested command and display
        // stat plot summaries
        TODO(this)
    }
)


/**
 *
 * @author
 * @since 2.0
 */
class CmdGuildList : WeebotCommand(
    "guildlist",
    "ALLGUILDS",
    null,
    arrayOf("guilds", "servers"),
    CAT_DEV,
    "Gets a paginated list of the guilds the bot is on.",
    HelpBiConsumerBuilder("Guild List")
        .setDescription("Gets a paginated list of the guilds the bot is on.").build(),
    ownerOnly = true,
    userPerms = arrayOf(MESSAGE_EMBED_LINKS),
    hidden = true,
    botPerms = arrayOf(MESSAGE_EMBED_LINKS, MESSAGE_ADD_REACTION)
) {

    val REG_DATE = Regex("(?i)-(d+a*t*e*)")

    override fun execute(event: WeebotCommandEvent) {
        val gs = JDA_SHARD_MNGR.guilds
        val sortByDate = event.args.matches(REG_DATE)

        SelectablePaginator(setOf(event.author),
            title = "All guilds housing ${SELF.name} (Total: ${gs.size})",
            description = """``Name ~ member count (percentage non-bots) on shardID``
                Total Unique Users: ${String.format("%,d", JDA_SHARD_MNGR.users.size)}
                Total Shards: ${JDA_SHARD_MNGR.shards.size}
                """.trimIndent(),
            items = gs.asSequence().sortedByDescending {
                if (sortByDate)
                    it.selfMember.joinDate.until(NOW(), ChronoUnit.MINUTES) * -1
                else it.trueSize.toLong()
            }.map {
                "**${it.name}** ~ ${it.size} (${((
                    it.trueSize / it.size.toDouble()) * 100).roundToInt()}%)" +
                    " on ${getGuildShard(it)?.shardId
                        ?: "N/A"}" to { _: Int, _: Message ->
                    it.infoEmbed(event).display(event.channel)
                }
            }.toList(), itemsPerPage = 10)
            .display(event.channel)

    }

    private fun getGuildShard(g: Guild): JDA.ShardInfo? =
        JDA_SHARD_MNGR.shards.find { s -> s.guilds.any { it.id == g.id } }?.shardInfo

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
        .setColor(if (roles.isNotEmpty()) this.roles[0].color else CLR_GREEN).build(),
        listOf(X_Red to { _: Message, _: User ->
            event.reply("Are you sure? (yes/no) (30 sec timeout)")
            WAITER.waitForEvent(MessageReceivedEvent::class.java,
                { e -> e.isValidUser(event.guild, setOf(event.author)) }, {
                    if (it.message.contentDisplay.matches(REG_YES)) {
                        event.reply("Reason? (send ``null`` if nothing)")
                        WAITER.waitForEvent(MessageReceivedEvent::class.java,
                            { e -> e.isValidUser(event.guild, setOf(event.author)) }, {
                                if (!it.message.contentDisplay.matches(
                                        Regex("(?i)null"))) {
                                    val g = getGuild(this.idLong) //update guild info
                                    if (g != null) {
                                        g.bot.settings.sendLog(it.message)
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
