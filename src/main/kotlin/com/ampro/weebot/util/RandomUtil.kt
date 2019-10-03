/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import com.soywiz.klock.DateTime
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

/** The root random instance */
val RAND by lazy { Random(420_69_98_4829 / (DateTime.now().minutes + 1)) }

/** Get a random int from Zero to [upper] exclusive. */
fun randInt(upper: Int): Int = ThreadLocalRandom.current().nextInt(upper)

const val alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val digit  = "0123456789"
/** [alpha] + [digit] excluding Zero */
const val alphaDigit = alpha + "123456789"

/**
 * An random ID generator. 1-9, A-Z.
 *
 * @param leng the length of an ID
 * @param prefix optional prefix to prepend to a generated ID
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
open class IdGenerator(var leng: Long = 7L, val prefix: String = "") {
    /** @return A new ID string */
    fun next() : String = prefix + RAND.ints(leng, 0, alphaDigit.length)
        .asSequence().map(alphaDigit::get).joinToString("")
}
