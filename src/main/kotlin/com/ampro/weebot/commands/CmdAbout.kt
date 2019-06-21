/**
 *
 */

package com.ampro.weebot.commands

import com.ampro.weebot.CMD_CLIENT
import com.ampro.weebot.GlobalWeebot
import com.ampro.weebot.JDA_SHARD_MNGR
import com.ampro.weebot.SELF
import com.ampro.weebot.commands.`fun`.CmdHelloThere
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.constants.LINK_DISCORD_BOTS
import com.ampro.weebot.database.constants.LINK_DISCORD_BOTS_LIST
import com.ampro.weebot.database.constants.LINK_INVITEBOT
import com.ampro.weebot.database.track
import com.ampro.weebot.extensions.EMBED_MAX_FIELD_VAL
import com.ampro.weebot.extensions.SelectablePaginator
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.extensions.size
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.extensions.strdPaginator
import com.ampro.weebot.extensions.trueSize
import com.ampro.weebot.games
import com.ampro.weebot.util.WKDAY_MONTH_YEAR_TIME
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_CHANNEL
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_SHARD
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.Game.GameType.DEFAULT
import net.dv8tion.jda.core.entities.Game.GameType.LISTENING
import net.dv8tion.jda.core.entities.Game.GameType.STREAMING
import net.dv8tion.jda.core.entities.Game.GameType.WATCHING
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Role.DEFAULT_COLOR_RAW
import kotlin.math.ceil

const val HELLO_THERE = "https://www.youtube.com/watch?v=rEq1Z0bjdwc"
const val AMPRO       = "https://www.aquaticmasteryproductions.com/"
const val LINK_HQTWITCH    = "https://www.twitch.tv/hqregent"

/**
 * Send an [MessageEmbed] giving information about Weebot's.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdAbout : WeebotCommand(
    "about", "ABOUT", "About", arrayOf("info"), CAT_GEN,
    "Get information about Weebot.", cooldown = 90,
    children = arrayOf(SubCmdAboutUser(), SubCmdAboutGuild(), CMD_HELP)
) {

    override fun execute(event: WeebotCommandEvent) {
        track(this, event.bot, event.author, event.creationTime)

        val eb = strdEmbedBuilder.setTitle("All about Weebot")
            .setThumbnail(CmdHelloThere.HELLO_THERE_GIFS[0])
        //Description
        val sBuilder = StringBuilder("[`Hello there!`]($HELLO_THERE) ").append(
            "I am ***Weebot***, a bot, that ")
            .append("is certainly not a weeb, no sir. No weebs here.\n")
            .append("I Have a bunch of fun, useful, and sometimes random commands ")
            .append("that aim to make life on Discord easy, fun, and intuitive!\n\n")
            .append("I was made by ***[`HQRegent`]($LINK_HQTWITCH)***, ")
            .append("using [`Kotlin`](https://kotlinlang.org) and the ")
            .append("[`JDA library`](https://github.com/DV8FromTheWorld/JDA).")
            .append("\nIf you need more Help using Weebot, want help ")
            .append("using JDA to make your own bot, or just want to say hi to ")
            .append("***[`HQRegent`]($LINK_HQTWITCH)***, join the ")
            .append("[`Numberless Liquidator Discord`](https://discord.gg/VdbNyxr).")
            .append("\n\nPlease [`invite me to your server!`]($LINK_INVITEBOT) and")
            .append(" vote for me on [`discordbots.org`]($LINK_DISCORD_BOTS) and ")
            .append("[`DiscordBotList.com`]($LINK_DISCORD_BOTS_LIST)!")
            .append("\n\n*Use \"${SELF.asMention} help\" for info using my commands.*")
            .append("\n\n**__Weebot Commands__**\n\n")

        sBuilder.append(COMMANDS.sortedBy { it.name.toLowerCase() }.filterNot {
            it.isOwnerCommand || it.isHidden || it.category == CAT_UNDER_CONSTRUCTION
        }.joinToString(", ") {
            "*${it.displayName ?: it.name[0].toUpperCase()+it.name.substring(1)}*"
        })
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
        eb.setTimestamp(CMD_CLIENT.initTime)

        //other (?)

        event.reply(eb.build())
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("About", """
            Get information about Weebot, yourself, or a guild.
            **About (Member):** "``me``"
            **About (Guild):** "``guild`` or ``here``"
        """.trimIndent())
            .build()
    }

}

/**
 * Send a [MessageEmbed] giving info about the user.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class SubCmdAboutUser : WeebotCommand(
    "me", "ABOUTUSER", null, arrayOf("aboutme"), CAT_GEN,
    "Get information about yourself.", cooldown = 90, guildOnly = true
) {
    override fun execute(event: WeebotCommandEvent) {
        track(this, event.bot, event.author, event.creationTime)
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

class SubCmdAboutGuild : WeebotCommand(
    "guild", "ABOUTGUILD", null, arrayOf("here"), CAT_GEN, "",
    cooldown = 90, cooldownScope = USER_CHANNEL, guildOnly = true
) {
    override fun execute(event: WeebotCommandEvent) {
        val g = event.guild
        val roles = g.roles.filterNot { it.isPublicRole }

        val e = makeEmbedBuilder("About ${g.name}", null, """
            **ID:** ${g.id}
            **Region:** ${g.region.getName()}
            **Owner:** ${g.owner.effectiveName}
        """.trimIndent())
            .setAuthor(g.name, null, g.iconUrl).setThumbnail(g.iconUrl).apply {
                if (g.roles.isNotEmpty() && g.roles[0].color != null)
                    setColor(g.roles[0].color)
            }.addField("Member Count", """
                **Humans:** ${g.trueSize} (${ceil((g.trueSize/g.size.toDouble()) * 100)}%)
                **Bots:** ${g.size.minus(g.trueSize)}
                **Total:** ${g.size}
            """.trimIndent(), true).addField("Channels", """
                **Voice:** ${g.voiceChannels.size}
                **Text:** ${g.textChannels.size}
                **Categories:** ${g.categories.size}
            """.trimIndent(), true).addField("Custom Emotes: ${g.emotes.size}",
                kotlin.run {
                    val sb = StringBuilder()
                    for (i in 0 until g.emotes.size) {
                        if (sb.length + g.emotes[i].asMention.length + 3
                                > EMBED_MAX_FIELD_VAL) {
                            sb.append("...")
                            break
                        }
                        sb.append(g.emotes[i].asMention).append("")
                    }
                    sb.toString()
                }, true).addField("Roles: ${roles.size}", kotlin.run {
                val sb = StringBuilder()

                for (i in 0 until roles.size) {
                    if (sb.length + roles[i].name.length + 3 > EMBED_MAX_FIELD_VAL) {
                        sb.append("...")
                        break
                    }
                    sb.append(roles[i].name).append(", ")
                }
                if (!sb.endsWith("...")) sb.setLength(sb.length - 2)
                "```css\n$sb\n```"
            }, true)
            .build()

        event.reply(e)
    }
}

/**
 * Send an [SelectablePaginator] giving help with Weebot Commands.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdHelp : WeebotCommand(
    "help", "HELP", "Help", arrayOf("helpo", "more", "halp"),
    CAT_GEN, "Get information about Weebot Commands and Usage.",
    cooldown = 30, guildOnly = false
) {

    private val catAll: Category = Category("All")

    override fun execute(event: WeebotCommandEvent) {
        val bot = if (event.isFromType(PRIVATE)) GlobalWeebot else event.guild.bot
        track(this, bot, event.author, event.creationTime)
        fun filterAndMap(predicate: (WeebotCommand) -> Boolean): List<String> {
            val sb = StringBuilder()
            return COMMANDS.filter(predicate).sortedBy { it.displayName ?: it.name }.map {
                    sb.clear()
                    sb.append("**").append(it.displayName ?: it.name).append("**\n")
                    if (!it.help.isNullOrBlank()) sb.append(it.help).append("\n")
                    if (it.aliases.isNotEmpty()) sb.append(
                        "*Aliases: ${(it.aliases + it.name).joinToString(", ")}*\n")
                    sb.append("Guild Only: ${it.isGuildOnly}\n\n").toString()
                }
        }
        val cats = listOf(catAll) + CATEGORIES.toMutableList().also {
            if (!event.isOwner) it.remove(CAT_DEV)
        }.filter { c -> COMMANDS.any { it.category == c } }
        SelectablePaginator(setOf(event.author), title = "Weebot Help",
            description = "Select a category or All", singleUse = true,
            items = cats.map { cat ->
                cat.name to { _: Int, m: Message ->
                    if (cat == catAll) {
                        strdPaginator.setText("All Weebot Commands")
                            .setUsers(event.author).setItemsPerPage(6).apply {
                                filterAndMap { !(it.isHidden || it.isOwnerCommand) }.forEach { s ->
                                    addItems(s)
                                }
                            }.build().apply {
                                event.author.openPrivateChannel().queue { paginate(it, 1) }
                            }
                    }
                    if (cat == CAT_DEV) {
                        strdPaginator.setText("Dev Commands")
                            .setUsers(event.author).apply {
                                filterAndMap {
                                    it.isHidden || it.isOwnerCommand || it.category == cat
                            }.forEach { s -> addItems(s) }
                        }.build().apply {
                            event.author.openPrivateChannel().queue { paginate(it, 1) }
                        }
                    } else strdPaginator
                        .setText("Weebot's ${cat.name} Commands").setItemsPerPage(6)
                        .setUsers(event.author).apply {
                            filterAndMap { it.category == cat && !it.isHidden }.forEach { s ->
                                addItems(s)
                            }
                        }.build().apply {
                            event.author.openPrivateChannel().queue { paginate(it, 1) }
                        }
                    m.delete().queue()
                }
            }, timeout = 1, exitAction = {it.delete().queue()},
            timeoutAction = {it.delete().queue()}).display(event.channel)

    }

}

class CmdWatchaDoin : WeebotCommand(
    "whatchadoin", "WHATDOING", "Whatcha Doin'?", arrayOf("whatsup"), CAT_GEN,
    "What am I up to?", cooldown = 0, cooldownScope = USER_SHARD, guildOnly = true
) {
    override fun execute(event: WeebotCommandEvent) {
        val game = event.selfMember.game ?: games.random()
        val s: String = when (game.type) {
            DEFAULT -> "I'm playing ${game.name}"
            STREAMING -> "I'm watching my creator's Live Stream! $LINK_HQTWITCH"
            LISTENING -> "I'm listening to ${game.name ?: "...something"}"
            WATCHING -> "I'm watching ${game.name ?: "...something"}."
            else -> "Huh...what was I doing...uhmmmm"
        }
        event.reply(s)
    }
}
