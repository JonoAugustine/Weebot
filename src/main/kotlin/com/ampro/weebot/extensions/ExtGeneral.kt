/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun <A, B> Iterable<A>.parMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}
