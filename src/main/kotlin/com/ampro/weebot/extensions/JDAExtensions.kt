/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.ampro.weebot.MLOG
import com.ampro.weebot.extensions.MentionType.*
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.requests.RestAction
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

/*
 * Extension methods used for JDA elements
 */

fun TODO(event: CommandEvent) = event.reply("This action is still under construction.")

fun CommandClientBuilder.addCommandsWithCheck(commands: Iterable<WeebotCommand>)
        :  CommandClientBuilder {
    var err = false
    commands.forEach { c ->
        if (c.name.toList().has { it.isUpperCase() }) {
            MLOG.elog(null,
                "Command name ${c.name} has is capitalized when it should not be!")
            err = true
        }
        c.aliases.forEach { a ->
            if (a.toList().has { it.isUpperCase() }) {
                MLOG.elog(null,
                    "Command name $a has is capitalized when it should not be!")
                err = true
            }
        }
        commands.filter { it != c }.forEach { c2 ->
            if ((c2.aliases + listOf(c2.name)).map { it.toLowerCase() }
                        .contains(c.name.toLowerCase())) {
                MLOG.elog(null,
                    "${c.name} shares a name with ${c2.name}!")
                err = true
            }
        }
    }
    if (err) System.exit(0)
    return this.addCommands(commands)
}

operator fun ShardManager.get(shardIndex: Int) = shards!![shardIndex]

enum class MentionType { USER, ROLE, CHANNEL }

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

/** The user and bot count of the [Guild] */
val Guild.size: Int get() = this.members.size
/** The non-bot user count of the [Guild] */
val Guild.trueSize: Int get() = this.members.filterNot { it.user.isBot }.size

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

/** Queue and ignore any result */
fun <T> RestAction<T>.queueIgnore(secDelay: Long = 0) {
    this.queueAfter(secDelay, TimeUnit.SECONDS, {},{})
}

val CommandEvent.creationTime: OffsetDateTime
    get() = this.message.creationTime

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
infix fun User.`is`(user: User) = this.idLong == user.idLong

fun MessageReceivedEvent.isValidUser(guild: Guild?, user: User,
                                     channel: MessageChannel? = null)
        = this.isValidUser(guild, setOf(user), emptySet(), channel)

fun MessageReceivedEvent.isValidUser(guild: Guild?, users: Set<User> = emptySet(),
                                     roles: Set<Role> = emptySet(),
                                     channel: MessageChannel? = null) = when {
    author.isBot -> false
    channel != null && channel.idLong != this.channel.idLong -> false
    guild != null && !isFromType(TEXT) -> false
    this.guild?.id ?: -2 != guild?.id ?: -2 -> false
    users.isEmpty() && roles.isEmpty() -> true
    users.contains(author) -> true
    !(guild?.isMember(author) ?: true) -> false
    else -> guild?.getMember(author)?.roles?.has { roles.contains(it) } ?: true
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


/**
 * Contains validation information and methods to restrict access to any entity.
 * <br></br>
 * Restrictions by positivity (disallow or allow access). <br></br>
 * Restrictions can be made by [Role], [TextChannel], or
 * [User]. <br></br>
 * Restrictions are made using
 * [net.dv8tion.jda.core.entities.ISnowflake] Long IDs. <br></br>
 *
 *
 * @author Jonathan Augustine
 */
class Restriction {

    /** An indication of the Command's restriction in a guild.  */
    enum class Status {
        /** The Item has no restrictions  */
        OPEN,
        /** The Item has restrictions  */
        RESTRICTED,
        /** The Item is disabled  */
        DISABLED
    }

    val allowedUsers: MutableList<Long> = ArrayList()
    val blockedUsers: MutableList<Long> = ArrayList()
    val allowedRoles: MutableList<Long> = ArrayList()
    val blockedRoles: MutableList<Long> = ArrayList()
    val allowedTextChannels: MutableList<Long> = ArrayList()
    val blockedTextChannels: MutableList<Long> = ArrayList()
    val allowedVoiceChannels: MutableList<Long> = ArrayList()
    val blockedVoiceChannels: MutableList<Long> = ArrayList()

    /**
     * Add allowed [users][User], [roles][Role], or
     * [TextChannels][TextChannel].
     * Will remove any from the "blocked" list if present.
     * @param iSnowflakes The iSnowflakes to allow
     */
    fun allow(iSnowflakes: List<ISnowflake>) {
        if (iSnowflakes.isEmpty()) { return }
        when {
            iSnowflakes[0] is User -> iSnowflakes.forEach { user ->
                this.blockedUsers.remove(user.idLong)
                this.allowedUsers.add(user.idLong)
            }
            iSnowflakes[0] is Role -> iSnowflakes.forEach { role ->
                this.blockedRoles.remove(role.idLong)
                this.allowedRoles.add(role.idLong)
            }
            iSnowflakes[0] is TextChannel -> iSnowflakes.forEach { channel ->
                this.blockedTextChannels.remove(channel.idLong)
                this.allowedTextChannels.add(channel.idLong)
            }
            iSnowflakes[0] is VoiceChannel -> iSnowflakes.forEach { channel ->
                blockedVoiceChannels.remove(channel.idLong)
                allowedVoiceChannels.add(channel.idLong)
            }
        }
    }

    /**
     * Add blocked [users][User], [roles][Role], or
     * [TextChannels][TextChannel].
     * Will remove any from the "allowed" list if present.
     *
     * @param iSnowflakes The iSnowflakes to block
     */
    fun block(iSnowflakes: List<ISnowflake>) {
        if (iSnowflakes.isEmpty()) {
            return
        }
        when {
            iSnowflakes[0] is User -> iSnowflakes.forEach { user ->
                this.allowedUsers.remove(user.idLong)
                this.blockedUsers.add(user.idLong)
            }
            iSnowflakes[0] is Role -> iSnowflakes.forEach { role ->
                this.allowedRoles.remove(role.idLong)
                this.blockedRoles.add(role.idLong)
            }
            iSnowflakes[0] is TextChannel -> iSnowflakes.forEach { channel ->
                this.allowedTextChannels.remove(channel.idLong)
                this.blockedTextChannels.add(channel.idLong)
            }
        }
    }

    /**
     * Add an allowed [User].
     * Will remove the user from the "blocked" list if present.
     * @param user The user to allow
     */
    fun allow(user: User) {
        this.blockedUsers.remove(user.idLong)
        this.allowedUsers.add(user.idLong)
    }

    /**
     * Block a user's access.
     * Will remove the user from "allowed" list if present
     * @param user The user to black access from
     */
    fun block(user: User) {
        this.allowedUsers.remove(user.idLong)
        this.blockedUsers.add(user.idLong)
    }

    /**
     * Check if a user has been allowed or blocked.
     * @param user The user to check
     * @return `true` if the user is allowed, or there are no restrictions.
     */
    fun isAllowed(user: User): Boolean {
        return if (!this.allowedUsers.isEmpty()) {
            this.allowedUsers.contains(user.idLong)
        } else if (!this.blockedUsers.isEmpty()) {
            !this.blockedUsers.contains(user.idLong)
        } else {
            true
        }
    }

    /**
     * Allow a Role access.
     * Will remove the role from the "blocked" list if present.
     * @param role The role to allow access
     */
    fun allow(role: Role) {
        this.blockedRoles.remove(role.idLong)
        this.allowedRoles.add(role.idLong)
    }

    /**
     * Block a Role's access.
     * Will remove the Role from the "blocked" list if present.
     * @param role The role to block access.
     */
    fun block(role: Role) {
        this.allowedRoles.remove(role.idLong)
        this.blockedRoles.add(role.idLong)
    }

    /**
     * Check if a Role has been allowed or blocked.
     * @param role The user to check
     * @return `true` if the role is allowed, or there are no restrictions.
     */
    fun isAllowed(role: Role): Boolean {
        return if (!this.allowedRoles.isEmpty()) {
            this.allowedRoles.contains(role.idLong)
        } else if (!this.blockedRoles.isEmpty()) {
            !this.blockedRoles.contains(role.idLong)
        } else {
            true
        }
    }

    /**
     * Allow a TextChannel access.
     * Will remove the channel from the "blocked" list if present.
     * @param channel The TextChannel to allow access
     */
    fun allow(channel: TextChannel) {
        this.blockedTextChannels.remove(channel.idLong)
        this.allowedTextChannels.add(channel.idLong)
    }

    /**
     * Block a TextChannel access.
     * Will remove the channel from the "allowed" list if present.
     * @param channel The TextChannel to block access
     */
    fun block(channel: TextChannel) {
        this.allowedTextChannels.remove(channel.idLong)
        this.blockedTextChannels.add(channel.idLong)
    }

    /**
     * Check if a TextChannel has been allowed or blocked.
     * @param channel The TextChannel to check
     * @return `true` if the TextChannel is allowed,
     * or there are no restrictions.
     */
    fun isAllowed(channel: TextChannel): Boolean {
        return if (!this.allowedTextChannels.isEmpty()) {
            this.allowedTextChannels.contains(channel.idLong)
        } else if (!this.blockedTextChannels.isEmpty()) {
            !this.blockedTextChannels.contains(channel.idLong)
        } else {
            true
        }
    }

    /** @return `true` if there are any restrictions.
     */
    fun restricted(): Boolean {
        return (allowedUsers.isEmpty() || blockedUsers.isEmpty() || allowedRoles.isEmpty() || blockedRoles.isEmpty() || allowedTextChannels.isEmpty() || blockedTextChannels.isEmpty())
    }

    /**
     * Get an [EmbedBuilder] with each allow or block list as it's own
     * field. The EmbedBuilder is in standard Weebot form, untitled with no
     * description.
     * @param guild The guild the restriction is housed in.
     * @return
     */
    fun toEmbedBuilder(guild: Guild): EmbedBuilder {
        val sb = StringBuilder()
        val eb = strdEmbedBuilder
        if (!allowedUsers.isEmpty()) {
            this.allowedUsers.forEach { u ->
                sb.append("*").append(guild.getMemberById(u).effectiveName).append("*\n")
            }
            eb.addField("Allowed Members", sb.toString(), true)
            sb.setLength(0)
        }
        if (!this.allowedRoles.isEmpty()) {
            this.allowedRoles.forEach { r ->
                sb.append("*").append(guild.getRoleById(r).name).append("*\n")
            }
            eb.addField("Allowed Roles", sb.toString(), true)
            sb.setLength(0)
        }
        if (!this.allowedTextChannels.isEmpty()) {
            this.allowedTextChannels.forEach { tc ->
                sb.append("*").append(guild.getTextChannelById(tc).name).append("*\n")
            }
            eb.addField("Allowed TextChannels", sb.toString(), true)
            sb.setLength(0)
        }
        if (!this.blockedUsers.isEmpty()) {
            this.blockedUsers.forEach { u ->
                sb.append("*").append(guild.getMemberById(u).effectiveName).append("*\n")
            }
            eb.addField("Blocked Members", sb.toString(), true)
            sb.setLength(0)
        }
        if (!this.blockedRoles.isEmpty()) {
            this.blockedRoles.forEach { r ->
                sb.append("*").append(guild.getRoleById(r).name).append("*\n")
            }
            eb.addField("BlockedRoles", sb.toString(), true)
            sb.setLength(0)
        }
        if (!this.blockedTextChannels.isEmpty()) {
            this.blockedTextChannels.forEach { tc ->
                sb.append("*").append(guild.getTextChannelById(tc).name).append("*\n")
            }
            eb.addField("Blocked TextChannels", sb.toString(), true)
            sb.setLength(0)
        }
        return eb
    }

}
