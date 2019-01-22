/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

val Any?.unit get() = Unit

/* **************
        Lists
 ***************/

/**
 * Splits an Iterable into two lists, one matching the [predicate] and the other not
 * matching.
 *
 * @return [Pair] of mutable lists. First element contains those matching the predicate.
 */
infix fun <E> Iterable<E>.splitBy(predicate: (E) -> Boolean)
        : Pair<MutableList<E>, MutableList<E>> {
    return (filter(predicate).toMutableList() to filterNot(predicate).toMutableList())
}

fun <T> List<T>.subList(fromIndex: Int): MutableList<T>  {
    return if (this.isNotEmpty()) subList(fromIndex, size).toMutableList()
    else mutableListOf()
}

/* ****************
        String
 *******************/

operator fun Regex.plus(regex: Regex) = Regex(pattern + regex.pattern)
operator fun Regex.plus(pattern: String) = Regex(this.pattern + pattern)

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

/* ***********************
        Map Extensions
 *************************/

fun <A, B> Iterable<A>.parMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

/**
 * @param predicate Items matching will be removed
 */
fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
    val targets = filter { predicate(it.key, it.value) }
    targets.forEach { t -> this.remove(t.key) }
}

/* ********
    Number & Boolean
 */

fun Boolean.toInt(): Int = if (this) 1 else 0
