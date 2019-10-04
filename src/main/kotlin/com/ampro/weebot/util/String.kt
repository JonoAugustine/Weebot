/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.util

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz

/** A collection of useful regex patterns. */
object Regecies {
    /** ignore case shortcut */
    const val ic = "(?i)"
    /** (?i)-{0,2} */
    val hyphen  = Regex("$ic-{1,2}")
    val yes     = Regex("$ic(y+e+s+)")
    val no      = Regex("$ic(n+o+)")
    val on      = Regex("$ic(o+n+)")
    val enable  = Regex("$ic(e+n+a+b+l+e?)")
    val off     = Regex("$ic(o+f{2,})")
    val disable = Regex("$ic(d+i+s+a+b+l+e+)")
    val default = Regex("$ic(d+e+f+a+u+l+t+)")
}

operator fun Regex.plus(pattern: String) = (this.pattern + pattern).toRegex()

fun String.matchesAny(vararg regex: Regex) = regex.any { matches(it) }

val DateTime.DD_MM_YYY_HH_MM
    get() = "$dayOfMonth/$month1/$yearInt $hours:$minutes"

val DateTimeTz.DD_MM_YYY_HH_MM
    get() = "$dayOfMonth/$month1/$yearInt $hours:$minutes"

infix fun String.and(any: Any): Pair<String, String> = this to any.toString()
