package com.ampro.weebot.commands

import com.ampro.weebot.bot.Weebot
import net.dv8tion.jda.core.events.Event

//TODO Stocks

internal const val FORMAT_OPTION
        = "{IN/OUT} {TICKER} \${{TARGET_PRICE}{CALL/PUT}} {EXPR_DATE} [@] " +
        "\${PRICE_PER_CONTRACT} {TIME_SPAN} {INFO}"

class Brokerage : IPassive {
    var dead = false
    override fun dead() = dead

    override fun accept(bot: Weebot, event: Event) {
        TODO("not implemented")
    }
}

//class CmdBrokerage : WeebotCommand("brokerage")
