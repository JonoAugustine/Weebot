/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.listeners

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.getWeebot
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Event Dispatcher is a global Listener that distributes events
 * to corresponding Weebots.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class EventDispatcher : ListenerAdapter() {

    /************************************************
     *               Generic Events                 *
     ************************************************/

    /** Sends a message when the Bot joins a Guild */
    override fun onGuildJoin(event: GuildJoinEvent) {
        //Add the bot to the database
        val newBot = Weebot(event.guild)
        DAO.addBot(newBot)

        val c = event.guild.systemChannel ?: event.guild.defaultChannel

        val desc = "Thank you for adding **Weebot** to your server! You can use" +
                " ``w!help`` or ``\\help`` to get help with using any of my shiny features."
        val gdpr = MessageEmbed.Field("Enable Tracking?",
                "Would you like to help Weebot's development by allowing your" +
                        "Weebot to send anonymous usage stats back to HQ? All data " +
                        "is completely anonymous and cannot be tracked back to your " +
                        "server. This feature can be turned off at any point and is " +
                        "turned off by default. *Thank you for your support!*",
                false)

        c?.sendMessage(strdEmbedBuilder
            .setTitle("***Kicks in door*** The Weebot has arrived!")
            .setDescription(desc).addField(gdpr).build())?.queue()
    }

    override fun onGuildLeave(event: GuildLeaveEvent?) {
        TODO()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.member.user.isBot) {
            getWeebot(event.guild.idLong)?.feedPassives(event)
        }
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (!event.member.user.isBot) {
            getWeebot(event.guild.idLong)?.feedPassives(event)
        }
    }

}
