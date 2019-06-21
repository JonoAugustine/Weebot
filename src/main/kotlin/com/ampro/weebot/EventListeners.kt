/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.database.bot
import com.ampro.weebot.extensions.SelectableEmbed
import com.ampro.weebot.extensions.hasPerm
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.util.Emoji.X_Red
import com.ampro.weebot.util.Emoji.heavy_check_mark
import com.ampro.weebot.util.NOW
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
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
object EventDispatcher : ListenerAdapter() {

    /** Feed all events to the Global Weebot */
    override fun onGenericEvent(event: Event) = GlobalWeebot.feedPassives(event)

    /************************************************
     *               Generic Events                 *
     ************************************************/

    /** Sends a message when the Bot joins a Guild */
    override fun onGuildJoin(event: GuildJoinEvent) = askTracking(event.guild)

    override fun onGuildLeave(event: GuildLeaveEvent) {
        event.guild.bot.leaveDate = NOW()
    }

    /* ************************
     *          Messages      *
     ***************************/

    override fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
        event.guild.bot.takeUnless { event.user.isBot }?.feedPassives(event)
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        event.guild.bot.takeUnless { event.isWebhookMessage || event.author.isBot }
            ?.feedPassives(event)
    }

    override fun onGenericPrivateMessage(event: GenericPrivateMessageEvent) =
        GlobalWeebot.feedPassives(event)

    /* ************************
     *          Voice          *
     ***************************/

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        event.guild.bot.takeUnless { event.member.user.isBot }
            ?.feedPassives(event)
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        event.guild.bot.takeUnless { event.member.user.isBot }
            ?.feedPassives(event)
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.guild.bot.takeUnless { event.member.user.isBot }
            ?.feedPassives(event)
    }


}

fun askTracking(guild: Guild) {
    val onOff = "Statistics Tracking can be turned on or off at anytime with " +
        "the command ``w!skynet [on/off]``."
    val acceptEmbed = strdEmbedBuilder.setTitle("Statistics Tracking Enabled!")
        .setDescription("Thank you for helping Weebot's development!\n$onOff").build()
    val denyEmbed = strdEmbedBuilder.setTitle("Keeping Statistics Tracking Off")
        .setDescription("This can be turned on later\n$onOff").build()

    val c: TextChannel = if (guild.textChannels.isNotEmpty())
        guild.systemChannel ?: guild.defaultChannel ?: return
    else return

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

    val se = SelectableEmbed(guild.members.filter { it hasPerm ADMINISTRATOR }
        .map { it.user }.toSet(), timeout = 5L, messageEmbed = strdEmbedBuilder
        .setTitle("***Kicks in door*** The Weebot has arrived!")
        .setDescription(desc).addField(gdpr).build(), options = listOf(
        heavy_check_mark to { message, _ ->
            message.channel.sendMessage(acceptEmbed).queue()
            guild.bot.settings.trackingEnabled = true
        },
        X_Red to { message, _ ->
            message.channel.sendMessage(denyEmbed).queue()
            guild.bot.settings.trackingEnabled = false
        }
    )) {
        it.clearReactions().queueAfter(250, MILLISECONDS)
        it.channel.sendMessage(denyEmbed).queue()
        guild.bot.settings.trackingEnabled = false
    }

    try {
        se.display(c)
    } catch (e: InsufficientPermissionException) {
    }
}
