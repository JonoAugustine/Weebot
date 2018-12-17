/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot

import com.ampro.weebot.extensions.strdEmbedBuilder
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*

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
