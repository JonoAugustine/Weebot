/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.api

import com.ampro.weebot.botCount
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime


const val site = "https://weebot-2.herokuapp.com"


@ExperimentalTime
fun initJavalin() {
    Javalin
        .create { it.enforceSsl = true }
        .routes {
            path("/") {
                get {
                    it.json(object {
                        val bot_count = runBlocking { botCount() }
                    })
                }
            }
        }
        .start(port)
}

val port by lazy { System.getenv("PORT")?.toInt() ?: 7000 }
