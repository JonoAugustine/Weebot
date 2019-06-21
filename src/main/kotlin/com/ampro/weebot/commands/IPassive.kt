/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.`fun`.CmdBiggifyEmoji
import com.ampro.weebot.commands.`fun`.CmdBiggifyEmoji.*
import com.ampro.weebot.commands.`fun`.CmdReacter
import com.ampro.weebot.commands.`fun`.CmdReacter.Reactor
import com.ampro.weebot.commands.`fun`.Reddicord
import com.ampro.weebot.commands.`fun`.games.cardgame.PlayerHandPassive
import com.ampro.weebot.commands.moderation.GateKeeper
import com.ampro.weebot.commands.moderation.VCGenerator
import com.ampro.weebot.commands.moderation.VCRoleManager
import com.ampro.weebot.commands.social.GuildSocialSettings
import com.ampro.weebot.commands.social.GuildSocialSettings.CurrencySettings
import com.ampro.weebot.commands.social.GuildSocialSettings.ProfileSettings
import com.ampro.weebot.commands.utility.OutHouse
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.*
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.dv8tion.jda.core.events.Event
import kotlin.jvm.internal.Reflection

/**
 * An interface defining an entity which accepts
 * events without direct invocation. <br>
 * This is best used for any entity that needs to read lots of commands,
 * or pay attention to messages while the bot is not directly called.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    Type(value = Biggifyer::class, name = "Biggifyer"),
    Type(value = CurrencySettings::class, name = "CurrencySettings"),
    Type(value = GateKeeper::class, name = "GateKeeper"),
    Type(value = PlayerHandPassive::class, name = "PlayerHandPassive"),
    Type(value = ProfileSettings::class, name = "ProfileSettings"),
    Type(value = Reactor::class, name = "Reactor"),
    Type(value = Reddicord::class, name = "Reddicord"),
    Type(value = VCRoleManager::class, name = "VCRoleManager"),
    Type(value = VCGenerator::class, name = "VCGenerator"),
    Type(value = OutHouse::class, name = "OutHouse")
)
interface IPassive {

    /**
     * Take in a {@link BetterMessageEvent} to interact with.
     * @param bot The weebot who called
     * @param event The event to receive.
     */
    fun accept(bot: Weebot, event: Event)

    /** @return {@code false} if the passive is no longer active */
    fun dead() : Boolean
}
