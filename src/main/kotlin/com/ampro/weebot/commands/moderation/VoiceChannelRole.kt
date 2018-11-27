/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.IPassive
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.Event

/**
 * The [IPassive] manager that creates, assigns, removes, and deletes
 * VoiceChannel roles.
 *
 * TODO: How to regulate which channels get roles when u can't mention voicechannels
 */
class VCRoleManager : IPassive {



    var dead = false

    override fun accept(bot: Weebot, event: Event) {

    }

    override fun dead(): Boolean {
        return false
    }
}

class VoiceChannelRole : Command() {

    init {
        name = "voicechannelrole"
        aliases = arrayOf("vcrc","vcr","vrc")
        userPermissions = arrayOf(Permission.MANAGE_ROLES)
        botPermissions = arrayOf(Permission.MANAGE_ROLES)
        //helpBiConsumer
    }



    override fun execute(event: CommandEvent?) {

    }

}
