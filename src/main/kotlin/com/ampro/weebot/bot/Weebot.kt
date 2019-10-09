package com.ampro.weebot.bot

import com.ampro.weebot.bot.commands.GateKeeper
import com.ampro.weebot.bot.commands.Reddicord
import com.ampro.weebot.bot.commands.VCRoleManager
import com.ampro.weebot.save
import com.serebit.strife.BotClient
import com.serebit.strife.data.Permission
import com.serebit.strife.entities.GuildMember
import com.serebit.strife.getGuild
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

suspend fun Weebot.guild(context: BotClient) = context.getGuild(guildID)


@Serializable
data class Weebot(
    @BsonId val guildID: Long,
    var prefix: String = WeebotInfo.defaultPrefix,
    val cmdSettings: MutableMap<String, CommandSettings> = mutableMapOf()
) {

    val passives: Passives = Passives()

    inner class Passives(
        var gateKeeper: GateKeeper? = null,
        var reddicord: Reddicord? = null,
        var vcRoleManager: VCRoleManager? = null
    ) {
        init {
            gateKeeper?.let { addPassive(guildID, it) }
            reddicord?.let { addPassive(guildID, it) }
            vcRoleManager?.let { addPassive(guildID, it) }
        }

        fun <P : Passive> add(p: P) {
            when (p) {
                is GateKeeper -> gateKeeper = p
                is Reddicord -> reddicord = p
                is VCRoleManager -> vcRoleManager = p
            }
            save()
        }

    }

}

data class CommandSettings(
    val permissions: MutableList<Permission>,
    val roles: MutableList<Long> = mutableListOf()
) {
    fun check(guildMember: GuildMember?): Boolean = guildMember?.run {
        roles.any { it.id in this@CommandSettings.roles } ||
            permissions.any { it in this@CommandSettings.permissions }
    } ?: false
}
