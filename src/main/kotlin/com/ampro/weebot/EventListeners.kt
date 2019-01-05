/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.Emoji.X_Red
import com.ampro.weebot.util.Emoji.heavy_check_mark
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.channel.priv.PrivateChannelCreateEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.events.message.priv.GenericPrivateMessageEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Event Dispatcher is a global Listener that distributes events
 * to corresponding Weebots.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class EventDispatcher : ListenerAdapter() {

    /** Feed all events to the Global Weebot */
    override fun onGenericEvent(event: Event) = DAO.GLOBAL_WEEBOT.feedPassives(event)

    /************************************************
     *               Generic Events                 *
     ************************************************/

    /** Sends a message when the Bot joins a Guild */
    override fun onGuildJoin(event: GuildJoinEvent) {
        val onOff = "Statistics Tracking can be turned on or off at anytime with " +
                "the command ``w!skynet [on/off]``."
        val acceptEmbed = strdEmbedBuilder.setTitle("Statistics Tracking Enabled!")
        .setDescription("Thank you for helping Weebot's development!\n$onOff").build()
        val denyEmbed = strdEmbedBuilder.setTitle("Keeping Statistics Tracking Off")
        .setDescription("This can be turned on later\n$onOff").build()

        //Add the bot to the database
        val newBot = Weebot(event.guild)
        DAO.addBot(newBot)

        val c: TextChannel = event.guild.systemChannel
                ?: event.guild.defaultChannel ?: return

        val desc = "Thank you for adding **Weebot** to your server! You can use" +
                " ``w!help`` or ``\\help`` to get help with using any of my shiny features."
        val gdpr = MessageEmbed.Field("Enable Tracking?",
                "Would you like to help Weebot's development by allowing your " +
                        "Weebot to send anonymous usage stats back to our HQ?" +
                        "\nAll data " +
                        "is completely anonymous and cannot be tracked back to your " +
                        "server.\nThis feature can be turned off at any point and is " +
                        "turned off by default.\n(React with a check to accept " +
                        "or an cross (X_Red) to keep it off. or type ``w!skynet on/off``)" +
                        "\n*Thank you for your support!*",
                false)

        val se = SelectableEmbed(event.guild.members.filter { it hasPerm ADMINISTRATOR }
            .map { it.user }.toSet(), timeout = 5L, messageEmbed = strdEmbedBuilder
                .setTitle("***Kicks in door*** The Weebot has arrived!")
                .setDescription(desc).addField(gdpr).build(), options =  listOf (
                heavy_check_mark to { message, user ->
                    message.channel.sendMessage(acceptEmbed).queue()
                    getWeebotOrNew(event.guild.idLong).settings.trackingEnabled = true
                },
                X_Red to { message, _ ->
                    message.channel.sendMessage(denyEmbed).queue()
                    getWeebotOrNew(event.guild.idLong).settings.trackingEnabled = false
                }
            )) {
            it.clearReactions().queueAfter(250, MILLISECONDS)
            it.channel.sendMessage(denyEmbed).queue()
            getWeebotOrNew(event.guild.idLong).settings.trackingEnabled = false
        }

        try { se.display(c) } catch (e: InsufficientPermissionException) {}

    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        DAO.removeBot(event.guild.idLong)
        //TODO other on-leave actions
    }

    /* ************************
     *          Messages      *
     ***************************/

    override fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
        if (event.user.isBot) return
        getWeebotOrNew(event.guild.idLong).feedPassives(event)
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.isWebhookMessage || event.author.isBot) return
        getWeebotOrNew(event.guild.idLong).feedPassives(event)
    }

    override fun onPrivateChannelCreate(event: PrivateChannelCreateEvent) {
        /*if (event.user.isBot) return
        //TODO bugged
        val desc = "Hello there! You can use ``w!help`` or ``\\help`` to get help with using any " +
                "of my shiny features."
        val gdpr = MessageEmbed.Field("Enable Tracking?",
                "Would you like to help Weebot's development by allowing your " +
                        "Weebot to send anonymous usage stats back to our HQ?" +
                        "\nAll data " +
                        "is completely anonymous and cannot be tracked back to your " +
                        "server.\nThis feature can be turned off at any point and is " +
                        "turned off by default.\n(React with a check to accept " +
                        "or an cross (X_Red) to keep it off. or type ``w!skynet on/off``)" +
                        "\n*Thank you for your support!*",
                false)

        event.channel.sendMessage(strdEmbedBuilder
            .setTitle("***Kicks in door*** The Weebot has arrived!")
            .setDescription(desc).addField(gdpr).build())
            .queue {
                GLOBAL_WEEBOT.getUesrPassiveList(event.user).add(TrackerInitPassive(it))
                it.addReaction(heavy_check_mark.toString()).queue()
                it.addReaction(X_Red.toString()).queue()
            }*/
    }

    override fun onGenericPrivateMessage(event: GenericPrivateMessageEvent) {
        DAO.GLOBAL_WEEBOT.feedPassives(event)
    }

    /* ************************
     *          Voice          *
     ***************************/

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.member.user.isBot) return
        getWeebotOrNew(event.guild.idLong).feedPassives(event)
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.member.user.isBot) return
        getWeebotOrNew(event.guild.idLong).feedPassives(event)
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.member.user.isBot) return
        getWeebotOrNew(event.guild.idLong).feedPassives(event)
    }


}
