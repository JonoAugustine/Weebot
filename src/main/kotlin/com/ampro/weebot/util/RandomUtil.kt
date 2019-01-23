/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

val RAND = Random(420_69_98_4829 / (NOW().minute + 1))

const val alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val digi  = "0123456789"
const val alphaDigi = alpha + "123456789" //Exclued Zero

/** (?i)-{0,2} */
val REG_HYPHEN  = Regex("(?i)^-{1,2}")
val REG_YES     = Regex("(?i)(y+e+s+)")
val REG_NO      = Regex("(?i)(n+o+)")
val REG_ON      = Regex("(?i)(o+n+)")
val REG_ENABLE  = Regex("(?i)(e+n+a+b+l+e+)")
val REG_OFF     = Regex("(?i)(o+f{2,})")
val REG_DISABLE = Regex("(?i)(d+i+s+a+b+l+e+)")
val REG_DEFAULT = Regex("(?i)(d+e+f+a+u+l+t+)")

/**
 * An random ID generator. 0-9, A-Z (caps)
 *
 * @param idLeng the length of an ID
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
open class IdGenerator(var idLeng: Long = 5L, val prefix: String = "") {
    fun next() : String {
        return prefix + RAND.ints(idLeng, 0, alphaDigi.length)
            .asSequence().map(alphaDigi::get).joinToString("")
    }
}

fun randInt(upper: Int) = ThreadLocalRandom.current().nextInt(upper)
