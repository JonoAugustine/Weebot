/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot


import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.bot.commands.Command
import com.ampro.weebot.bot.commands.Suggestion
import com.ampro.weebot.stats.BotStatCollection
import com.ampro.weebot.stats.BotStatistic
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import com.serebit.strife.entities.Guild
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.`in`
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.nin
import org.litote.kmongo.reactivestreams.KMongo
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
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

// Weebot

val globalWeebot = Cache.bots[0]!!

/** Retrieve a [Weebot] for the guild. */
val Guild.bot get() = bot(id)

/** Retrieve a [Weebot] for the [guildID]. */
fun bot(guildID: Long): Weebot = Cache.bots[guildID]!!

fun Weebot.save() = Dao.SaveQueue.Bots.enqueue(this)

suspend fun botCount() = Dao.bots.estimatedDocumentCount()

suspend fun Weebot.modify(modify: suspend Weebot.() -> Unit) = apply {
    modify(this)
    save()
}

/** Queue this bot for deletion. */
fun Weebot.delete() = Dao.DeletionQueue.Bots.enqueue(this)

// Suggestion

suspend fun suggestions() = Cache.suggestions.asMap().toMutableMap()
    .apply {
        putAll(
            Dao.suggestions.find(Suggestion::_id nin keys)
                .toList()
                .associateBy(Suggestion::_id)
        )
    }

fun suggestion(id: String): Suggestion? = Cache.suggestions[id]

fun Suggestion.save() = Dao.SaveQueue.Suggestions.enqueue(this)

suspend fun Suggestion.modify(modify: suspend Suggestion.() -> Unit) = apply {
    modify(this)
    save()
}

fun Suggestion.delete() = Dao.DeletionQueue.Suggestions.enqueue(this)

// Statistics

fun statistic(cmdName: String) = Cache.statistics[cmdName]!!

val Command.statistic get() = statistic(name)

fun BotStatCollection.addPoint(stat: BotStatistic) = apply {
    stats.add(stat)
    save()
}

fun BotStatCollection.save() = Dao.SaveQueue.Statistic.enqueue(this)


// Objects

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
    val stats by lazy { mongo.getCollection<BotStatCollection>() }
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
            collection.namespace.collectionName + "_DELETE",
            daemon = true,
            period = 5.minutes.toLongMilliseconds()
        ) {
            if (queue.isNotEmpty()) {
                runBlocking {
                    retry(
                        limitAttempts(3) + binaryExponentialBackoff(10, 5000)
                    ) {
                        if (
                            !collection.deleteMany(id `in` queue)
                                .wasAcknowledged()
                        ) {
                            logger.error("Failed to delete bot queue.")
                            throw Exception()
                        } else {
                            logger.error("Successfully deleted bot queue.")
                            queue.clear()
                        }
                    }
                }
            }
        }

        /** Add an object to the deletion queue */
        fun enqueue(t: T): Boolean = queue.add(id(t).toString())

        object Bots : DeletionQueue<Weebot>(bots, Weebot::guildID)

        object Suggestions :
            DeletionQueue<Suggestion>(suggestions, Suggestion::_id)

        object Statistic : DeletionQueue<BotStatCollection>(
            stats, BotStatCollection::command
        )

    }

    sealed class SaveQueue<T : Any>(
        val collection: CoroutineCollection<T>,
        val id: KProperty1<T, Any>
    ) {

        private val queue = mutableMapOf<String, T>()
        private val name = collection.namespace.collectionName

        @ExperimentalTime
        private val timer = fixedRateTimer(
            name + "_SAVE", daemon = true,
            period = 5.minutes.toLongMilliseconds()
        ) {
            if (queue.isNotEmpty()) {
                runBlocking {
                    retry(
                        limitAttempts(3) + binaryExponentialBackoff(10, 5000)
                    ) {
                        logger.trace(
                            "Attempting backup of collection ${
                            collection.namespace.collectionName}"
                        )
                        val sr = queue
                            .mapValues { (_, v) -> collection.save(v) }
                            .mapValues { it.value?.wasAcknowledged() == true }
                        val s = sr.count { it.value }
                        val f = sr.size - s
                        sr.filterValues { it }
                            .forEach { queue.remove(it.key) }
                        if (s > 0) {
                            logger.trace(
                                "Successfully saved $s/${sr.size} from $name"
                            )
                        } else {
                            logger.error(
                                "Failed to save $f/${sr.size} from $name."
                            )
                            throw Exception()
                        }
                    }
                }
            }
        }

        /** Add an object to the deletion queue */
        fun enqueue(t: T) {
            queue[id(t).toString()] = t
        }

        object Bots : SaveQueue<Weebot>(bots, Weebot::guildID)

        object Suggestions : SaveQueue<Suggestion>(suggestions, Suggestion::_id)

        object Statistic : SaveQueue<BotStatCollection>(
            stats, BotStatCollection::command
        )

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

    val statistics by lazy {
        Caffeine.newBuilder()
            .maximumSize(500)
            .removalListener<String, BotStatCollection> { _, v, cause ->
                GlobalScope.launch {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }
            .recordStats()
            .build { cmdName: String ->
                runBlocking { Dao.stats.findOneById(cmdName) }
                    ?: BotStatCollection(cmdName).apply { save() }
            }
    }

}

