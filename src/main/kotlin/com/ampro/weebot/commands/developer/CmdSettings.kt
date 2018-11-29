package com.ampro.weebot.commands.developer

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.constants.Emoji
import com.ampro.weebot.database.constants.Emoji.*
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.getWeebot
import com.ampro.weebot.hasPerm
import com.ampro.weebot.util.slog
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent




/**
 * Waits for a response to the initial message asking for tracking permissions.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class TrackerInitPassive(val enableMessage: Message) : IPassive {

    var dead: Boolean = false

    companion object {
        const val onOff = "Statistics Tracking can be turned on or off at anytime with " +
                "the command ``w!skynet [on/off]``."
        val acceptEmbed get() = strdEmbedBuilder.setTitle("Statistics Tracking Enabled!")
            .setDescription("Thank you for helping Weebot's development!\n$onOff").build()
        val denyEmbed   get() =  strdEmbedBuilder.setTitle("Keeping Statistics Tracking Off")
            .setDescription("This can be turned on later\n$onOff").build()
    }

    override fun accept(bot: Weebot, event: Event) {
        if (dead) return
        when (event) {
            is GenericGuildMessageEvent -> {
                val guild = event.guild
                val messageID = event.messageIdLong
                when (event) {
                    //Accept by react
                    is GuildMessageReactionAddEvent -> {
                        if (event.user.isBot) return
                        if (messageID != enableMessage.idLong) return
                        val emote = event.reaction.reactionEmote
                        event.reaction.users.forEach { user ->
                            //If an Admin reacts
                            if (guild.getMemberById(user.id) hasPerm ADMINISTRATOR) {
                                when {
                                    emote.name == heavy_check_mark.unicode -> {
                                        //enable
                                        event.channel.sendMessage(acceptEmbed).queue()
                                        getWeebot(
                                            guild.idLong)?.settings?.trackingEnabled = true
                                        dead = true
                                    }
                                    emote.name == X.unicode -> {
                                        //Disable
                                        event.channel.sendMessage(denyEmbed).queue()
                                        getWeebot(
                                            guild.idLong)?.settings?.trackingEnabled = false
                                        dead = true
                                    }
                                    else -> return
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dead() = dead
}

class CmdSettings : Command() {
    override fun execute(event: CommandEvent?) {
        TODO("not implemented")
    }
}
