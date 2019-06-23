/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.api

import com.ampro.weebot.database.Cache
import com.github.benmanes.caffeine.cache.LoadingCache
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify

@ImplicitReflectionSerializer
private inline fun <reified T : Any> T.stringify() = Json.nonstrict.stringify(this)

@Serializable
open class CacheStatus(@Transient private val ref: LoadingCache<*, *>? = null) {
    constructor() :this(null)
    val hitCount = ref?.stats()?.hitCount()
    val missCount = ref?.stats()?.missCount()
    val loadSuccessCount = ref?.stats()?.loadSuccessCount()
    val loadFailureCount = ref?.stats()?.loadFailureCount()
    val averageLoadPenalty = ref?.stats()?.averageLoadPenalty()
    val totalLoadTime = ref?.stats()?.totalLoadTime()
    val evictionCount = ref?.stats()?.evictionCount()
    val evictionWeight = ref?.stats()?.evictionWeight()
}

object Status {
    object Caches {
        val bots get() = CacheStatus(Cache.bots)
        val suggs get() = CacheStatus(Cache.suggestions)
        //val stats = CacheStatus(Cache.statistics)
        val userData get() = CacheStatus(Cache.userData)
        val cahInfo get() = CacheStatus(Cache.cahInfo)
        val notes get() = CacheStatus(Cache.notes)
    }
}


@UseExperimental(ImplicitReflectionSerializer::class)
val javalin = Javalin.create {
    it.defaultContentType = "application/json"
    //it.enforceSsl = true
    it.enableDevLogging()
    it.enableCorsForAllOrigins()
}.routes {
    path("internal") {
        path("status") {
            path("cache") {
                get("bots") {
                    it.result(Status.Caches.bots.stringify())
                }
                get("suggestions") {
                    it.result(Status.Caches.suggs.stringify())
                }
                get("users") {
                    it.result(Status.Caches.userData.stringify())
                }
                get("cah") {
                    it.result(Status.Caches.cahInfo.stringify())
                }
                get("notes") {
                    it.result(Status.Caches.notes.stringify())
                }
            }
        }
    }
}
