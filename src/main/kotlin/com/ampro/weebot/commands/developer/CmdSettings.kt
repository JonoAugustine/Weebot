package com.ampro.weebot.commands.developer

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.IPassive
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

//TODO

class TrackerInitPassive : IPassive {

    var checked: Boolean = false

    override fun accept(bot: Weebot, event: Event) {
        if (event !is GuildMessageReceivedEvent) return
        //Ask for tracking perms
    }

    override fun dead(): Boolean {
        return false
        TODO()
    }
}
