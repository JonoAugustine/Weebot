/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** @return the string arguments of the message split into a [List] */
fun CommandEvent.splitArgs(): List<String> = this.args.split(" ")

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete
 */
fun CommandEvent.deleteWithResponse(reason: String, delay: Int = 10) {
    this.reply("*$reason*") {
        GlobalScope.launch (Dispatchers.Default) {
            delay(delay * 1000L)
            event.message.delete().reason(reason).queue()
            it.delete().reason(reason).queue()
        }
    }
}
