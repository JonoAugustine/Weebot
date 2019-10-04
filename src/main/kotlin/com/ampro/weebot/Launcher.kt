/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.bot.initWeebot
import com.ampro.weebot.api.initJavalin
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import com.serebit.logkat.writers.*

val logger = Logger().apply {
    level = LogLevel.TRACE
    writer = MultiWriter(ConsoleWriter())
}

suspend fun main(args: Array<String>) {
    initJavalin()
    initWeebot(args.getOrNull(0)?.equals("-w") ?: false)
}
