/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.moderation.VCRoleManager.Limit.*
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.splitArgs
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.*

/**
 * The [IPassive] manager that creates, assigns, removes, and deletes
 * VoiceChannel roles.
 *
 * TODO: How to regulate which channels get roles when u can't mention voicechannels
 * TODO: Clean all roles on deletion
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

    /** TODO: Deletes all roles created for [VCRoleManager] */
    fun clean() {

    }

    /**
     * Check if the voice channel allows VCRoles.
     *
     * @param voiceChannel The [VoiceChannel] to check
     */
    fun limitSafe(voiceChannel: VoiceChannel) = when (limit) {
        ALL -> true
        NONE -> false
        PUBLIC -> voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                    ?.allowed?.contains(Permission.VOICE_CONNECT) ?: true
                || !(voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                ?.denied?.contains(Permission.VOICE_CONNECT) ?: true)
    }

    /**
     * When a user joins a voice channel, assign the Member a role named after
     * the VoiceChannel. If the role does not exist, it is made. When there are
     * no members in the voice channel, the role is deleted.
     */
    override fun accept(bot: Weebot, event: Event) {
        when (event) {
            is GuildVoiceJoinEvent -> {
                val guild = event.guild
                val channel = event.channelJoined
                if (!limitSafe(channel)) return
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
                    }, {
                        bot.settings.sendLog(strdEmbedBuilder.setTitle("Log Message")
                            .setDescription(
                                "Failed to assign VoiceChannel Role: ${channel.name}").build())
                    })

            }
            is GuildVoiceUpdateEvent-> {
                val guild = event.guild
                val channel = event.channelLeft
                if (!limitSafe(channel)) return
                val controller = guild.controller
                guild.roles.forEach {
                    if (it.name.equals(channel.name, true)) {
                        controller.removeSingleRoleFromMember(event.member, it).queue()
                        if (channel.members.isEmpty()) {
                            it.delete().reason("VCRoleManager").queue()
                        }
                        return
                    }
                }
            }
        }
    }

}

/**
 * A Controller Command for the passive [VCRoleManager].
 * Can enable and change the VCRole limits
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdVoiceChannelRole : WeebotCommand("voicechannelrole",
    arrayOf("vcrc","vcr","vrc", "vcrole"), CAT_MOD, "[enable/disable] [limit] or [limit]",
    "A manager that creates, assigns, removes, and deletes VoiceChannel roles.",
    cooldown = 10, userPerms = arrayOf(Permission.MANAGE_ROLES),
    botPerms = arrayOf(Permission.MANAGE_ROLES)
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Voice Channel Roles")
            .setDescription("Allow Weebot to create and automatically assign ")
            .appendDesc("roles to anyone in a voice channel. The roles will ")
            .appendDesc("have the same name as the channel and can be ")
            .appendDesc("@/mentioned by anyone. You can choose to restrict ")
            .appendDesc("which channels get roles by their publicity (all ")
            .appendDesc("channels or only channels public to @/everyone)")
            .addField("Arguments", "[enable/disable/on/off] [all/public]" +
                    "\n[all/public] (if already enabled)", false)
            .addField("Aliases",
                    "$name, ${aliases.contentToString().removeSurrounding("[","]")}", false)
            .build()
    }

    companion object {
        val activatedEmbed
            get() = strdEmbedBuilder
                .setTitle("Voice Channel Roles Activated!")
                .setDescription("""
                |The next time someone joins a voice channel, they will
                |be assigned a Role with the same name of the channel that can
                |be mentioned by anyone.
            """.trimMargin()).build()
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        val args = event.splitArgs()
        if (args.isEmpty()) return
        val vcp = bot.getPassive(VCRoleManager::class)
        val vcRoleManager = if (vcp != null) { vcp as VCRoleManager } else { null }
        when (args[0].toUpperCase()) {
            "ENABLE", "ON" -> {
                if (vcRoleManager == null) {
                    val lim = if (args.size > 1) {
                        try {
                            VCRoleManager.Limit.valueOf(args[1].toUpperCase())
                        } catch (e: Exception) {
                            event.reply("${args[1]} is not a valid restriction. ``w!help vcrole``")
                            return
                        }
                    } else { ALL }
                    bot.passives.add(VCRoleManager(lim))
                    event.reply(activatedEmbed)
                    return
                } else {
                    event.reply("*The VoiceChannelRole is already enabled.* ``w!help vcrole``")
                    return
                }
            }
            "DISABLE", "OFF" -> {
                if (vcRoleManager != null) {
                    vcRoleManager.clean()
                    bot.passives.remove(vcRoleManager)
                    event.reply("*VoiceChannelRoles are now disabled*")
                    return
                } else {
                    event.reply("*VoiceChannelRoles are not enabled.*")
                    return
                }
            }
            "SETLIMIT", "SL", "LIMIT" -> {
                if (vcRoleManager != null) {
                    vcRoleManager.limit = if (args.size > 1) {
                        try {
                            VCRoleManager.Limit.valueOf(args[1].toUpperCase())
                        } catch (e: Exception) {
                            event.reply("${args[1]} is not a valid restriction. ``w!help vcrole``")
                            return
                        }
                    } else {
                        //TODO
                        return
                    }
                } else {
                    //TODO
                }
            }
            ALL.name -> { //TODO

            }
            NONE.name -> {//TODO

            }
            PUBLIC.name -> {//TODO

            }
        }

    }

}
