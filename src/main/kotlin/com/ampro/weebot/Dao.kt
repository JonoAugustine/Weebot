/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot


import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.commands.Suggestion
import com.ampro.weebot.stats.BotStatistic
import com.github.benmanes.caffeine.cache.Caffeine
import com.serebit.strife.entities.Guild
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.`in`
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit.*
import kotlin.concurrent.fixedRateTimer
import kotlin.reflect.KProperty1
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/** The root wbot file. */
val DIR = File("wbot")
    get() = when {
        field.exists() && field.isDirectory -> field
        else -> {
            Files.createDirectory(field.toPath())
            field
        }
    }

val globalWeebot = Cache.bots[-1]!!

/** Retrieve a [Weebot] for the guild. */
val Guild.bot get() = bot(id)

/** Retrieve a [Weebot] for the [guildID]. */
fun bot(guildID: Long): Weebot = Cache.bots[guildID]!!

fun suggestion(id: String): Suggestion? = Cache.suggestions[id]

suspend fun botCount() = Dao.bots.estimatedDocumentCount()

suspend fun Weebot.save() = Dao.bots.save(this)?.wasAcknowledged() == true

suspend fun Suggestion.save() =
    Dao.suggestions.save(this)?.wasAcknowledged() == true

suspend fun Weebot.modify(modify: suspend Weebot.() -> Unit) = apply {
        modify(this)
        save()
    }

/** Queue this bot for deletion. */
fun Weebot.delete() = Dao.DeletionQueue.Bots.enqueue(this)

private object Dao {

    /** Dao access */
    val mongo by lazy {
        KMongo.createClient(
            "mongodb://weebot:nsU9EY542pdU4QW@ds135217.mlab.com:35217/heroku_crvntpr6"
        ).getDatabase("heroku_crvntpr6").coroutine
    }

    /** [Weebot] collection. */
    val bots by lazy { mongo.getCollection<Weebot>() }
    /** [Suggestion] collection. */
    val suggestions by lazy { mongo.getCollection<Suggestion>() }
    /** [BotStatistic] collection. */
    val stats by lazy { mongo.getCollection<BotStatistic>() }
    /** Internal Data collection */
    //val userData by lazy { mongo.getCollection<UserData>() }
    /** Cards Against Humanity customizations. */
    //val cah by lazy { mongo.getCollection<CahInfo>() }
    //val notes by lazy { mongo.getCollection<NotePadCollection>("notePad") }

    sealed class DeletionQueue<T : Any>(
        val collection: CoroutineCollection<T>,
        val id: KProperty1<T, Any>
    ) {

        private val queue: MutableSet<String> = mutableSetOf()

        @ExperimentalTime
        private val timer = fixedRateTimer(
            collection.namespace.collectionName,
            daemon = true,
            period = 5.minutes.toLongMilliseconds()
        ) {
            if (queue.isNotEmpty()) {
                runBlocking {
                    collection.deleteMany(id `in` queue)
                }
            }
        }

        /** Add an object to the deletion queue */
        fun enqueue(t: T): Boolean {
            val s = id(t).toString()
            return queue.add(s)
        }

        object Bots : DeletionQueue<Weebot>(bots, Weebot::guildID)

    }

}

object Cache {

    val bots by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(5, MINUTES)
            .expireAfterAccess(10, MINUTES)
            .removalListener<Long, Weebot> { _, w, cause ->
                GlobalScope.launch {
                    w.takeIf { cause.wasEvicted() }?.save()
                }
            }.recordStats()
            .build { id: Long ->
                runBlocking {
                    Dao.bots.findOneById(id.toString())
                        ?: Weebot(id).apply { save() }
                }
            }
    }

    val suggestions by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(1, MINUTES)
            .expireAfterAccess(5, MINUTES)
            .removalListener<String, Suggestion> { _, v, cause ->
                GlobalScope.launch {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }
            .recordStats()
            .build { id: String ->
                runBlocking { Dao.suggestions.findOneById(id) }
            }
    }

}

