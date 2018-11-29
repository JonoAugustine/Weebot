package com.ampro.weebot.commands.developer

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.constants.Emoji.heavy_check_mark
import com.ampro.weebot.hasPerm
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

    override fun accept(bot: Weebot, event: Event) {
        if (dead) return
        when (event) {
            is GenericGuildMessageEvent -> {
                val guild = event.guild
                val messageID = event.messageIdLong

                when (event) {
                    //Accept by react
                    is GuildMessageReactionAddEvent -> {
                        if (messageID != enableMessage.idLong) return
                        val react = event.reaction
                        val emote = react.reactionEmote.emote
                        react.users.forEach { user ->
                            //If an Admin reacts
                            if (guild.getMemberById(user.id) hasPerm ADMINISTRATOR) {
                                //TODO enable/disable
                                if (emote.name == heavy_check_mark.name) {
                                    //enable
                                }
                                return
                            }
                        }
                    }
                    //Accept by message
                    is GuildMessageReceivedEvent -> {
                        //TODO enable/disable
                    }
                }
            }
        }
    }

    override fun dead() = dead
}

class CmdSettings : Command() {
    override fun execute(event: CommandEvent?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
