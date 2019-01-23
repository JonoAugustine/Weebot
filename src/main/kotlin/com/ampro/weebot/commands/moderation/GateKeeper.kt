package com.ampro.weebot.commands.moderation

import com.ampro.weebot.WAITER
import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.CAT_UNDER_CONSTRUCTION
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.member.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.apache.commons.lang3.tuple.MutablePair
import java.util.concurrent.TimeUnit.MINUTES


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

    companion object { val MAX_FORMATS = 20 }

    /** Roles IDs to assign on member join*/
    internal val autoRoles = mutableListOf<Long>()

    /** Message to send to new Member on join */
    internal var joinDm: String? = null

    /**
     * Message to send in-server on member join
     * [TextChannel.getIdLong] to Formats
     */
    internal var memberJoin = MutablePair(-1L, mutableListOf<String>())

    /**
     *  Message to send in-server on member leave
     *  [TextChannel.getIdLong] to Formats
     */
    internal var memberLeave = MutablePair(-1L, mutableListOf<String>())

    override fun accept(bot: Weebot, event: Event) {
        if (event !is GenericGuildMemberEvent) return

        autoRoles.removeIf { event.guild.getRoleById(it) == null }
        val member = event.member
        val bot = getWeebotOrNew(event.guild)

        when (event) {
            is GuildMemberJoinEvent -> {
                //Send In Server
                if (memberJoin.left != -1L && memberJoin.right.isNotEmpty()) {
                    event.guild.getTextChannelById(memberJoin.left)?.apply {
                        sendMessage(fillFormat(member, memberJoin.right.random())).queue()
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
    arrayOf("gatekeeper", "wmc", "gkc"), CAT_UNDER_CONSTRUCTION, "",
    "Set Messages to be sent and Roles to be set when a new Member joins",
    guildOnly = true, userPerms = arrayOf(ADMINISTRATOR)) {

    val eb: EmbedBuilder get() {
        return makeEmbedBuilder("Gate Keeper", null, """
                $InboxTray to change the member join message
                $Beginner to change the on-join auto-assigned roles
                $IncomingEnvelope to change the member join direct message
                $OutboxTray to change the member leave message
                """.trimIndent()).addField("Guide", """
                    You can include ``#channelMentions`` and ``@userMentions``.
                    You can use ``{USER}`` in place of the member's name.
                """.trimIndent(), true)
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author, event.creationTime)
        val gk: GateKeeper = bot.getPassive() ?: run {
            val gk = GateKeeper()
            bot.add(gk)
            return@run gk
        }

        val menu = SelectableEmbed(event.author, false, eb.build(),
            listOf(InboxTray to { _, _ ->
                com.ampro.weebot.extensions.TODO(event)
                event.reply("What channel do you want me to send the member " +
                        "join message to?\n``#channelMention``")
                waitThen(event) { first ->
                    if (first.message.mentionedChannels.isEmpty())
                        return@waitThen event.respondThenDeleteBoth("No channel mentioned.")
                    val channel = first.message.mentionedChannels.first()
                    event.reply(
                        "Please enter the member join message that will be sent to ${channel.asMention}"
                    )
                    waitThen(event) {
                        gk.memberJoin.left = channel.idLong
                        gk.memberJoin.right.add(it.message.contentRaw)
                        event.reply("Set!")
                    }
                }
            }, Beginner to { _, _ ->
                com.ampro.weebot.extensions.TODO(event)
                waitThen(event) {
                    waitThen(event) {

                    }
                }
            }, IncomingEnvelope to { _, _ ->
                com.ampro.weebot.extensions.TODO(event)
                waitThen(event) {}
            }, OutboxTray to { _, _ ->
                com.ampro.weebot.extensions.TODO(event)
                waitThen(event) {
                    waitThen(event) {

                    }
                }
            }), 5) { m ->

        }.display(event.channel)
    }

    fun waitThen(event: CommandEvent, action: (MessageReceivedEvent) -> Unit) {
        WAITER.waitForEvent(MessageReceivedEvent::class.java, {
            it.isValidUser(event.guild, event.member.user, event.channel)
        }, action, 5L, MINUTES) {}
    }

}
