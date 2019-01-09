package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_MOD
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.WeebotCommand
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.member.*


internal const val MAX_AUTO_ROLES   = 20
internal const val MAX_MEMBER_JOIN  = 500
internal const val MAX_MEMBER_LEAVE = 500

internal val REG_USER = Regex("(?i)\\{USER}")

/** Replace a format String with a sendable string */
internal fun fillFormat(member: Member, format: String)
    = format.replace(REG_USER, member.effectiveName)

/**
 * Sends messages to new Members, in-server greeting messages, setting roles on join
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class GateKeeper : IPassive {
    internal var dead = false
    override fun dead() = dead

    /** Roles IDs to assign on member join*/
    internal val autoRoles = mutableListOf<Long>()

    /** Message to send to new Member on join */
    internal var joinDm: String? = null

    /**
     * Message to send in-server on member join
     * [TextChannel.getIdLong] to Formats
     */
    internal var memberJoin = Pair(-1L, mutableListOf<String>())

    /**
     *  Message to send in-server on member leave
     *  [TextChannel.getIdLong] to Formats
     */
    internal var memberLeave = Pair(-1L, mutableListOf<String>())

    override fun accept(bot: Weebot, event: Event) {
        if (event !is GenericGuildMemberEvent) return

        autoRoles.removeIf { event.guild.getRoleById(it) == null }
        val member = event.member
        val bot = getWeebotOrNew(event.guild)

        when (event) {
            is GuildMemberJoinEvent -> {
                //Send In Server
                if (memberJoin.first != -1L && memberJoin.second.isNotEmpty()) {
                    event.guild.getTextChannelById(memberJoin.first)?.apply {
                        sendMessage(fillFormat(member, memberJoin.second.random())).queue()
                    } ?: bot.settings.sendLog("Member Join Greeting Channel could not be found")
                }

                //Send DM
                if (!joinDm.isNullOrBlank()) {
                    event.member.user.openPrivateChannel().queue {
                        it.sendMessage(fillFormat(member, joinDm!!)).queue()
                    }
                }

                //Give Roles
                if (autoRoles.isNotEmpty()) event.guild.controller.addRolesToMember(
                    member, autoRoles.map { event.guild.getRoleById(it) })
            }
            is GuildMemberLeaveEvent -> {
                TODO("not implemented")
            }
        }
    }
}

/**
 * @author Jonathan Augustine
 * @since 2.1
 */
class CmdWelcomeMsg : WeebotCommand("welcome", "Welcome Messages",
    arrayOf("gatekeeper", "wmc", "gcc"), CAT_MOD, "",
    "Set Messages to be sent and Roles to be set when a new Member joins",
    guildOnly = true, userPerms = arrayOf(ADMINISTRATOR)) {

    override fun execute(event: CommandEvent) {
        com.ampro.weebot.extensions.TODO(event)
    }

}
