/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.serebit.strife.entities.inlineField
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title

object Settings : Command(
    "Settings",
    listOf("set"),
    rateLimit = 30,
    details = "View and Change the settings of this Server.",
    action = {
        message.sendWEmbed {
            title("${message.guild!!.name} Settings")
            inlineField("Prefix") { it.prefix }
        }
    }
) {

    object Prefix : Command(
        "Prefix",
        listOf("pref"),
        guildOnly = true,
        details = "Set my command prefix for this Server.",
        params = listOfParams("new_prefix"),
        rateLimit = 30,
        action = {
            it.prefix = message.args[1]
            message.reply("Prefix set to `${it.prefix}`")
        })
}

