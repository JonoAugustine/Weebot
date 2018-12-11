/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.jagrosh.jdautilities.command.Command.Category

//TODO Category predicates

val CAT_GEN     = Category("General")
val CAT_DEV     = Category("Developer")
val CAT_MOD     = Category("Moderator")
val CAT_PROG    = Category("Programmer")
val CAT_GAME    = Category("Game")
val CAT_FUN     = Category("Fun")
val CAT_SOC     = Category("Social")
val CAT_UTIL    = Category("Utility")

val categories = listOf(CAT_GEN, CAT_DEV, CAT_MOD, CAT_PROG, CAT_GAME, CAT_FUN,
    CAT_SOC, CAT_UTIL)
