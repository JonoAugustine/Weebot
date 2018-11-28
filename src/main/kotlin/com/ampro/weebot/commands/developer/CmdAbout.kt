/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */
package com.ampro.weebot.commands.developer

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo
import com.jagrosh.jdautilities.doc.standard.CommandInfo
import com.jagrosh.jdautilities.examples.doc.Author
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDAInfo
import net.dv8tion.jda.core.Permission
import org.slf4j.LoggerFactory
import java.awt.Color

/**
 *
 * @author John Grosh (jagrosh)
 */
@CommandInfo(name = arrayOf("About"), description = "Gets information about the bot.")
@Author("John Grosh (jagrosh)")
class CmdAbout(private val color: Color, private val description: String,
               private val features: Array<String>, vararg perms: Permission) :
        Command() {
    private var IS_AUTHOR = true
    private var REPLACEMENT_ICON = "+"
    private val perms: Array<Permission>
    private var oauthLink: String? = null

    init {
        this.name = "about"
        this.help = "shows info about the bot"
        this.guildOnly = false
        this.perms = perms as Array<Permission>
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    fun setIsAuthor(value: Boolean) {
        this.IS_AUTHOR = value
    }

    fun setReplacementCharacter(value: String) {
        this.REPLACEMENT_ICON = value
    }

    override fun execute(event: CommandEvent) {
        if (oauthLink == null) {
            try {
                val info = event.jda.asBot().applicationInfo.complete()
                oauthLink = if (info.isBotPublic) info.getInviteUrl(0L, *perms) else ""
            } catch (e: Exception) {
                val log = LoggerFactory.getLogger("OAuth2")
                log.error("Could not generate invite link ", e)
                oauthLink = ""
            }

        }
        val builder = EmbedBuilder()
        builder.setColor(if (event.guild == null) color else event.guild.selfMember.color)
        builder.setAuthor("All about " + event.selfUser.name + "!", null,
            event.selfUser.avatarUrl)
        val join = !(event.client.serverInvite == null || event.client.serverInvite.isEmpty())
        val inv = !oauthLink!!.isEmpty()
        val invline = ("\n" + (if (join) "Join my server [`here`](" + event.client.serverInvite + ")" else if (inv) "Please " else "") + (if (inv) (if (join) ", or " else "") + "[`invite`](" + oauthLink + ") me to your server" else "") + "!")
        val author = if (event.jda.getUserById(
                    event.client.ownerId) == null) "<@" + event.client.ownerId + ">"
        else event.jda.getUserById(event.client.ownerId).name
        val descr = StringBuilder().append("Hello! I am **").append(event.selfUser.name)
            .append("**, ").append(description).append("\nI ")
            .append(if (IS_AUTHOR) "was written in Java" else "am owned").append(" by **")
            .append(author)
            .append(
                "** using " + JDAUtilitiesInfo.AUTHOR + "'s [Commands Extension](" + JDAUtilitiesInfo.GITHUB + ") (")
            .append(JDAUtilitiesInfo.VERSION)
            .append(") and the [JDA library](https://github.com/DV8FromTheWorld/JDA) (")
            .append(JDAInfo.VERSION).append(")\nType `")
            .append(event.client.textualPrefix).append(event.client.helpWord)
            .append("` to see my commands!").append(if (join || inv) invline else "")
            .append("\n\nSome of my features include: ```css")
        for (feature in features) descr.append("\n").append(
            if (event.client.success.startsWith(
                        "<")) REPLACEMENT_ICON else event.client.success).append(
            " ").append(feature)
        descr.append(" ```")
        builder.setDescription(descr)
        if (event.jda.shardInfo == null) {
            builder.addField("Stats",
                event.jda.guilds.size.toString() + " servers\n1 shard", true)
            builder.addField("Users",
                event.jda.users.size.toString() + " unique\n" + event.jda.guilds.stream().mapToInt { g -> g.members.size }.sum() + " total",
                true)
            builder.addField("Channels",
                event.jda.textChannels.size.toString() + " Text\n" + event.jda.voiceChannels.size + " Voice",
                true)
        } else {
            builder.addField("Stats",
                event.client.totalGuilds.toString() + " Servers\nShard " + (event.jda.shardInfo.shardId + 1) + "/" + event.jda.shardInfo.shardTotal,
                true)
            builder.addField("This shard",
                event.jda.users.size.toString() + " Users\n" + event.jda.guilds.size + " Servers",
                true)
            builder.addField("",
                event.jda.textChannels.size.toString() + " Text Channels\n" + event.jda.voiceChannels.size + " Voice Channels",
                true)
        }
        builder.setFooter("Last restart", null)
        builder.setTimestamp(event.client.startTime)
        event.reply(builder.build())
    }

}
