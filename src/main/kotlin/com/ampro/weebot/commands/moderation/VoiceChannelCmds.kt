/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.moderation.VCRoleManager.Limit.*
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent
import java.util.concurrent.ConcurrentHashMap

/* ***************
    VC Roles
 *****************/

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
    private fun limitSafe(voiceChannel: VoiceChannel) = when (limit) {
        ALL -> true
        NONE -> false
        PUBLIC -> voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                    ?.allowed?.contains(VOICE_CONNECT) ?: true
                || !(voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                ?.denied?.contains(VOICE_CONNECT) ?: true)
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
    cooldown = 10, userPerms = arrayOf(MANAGE_ROLES),
    botPerms = arrayOf(MANAGE_ROLES)
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Voice Channel Roles")
            .setDescription("Allow Weebot to create and automatically assign ")
            .addToDesc("roles to anyone in a voice channel. The roles will ")
            .addToDesc("have the same name as the channel and can be ")
            .addToDesc("@/mentioned by anyone. You can choose to restrict ")
            .addToDesc("which channels get roles by their publicity (all ")
            .addToDesc("channels or only channels public to @/everyone)")
            .addField("Arguments", "[enable/disable/on/off] [all/public]" +
                    "\n[all/public] (if already enabled)", false)
            .setAliases(aliases)
            .build()
    }

    companion object {
        val activatedEmbed: MessageEmbed
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
        STAT.track(this, bot, event.author)
        val vcp = bot.getPassive<VCRoleManager>()
        when (args[0].toUpperCase()) {
            "ENABLE", "ON" -> {
                if (vcp == null) {
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
                if (vcp != null) {
                    vcp.clean()
                    vcp.dead = true
                    event.reply("*VoiceChannelRoles are now disabled*")
                    return
                } else {
                    event.reply("*VoiceChannelRoles are not enabled.*")
                    return
                }
            }
            "SETLIMIT", "SL", "LIMIT" -> {
                if (vcp != null) {
                    vcp.limit = if (args.size > 1) {
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

/* *******************
    Personal Auto VC
 *********************/

/**
 * Creates a temp [VoiceChannel] for a User after they join a designated"[baseChannel]".
 * The [VoiceChannel] is deleted on empty.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class VCGenerator(var baseChannel: VoiceChannel) : IPassive {
    var dead = false
    override fun dead() = dead

    /** Whether the generator is set to shutdown (block incoming requests then die)*/
    var inShutdown = false

    /**
     * @param limit the max users that can join the channel 1-99 (0 if no limit)
     * @param name The name of the Generated [VoiceChannel]. 1-99 char
     *
     * @throws IllegalArgumentException if either param is not in the allowed range
     */
    internal data class Settings(var limit: Int, var name: String) {
        init {
            if (limit !in 0..99)
                throw IllegalArgumentException(limit.toString())
            if (name.length !in 1..99)
                throw IllegalArgumentException(name)
        }
        fun asEmbed(member: Member) : MessageEmbed {
            TODO()
            return strdEmbedBuilder.setColor(member.color)
                .build()
        }
    }

    /** User default settings for their generated channel */
    internal val userSettings = ConcurrentHashMap<Long, Settings>()

    /** The Guild Settings for default Generated Channels */
    internal val guildSettings = Settings(-1, "{USER}'s Channel ")

    /** All active generated [VoiceChannel]s */
    val generatedChannels = mutableListOf<Long>()

    override fun accept(bot: Weebot, event: Event) {
        TODO("not implemented")
    }

    private fun nameGen(member: Member) = "${member.effectiveName}'s Channel"

    private fun nameGen(member: Member, format: String)
            = format.replace(Regex("(?i)(\\{U+S+E+R+})"), member.effectiveName)

    fun asEmbed(guild: Guild) : MessageEmbed {
        TODO()
    }
}

/**
 * A [WeebotCommand] to moderate a [Guild]'s [VCGenerator].
 *
 */
class CmdVoiceChannelGenerator : WeebotCommand("voicechannelgenerator",
    arrayOf("vcg", "vcgenerator", "vcgen"), CAT_UNDER_CONSTRUCTION, "",
    "Creates a temp VoiceChannel for a User after joining a designated Voice Channel",
    guildOnly = true, children = arrayOf()
) {

    /** Turn ON or OFF */
    internal class SubCmdEnable : WeebotCommand("enable",
        arrayOf("on", "disable", "off"), CAT_MOD, "", "", guildOnly = true,
        cooldown = 30,  botPerms = arrayOf(MANAGE_CHANNEL),
        userPerms = arrayOf(MANAGE_CHANNEL)) {
        public override fun execute(event: CommandEvent) {
            val arg = event.getInvocation()
            when {
                arg.matchesAny(REG_ON, REG_ENABLE) -> {
                    TODO("Generate VCGen")
                }
                arg.matchesAny(REG_OFF, REG_DISABLE) -> {
                    TODO("Set VCGen to shutdown mode.")
                }
            }
        }
    }

    internal class SubCmdServerDefaults : WeebotCommand("def",
        arrayOf("serverdefaults", "sdef", "servdef"), CAT_MOD, "", "",
        botPerms = arrayOf(MANAGE_CHANNEL), userPerms = arrayOf(MANAGE_CHANNEL),
        guildOnly = true) {
        override fun execute(event: CommandEvent) {
            TODO("not implemented")
        }
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Voice Channel Generator", """
            Creates a temp VoiceChannel for a User after joining a designated Voice
            Channel.

            **Changing Settings**
            There are two settings for Generated Voice Channels: (User) Limit and Name
            When settings these, it is important to follow these guidelines.
            **User Limit** can be any number from 0 to 99. 0 means there is no limit.
            **Name** is the name of the generated channel and will replace ``{USER}``
            with the user's name. For example, ``{USER}'s room`` becomes ``Bill's Room``
        """.trimIndent())
            .setAliases(aliases)
            .addField("Enable/Disable",
                "``on/off``\n*Must have ${MANAGE_CHANNEL.name} permission.*", true)
            .addField("Set/See Server Defaults","``def [-L userLimit] [-n channelName]``"
                    + "\n*Must have ${MANAGE_CHANNEL.name} permission.*", true)
            .addField("Set/See Your Defaults","``set [-L userLimit] [-n channelName]``", true)
            .build()
    }
}
