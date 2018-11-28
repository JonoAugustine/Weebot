/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */
package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.commands.getInvocation
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.doc.standard.CommandInfo
import com.jagrosh.jdautilities.examples.doc.Author
import java.time.temporal.ChronoUnit

/**
 *
 * @author John Grosh (jagrosh)
 */
@CommandInfo(name = arrayOf("Ping", "Pong"), description = "Checks the bot's latency")
@Author("John Grosh (jagrosh)")
class PingCommand : Command() {
    init {
        name = "ping"
        help = "checks the bot's latency"
        guildOnly = false
        aliases = arrayOf("pong")
        category = CAT_DEV
    }

    override fun execute(event: CommandEvent) {
        val r = if (event.getInvocation().toLowerCase() == "pong") "Ping" else "Pong"
        event.reply("$r: ...") { m ->
            val ping = event.message.creationTime.until(m.creationTime, ChronoUnit.MILLIS)
            m.editMessage("$r! :ping_pong: Ping: " + ping + "ms | Websocket: "
                    + event.jda.ping + "ms").queue()
        }
    }

}
