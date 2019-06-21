/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.CACHED_POOL
import com.ampro.weebot.ON
import com.ampro.weebot.commands.CAT_MOD
import com.ampro.weebot.commands.CAT_UNDER_CONSTRUCTION
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.database.track
import com.ampro.weebot.extensions.JDA_MESSAGE_HISTORY_MAX
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.plus
import com.ampro.weebot.extensions.removeIf
import com.ampro.weebot.extensions.respondThenDelete
import com.ampro.weebot.extensions.respondThenDeleteBoth
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.util.REG_HYPHEN
import com.ampro.weebot.util.formatTime
import com.jagrosh.jdautilities.command.Command.CooldownScope.GUILD
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_CHANNEL
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission.MANAGE_CHANNEL
import net.dv8tion.jda.core.Permission.MESSAGE_HISTORY
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.Permission.MESSAGE_WRITE
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Message.MentionType.ROLE
import net.dv8tion.jda.core.entities.Message.MentionType.USER
import net.dv8tion.jda.core.entities.MessageHistory
import net.dv8tion.jda.core.entities.TextChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.MutableList
import kotlin.collections.any
import kotlin.collections.filterNot
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.indexOfFirst
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.none
import kotlin.collections.reversed
import kotlin.collections.set
import kotlin.system.measureTimeMillis

/**
 * A File containing [WeebotCommand]s regarding [Message] Management (deletion, etc(?))
 */


/**
 *
 */
 class CmdMoveConversation : WeebotCommand(
    "mvm", "MOVECOVO", "Move Conversation",
    arrayOf("mcc", "moveconvo", "convomove"),
    CAT_MOD, "Copies and pastes message into a different channel",
    guildOnly = true, botPerms = arrayOf(MESSAGE_MANAGE, MESSAGE_HISTORY),
    userPerms = arrayOf(MESSAGE_MANAGE)
) {

    override fun execute(event : WeebotCommandEvent) {
        if (event.args.isBlank()) return
        if (event.message.mentionedChannels.isEmpty())
            return event.respondThenDeleteBoth("A destination channel must be mentioned.", 15)

        //mvm [num_messages] <#channel> [@/members...]
        val ment = event.message.mentionedMembers
        val channel = event.message.mentionedChannels.first()
        val num = try {
            val i = event.argList[0].toInt()
            if (i > JDA_MESSAGE_HISTORY_MAX) return event.respondThenDeleteBoth(
                    "Can only move 1 to 100 messages", 15)
            i
        } catch (e: Exception) {1}

        MessageHistory(event.textChannel).retrievePast(100).queue succ@{ his ->
            his.removeIf { it.idLong == event.message.idLong }
            val s = if (ment.isNotEmpty()) his.indexOfFirst { ment.any {
                m -> m.user.id == it.author.id } } else 0
            if (s == -1) return@succ event.respondThenDeleteBoth(
                    "Could not locate any messages ${ment.joinToString { it.effectiveName }}", 20)
            val target: MutableList<Message> = his.subList(s,
                    if (num in s until his.size) num else his.size).apply {
                if (ment.isNotEmpty())
                    removeIf { mes -> ment.none { mem -> mem.user.idLong == mes.author.idLong } }
                removeIf { it.contentDisplay.isNullOrBlank() }
            }

            if (target.isEmpty()) return@succ event.respondThenDeleteBoth(
                    "Could not locate any messages ${ment.joinToString {
                        it?.effectiveName ?:"" }}", 20)

            val messages = target.map { "*``${event.guild.getMember(it.author).effectiveName
            }``* : ${it.contentDisplay}" }.reversed()
            /** time delay to ensure ordered messages */ var t = 0L
            MessageBuilder("""***Moved messages from*** ${event.textChannel.asMention}
                ${messages.joinToString("\n")}""".trimIndent()).buildAll()
                .forEach { channel.sendMessage(it).queueAfter(t, MILLISECONDS); t += 50 }
            event.respondThenDelete("${event.member.effectiveName
            } moved ${target.size} messages to ${channel.asMention}", 30)
            if (target.none { it.idLong == event.message.idLong }) target.add(event.message)
            event.textChannel.deleteMessages(target).queueAfter(250, MILLISECONDS)
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Conversation Move", """
            """.trimIndent())
            //TODO
            .build()
    }

}

/**
 * Automatically deletes the marked message after a given time or 30 seconds by default
 *
 * Formatted: \<deleteme> [-t time] [message]
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdSelfDestruct : WeebotCommand(
    "selfdestruct", "SELFDESTRUCT", "Self Destruct Message",
    arrayOf("deleteme","cleanthis","deletethis","covertracks","whome?","podh","sdc"),
    CAT_MOD,
    "Deletes the marked message after the given amount of time (30 sec default)",
    HelpBiConsumerBuilder("SelfDestruct Message")
        .setDescription("Deletes the marked message after the given amount of" +
            " time (30 sec by " + "default)")
        .addField("Arguments", "[-t delaySeconds] [message]")
        .addField("Aliases", "deleteme, cleanthis, deletethis, covertracks, whome?, podh,sdc")
        .build(),
    true, cooldown = 0, botPerms = arrayOf(MESSAGE_MANAGE)
) {
    override fun execute(event: WeebotCommandEvent) {
        track(this, event.guild.bot, event.author, event.creationTime)
        val args = event.splitArgs()
        val delay = try {
            if (args[0].matches(Regex("^-[t|T]$"))) {
                args[1].toLong()
            } else { 30L }
        } catch (e: Exception) { 30L }
        event.reactWarning()
        event.message.delete().queueAfter(delay, SECONDS)
    }
}

/**
 * A [WeebotCommand] to delete a number (2-1,000) messages from a given text channel.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdPurge : WeebotCommand(
    "purge", "PURGE", "Chat Purge", arrayOf("prune", "clean", "clear"),
    CAT_MOD, "Delete multiple messages, 2-1,000.",
    guildOnly = true, cooldown = 3, cooldownScope = GUILD,
    botPerms = arrayOf(MESSAGE_MANAGE), userPerms = arrayOf(MESSAGE_MANAGE)
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Chat Purge", false).setDescription(
            "Delete 2 to 1,000 messages from the chat.\n``purge <2...1,000>``"
        ).setAliases(this.aliases).build()
    }

    // \purge #
    override fun execute(event: WeebotCommandEvent)  {
        var toDelete = try {
            event.splitArgs()[0].toInt() + 1
        } catch (e: Exception) { return }
        val td = toDelete

        if (toDelete !in 2..1_000) {
            event.respondThenDeleteBoth("You must choose a number between 2 and 1,000.")
            return
        }
        track(this, event.guild.bot, event.author, event.creationTime)
        val list = mutableListOf<Int>()
        while (toDelete > 0) {
            if (toDelete >= 100) {
                list.add(100)
                toDelete -= 100
            } else {
                list.add(toDelete)
                toDelete = 0
            }
        }

        GlobalScope.launch {
            list.forEach { toDel ->
                MessageHistory(event.textChannel).retrievePast(toDel).queue {
                    event.textChannel.deleteMessages(it).queue()
                }
                delay(1_000)
            }
            event.respondThenDeleteBoth("*${event.author.name} cleared $td messages from ${
            event.textChannel.name}*", 5)
        }

    }

}

/**
 * A [WeebotCommand] to lock all messages from being sent in a chat
 * by Locking out mentioned roles/members that Weebot has power over.
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class CmdChatLock : WeebotCommand(
    "chatlock", "CHATLOCK", "Chat Lock",
    arrayOf("lockchat", "pausechat", "blockchat"),
    CAT_UNDER_CONSTRUCTION, "Block messages to the TextChannel for a given time.",
    guildOnly = true, cooldown = 15, cooldownScope = USER_CHANNEL,
    botPerms = arrayOf(MESSAGE_MANAGE, MANAGE_CHANNEL),
    userPerms = arrayOf(MANAGE_CHANNEL)
) {

    /** [TextChannel.getIdLong] -> [AtomicInteger] remaining time, ([Long] -> [Unit]) */
    private var lockMap = ConcurrentHashMap<Long, Pair<AtomicInteger, (Long) -> Unit>>()

    private val lockWaiter by lazy {
        GlobalScope.launch(CACHED_POOL) {
            while (ON) {
                lockMap.forEach {
                    if (it.value.first.decrementAndGet() <= 0)
                        it.value.second(it.key)
                }
                val delay = 1_000 - measureTimeMillis {
                    lockMap.removeIf { _, pair -> pair.first.get() <= 0 }
                }
                if (delay >= 0L) delay(delay)
            }
        }
    }

    override fun execute(event: WeebotCommandEvent) {
        //"<[-s seconds] [-m minutes]> [from @/Roles @/Members]",
        val args = event.splitArgs()
        if (args.isEmpty()) {
            return event.respondThenDeleteBoth("*Please specify a time span.*")
        }
        val channel = event.textChannel
        //Parse time
        var seconds = 0

        fun value(index: Int) : Int = if (index >= 0) try {
            args[index + 1].toInt()
        } catch (e: Exception) {
            event.respondThenDeleteBoth("*Invalid value at arg* ``${index + 1}``")
            -2
        } else -2

        val sec = value(args.indexOfFirst { it.matches(REG_HYPHEN + "s+") })
        val min = value(args.indexOfFirst { it.matches(REG_HYPHEN + "m+") })

        if (sec > 0) seconds += sec
        if (min > 0) seconds += min * 60

        if (seconds !in 30..(30 * 60)) {
            return event.respondThenDeleteBoth(
                "*Must be between ``30`` seconds and ``30`` minutes.*", 20)
        }

        lockMap[channel.idLong]?.also {
            event.respondThenDeleteBoth("This channel is already on lockdown.")
        } ?: run {
            //Start Lockdown
            val origOverrides = channel.permissionOverrides.filterNot {
                event.message.isMentioned(it.role ?: it.member)
            }

            //TODO Resetting permissions not working
            val addedPerms = event.message.mentionedUsers.map {
                it to listOf(MESSAGE_WRITE)
            }

            channel.manager.apply {
                //set all permission overrides
                origOverrides.forEach {
                    removePermissionOverride(it.role ?: it.member)
                    if (it.isMemberOverride) {
                        putPermissionOverride(it.member, null, listOf(MESSAGE_WRITE))
                    } else putPermissionOverride(it.role, null, listOf(MESSAGE_WRITE))
                }
                //addedPerms.forEach { putPermissionOverride(it.first, null, it.second) }
            }.queue()
            lockMap[channel.idLong] = AtomicInteger(seconds) to { id ->
                //UNset all overrides
                channel.manager.apply {

                    //addedPerms.forEach { removePermissionOverride(it.first) }

                    origOverrides.forEach { override ->
                        if (override.isMemberOverride)
                            putPermissionOverride(override.member, override.allowed,
                                override.denied)
                        else putPermissionOverride(override.role, override.allowed,
                            override.denied)
                    }
                    event.reply("*Channel Unlocked*")
                }.queue()
            }
            event.reply(
                "*Chat locked to ${event.message.getMentions(USER, ROLE).joinToString(
                    ", ") { it.asMention }} for ${seconds.toLong().formatTime()}*")
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Chat Lock", """
            Temporarily lock a TextChannel from any messages being sent.
            ``<[-s seconds] [-m minutes] [-h hours]> [@ Roles or Members]``
            Mention the Roles and/or users you want to allow to send messages during the
             lockdown.
             Minimum time: 30 seconds
             Maximum time: 30 minutes
        """.trimIndent())
            .setAliases(aliases)
            .build()
    }
}
