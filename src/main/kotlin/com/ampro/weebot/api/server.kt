/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.api

import com.ampro.weebot.database.Cache
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import java.lang.System.*


class Status {
    object Caching {
        object Bots {
            val hitCount get() = Cache.bots.stats().hitCount()
            val missCount get() = Cache.bots.stats().missCount()
            val loadSuccessCount get() = Cache.bots.stats().loadSuccessCount()
            val loadFailureCount get() = Cache.bots.stats().averageLoadPenalty()
            val totalLoadTime get() = Cache.bots.stats().totalLoadTime()
            val evictionCount get() = Cache.bots.stats().evictionCount()
            val evictionWeight get() = Cache.bots.stats().evictionWeight()
        }
    }
}


val javalin = Javalin.create {
    it.defaultContentType = "application/json"
    it.enforceSsl = true
}.routes {
    path("internal") {
        get("status") {

        }
    }
}.start(getenv("PORT")?.toInt() ?: 6900)
