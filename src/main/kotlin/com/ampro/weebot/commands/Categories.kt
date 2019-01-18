/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.jagrosh.jdautilities.command.Command.Category

internal const val GEN_FAILURE_MESSAGE = "You do not have access to this command."

val CAT_GEN     = Category("General", GEN_FAILURE_MESSAGE) {true}
val CAT_DEV     = Category("Developer", GEN_FAILURE_MESSAGE) {true}
val CAT_MOD     = Category("Moderator", GEN_FAILURE_MESSAGE) {true}
val CAT_PROG    = Category("Programmer", GEN_FAILURE_MESSAGE) {true}
val CAT_GAME    = Category("Game", GEN_FAILURE_MESSAGE) {true}
val CAT_FUN     = Category("Fun", GEN_FAILURE_MESSAGE) {true}
val CAT_SOC     = Category("Social", GEN_FAILURE_MESSAGE) {true}
val CAT_UTIL    = Category("Utility", GEN_FAILURE_MESSAGE) {true}
val CAT_MISC    = Category("Miscellaneous", GEN_FAILURE_MESSAGE) { true }
val CAT_UNDER_CONSTRUCTION = Category("Under Construction",
    "This command is still under (re)construction") { it.isOwner }

val CATEGORIES = listOf(CAT_GEN, CAT_DEV, CAT_MOD, CAT_PROG, CAT_GAME, CAT_FUN,
    CAT_SOC, CAT_UTIL, CAT_MISC)
