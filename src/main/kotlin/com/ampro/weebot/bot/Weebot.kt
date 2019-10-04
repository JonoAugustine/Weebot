package com.ampro.weebot.bot

import com.serebit.strife.data.Permission
import com.serebit.strife.entities.GuildMember
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId

/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */


object WeebotInfo {
    const val version = "4.0.0"
    const val jonoGitLab = "https://gitlab.com/JonoAugustine"
    const val defaultPrefix = ">"
    /** Weebot or Tobeew */
    lateinit var name: String
    val devIDs = listOf(139167730237571072L, 186130584693637131L)
}

@Serializable
data class Weebot(
    @BsonId val guildID: Long,
    var prefix: String = WeebotInfo.defaultPrefix,
    val cmdSettings: MutableMap<String, CommandSettings> = mutableMapOf()
) : Memory

data class CommandSettings(
    val permissions: MutableList<Permission>,
    val roles: MutableList<Long> = mutableListOf()
) {
    fun check(guildMember: GuildMember?): Boolean = guildMember?.run {
        roles.any { it.id in this@CommandSettings.roles } ||
            permissions.any { it in this@CommandSettings.permissions }
    } ?: false
}
