/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.ampro.weebot.extensions.MentionType.*
import com.jagrosh.jdautilities.command.CommandClientBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent

/*
 * Extension methods used for JDA elements
 */

enum class MentionType {USER, ROLE, CHANNEL}

val userMentionRegex: Regex = "^(<@!?\\d+>)$".toRegex()
val roleMentionRegex: Regex = "^(<@&\\d+>)$".toRegex()
val channelMentionRegex = "^(<#\\d+>)\$".toRegex()

/**
 * Attempt to get a [IMentionable] ID from a raw string
 *
 * @return the ID as a [Long] else -1
 */
fun String.parseMentionId() : Long
        = if (this.matchesAny(userMentionRegex, roleMentionRegex, channelMentionRegex)) {
            try {
                this.removeAll("[^0-9]".toRegex()).toLong()
            } catch (e: NumberFormatException) {
                -1L
            }
        } else {
    -1L
}

fun String.mentionType() : MentionType?  = when {
    this matches userMentionRegex -> USER
    this matches roleMentionRegex -> ROLE
    this matches channelMentionRegex -> CHANNEL
    else -> null
}

/**
 * Convert a [ISnowflake] ID to a mention String
 * @param mentionType the type of mention to convert to
 */
fun Long.asMention(mentionType: MentionType) : String = when (mentionType) {
    USER -> "<@$this>"
    ROLE -> "<@$this>"
    CHANNEL -> "<#$this>"
}

infix fun Member.outRanks(other: Member) : Boolean {
    var myhigh = -1
    this.roles.forEach { if (it.position > myhigh) myhigh = it.position }
    var theirHigh = -1
    other.roles.forEach { if (it.position > theirHigh) theirHigh = it.position }
    return myhigh > theirHigh
}

infix fun Member.compareHighestRoleTo(other: Member) : Int {
    var myhigh = -1
    this.roles.forEach { if (it.position > myhigh) myhigh = it.position }
    var theirHigh = -1
    other.roles.forEach { if (it.position > theirHigh) theirHigh = it.position }
    val c = myhigh - theirHigh
    return when {
        c > 0 -> 1
        c < 0 -> -1
        else -> 0
    }
}

fun Member.hasPerms(vararg p: Permission) = this.permissions.containsAll(p.toList())

fun Member.hasOneOfPerms(vararg p: Permission) : Boolean {
    p.forEach { return this.permissions.contains(it) }
    return false
}

infix fun Member.hasPerm(perm: Permission) = this.permissions.contains(perm)

fun MessageReceivedEvent.splitArgsRaw() = message.contentRaw.split("\\s+".toRegex())
fun GuildMessageReceivedEvent.splitArgsRaw() = message.contentRaw.split("\\s+".toRegex())

/**
 * Add multiple commands from an [Iterable].
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun CommandClientBuilder.addCommands(commands: Iterable<WeebotCommand>)
        : CommandClientBuilder {
    commands.forEach { this.addCommand(it) }
    return this
}

infix fun User.`is`(id: Long) = this.idLong == id

fun GuildMessageReceivedEvent.isValidUser(guild: Guild, users: Set<User> = emptySet(),
                                          roles: List<Role> = emptyList()) : Boolean {
    return when {
        author.isBot -> false
        users.isEmpty() && roles.isEmpty() -> true
        users.contains(author) -> true
        !guild.isMember(author) -> false
        else -> guild.getMember(author).roles.stream().anyMatch { roles.contains(it) }
    }
}

fun GuildMessageReactionAddEvent.isValidUser(roles: List<Role> = emptyList(),
                                          users: Set<User> = emptySet(),
                                          guild: Guild) : Boolean {
    return when {
        user.isBot -> false
        users.isEmpty() && roles.isEmpty() -> true
        users.contains(user) -> true
        !guild.isMember(user) -> false
        else -> guild.getMember(user).roles.stream().anyMatch { roles.contains(it) }
    }
}
