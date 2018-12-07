/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.jagrosh.jdautilities.command.CommandClientBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member

/*
 * Extension methods used for JDA elements
 */

val userMentionRegex: Regex = "^(<@.*\\d+>)$".toRegex()

fun Member.hasPerms(vararg p: Permission) = this.permissions.containsAll(p.toList())

fun Member.hasOneOfPerms(vararg p: Permission) : Boolean {
    p.forEach { return this.permissions.contains(it) }
    return false
}

infix fun Member.hasPerm(perm: Permission) = this.permissions.contains(perm)

/**
 * Add multiple commands from an [Iterable].
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
fun CommandClientBuilder.addCommands(commands: Iterable<WeebotCommand>)
        : CommandClientBuilder {
    commands.forEach { this.addCommand(it) }
    return this
}
