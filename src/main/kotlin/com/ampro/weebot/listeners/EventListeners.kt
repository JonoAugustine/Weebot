/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.listeners

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.developer.TrackerInitPassive
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.constants.Emoji.X
import com.ampro.weebot.database.constants.Emoji.heavy_check_mark
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.getWeebot
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
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

        val c: TextChannel = event.guild.systemChannel
                ?: event.guild.defaultChannel
                ?: return //If there isnt a default channel to send to just wait till
                          //The first usage of the bot TODO

        val desc = "Thank you for adding **Weebot** to your server! You can use" +
                " ``w!help`` or ``\\help`` to get help with using any of my shiny features."
        val gdpr = MessageEmbed.Field("Enable Tracking?",
                "Would you like to help Weebot's development by allowing your" +
                        "Weebot to send anonymous usage stats back to our HQ? All data " +
                        "is completely anonymous and cannot be tracked back to your " +
                        "server. This feature can be turned off at any point and is " +
                        "turned off by default.\n(React with a check to accept " +
                        "or an cross (X) to keep it off. or type ``w!skynet on/off``)" +
                        "\n*Thank you for your support!*",
                false)

        c.sendMessage(strdEmbedBuilder
            .setTitle("***Kicks in door*** The Weebot has arrived!")
            .setDescription(desc).addField(gdpr).build())
            .queue {
                newBot.passives.add(TrackerInitPassive(it))
                it.addReaction(heavy_check_mark.toString()).queue()
                it.addReaction(X.toString()).queue()
            }
    }

    override fun onGenericEvent(event: Event) {
        DAO.WEEBOTS.forEach { _, bot -> bot.feedPassives(event) }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        DAO.removeBot(event.guild.idLong)
        //TODO other on-leave actions
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
