/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.bot.initWeebot
import com.ampro.weebot.site.initKweb
import com.serebit.logkat.LogLevel
import com.serebit.logkat.Logger
import com.serebit.logkat.writers.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

val logger = Logger().apply {
    level = LogLevel.TRACE
    writer = MultiWriter(ConsoleWriter())
}

suspend fun main(args: Array<String>) {
    //GlobalScope.launch { initKweb() }
    initWeebot(args[0] == "-w")
}
