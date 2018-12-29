/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_MOD
import com.ampro.weebot.commands.CAT_UNDER_CONSTRUCTION
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.main.WAITER
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES

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
            .build()
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild.idLong)
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
            .addField("Passives", if (config.enablePassives) "on" else "off", true)
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
                SelectableEmbed(event.author, strdEmbedBuilder
                    .setTitle("Prefix").setDescription("""
                        My current prefixes are: ``${
                    bot.settings.prefixs.joinToString(", ")}
                        $A to add a prefix
                        $C to change the prefix
                    """.trimIndent())
                    .build(),
                    listOf(
                        A to { _ ->
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
                        }, C to { _ ->
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
        TODO()
    }
}

private class CmdSetNsfw : WeebotCommand("nsfw", arrayOf("naughty"), CAT_UNDER_CONSTRUCTION,
    "<nsfw> [newSetting]", "View or Change your weebot's nsfw setting",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)){

    companion object {
        //TODO
        val normField: Field = Field("NSFW", "this doesnt do anything....*yet*${Smirk
            .unicode}",
            true)
    }

    override fun execute(event: CommandEvent) {
        TODO()
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
        val channels = event.message.mentionedChannels

        when {
            channels.isEmpty() -> {
                SelectableEmbed(event.author, strdEmbedBuilder
                    //TODO
                    .build(), listOf(C to { _ ->
                    event.reply("Which channel should I send log messages to?")
                    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java, {
                        it.isValidUser(users = setOf(event.author), guild = event.guild)
                    }, { event_2 ->
                        val channels_2 = event_2.message.mentionedChannels
                        if (channels_2.isEmpty() || channels_2.size > 1) event.reply(
                            "*Please mention ONE (1) text channel.*")
                        else set(event, bot, channels_2[0])
                    }, 3L, MINUTES) { }
                })) { it.clearReactions().queueAfter(250, MILLISECONDS) }.display(
                    event.textChannel)
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
            "Enable anonymous usage tracking for this server's Weebot\n``set skynet " +
                    "[on/off]``\nAliases: track, tracking", true)
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}
