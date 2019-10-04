/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking



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
