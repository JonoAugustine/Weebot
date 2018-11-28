/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.moderation.VCRoleManager.Limit.*
import com.ampro.weebot.main.constants.strdEmbedBuilder
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent

/**
 * The [IPassive] manager that creates, assigns, removes, and deletes
 * VoiceChannel roles.
 *
 * TODO: How to regulate which channels get roles when u can't mention voicechannels
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class VCRoleManager(var limit: Limit = ALL) : IPassive {

    /** Defines which voice channels will have roles made for them */
    enum class Limit {
        /** Make roles for all voice channels */
        ALL,
        /** Only make roles for voice channels open to @everyone*/
        PUBLIC,
        /** Don't make roles for any channel (turn it off) */
        NONE
    }

    /** Whether the [IPassive] is set to be destroyed */
    var dead = false
    override fun dead() = dead

    /**
     * Check if the voice channel allows VCRoles.
     *
     * @param voiceChannel The [VoiceChannel] to check
     */
    fun checkLimit(voiceChannel: VoiceChannel) = when (limit) {
        ALL -> true
        NONE -> false
        PUBLIC -> voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                    .allowed.contains(Permission.VOICE_CONNECT)
                || !voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                .denied.contains(Permission.VOICE_CONNECT)
    }

    /**
     * When a user joins a voice channel, assign the Member a role named after
     * the VoiceChannel. If the role does not exist, it is made. When there are
     * no members in the voice channel, the role is deleted.
     *
     */
    override fun accept(bot: Weebot, event: Event) {
        when (event) {
            is GuildVoiceJoinEvent -> {
                val guild = event.guild
                val channel = event.channelJoined
                val controller = guild.controller
                //Check the voice channel for existing roles
                guild.roles.forEach {
                    if (it.name.equals(channel.name, true)) {
                        controller.addRolesToMember(event.member, it).queue()
                        return
                    }
                }
                guild.controller.createRole().setName(channel.name)
                    .setHoisted(false).setMentionable(true).queue({
                        controller.addRolesToMember(event.member, it).queue()
                    },{
                        bot.settings.logchannel?.sendMessage(
                                strdEmbedBuilder.setTitle("Log Message")
                                    .setDescription(
                                            "Failed to assign VoiceChannel Role: ${channel.name}"
                                    ).build()
                        )?.queue()
                    })

            }
            is GuildVoiceLeaveEvent -> {
                val guild = event.guild
                val channel = event.channelLeft
                val controller = guild.controller
                guild.roles.forEach {
                    if (it.name.equals(channel.name, true)) {
                        controller.removeSingleRoleFromMember(event.member, it).queue()
                        return
                    }
                }
            }
        }
    }

}

class CmdVoiceChannelRole : Command() {

    init {
        name = "voicechannelrole"
        aliases = arrayOf("vcrc","vcr","vrc")
        userPermissions = arrayOf(Permission.MANAGE_ROLES)
        botPermissions = arrayOf(Permission.MANAGE_ROLES)
        //helpBiConsumer
    }



    override fun execute(event: CommandEvent) {

    }

}
