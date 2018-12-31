package com.ampro.weebot.commands

import com.ampro.weebot.database.STAT
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.extensions.weebotAvatar
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.database.constants.LINK_INVITEBOT
import com.ampro.weebot.database.getWeebotOrNew
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS

/**
 * Sends a link to invite the bot to another server.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdInviteLink : WeebotCommand("invitelink", arrayOf("ilc", "inviteme", "invite"),
        CAT_GEN, "", "Get an invite link for Weebot.",
        HelpBiConsumerBuilder("Get an invite link for Weebot")
            .setDescription("[`Or just invite me with this link I guess`]($LINK_INVITEBOT)")
            .setThumbnail(weebotAvatar).build(), cooldown = 360,
        botPerms = arrayOf(MESSAGE_EMBED_LINKS), userPerms = arrayOf(MESSAGE_EMBED_LINKS)
) {
    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
        event.reply(strdEmbedBuilder.setTitle("Invite me to another server!")
            .setDescription(LINK_INVITEBOT).setThumbnail(
                weebotAvatar).build())
    }
}
