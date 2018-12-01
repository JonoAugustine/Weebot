/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.database.constants.Emoji.X
import com.ampro.weebot.database.constants.Emoji.heavy_check_mark
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.database.getWeebot
import com.ampro.weebot.extensions.hasPerm
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent


/**
 * Waits for a response to the initial message asking for tracking permissions.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class TrackerInitPassive(val enableMessage: Message) : IPassive {

    var dead: Boolean = false

    companion object {
        const val onOff = "Statistics Tracking can be turned on or off at anytime with " +
                "the command ``w!skynet [on/off]``."
        val acceptEmbed get() = strdEmbedBuilder.setTitle("Statistics Tracking Enabled!")
            .setDescription("Thank you for helping Weebot's development!\n$onOff").build()
        val denyEmbed   get() =  strdEmbedBuilder.setTitle("Keeping Statistics Tracking Off")
            .setDescription("This can be turned on later\n$onOff").build()
    }

    override fun accept(bot: Weebot, event: Event) {
        if (dead) return
        when (event) {
            is GenericGuildMessageEvent -> {
                val guild = event.guild
                val messageID = event.messageIdLong
                when (event) {
                    //Accept by react
                    is GuildMessageReactionAddEvent -> {
                        if (event.user.isBot) return
                        if (messageID != enableMessage.idLong) return
                        val emote = event.reaction.reactionEmote
                        event.reaction.users.forEach { user ->
                            //If an Admin reacts
                            if (guild.getMemberById(user.id) hasPerm ADMINISTRATOR) {
                                when {
                                    emote.name == heavy_check_mark.unicode -> {
                                        //enable
                                        event.channel.sendMessage(
                                            acceptEmbed).queue()
                                        getWeebot(
                                            guild.idLong)?.settings?.trackingEnabled = true
                                        dead = true
                                    }
                                    emote.name == X.unicode -> {
                                        //Disable
                                        event.channel.sendMessage(
                                            denyEmbed).queue()
                                        getWeebot(
                                            guild.idLong)?.settings?.trackingEnabled = false
                                        dead = true
                                    }
                                    else -> return
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dead() = dead
}

/**
 * Parent class for viewing and changing [Weebot] settings
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdSettings : WeebotCommand("settings", arrayOf("setting", "config", "set"),
    CAT_MOD, "[settingName] [newSetting]", "View or Change your weebot's settings",
    guildOnly = true, children = arrayOf(
        CmdSetName(),
        CmdSetPrefix(),
        CmdSetExplicit(),
        CmdSetNsfw(),
        CmdSetEnablePassive(),
        CmdSetLogChannel(),
        CmdSetTracking()),
    cooldown = 10
) {
    init {
        helpBiConsumer = HelpBiConsumerBuilder("Weebot Settings")
            .setDescription("View or Change your weebot's settings.")
            .addField("Available Settings:", "", false).apply {
                this.addField(CmdSetName.normField)
                //TODO other settings fields
            }
            .build()
    }

    override fun execute(event: CommandEvent) {
        TODO("not implemented")
    }
}

private class CmdSetName : WeebotCommand("nickname", arrayOf("name", "changename"),
    CAT_MOD, "<name> [newSetting]", "View or Change your weebot's nickname",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(NICKNAME_MANAGE),
    botPerms = arrayOf(NICKNAME_CHANGE)) {

    companion object {
        val normField: Field = Field("NickName",
        "Change the bot's nickname\n``\\set nickname <Name>``\nAliases: name, changename",
            true)
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}

private class CmdSetPrefix : WeebotCommand("prefix", emptyArray(), CAT_MOD,
    "<prefix> [newSetting]", "View or Change your weebot's prefix",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(NICKNAME_MANAGE)) {

    init {
        //val normField: Field = Field()
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}

private class CmdSetExplicit : WeebotCommand("explicit", arrayOf("expl", "cuss"),
    CAT_MOD, "<expl> [newSetting]", "View or Change your weebot's explicit setting",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)){

    init {
        //val normField: Field = Field()
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}

private class CmdSetNsfw : WeebotCommand("nsfw", arrayOf("naughty"), CAT_MOD,
    "<nsfw> [newSetting]", "View or Change your weebot's nsfw setting",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)){

    init {
        //val normField: Field = Field()
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}

private class CmdSetEnablePassive : WeebotCommand("passive", arrayOf("alwayson"),
    CAT_MOD, "<passive> [newSetting]", "View or Change your weebot's passive settings",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)) {

    init {
        //val normField: Field = Field()
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}

private class CmdSetLogChannel : WeebotCommand("log", arrayOf("logchannel", "setlog"),
    CAT_MOD, "[settingName] [newSetting]",
    "View or Change your weebot's logging channel", guildOnly = true, cooldown = 10,
    userPerms = arrayOf(ADMINISTRATOR)) {

    init {
        //val normField: Field = Field()
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}

private class CmdSetTracking : WeebotCommand("skynet", arrayOf("track", "tracking"),
    CAT_MOD, "<skynet> [newSetting]", "View or Change your weebot's tracking settings",
    guildOnly = true, cooldown = 10, userPerms = arrayOf(ADMINISTRATOR)) {

    init {
        //val normField: Field = Field()
    }

    override fun execute(event: CommandEvent) {
        TODO()
    }
}
