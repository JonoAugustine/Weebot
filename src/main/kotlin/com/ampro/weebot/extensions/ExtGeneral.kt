/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/* ****************
        String
 *******************/

/**
 * Remove all instances of the given regex
 *
 * @param The [Regex] to remove
 * @return The string with all instances of the given regex removed
 */
fun String.removeAll(regex: Regex) = this.replace(regex, "")

fun String.removeAll(string: String) = this.replace(string.toRegex(), "")

fun String.matchesAny(regecies: Collection<Regex>) : Boolean {
    regecies.forEach { if (this.matches(it)) return true }
    return false
}

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


/* ***********************
        Map Extensions
 *************************/

fun <A, B> Iterable<A>.parMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
    putAll( filter { predicate(it.key, it.value) } )
}
