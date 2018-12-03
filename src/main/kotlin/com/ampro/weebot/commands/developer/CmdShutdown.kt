/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.main.shutdown
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.examples.command.ShutdownCommand

class CmdShutdown : ShutdownCommand() {
    init {
        aliases = arrayOf("tite", "killbot", "devkill")
        category = CAT_DEV
        hidden = true
    }

    override fun execute(event: CommandEvent) {
        event.reactWarning()
        event.reply("Shutting down all Weebots...")
        //TODO send text to Jono
        shutdown(event.author)
    }
}
