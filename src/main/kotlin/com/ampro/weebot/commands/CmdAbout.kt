/**
 *
 */

package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.reactions.CmdHelloThere
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.constants.weebotAvatarUrl
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.main.JDA_SHARD_MNGR
import com.ampro.weebot.main.LINK_INVITEBOT
import com.ampro.weebot.main.SELF
import com.jagrosh.jdautilities.command.CommandEvent

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
        val bot = getWeebotOrNew(event.guild)
        event.jda.asBot().applicationInfo.queue { appInfo ->

            val eb = strdEmbedBuilder.setTitle("All about Weebot")
                .setThumbnail(CmdHelloThere.HELLO_THERE_GIFS[0])

            //Description
            val sBuilder = StringBuilder("[`Hello there!`]($HELLO_THERE) ")
                .append("I am ***Weebot***, a bot, that ")
                .append("is certainly not a weeb, no sir. No weebs here.\n")
                .append("I Have a bunch of fun, useful, and sometimes random commands ")
                .append("that aim to make life on Discord easy, fun, and intuitive!\n")
                .append("I was made by ***[`HQRegent`]($HQTWITCH)***, ")
                .append("using [`Kotlin`](https://kotlinlang.org) and the ")
                .append("[`JDA library`](https://github.com/DV8FromTheWorld/JDA).\n")
                .append("Please [`invite me to your server!`]($LINK_INVITEBOT)")
                .append("\n\n*Use ${SELF.asMention} help for info using my commands.*")
                .append("\n*Use ``${bot.settings.prefixs[0]}about more`` for more details.*")
                .append("\n\n**__Weebot Commands__**\n")

            //List of commands

            //For each command build a "fake" field with styling
            commands.sortedBy { it.name.toLowerCase() }.forEach { cmd ->
                if (cmd.isOwnerCommand) return@forEach
                sBuilder.append("*${cmd.name}*, ")
            }
            sBuilder.setLength(sBuilder.length - 2)
            sBuilder.append("\n\n")
            eb.setDescription(sBuilder.toString())

            sBuilder.setLength(0)

            //Global stats (server count, shard count)
            //Shard-level stats (User count, server count)
            if (event.jda.shardInfo == null) {
                eb.addField("Stats", "${event.jda.guilds.size} servers\n1 shard", true)
                eb.addField("Users","${event.jda.users.size} unique\n${event.jda.guilds
                    .stream().mapToInt { g -> g.members.size }.sum()} total", true)
                eb.addField("Channels",
                    "${event.jda.textChannels.size} Text\n${event.jda.voiceChannels.size} Voice",
                    true)
            } else {
                eb.addField("Stats",
                    "${JDA_SHARD_MNGR.guilds.size} Servers\nShard ${event.jda.shardInfo
                        .shardId + 1}/${event.jda.shardInfo.shardTotal}", true)
                eb.addField("This shard",
                    "${event.jda.users.size} Users\n${event.jda.guilds.size} Servers",
                    true)
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

}


/**
 * Send a [MessageEmbed] giving info about the user.
 * TODO()
 */
class CmdAboutUser : WeebotCommand("aboutme", arrayOf("me"), CAT_GEN,
    "", "Get information about Weebot.", cooldown = 90
) {
    override fun execute(event: CommandEvent?) {
        TODO("not implemented")
    }
}


/**
 * Send an [MessageEmbed] giving help with Weebot Commands.
 *TODO()
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdHelp : WeebotCommand("help", arrayOf("helpo", "more"), CAT_GEN,
        "[category] [command name]", "Get information about Weebot Commands and Usage.",
        cooldown = 90
) {
    //"Weebot's little brother."
    public override fun execute(event: CommandEvent) {

        val bot = getWeebotOrNew(event.guild)

        val HQR = if (event.jda.getUserById(event.client.ownerId) == null) {
            "<@${event.client.ownerId}>"
        } else {
            event.jda.getUserById(event.client.ownerId).name
        }

        event.jda.asBot().applicationInfo.queue { appInfo ->

            val eb = strdEmbedBuilder.setTitle("All about Weebot")
                .setThumbnail(weebotAvatarUrl)
            val r = "[\\[\\]]" // Regex for clearing array brackets

            //Description
            val descBuilder = StringBuilder("[`Hello there!`](\"$HELLO_THERE\") ")
                .append("I am ***Weebot***, a bot, that ")
                .append("is certainly not a weeb, no sir. No weebs here.")
                .append("\nI was made by ***$HQR***, you can learn more about my")
                .append(" father [`here`]($AMPRO).")
                .append("\nPlease [`invite me to your server!`]($LINK_INVITEBOT)!")
                .append("\n\n**Use ``${bot.settings.prefixs[0]}help`` ")
                .append("for help using Weebot.**")
                .append("*Use ``${bot.settings.prefixs[0]}about more`` for more details.")
                .append("\n\n**__Weebot Commands__**")

            //List of commands

            //For each command build a "fake" field with styling
            commands.forEach { cmd ->
                if (cmd.isOwnerCommand) return@forEach
                descBuilder.append("*__${cmd.name}__*").append("${cmd.help}\n")
                    .append("Aliases: ${cmd.aliases.toString().replace(r,"")}\n")
                    .append("User Permissions: ${cmd.userPermissions.toString()
                        .replace(r,"")}\n")
                    .append("Cooldown: ${cmd.cooldown} seconds\n")
                    .append("In-guild only: ${cmd.isGuildOnly}\n")
                if (!cmd.requiredRole.isNullOrBlank()) {
                    descBuilder.append("Required Role: ${cmd.requiredRole}\n")
                }
                descBuilder.append("\n")
            }
            eb.setDescription(descBuilder.toString())
            //Global stats (server count, shard count)
            //Shard-level stats (User count, server count)

            //other (?)

            event.reply(eb.build())
        }
    }

}
