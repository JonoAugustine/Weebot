package com.ampro.weebot.commands

import com.ampro.weebot.main.LINK_INVITEBOT
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.constants.weebotAvatarUrl
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent

/**
 * Sends a link to invite the bot to another server.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdInviteLink : Command() {
    init {
        name = "invitelink"
        aliases = arrayOf("ilc", "inviteme", "invite")
        guildOnly = false
        cooldown = 360
    }

    override fun execute(event: CommandEvent) {
        event.reply(strdEmbedBuilder.setTitle("Invite me to another server!")
            .setDescription(LINK_INVITEBOT).setThumbnail(weebotAvatarUrl).build())
    }
}
