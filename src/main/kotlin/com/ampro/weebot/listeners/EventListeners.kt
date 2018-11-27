/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.listeners

import com.ampro.weebot.database.getWeebot
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
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
        val c = event.guild.systemChannel ?: event.guild.defaultChannel
        c?.sendMessage("*Kicks in door* The Weebot has arrived!")?.queue()
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
