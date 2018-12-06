/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.IPassive
import net.dv8tion.jda.core.events.Event

/**
 *
 * TODO
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class Reddicord : IPassive {
    var dead: Boolean = false
    override fun dead() = dead

    override fun accept(bot: Weebot, event: Event) {

    }
}
