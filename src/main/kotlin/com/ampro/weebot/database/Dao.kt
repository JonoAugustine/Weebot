/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.database

import com.ampro.weebot.CACHED_POOL
import com.ampro.weebot.GlobalWeebot
import com.ampro.weebot.JDA_SHARD_MNGR
import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.`fun`.games.cardgame.CahInfo
import com.ampro.weebot.commands.developer.Suggestion
import com.ampro.weebot.commands.utility.NotePadCollection
import com.ampro.weebot.database.constants.CLIENT_WBT
import com.ampro.weebot.database.constants.KEY_DISCORD_BOTS_ORG
import com.ampro.weebot.extensions.WeebotCommand
import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.discordbots.api.client.DiscordBotListAPI
import org.litote.kmongo.`in`
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/* ************************
     Discord Bot List API
 *************************/

val DISCORD_BOTLIST_API: DiscordBotListAPI = DiscordBotListAPI.Builder()
    .token(KEY_DISCORD_BOTS_ORG).botId(CLIENT_WBT.toString()).build()

lateinit var BOT_LIST_API_UPDATERS: Job

infix fun User.hasVoted(handler: (Boolean, Throwable) -> Boolean) {
    DISCORD_BOTLIST_API.hasVoted(this.id).handleAsync(handler)
}


/* ************************
        Database
 *************************/

private object Database {

    /** Database access */
    private val mongo by lazy {
        KMongo.createClient(
            "mongodb://weebot:nsU9EY542pdU4QW@ds135217.mlab.com:35217/heroku_crvntpr6"
        ).getDatabase("heroku_crvntpr6").coroutine
    }

    /** [Weebot] collection. */
    val bots by lazy { mongo.getCollection<Weebot>() }
    /** [Suggestion] collection. */
    val suggestions by lazy { mongo.getCollection<Suggestion>() }
    /** [Statistics] collection. */
    val stats by lazy { mongo.getCollection<StatisticPlot>() }
    /** Internal Data collection */
    val userData by lazy { mongo.getCollection<UserData>() }

    /** Cards Against Humanity customizations. */
    val cah by lazy { mongo.getCollection<CahInfo>() }

    val notes by lazy { mongo.getCollection<NotePadCollection>("notePad") }
}

/* ************************
        Cache
 *************************/

object Cache {

    // Base //

    val bots by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener<Long, Weebot> { _, v, cause ->
                GlobalScope.launch(CACHED_POOL) {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }.recordStats()
            .build { id: Long ->
                runBlocking { Database.bots.findOneById(id.toString()) }
            }
    }

    val suggestions by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener<String, Suggestion> { _, v, cause ->
                GlobalScope.launch(CACHED_POOL) {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }
            .recordStats()
            .build { id: String ->
                runBlocking { Database.suggestions.findOneById(id) }
            }
    }

    val statistics by lazy {
        runBlocking { Database.stats.find().toList() }
            .associateBy { it._id }.toMutableMap()
    }

    val userData by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener<Long, UserData> { _, v, cause ->
                GlobalScope.launch(CACHED_POOL) {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }
            .recordStats()
            .build { id: Long ->
                runBlocking { Database.userData.findOneById(id.toString()) }
            }
    }

    // Unique //

    val cahInfo by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener<String, CahInfo> { _, v, cause ->
                GlobalScope.launch(CACHED_POOL) {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }
            .recordStats()
            .build<String, CahInfo> {
                runBlocking { Database.cah.findOneById(it) }
            }
    }

    val notes by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener<String, NotePadCollection> { _, v, cause ->
                GlobalScope.launch(CACHED_POOL) {
                    v.takeIf { cause.wasEvicted() }?.save()
                }
            }
            .recordStats()
            .build<String, NotePadCollection> {
                runBlocking { Database.notes.findOneById(it) }
            }
    }

}

/* ************************
          Access
 *************************/

// Guild & Bot //

/** Get the [Weebot] assigned to this [Guild] or create a new one. */
val Guild.bot: Weebot get() = runBlocking { getWeebotOrNew(this@bot) }

suspend fun getWeebotOrNew(guild: Guild) = getWeebotOrNew(guild.idLong)

suspend fun getWeebotOrNew(guildID: Long) = getWeebot(guildID)
    ?: run { Weebot(guildID).also { Database.bots.save(it) } }

/** Attempt to pull a [Weebot] from cache. */
fun getWeebot(guildID: Long) = Cache.bots[guildID]

val bots get() = JDA_SHARD_MNGR.guilds.map { it.bot }

/**
 * Get a guild matching the ID given.
 *
 * @param id long ID
 * @return requested Guild <br></br> null if not found.
 */
fun getGuild(id: Long): Guild? = JDA_SHARD_MNGR.getGuildById(id)

suspend fun Weebot.save() = Database.bots.save(this)?.wasAcknowledged() == true

// User //

val Long.userData: UserData? get() = Cache.userData[this]

val User.data: UserData? get() = Cache.userData[this.idLong]

val Long.user: User? get() = JDA_SHARD_MNGR.getUserById(this)

infix fun User.canUse(klass: KClass<out WeebotCommand>) =
    data?.blacklists?.contains(klass)?.not() ?: true

infix fun WeebotCommand.allows(user: User) = user.canUse(this::class)

suspend fun UserData.save() = Database.userData.save(this)?.wasAcknowledged() == true

// Stats  & Suggs //

suspend fun getSuggestions() = Database.suggestions.find().toList()
    .apply { Cache.suggestions.putAll(associateBy(Suggestion::_id)) }

suspend fun getSuggestion(id: String) = Database.suggestions.findOneById(id)

suspend fun getSuggestions(ids: Iterable<String>) =
    Database.suggestions.find(Suggestion::_id `in` ids).toList()

suspend fun deleteSuggestions(ids: Iterable<String>) =
    Database.suggestions.deleteMany(Suggestion::_id `in` ids).wasAcknowledged()

suspend fun Suggestion.delete() = Database.suggestions.deleteOneById(_id)

suspend fun Suggestion.save() = Database.suggestions.save(this)?.wasAcknowledged() == true

fun getStatPlot(id: String) = Cache.statistics[id] ?: runBlocking {
    StatisticPlot(id).also {
        Database.stats.save(it)
        Cache.statistics[it._id] = it
    }
}

suspend fun StatisticPlot.save() = Database.stats.save(this)?.wasAcknowledged() == true

// Other //

fun getCahInfo(guildID: Long): CahInfo? = Cache.cahInfo[guildID.toString()]

fun getCahInfoAll(): MutableMap<String, CahInfo> =
    Cache.cahInfo.getAll(JDA_SHARD_MNGR.guilds.map { it.id })

suspend fun CahInfo.save() = Database.cah.save(this)?.wasAcknowledged() == true

suspend fun NotePadCollection.save() = Database.notes.save(
    this)?.wasAcknowledged() == true
