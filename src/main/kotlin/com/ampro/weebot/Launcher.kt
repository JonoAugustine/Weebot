/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.api.initJavalin
import com.ampro.weebot.bot.initWeebot
import com.ampro.weebot.util.matches
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import com.serebit.logkat.writers.ConsoleWriter
import com.serebit.logkat.writers.MultiWriter
import kotlin.time.ExperimentalTime

val logger = Logger().apply {
    level = LogLevel.TRACE
    writer = MultiWriter(ConsoleWriter())
}

@ExperimentalTime
suspend fun main(args: Array<String>) {
    initJavalin()
    initWeebot(
        if (args[0].matches("^-{1,2}e(nv)?$")) System.getenv(args[1])
        else args[0]
    )
}
