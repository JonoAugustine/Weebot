/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import java.util.*
import kotlin.streams.asSequence

const val alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val digi  = "0123456789"
const val alphaDigi = alpha + digi

/**
 * An random ID generator. 0-9, A-Z (caps)
 *
 * @param idLeng the length of an ID
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
open class IdGenerator(var idLeng: Long = 5L) {
    fun next() : String {
        return Random().ints(idLeng, 0, alphaDigi.length)
            .asSequence().map(alphaDigi::get).joinToString("")
    }
}
