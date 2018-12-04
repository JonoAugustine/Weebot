/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

/**
 * This file holds [WeebotCommand]s for Shutdown, GuildList, and Ping
 */

import com.ampro.weebot.commands.*
import com.ampro.weebot.main.shutdown
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.doc.standard.*
import com.jagrosh.jdautilities.examples.command.ShutdownCommand
import com.jagrosh.jdautilities.examples.doc.Author
import com.jagrosh.jdautilities.menu.Paginator
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.exceptions.PermissionException
import java.awt.Color
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
class PingCommand : WeebotCommand("ping", arrayOf("pong"), CAT_DEV,
    "", "Checks the bot's latency.", HelpBiConsumerBuilder("Ping ~ Pong")
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

    override fun execute(event: CommandEvent) {
        event.reactWarning()
        event.reply("Shutting down all Weebots...")
        //TODO send text to Jono
        shutdown(event.author)
    }
}

/**
 * TODO Customize
 * @author John Grosh (jagrosh)
 */
@CommandInfo(name = arrayOf("Guildlist"),
    description = "Gets a paginated list of the guilds the bot is on.",
    requirements = arrayOf("The bot has all necessary permissions.",
        "The user is the bot's owner."))
@Error(value = "If arguments are provided, but they are not an integer.",
    response = "[PageNumber] is not a valid integer!")
@RequiredPermissions(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION)

class GuildlistCommand(waiter: EventWaiter) : WeebotCommand("guildlist",
    arrayOf("guilds", "serverlist", "servers"), CAT_DEV, "",
    "Gets a paginated list of the guilds the bot is on."
) {

    private val pbuilder: Paginator.Builder

    init {
        this.name = "guildlist"
        aliases = arrayOf("listguilds")
        this.help = "shows the list of guilds the bot is on"
        this.arguments = "[pagenum]"
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ADD_REACTION)
        this.guildOnly = false
        this.ownerCommand = true
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
            try {
                page = Integer.parseInt(event.args)
            } catch (e: NumberFormatException) {
                event.reply(
                    event.client.error + " `" + event.args + "` is not a valid integer!")
                return
            }

        }
        pbuilder.clearItems()
        event.jda.guilds.stream()
            .map { g -> "**" + g.name + "** (ID:" + g.id + ") ~ " + g.members.size + " Members" }
            .forEach(Consumer<String> { pbuilder.addItems(it) })
        val p = pbuilder.setColor(if (event.isFromType(
                    ChannelType.TEXT)) event.selfMember.color else Color.black)
            .setText(
                event.client.success + " Guilds that **" + event.selfUser.name + "** is connected to" + if (event.jda.shardInfo == null) ":" else "(Shard ID " + event.jda.shardInfo.shardId + "):")
            .setUsers(event.author).build()
        p.paginate(event.channel, page)
    }

}
