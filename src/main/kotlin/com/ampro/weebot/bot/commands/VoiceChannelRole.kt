/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.Passive
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.add
import com.ampro.weebot.bot.getAll
import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.delete
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.ampro.weebot.util.Regecies
import com.ampro.weebot.util.matchesAny
import com.ampro.weebot.util.plus
import com.serebit.strife.data.Color
import com.serebit.strife.data.Permission
import com.serebit.strife.entities.*
import com.serebit.strife.events.Event
import com.serebit.strife.events.VoiceStateUpdateEvent
import com.serebit.strife.text.italic
import kotlin.time.ExperimentalTime

/**
 * The [Passive] manager that creates, assigns, removes, and deletes
 * VoiceChannel roles.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class VCRoleManager(var limit: Limit = Limit.ALL) : Passive {

    /** Defines which voice channels will have roles made for them */
    enum class Limit {
        /** Make roles for all voice channels */
        ALL,
        /** Only make roles for voice channels open to @everyone*/
        PUBLIC
    }

    override var active: Boolean = true

    /** VC to RoleID */
    private val generatedRoles = mutableMapOf<Long, Long>()

    override suspend fun predicate(event: Event, bot: Weebot) =
        event is VoiceStateUpdateEvent

    override suspend fun consume(event: Event, bot: Weebot) {
        event as VoiceStateUpdateEvent
        val channel = event.voiceState.voiceChannel

        if (channel != null) {
            // Get existing role or make new one
            channel.getGeneratedRole()?.id
                ?.let {
                    generatedRoles[channel.id] = it
                    event.member.addRole(it)
                } // Assign role
        } else {
            event.member.roles
                .filter { it.id in generatedRoles.values }
                .forEach { event.member.removeRole(it) }
        }
    }

    /** Gets a guild role */
    private suspend fun GuildVoiceChannel.getGeneratedRole(): GuildRole? =
        generatedRoles[id]?.let { guild.getRole(it) }
            ?: guild.roles.firstOrNull { it.name.equals(name, true) }
            ?: guild.createRole(name, color = Color.BLACK, mentionable = true)
                ?.let { guild.getRole(it) }
                ?.also { generatedRoles[id] = it.id }

    /** Deletes all roles created for [VCRoleManager] */
    suspend fun clean(guild: Guild) = generatedRoles.apply {
        toMap().forEach { (k, v) ->
            if (guild.deleteRole(v))
                generatedRoles.remove(k)
        }
    }.size

    /** Check if the voice channel allows VCRoles. */
    private fun limitSafe(channel: GuildVoiceChannel) =
        (limit == Limit.PUBLIC &&
            channel.permissionOverrides
                .firstOrNull { it.id == channel.guild.id }
                ?.allow?.contains(Permission.Connect) == true) ||
            limit == Limit.ALL
}

/**
 * A Controller Command for the passive [VCRoleManager].
 * Can enable and change the VCRole limits
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
@ExperimentalTime
object VoiceChannelRole : Command(
    "VoiceChannelRole",
    listOf("vcr", "vcrole"),
    details = buildString {
        append("A manager that creates, assigns, removes, ")
        append("and deletes VoiceChannel roles.")
    },
    guildOnly = true,
    rateLimit = 30,
    enabled = false,
    params = listOfParams("on/off" to true, "public/all" to true),
    action = a@{
        val args = message.args
        val vcRoleManager = message.guild!!
            .getAll<VCRoleManager>()
            ?.firstOrNull(VCRoleManager::active)

        when {
            args[1].matchesAny(Regecies.on, Regecies.enable) -> {
                if (vcRoleManager == null) {
                    val lim = if (args.size > 2) try {
                        VCRoleManager.Limit.valueOf(args[2].toUpperCase())
                    } catch (e: Exception) {
                        message.reply(
                            "${args[2]} is not valid. Try `ALL` or `PUBLIC`"
                        )
                        return@a
                    } else VCRoleManager.Limit.ALL
                    message.guild!!.add(VCRoleManager(lim))
                    message.sendWEmbed {
                        title("Voice Channel Roles Activated!")
                        description = buildString {
                            append("The next time someone joins a voice ")
                            append("channel they will be assigned a Role ")
                            append("with the same name of the channel ")
                            append("that can be mentioned by anyone.")
                        }
                    }
                } else message.reply(
                    buildString {
                        append("Voice Channel Roles are already enabled.")
                    }.italic
                )
            }
            args[1].matchesAny(Regecies.off, Regecies.disable) -> {
                vcRoleManager?.run {
                    clean(message.guild!!)
                    active = false
                }
                message.reply("Voice Channel Roles are now disabled".italic)
            }
            args[1].matches(Regecies.hyphen + "al*") -> {
                vcRoleManager?.apply {
                    limit = VCRoleManager.Limit.ALL
                    message.reply(
                        "Voice Channel Role set to watch All Channels.".italic
                    )
                } ?: message.reply(
                    "There is no VCRole Manager active. Use `vcr on`"
                )?.delete(30)
            }
            args[1].matches(Regecies.hyphen + "p(ub(lic)?)?") -> {
                vcRoleManager?.apply {
                    limit = VCRoleManager.Limit.PUBLIC
                    message.reply(
                        "Voice Channel Roles set to watch Public Channels."
                            .italic
                    )
                } ?: message.reply(
                    "There is no VCRole Manager active. Use `vcr on`"
                )?.delete(30)
            }
            args[1].matches(Regecies.hyphen + "c(lear|lr)?") -> {
                vcRoleManager?.apply {
                    clean(message.guild!!)
                    message.reply("VCRoles Cleared")?.delete(30)
                } ?: message.reply(
                    "There is no VCRole Manager active. Use `vcr on`"
                )?.delete(30)
            }
        }
    }
)

