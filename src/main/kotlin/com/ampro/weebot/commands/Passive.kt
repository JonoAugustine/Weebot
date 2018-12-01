/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.bot.Weebot
import net.dv8tion.jda.core.events.Event

/**
 * An interface defining an entity which accepts
 * events without direct invocation. <br>
 * This is best used for any entity that needs to read lots of commands,
 * or pay attention to messages while the bot is not directly called.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
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
