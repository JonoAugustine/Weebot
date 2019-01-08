/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.WAITER
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.extensions.MentionType.CHANNEL
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.Command.CooldownScope.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES

/**
 * A [WeebotCommand] to view any running [IPassive]s
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class CmdTaskManager : WeebotCommand("taskmanager", arrayOf("task", "tasks"),
    CAT_GEN, "[commandName]","View all active Passives in the guild.",
    cooldown = 60, cooldownScope = USER_GUILD,
    userPerms = arrayOf(ADMINISTRATOR)) {
    override fun execute(event: CommandEvent) {
        val passives = if (event.guild != null) getWeebotOrNew(event.guild).passives
        else DAO.GLOBAL_WEEBOT.getUesrPassiveList(event.author)

        if (passives.isEmpty()) {
            event.reply("*No active COMMANDS.*")
            return
        }

        SelectablePaginator(title = "Task manager",
            description = "All running background Commands.",
            color = event.guild?.roles?.get(0)?.color ?: STD_GREEN,
            items = passives.map { passive ->
                passive::class.java.simpleName!! to { _:Int, _: Message ->
                    strdButtonMenu.setText("_").setDescription("""
                        Click $Fire to end this command
                    """.trimIndent()).setColor(Color.RED).addChoice(Fire.unicode)
                        .setAction { emote ->
                            if (emote.toEmoji()?.equals(Fire) == true) {
                                if (passives.remove(passive)) {
                                    event.reply("Removed.")
                                } else {
                                    event.reply(GEN_FAILURE_MESSAGE)
                                }
                            }
                        }.setFinalAction { it.delete().queue({},{}) }
                        .build().display(event.channel)
                }
            }, exitAction = {
                it.clearReactions().queue({},{})
            }).display(event.channel)
    }
}

/**
 * Parent class for viewing and changing [Weebot] settings
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdSettings : WeebotCommand("settings", arrayOf("setting", "config", "set"),
    CAT_MOD, "[settingName] [newSetting]", "View or Change your weebot's settings",
    guildOnly = true, cooldown = 30,
    children = arrayOf(
        CmdSetName(),
        CmdSetPrefix(),
        CmdSetExplicit(),
        CmdSetNsfw(),
        CmdSetLogChannel(),
        CmdSetTracking())

) {
    init {
        helpBiConsumer = HelpBiConsumerBuilder("Weebot Settings").setDescription(
            "View or Change your weebot's settings.").addField("Available Settings:", "")
            .addField(CmdSetName.normField).addField(CmdSetPrefix.normField)
            .addField(CmdSetExplicit.normField).addField(CmdSetNsfw.normField)
            .addField(CmdSetLogChannel.normField).addField(CmdSetTracking.normField)
            .addField(CmdLock.normField).addField(CmdBlock.normField)
            .build()
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild.idLong)
        STAT.track(this, bot, event.author, event.creationTime)
        val config = bot.settings
        val log = if (config.logchannel == -1L) "not set" else {
            event.guild.getTextChannelById(config.logchannel).asMention
        }
        event.reply(strdEmbedBuilder
            .setTitle("${event.guild.name}'s Weebot Settings")
            .addField("Nickname", config.nickname, true)
            .addField("Prefix", config.prefixes.joinToString(" ") , true)
            .addField("Explicit", if (config.explicit) "on" else "off", true)
            .addField("NSFW", if (config.nsfw) "on" else "off", true)
            .addField("LogChannel", log, true)
            .addField("Statistics Tracking", if (config.trackingEnabled) "on" else "off", true)
            .build()
        )
    }
}

private class CmdSetName : WeebotCommand("nickname", arrayOf("name", "changename"),
    CAT_MOD, "<name> [newSetting]", "Change your Weebot's nickname",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(NICKNAME_MANAGE),
    botPerms = arrayOf(NICKNAME_CHANGE)) {

    companion object {
        val normField: Field = Field("NickName",
        "Change the bot's nickname\n``\\set nickname <Name>``\nAliases: name, changename",
            true)
    }

    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
        if (event.args.isBlank()) return
        val name = event.splitArgs().joinToString(" ")
        val old = getWeebotOrNew(event.guild).settings.nickname
        event.guild.controller.setNickname(event.selfMember, name)
        event.reply("Say goodbye to $old and hello to *$name*")
        if (name.equals("Weebot", true) || name.equals(old, true))
            event.reply("*....wait a second*")
    }
}

private class CmdSetPrefix : WeebotCommand("prefix", emptyArray(), CAT_MOD,
    "[newPrefix]", "View or Change your weebot's prefix",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(NICKNAME_MANAGE)) {

    companion object {
        val normField: Field = Field("Prefix",
            "Change your weebot's prefix\n``set prefix [prefix]``\nUp to 3 characters",
            true)
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        when {
            event.args.isBlank() -> {
                STAT.track(this, bot, event.author, event.creationTime)
                SelectableEmbed(event.author, false, strdEmbedBuilder
                    .setTitle("Prefix").setDescription("""
                        My current prefixes are: ``${
                    bot.settings.prefixs.joinToString(", ")}``
                        $A to add a prefix
                        $C to change the prefix
                    """.trimIndent())
                    .build(),
                    listOf(
                        A to { _, _ ->
                            event.reply("What would you like to add? " +
                                    "*(must be under 4 characters, e.g. pw!, w!, \\)*")
                            WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                                { event_2 ->
                                    event_2.isValidUser(users = setOf(event.author),
                                        guild = event.guild)
                                }, { event_2 ->
                                    if (event_2.message.contentDisplay.length > 3) {
                                        event.reply("*That prefix is too long*")
                                    } else {
                                        bot.settings.prefixs.add(
                                            event_2.message.contentDisplay
                                        )
                                        event.reply("You can now call me with ``${
                                        event_2.message.contentDisplay}``")
                                    }
                                }, 1L, MINUTES) {}
                        }, C to { _, _ ->
                            event.reply("What would you like to change it to? " +
                                    "*(must be under 4 characters, e.g. pw!, w!, \\)*")
                            WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                                { event_2 ->
                                    event_2.isValidUser(users = setOf(event.author),
                                        guild = event.guild)
                                }, { event_2 ->
                                    if (event_2.message.contentDisplay.length > 3) {
                                        event.reply("*That prefix is too long*")
                                    } else {
                                        bot.settings.prefixs.clear()
                                        bot.settings.prefixs.add(
                                            event_2.message.contentDisplay
                                        )
                                        event.reply("You can now call me with ``${
                                        event_2.message.contentDisplay}``")
                                    }
                                }, 1L, MINUTES) {}
                        }
                    )) { it.clearReactions().queueAfter(250, MILLISECONDS) }
                    .display(event.channel)
            }
            event.args.length > 3 -> {
                event.reply("*That prefix is too long" +
                        "(must be under 4 characters, e.g. pw!, w!, \\)*")
            }
            else -> {
                STAT.track(this, bot, event.author, event.creationTime)
                bot.settings.prefixs.clear()
                bot.settings.prefixs.add(event.args)
                event.reply("You can now call me with ${event.args}")
            }
        }
    }

}

private class CmdSetExplicit : WeebotCommand("explicit", arrayOf("expl", "cuss"),
    CAT_UNDER_CONSTRUCTION, "<expl> [newSetting]",
    "View or Change your weebot's explicit setting",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)){

    companion object {
        val normField: Field = Field("Explicit",
            "Enable explicit language\n``set expl [on/off]``\nAliases: explicit, cuss",
            true)
    }

    override fun execute(event: CommandEvent) {
        TODO("Explicit Settings")
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
    }
}

private class CmdSetNsfw : WeebotCommand("nsfw", arrayOf("naughty"), CAT_UNDER_CONSTRUCTION,
    "<nsfw> [newSetting]", "View or Change your weebot's nsfw setting",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)){

    companion object {
        val normField: Field = Field("NSFW", "this doesnt do anything....*yet*${Smirk
            .unicode}",
            true)
    }

    override fun execute(event: CommandEvent) {
        TODO("NSFW Setting")
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
    }
}

private class CmdSetLogChannel : WeebotCommand("log",
    arrayOf("logchannel", "setlog", "logger"), CAT_MOD,
    "[logChannel]", "View or Change your weebot's logging channel",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)) {

    companion object {
        val normField: Field = Field("Bot Loggging Channel",
            "Set the TextChannel for me to send logs to.\n``set log [#channelMention]``",
            true)
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author, event.creationTime)
        val channels = event.message.mentionedChannels

        when {
            channels.isEmpty() -> {
                SelectableEmbed(event.author, false, strdEmbedBuilder
                    .setTitle("Logging Channen").setDescription("""
                        ${if(bot.settings.logchannel != -1L) {
                        "My Logging channel is currently set to ${
                        bot.settings.logchannel.asMention(CHANNEL)}"
                    } else "My logging channel has not been set."}
                     $C to set or change the logging channel
                    """.trimIndent())
                    .build(), listOf(C to { _, _ ->
                    event.reply("Which channel should I send log messages to?")
                    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java, {
                        it.isValidUser(users = setOf(event.author), guild = event.guild)
                    }, { event_2 ->
                        val channels_2 = event_2.message.mentionedChannels
                        if (channels_2.isEmpty() || channels_2.size > 1) event.reply(
                            "*Please mention ONE (1) text channel.*")
                        else {
                            set(event, bot, channels_2[0])
                            event.reply("The logging channel has been set to " + channels_2[0].asMention)
                        }
                    }, 3L, MINUTES) { event.respondThenDelete("*Timed out*") }
                })) { it.clearReactions().queueAfter(250, MILLISECONDS) }
                    .display(event.textChannel)
            }
            channels.size == 1 -> set(event, bot, channels[0])
            else -> event.reply("*Please mention only ONE (1) text channel.*")
        }
    }

    private fun set(event: CommandEvent, bot: Weebot, textChannel: TextChannel) {
        if (textChannel.canTalk()) {
            bot.settings.logchannel = textChannel.idLong
            event.reply("*The loggin channel has been set to ${textChannel.asMention}*")
        } else {
            event.reply("*I am unable to send messages to this channel. Check my " +
                    "permission settings and then try again.*")
        }
    }

}

private class CmdSetTracking : WeebotCommand("skynet", arrayOf("track", "tracking"),
    CAT_UNDER_CONSTRUCTION, "<skynet> [newSetting]",
    "View or Change your weebot's tracking settings",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)) {

    companion object {
        val normField: Field = Field("Usage Tracking",
            "Enable anonymous usage tracking\n``set skynet " +
                    "[on/off]``\nAliases: track, tracking", true)
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        STAT.track(this, bot, event.author, event.creationTime)
        val args = event.splitArgs()
        val enabled = bot.settings.trackingEnabled

        when {
            args.isEmpty() -> event.reply(
                "Anonymous Statistics ${if (enabled) "on" else "off"}"
            )
            args[0].matchesAny(REG_ON, REG_ENABLE) -> {
                bot.settings.trackingEnabled = true
                event.reply("Anonymous Statistics Enabled. Thank you for helping Weebot!")
            }
            args[0].matchesAny(REG_OFF, REG_DISABLE) -> {
                bot.settings.trackingEnabled = false
                event.reply("Anonymous Statistics Disabled.")
            }
        }
    }
}

private class CmdLock : WeebotCommand("lock", arrayOf("lockto"), CAT_MOD,
    "<commandName> [#textChannel]", "Lock a command to one or more TextChannels",
    guildOnly = true, cooldown = 10, cooldownScope = USER_GUILD,
    userPerms = arrayOf(ADMINISTRATOR)) {
    companion object {
        val normField = Field("Lock Command", "Lock a Command to a specific channel." +
                "\n``set lock [#channelMention...]``", true)
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        val args = event.splitArgs()
        if (args.isEmpty()) {
            return event.respondThenDelete("No Command mentioned")
        }
        val cmd = COMMANDS.firstOrNull { it.isCommandFor(args[0]) }
        STAT.track(this, bot, event.author, event.creationTime)

    }
}

private class CmdBlock : WeebotCommand("block", emptyArray(), CAT_MOD,
    "<commandName> [#textChannel]", "Block a command from a TextChannel or server",
    guildOnly = true, cooldown = 10, cooldownScope = USER_GUILD,
    userPerms = arrayOf(ADMINISTRATOR)) {
    companion object {
        val normField = Field("Block Command", "Block a Command from a channel or " +
                "entirely\n``set block [#channelMention...]``", true)
    }
    override fun execute(event: CommandEvent) {
        TODO(event)
    }
}
