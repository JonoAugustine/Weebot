/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.Passive
import com.ampro.weebot.bot.Weebot
import com.serebit.strife.entities.GuildMember
import com.serebit.strife.entities.User
import com.serebit.strife.events.Event
import com.serebit.strife.events.GuildMemberEvent
import com.serebit.strife.events.GuildMemberJoinEvent
import com.serebit.strife.events.GuildMemberLeaveEvent
import com.serebit.strife.text.inlineCode

private const val MAX_AUTO_ROLES = 20
private const val MAX_MEMBER_JOIN = 500
private const val MAX_MEMBER_LEAVE = 500

private val REG_USER = Regex("(?i)\\{USER}")

/** Replace a format String with a sendable string */
private fun fillFormat(member: GuildMember, format: String) =
    format.replace(REG_USER, member.nickname ?: member.user.username)

private fun fillFormat(user: User, format: String) =
    format.replace(REG_USER, user.username)

/**
 * Sends messages to new Members, in-server greeting messages, setting roles on join
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class GateKeeper : Passive {

    private inner class InServerMessage(
        var channelID: Long = -1,
        var formats: MutableList<String> = mutableListOf()
    )

    private inner class PrivateMessage(var format: String)

    /** Roles IDs to assign on member join*/
    private val autoRoles = mutableListOf<Long>()

    /** Message to send to new Member on join */
    private var privateMessage: PrivateMessage? = null

    /** Message to send in-server on member join */
    private var memberJoin = InServerMessage()

    /** Message to send in-server on member leave */
    private var memberLeave = InServerMessage()

    override var active: Boolean = true

    override suspend fun predicate(event: Event, bot: Weebot): Boolean =
        event is GuildMemberEvent

    override suspend fun consume(event: Event, bot: Weebot) {
        when (event) {
            is GuildMemberJoinEvent -> {
                //Send In Server
                memberJoin
                    .takeIf { it.channelID != -1L && it.formats.isNotEmpty() }
                    ?.run {
                        event.guild.getTextChannel(channelID)
                            ?.send(fillFormat(event.member, formats.random()))
                    }
                //Send DM
                privateMessage?.run {
                    event.member.user.createDmChannel()
                        ?.send(fillFormat(event.member, format))
                }
                //Give Roles
                autoRoles.forEach { event.member.addRole(it) }
            }
            is GuildMemberLeaveEvent -> {
                memberLeave
                    .takeIf { it.channelID != -1L && it.formats.isNotEmpty() }
                    ?.run {
                        event.guild.getTextChannel(channelID)
                            ?.send(fillFormat(event.user, formats.random()))
                    }
            }
        }
    }
}

/**
 * @author Jonathan Augustine
 * @since 2.1
 */
object GateKeeperCmd : Command(
    "GateKeeper",
    listOf("gkc", "welcome"),
    details = buildString {
        append("Make welcome & goodbye messages for users joining the server")
        append(" and set roles to be given when a new user joins.\n")
        append("Use ").append("{USER}".inlineCode).append(" in place of the ")
        append("member's name. You can also include ")
        append("#channel_mentions".inlineCode).append(" and ")
        append("@user_mentions".inlineCode).append(".")
    },
    guildOnly = true,
    rateLimit = 30,
    params = listOfParams(),
    enabled = false,
    action = {
        TODO("GateKeeper wating on MENU")
    }
)
