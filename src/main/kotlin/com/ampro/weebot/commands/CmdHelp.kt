/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.constants.weebotAvatarUrl
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.main.LINK_INVITEBOT
import com.jagrosh.jdautilities.command.CommandEvent


/**
 * Send an [MessageEmbed] giving information about Weebot.
 *TODO()
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdHelp() : WeebotCommand("help", arrayOf("helpo"), CAT_DEV,
    "[category] [command name]", "Get information about Weebot Commands and Usage.",
    cooldown = 90
) {
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
                descBuilder.append("*__${cmd.name}__*").append("$help\n")
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
