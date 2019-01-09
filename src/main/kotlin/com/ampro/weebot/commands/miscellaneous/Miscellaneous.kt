/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.miscellaneous

import com.ampro.weebot.commands.CAT_GEN
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.jagrosh.jdautilities.command.CommandEvent
import java.time.temporal.ChronoUnit

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
class PingCommand : WeebotCommand("ping", null, arrayOf("pong"), CAT_GEN,
    "", "Checks the bot's latency.", HelpBiConsumerBuilder("Ping ~ Pong", false)
        .setDescription("Checks the bot's latency.").build(), false, cooldown = 10
) {
    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
        val r = if (event.getInvocation().toLowerCase() == "pong") "Ping" else "Pong"
        event.reply("$r: ...") { m ->
            val ping = event.message.creationTime.until(m.creationTime, ChronoUnit.MILLIS)
            m.editMessage("$r! :ping_pong: Ping: " + ping + "ms | Websocket: "
                    + event.jda.ping + "ms").queue()
        }
    }
}
