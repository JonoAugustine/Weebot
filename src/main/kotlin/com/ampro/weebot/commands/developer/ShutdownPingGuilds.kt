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
import com.ampro.weebot.extensions.STD_GREEN
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.getInvocation
import com.ampro.weebot.main.shutdown
import com.ampro.weebot.util.sendSMS
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.menu.Paginator
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import net.dv8tion.jda.core.exceptions.PermissionException
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

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
class GuildlistCommand(waiter: EventWaiter) : WeebotCommand("guildlist",
    arrayOf("guilds", "serverlist", "servers"), CAT_DEV, "[pagenum]",
    "Gets a paginated list of the guilds the bot is on.",
        HelpBiConsumerBuilder("Guild List")
            .setDescription("Gets a paginated list of the guilds the bot is on.").build(),
        ownerOnly = true, userPerms = arrayOf(MESSAGE_EMBED_LINKS), hidden = true,
        botPerms = arrayOf(MESSAGE_EMBED_LINKS, MESSAGE_ADD_REACTION)
) {

    private val pbuilder: Paginator.Builder

    init {
        pbuilder = Paginator.Builder().setColumns(1).setItemsPerPage(10)
            .showPageNumbers(true).waitOnSinglePage(false).useNumberedItems(false)
            .setFinalAction { m ->
                try {
                    m.clearReactions().queue()
                } catch (ex: PermissionException) {
                    m.delete().queue()
                }
            }.setEventWaiter(waiter).setTimeout(1, TimeUnit.MINUTES)
    }

    override fun execute(event: CommandEvent) {
        var page = 1
        if (!event.args.isEmpty()) {
            page = try {
                Integer.parseInt(event.args)
            } catch (e: NumberFormatException) {
                event.reply("${event.client.error} `${event.args}` is not a valid integer!")
                return
            }
        }
        pbuilder.clearItems()
        event.jda.guilds.stream()
            .map { g -> "**${g.name}** (ID:${g.id}) ~ ${g.members.size} Members" }
            .forEach { pbuilder.addItems(it) }
        val p = pbuilder.setColor(STD_GREEN)
            .setText("${event.client.success} Guilds that **${event.selfUser.name}**" +
                    " is connected to ${if (event.jda.shardInfo == null) ":"
                    else "(Shard ID " + event.jda.shardInfo.shardId + "):"}")
            .setUsers(event.author).build()
        p.paginate(event.channel, page)
    }

}
