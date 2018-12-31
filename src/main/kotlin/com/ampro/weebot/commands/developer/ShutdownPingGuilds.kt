/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

/**
 * This file holds [WeebotCommand]s for Shutdown, GuildList, and Ping
 */

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.commands.CAT_GEN
import com.ampro.weebot.database.constants.PHONE_JONO
import com.ampro.weebot.database.getGuild
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.main.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.X_Red
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.*
import kotlin.math.roundToInt

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
class PingCommand : WeebotCommand("ping", arrayOf("pong"), CAT_GEN,
    "", "Checks the bot's latency.", HelpBiConsumerBuilder("Ping ~ Pong", false)
        .setDescription("Checks the bot's latency.").build(), false, cooldown = 10
) {
    override fun execute(event: CommandEvent) {
        val r = if (event.getInvocation().toLowerCase() == "pong") "Ping" else "Pong"
        event.reply("$r: ...") { m ->
            val ping = event.message.creationTime.until(m.creationTime, ChronoUnit.MILLIS)
            m.editMessage("$r! :ping_pong: Ping: " + ping + "ms | Websocket: "
                    + event.jda.ping + "ms").queue()
        }
    }
}

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

/**
 *
 * @author John Grosh (jagrosh)
 * @since 2.0
 */
class GuildlistCommand : WeebotCommand("guildlist",
    arrayOf("guilds", "serverlist", "servers"), CAT_DEV, "[pagenum]",
    "Gets a paginated list of the guilds the bot is on.",
        HelpBiConsumerBuilder("Guild List")
            .setDescription("Gets a paginated list of the guilds the bot is on.").build(),
        ownerOnly = true, userPerms = arrayOf(MESSAGE_EMBED_LINKS), hidden = true,
        botPerms = arrayOf(MESSAGE_EMBED_LINKS, MESSAGE_ADD_REACTION)
) {

    override fun execute(event: CommandEvent) {
        val op = JDA_SHARD_MNGR.guilds
        SelectablePaginator(setOf(event.author),
            title = "All guilds housing ${SELF.name} (Total: ${op.size})",
            description = "``Name ~ member count (percentage non-bots) on shardID``",
            items = op.sortedByDescending { it.trueSize }.map {
                "**${it.name}** ~ ${it.size} (${((
                        it.trueSize/it.size.toDouble())*100).roundToInt()}%)" +
                        " on ${getGuildShard(it)?.shardId ?: "N/A"}" to {
                        _: Int, _: Message -> it.infoEmbed(event).display(event.channel)
                }
            }, itemsPerPage = 10).display(event.channel)

    }

    private fun getGuildShard(g: Guild): JDA.ShardInfo?
        = JDA_SHARD_MNGR.shards.find { s -> s.guilds.has { it.id == g.id } }?.shardInfo

}

fun Guild.infoEmbed(event: CommandEvent): SelectableEmbed {
    return SelectableEmbed(event.author, makeEmbedBuilder("Guild: $name", null, """
        **Weebot Join Date:**   ${selfMember.joinDate.format(DD_MM_YYYY_HH_MM)}
        **Size:**     $size
        **TrueSize**: $trueSize (**Bot Count**: ${size - trueSize})
        **Roles:**          ${roles.size}
        **Text Channels:**  ${textChannels.size}
        **Voice Channels:** ${voiceChannels.size}
        **Custom Emotes:**  ${emotes.size}
    """.trimIndent()).setThumbnail(iconUrl).addField("Options", """
        $X_Red Remove Weebot from this guild""".trimIndent(), false)
        .setColor(if (roles.isNotEmpty()) this.roles[0].color else STD_GREEN).build(),
        listOf(X_Red to { _: Message ->
            event.reply("Are you sure? (yes/no) (30 sec timeout)")
            //TODO wait for confirm and then wait for reason
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
