/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */
package com.ampro.weebot.commands.developer

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.doc.standard.CommandInfo
import com.jagrosh.jdautilities.doc.standard.Error
import com.jagrosh.jdautilities.doc.standard.RequiredPermissions
import com.jagrosh.jdautilities.examples.doc.Author
import com.jagrosh.jdautilities.menu.Paginator
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.exceptions.PermissionException

import java.awt.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 *
 * @author John Grosh (jagrosh)
 */
@CommandInfo(name = arrayOf("Guildlist"),
    description = "Gets a paginated list of the guilds the bot is on.",
    requirements = arrayOf("The bot has all necessary permissions.",
        "The user is the bot's owner."))
@Error(value = "If arguments are provided, but they are not an integer.",
    response = "[PageNumber] is not a valid integer!")
@RequiredPermissions(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION)
@Author("John Grosh (jagrosh)")
class GuildlistCommand(waiter: EventWaiter) : Command() {

    private val pbuilder: Paginator.Builder

    init {
        this.name = "guildlist"
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
