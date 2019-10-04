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

val DateTime.DD_MM_YYY_HH_MM
    get() = "$dayOfMonth/$month1/$yearInt $hours:$minutes"

val DateTimeTz.DD_MM_YYY_HH_MM
    get() = "$dayOfMonth/$month1/$yearInt $hours:$minutes"

infix fun String.and(any: Any): Pair<String, String> = this to any.toString()

operator fun Regex.plus(regex: Regex) = Regex(pattern + regex.pattern)

/**
 * @param set a [Regex] paired to the [String] with which to replace it with
 */
fun String.replace(vararg set: Pair<Regex, String>) : String {
    var s = this
    set.forEach {
        s = s.replace(it.first, it.second)
    }
    return s
}

/**
 * @param set a [Regex] paired to the [String] with which to replace it with
 */
fun String.replaceSet(vararg set: Pair<String, String>) : String {
    var s = this
    set.forEach {
        s = s.replace(it.first.toRegex(), it.second)
    }
    return s
}

/**
 * Remove all instances of the given regex
 *
 * @param The [Regex] to remove
 * @return The string with all instances of the given regex removed
 */
fun String.removeAll(regex: Regex) = this.replace(regex, "")

fun String.removeAll(string: String) = this.replace(string.toRegex(), "")

infix fun String.matchesAny(regecies: Collection<Regex>) : Boolean {
    regecies.forEach { if (this.matches(it)) return true }
    return false
}

infix fun String.matches(regex: String) = this.matches(regex.toRegex())

fun String.matchesAny(vararg regecies: Regex) : Boolean {
    regecies.forEach { if (this.matches(it)) return true }
    return false
}

fun String.matchesAnyConfirm(regecies: Collection<Regex>) : List<Regex> {
    val list = mutableListOf<Regex>()
    regecies.forEach { if (this.matches(it)) list.add(it) }
    return list
}

fun List<String>.contains(regex: Regex) : Boolean {
    forEach { if (it.matches(regex)) return true }
    return false
}

fun String.containsAny(strings: Collection<String>, ignoreCase: Boolean = true)
    : Boolean = strings.asSequence().any { this.contains(it, ignoreCase) }
