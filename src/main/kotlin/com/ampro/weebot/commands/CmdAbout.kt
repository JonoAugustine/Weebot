/**
 *
 */

package com.ampro.weebot.commands

import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.constants.weebotAvatarUrl
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.main.JDA_SHARD_MNGR
import com.ampro.weebot.main.LINK_INVITEBOT
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
class CmdAbout : WeebotCommand("about", emptyArray(), CAT_DEV,
    "", "Get information about Weebot.", children = arrayOf(CmdAboutUser()),
    cooldown = 90) {
    //"Weebot's little brother."
    override fun execute(event: CommandEvent) {

        val bot = getWeebotOrNew(event.guild)

        val HQR = if (event.jda.getUserById(event.client.ownerId) == null) {
            "<@${event.client.ownerId}>"
        } else {
            event.jda.getUserById(event.client.ownerId).name
        }

        event.jda.asBot().applicationInfo.queue { appInfo ->

            val eb = strdEmbedBuilder.setTitle("All about Weebot")
                .setThumbnail(weebotAvatarUrl)


            //Description
            val sBuilder = StringBuilder("[`Hello there!`]($HELLO_THERE) ")
                .append("I am ***Weebot***, a bot, that ")
                .append("is certainly not a weeb, no sir. No weebs here.")
                .append("\nI was made by ***$HQR***, you can learn more about my")
                .append(" father [`here`]($HQTWITCH).")
                .append("\nPlease [`invite me to your server!`]($LINK_INVITEBOT)!")
                .append("\n\n**Use ``${bot.settings.prefixs[0]}help`` ")
                .append("for help using Weebot.**\n*")
                .append("Use ``${bot.settings.prefixs[0]}about more`` for more details.*")
                .append("\n\n**__Weebot Commands__**\n\n")

            //List of commands

            //For each command build a "fake" field with styling
            commands.forEach { cmd ->
                if (cmd.isOwnerCommand) return@forEach
                sBuilder.append("**__${cmd.name}__**: ").append("$help\n")
                if (cmd.aliases.isNotEmpty())
                    sBuilder.append("*Aliases: ${cmd.aliases!!.contentToString()
                        .removeSurrounding("[","]")}*\n")
                sBuilder.append("\n")
            }
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
class CmdAboutUser : WeebotCommand("aboutme", arrayOf("me"), CAT_DEV,
    "", "Get information about Weebot.", cooldown = 90
) {
    override fun execute(event: CommandEvent?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
