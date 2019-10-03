/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.util

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz

/** A collection of useful regex patterns. */
object Regecies {
    /** (?i)-{0,2} */
    val hyphen  = Regex("(?i)^-{1,2}")
    val yes     = Regex("(?i)(y+e+s+)")
    val no      = Regex("(?i)(n+o+)")
    val on      = Regex("(?i)(o+n+)")
    val enable  = Regex("(?i)(e+n+a+b+l+e?)")
    val off     = Regex("(?i)(o+f{2,})")
    val disable = Regex("(?i)(d+i+s+a+b+l+e+)")
    val default = Regex("(?i)(d+e+f+a+u+l+t+)")
}

val DateTime.DD_MM_YYY_HH_MM
    get() = "$dayOfMonth/$month1/$yearInt $hours:$minutes"

val DateTimeTz.DD_MM_YYY_HH_MM
    get() = "$dayOfMonth/$month1/$yearInt $hours:$minutes"

infix fun String.and(any: Any): Pair<String, String> = this to any.toString()
