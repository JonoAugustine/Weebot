/**
 *
 */

package com.ampro.weebot.commands

import com.ampro.weebot.*
import com.ampro.weebot.commands.`fun`.CmdHelloThere
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.WKDAY_MONTH_YEAR_TIME
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.menu.OrderedMenu
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Role.DEFAULT_COLOR_RAW
import java.util.concurrent.TimeUnit.MINUTES

const val HELLO_THERE = "https://www.youtube.com/watch?v=rEq1Z0bjdwc"
const val AMPRO       = "https://www.aquaticmasteryproductions.com/"
const val HQTWITCH    = "https://www.twitch.tv/hqregent"

/**
 * Send an [MessageEmbed] giving information about Weebot's.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdAbout : WeebotCommand("about", emptyArray(), CAT_GEN,
    "[me/more]", "Get information about Weebot.",
        children = arrayOf(CmdAboutUser(), CMD_HELP), cooldown = 90
) {
    //"Weebot's little brother."
    override fun execute(event: CommandEvent) {
        val bot = if (event.isFromType(ChannelType.PRIVATE)) DAO.GLOBAL_WEEBOT
        else getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author)
        val eb = strdEmbedBuilder.setTitle("All about Weebot")
            .setThumbnail(CmdHelloThere.HELLO_THERE_GIFS[0])

        //Description
        val sBuilder = StringBuilder("[`Hello there!`]($HELLO_THERE) ").append(
            "I am ***Weebot***, a bot, that ")
            .append("is certainly not a weeb, no sir. No weebs here.\n")
            .append("I Have a bunch of fun, useful, and sometimes random commands ")
            .append("that aim to make life on Discord easy, fun, and intuitive!\n\n")
            .append("I was made by ***[`HQRegent`]($HQTWITCH)***, ")
            .append("using [`Kotlin`](https://kotlinlang.org) and the ")
            .append("[`JDA library`](https://github.com/DV8FromTheWorld/JDA).")
            .append("\nIf you need more Help using Weebot, want help ")
            .append("using JDA to make your own bot, or just want to say hi to ")
            .append("***[`HQRegent`]($HQTWITCH)***, join the ")
            .append("[`Numberless Liquidator Discord`](https://discord.gg/VdbNyxr).")
            .append("\n\nPlease [`invite me to your server!`]($LINK_INVITEBOT) and")
            .append(" vote for me on [`discordbots.org`]($LINK_DISCORD_BOTS) and ")
            .append("[`DiscordBotList.com`]($LINK_DISCORD_BOTS_LIST)!")
            .append("\n\n*Use \"${SELF.asMention} help\" for info using my commands.*")
            .append("\n\n**__Weebot Commands__**\n\n")

        sBuilder.append(commands.sortedBy { it.name.toLowerCase() }
            .filterNot { it.isOwnerCommand }.joinToString(", ") { "*${it.name}*" })
        sBuilder.setLength(sBuilder.length - 2)
        sBuilder.append("\n\n")
        eb.setDescription(sBuilder.toString())
        sBuilder.setLength(0)

        //Global stats (server count, shard count)
        //Shard-level stats (User count, server count)
        if (event.jda.shardInfo == null) {
            eb.addField("Stats", "${event.jda.guilds.size} servers\n1 shard", true)
            eb.addField("Users",
                "${event.jda.users.size} unique\n${event.jda.guilds.stream()
                    .mapToInt { g -> g.members.size }.sum()} total", true)
            eb.addField("Channels",
                "${event.jda.textChannels.size} Text\n${event.jda.voiceChannels.size} Voice",
                true)
        } else {
            eb.addField("Stats",
                "${JDA_SHARD_MNGR.guilds.size} Servers\nWeebot Centre ${event.jda.shardInfo.shardId + 1}/${event.jda.shardInfo.shardTotal}",
                true)
            eb.addField("This Weebot Centre",
                "${event.jda.users.size} Users on ${event.jda.guilds.size} Servers", true)
            eb.addField("Channels",
                "${event.jda.textChannels.size} Text Channels\n${event.jda.voiceChannels.size} Voice Channels",
                true)
        }
        eb.setFooter("Last restart", null)
        eb.setTimestamp(event.client.startTime)

        //other (?)

        event.reply(eb.build())
    }

}


/**
 * Send a [MessageEmbed] giving info about the user.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdAboutUser : WeebotCommand("aboutme", arrayOf("me"), CAT_GEN,
    "", "Get information about Weebot.", cooldown = 90, guildOnly = true
) {
    override fun execute(event: CommandEvent) {
        val bot = if (event.isFromType(ChannelType.PRIVATE)) DAO.GLOBAL_WEEBOT
        else getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author)
        val roles = event.member.roles
        event.reply(
                strdEmbedBuilder.apply {
                    if (roles.isNotEmpty() && roles[0].colorRaw != DEFAULT_COLOR_RAW)
                        setColor(event.member.roles[0].colorRaw)
                    setThumbnail(event.author.avatarUrl)
                    addField("ID", """
                        ***Name:***     ${event.author.name} #${event.author.discriminator}
                        ***Nickname:*** ${event.member.effectiveName}
                        ***ID:***       ${event.author.id}
                    """.trimIndent(), true)
                    addField("Account Created", event.author.creationTime.format(
                            WKDAY_MONTH_YEAR_TIME), true)
                    addField("Join Date", event.member.joinDate.format(WKDAY_MONTH_YEAR_TIME), true)
                    addField("Shared Weebot Servers: ${event.author.mutualGuilds.size}", "", true)
                    addField("${roles.size} Roles", if (roles.isEmpty()) "" else
                        roles.joinToString(", ", transform = { it.name }), true)
                }.build()
        )
    }
}


/**
 * Send an [MessageEmbed] giving help with Weebot Commands.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdHelp : WeebotCommand("help", arrayOf("helpo", "more"), CAT_GEN,
        "[category] [command name]", "Get information about Weebot Commands and Usage.",
        cooldown = 90, guildOnly = false
) {
    public override fun execute(event: CommandEvent) {
        val bot = if (event.isFromType(ChannelType.PRIVATE)) DAO.GLOBAL_WEEBOT
        else getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author)
        OrderedMenu.Builder().setEventWaiter(WAITER).setUsers(event.author)
            .useCancelButton(true).setDescription("Weebot Help").apply {
                addChoice("All")
                categories.forEach { addChoice(it.name) }
            }.setTimeout(1, MINUTES).setSelection { _, i ->
                event.delete()
                if (i == 1) { //if ALL
                    strdPaginator.setText("All Weebot Commands").setUsers(event.author)
                        .apply {
                            commands.filterNot { it.isHidden || it.isOwnerCommand }
                                .sortedBy { it.name }
                                .forEach {
                                    val ali = if (it.aliases.isNotEmpty()) {
                                        "\n*Aliases: ${it.aliases.joinToString(", ")}*"
                                    } else ""
                                    addItems("**${it.name}**\n${it.help}$ali\n")
                                }
                        }.build().apply {
                            event.author.openPrivateChannel().queue {
                                paginate(it, 1)
                            }
                        }
                    return@setSelection
                }
                val cat = categories[i - 2]
                strdPaginator.setText("Weebot's ${cat.name} Commands")
                    .setUsers(event.author).apply {
                        commands.filter { it.category == cat && !it.isHidden }
                            .sortedBy { it.name }.forEach {
                                val ali = if (it.aliases.isNotEmpty()) {
                                    "\n*Aliases: ${it.aliases.joinToString(", ")}*"
                                } else ""
                                addItems("**${it.name}**\n${it.help}$ali\n")
                            }
                    }.build().apply {
                        event.author.openPrivateChannel().queue {
                            paginate(it, 1)
                        }
                    }
            }.build().display(event.channel)
    }
}
