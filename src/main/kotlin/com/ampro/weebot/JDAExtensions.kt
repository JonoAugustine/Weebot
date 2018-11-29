/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member

/*
 * Extension methods used for JDA elements
 */


fun Member.hasPerms(vararg p: Permission) = this.permissions.containsAll(p.toList())

fun Member.hasOneOfPerms(vararg p: Permission) : Boolean {
    p.forEach { return this.permissions.contains(it) }
    return false
}

infix fun Member.hasPerm(perm: Permission) = this.permissions.contains(perm)

