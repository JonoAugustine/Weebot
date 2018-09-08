package com.ampro.weebot.commands.stonks

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.Command
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.listener.events.BetterMessageEvent


class PositionTrackerCmd
    : Command("PositionTracker", arrayOf("ptc", "portfolio", "port"), "", "",
        true, false, 0, false, false)
      , IPassive {

    /**
     * Performs the action of the command.
     * @param bot The [Weebot] which called this command.
     * @param event The [BetterMessageEvent] that called the command.
     */
    override fun execute(bot: Weebot?, event: BetterMessageEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Take in a [BetterMessageEvent] to interact with.
     * @param bot The weebot who called
     * @param event The event to receive.
     */
    override fun accept(bot: Weebot?, event: BetterMessageEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** @return `false` if the passive is no longer active
     */
    override fun dead(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
