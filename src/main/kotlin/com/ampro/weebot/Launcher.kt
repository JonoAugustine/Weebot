/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import kotlinx.coroutines.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA.Status.CONNECTED
import net.dv8tion.jda.core.JDA.Status.SHUTDOWN
import net.dv8tion.jda.core.entities.Game.*
import net.dv8tion.jda.core.entities.SelfUser
import net.dv8tion.jda.core.entities.User
import twitter4j.*
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.security.auth.login.LoginException
import kotlin.system.measureTimeMillis
import twitter4j.FilterQuery



lateinit var SAVE_JOB: Job
/** How ofter to asve to file in Seconds */
const val SAVE_INTER = 30

val CACHED_POOL =  Executors.newCachedThreadPool().asCoroutineDispatcher()

/** Main Logger */
lateinit var MLOG: FileLogger

//JDA connection shard Client
lateinit var JDA_SHARD_MNGR: ShardManager
var ON = true
lateinit var CMD_CLIENT: CommandClient
val WAITER: EventWaiter = EventWaiter()
/** The bot's selfuser from */
lateinit var SELF: SelfUser

const val GENERIC_ERR_MESG = "*Sorry, I tripped over my shoelaces. Please try that " +
        "again later*"

val games = listOf(listening("@Weebot Help"), playing("with wires"),
    watching("Humans Poop"), listening("your thoughts"), playing("Weebot 2.1 Kotlin!"))


/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 *
 * @param args_
 * @throws LoginException
 * @throws InterruptedException
 */
fun main(args_: Array<String>) { runBlocking {
    slog("Launching...")
    slog("\tBuilding Directories...")
    if (!buildDirs()) {
        elog("\tFAILED: Build Dir | shutting down...")
        System.exit(-1)
    }
    slog("\t...DONE")
    slog("Initializing Main Logger")
    MLOG = FileLogger("Launcher $NOW_STR_FILE")
    slog("...DONE\n\n")

    /** Setup for random methods and stuff that is needed before launch */
    val genSetup = listOf(launch { setupWebFuel() })

    //Debug
    //RestAction.setPassContext(true) // enable context by default
    //RestAction.DEFAULT_FAILURE = Consumer(Throwable::printStackTrace)

    //LOGIN & LISTENERS
    val reg_wbot = Regex("(?i)(w|weebot|full)")
    val reg_tBot = Regex("(?i)(t|tobeew|test)")

    val weebot = !(args_.isNotEmpty() && args_[0].matches(reg_tBot))

    val alt = if (weebot) "\\" else "t\\"

    //COMMAND CLIENT
    CMD_CLIENT = CommandClientBuilder().setOwnerId(DEV_IDS[0].toString())
        .setCoOwnerIds(DEV_IDS[1].toString())
        .setGuildSettingsManager { getWeebotOrNew(it.idLong).settings }
        .setAlternativePrefix(alt)
        .setGame(games.random())
        .addCommandsWithCheck(COMMANDS)
        .setEmojis(heavy_check_mark.unicode, Warning.unicode, X_Red.unicode)
        .setServerInvite(LINK_INVITEBOT)
        .setHelpConsumer { event ->
            //If the only argument is the command invoke
            val args = event.splitArgs()
            if (args.isEmpty()) {
                CMD_HELP.execute(event)
            } else {
                COMMANDS.forEach { cmd ->
                    if (cmd.isCommandFor(args[0]) && (!cmd.isHidden || event.isOwner)) {
                        if (cmd.getHelpBiConsumer() != null) {
                            cmd.getHelpBiConsumer()!!.accept(event, cmd)
                            return@setHelpConsumer
                        } else if (!cmd.help.isNullOrBlank()) {
                            event.reply("*${cmd.help}*")
                            return@setHelpConsumer
                        } else {
                            event.reply(
                                "*Help is currently unavailable for this command. You can use ``@Weebot sugg`` to send feedback to the Developers and remind them they have a job to do!* $Rage")
                            return@setHelpConsumer
                        }
                    }
                }
            }
        }
        .build()

    //DATABASE
    setUpDatabase()
    //LOGIN & LISTENERS
    JDA_SHARD_MNGR = (if (weebot) jdaShardLogIn() else jdaDevShardLogIn())
        .addEventListeners(EventDispatcher(), CMD_CLIENT, WAITER).build()
    launch(CACHED_POOL) {
        delay(20_000)
        JDA_SHARD_MNGR.setGame(games.random())
        while (ON) {
            delay(10 * 60_000)
            JDA_SHARD_MNGR.setGame(games.random())
        }
    }
    //WEEBOTS
    startupWeebots()

    //WAIT FOR SHARD CONNECT
    MLOG.slog("All ${JDA_SHARD_MNGR.shards.size} Shards connected! ${measureTimeMillis {
        MLOG.slog("Waiting for all ${JDA_SHARD_MNGR.shards.size} shards to connect...")
        while (JDA_SHARD_MNGR.shards.has { it.status != CONNECTED }) {
            Thread.sleep(500)
        }
    } / 1_000} seconds")

    //SET SELF AND AVATAR URL
    SELF = JDA_SHARD_MNGR.shards[0].selfUser
    weebotAvatar = SELF.avatarUrl

    //Stats
    setUpStatistics()
    //Bot List website APIs
    setupBotListApis()

    genSetup.joinAll() //Ensure all gensetup is finished

    MLOG.slog("Starting Save Job...")
    SAVE_JOB = saveTimer()

    MLOG.slog("Launch Complete!\n\n")

    JDA_SHARD_MNGR.getTextChannelById(BOT_DEV_CHAT).sendMessage("ONLINE!")
        .queueAfter(850, MILLISECONDS)

    //Watch HQStream
    launch(CACHED_POOL) {
        delay(20 * 60 * 1_000)
        CLIENT_TWTICH_PUB.helix
            .getStreams("","",null,null,null,null, listOf("127416768"),null)
            .observe().subscribe { streamList ->
                streamList.streams.firstOrNull { it.userId == TWITCH_HQR_ID }
                    ?.also {
                        JDA_SHARD_MNGR.setGame(streaming(it.title, LINK_HQTWITCH))
                    }
            }
        delay(20 * 60 * 1_000)
        CLIENT_TWTICH_PUB.helix
            .getStreams("","",null,null,null,null, listOf("127416768"),null)
            .observe().subscribe { streamList ->
                if (streamList.streams.none { it.userId == TWITCH_HQR_ID }) {
                    JDA_SHARD_MNGR.setGame(games.random())
                }
            }
    }
    //Watch HQTwitter
    launch(CACHED_POOL) {
        val stream = TwitterStreamFactory(TWITTER_CONFIG).instance
            .addListener(object :
                StatusListener {
            /**
             * This notice will be sent each time a limited stream becomes unlimited.<br></br>
             * If this number is high and or rapidly increasing, it is an indication that your predicate is too broad, and you should consider a predicate with higher selectivity.
             *
             * @param numberOfLimitedStatuses an enumeration of statuses that matched the track predicate but were administratively limited.
             * @see [Streaming API Concepts - Filter Limiting | Twitter Developers](https://dev.twitter.com/docs/streaming-api/concepts.filter-limiting)
             *
             * @see [Streaming API Concepts - Parsing Responses | Twitter Developers](https://dev.twitter.com/docs/streaming-api/concepts.parsing-responses)
             *
             * @see [Twitter Development Talk - Track API Limit message meaning](http://groups.google.co.jp/group/twitter-development-talk/browse_thread/thread/15d0504b3dd7b939)
             *
             * @since Twitter4J 2.1.0
             */
            override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
                slog(numberOfLimitedStatuses)
            }

            override fun onStallWarning(warning: StallWarning) {
                slog(warning)
            }

            override fun onException(ex: Exception?) { ex?.printStackTrace() }

            override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
                slog(statusDeletionNotice)
            }

            override fun onStatus(status: Status) {
                slog(status)
            }

            override fun onScrubGeo(userId: Long, upToStatusId: Long) {
                slog(userId to upToStatusId)
            }
        }).filter(FilterQuery().follow(1059304575956606976))
        stream.sample("java")

    }
}}

/**
 * Attempts to load Statistics data from file. Sets [STAT] to the loaded data
 * or makes a new instance
 */
fun setUpStatistics() {
    MLOG.slog("Setting up Statistics...")
    MLOG.slog("\tLoading Statistics...")
    val stat: Statistics? = loadJson<Statistics>(STAT_SAVE)
    if (stat == null) {
        MLOG.slog("\t\tUnable to load Statistics, creating new instance.")
        STAT = Statistics()
        if (STAT.saveJson(STAT_SAVE) == -1) {
            MLOG.slog("\tFAILED")
            return
        }
        MLOG.slog("\tStatistics instance created and saved to file.")
    } else {
        MLOG.slog("\tStatistics located.")
        STAT = stat
    }
    MLOG.slog("\tBacking up Statistics.")
    STAT.saveJson(STAT_BK)
    MLOG.slog("\t...DONE")
    MLOG.slog("...DONE")
}

/**
 * Attempts to loadDao a database from file.
 * If a database could not be loaded, a new one is created.
 * Is called only once during setup.
 */
private fun setUpDatabase() {
    MLOG.slog("Setting up Database...")
    MLOG.slog("\tLoading database...")
    var tdao = loadDao()
    if (tdao == null) {
        MLOG.slog("\t\tUnable to loadDao database, creating new database.")
        tdao = Dao()
        MLOG.slog("\t\tLoading known Guilds")
        tdao.save()
        MLOG.slog("\tDatabase created and saved to file.")
        DAO = tdao
    } else {
        MLOG.slog("\tDatabase located. Updating registered Guilds.")
        DAO = tdao
    }
    //DAO.updatePremiumUsers()
    MLOG.slog("\tBacking up database.")
    DAO.backUp()
    MLOG.slog("\t...DONE")
    MLOG.slog("...DONE")
}

/**
 * Sends data to the Discor Bot List Websites (in order)
 * https://discordbots.org/bot/437851896263213056
 * https://discordbotlist.com/bots/437851896263213056
 *
 * @since 2.1
 */
private fun setupBotListApis() {
    BOT_LIST_API_UPDATERS = GlobalScope.launch(CACHED_POOL) {
        while (ON) {
            //discordbots.org
            MLOG.slog("Sending Stats to discordbots.org...")
            DISCORD_BOTLIST_API.setStats(JDA_SHARD_MNGR.shards.map { it.guilds.size })
            //discordbotlist.com
            /*MLOG.slog("Sending Stats to discordbotlist.com && discord.bots.gg...")
            JDA_SHARD_MNGR.shards.forEachIndexed { i, shard ->
                "https://discordbotlist.com/api/bots/${SELF.id}/stats".httpPost(
                    listOf("shard_id" to i, "guilds" to shard.guilds.size,
                        "users" to shard.users.size))
                    .apply { headers.clear() }
                    .header("Authorization" to "Bot $KEY_DISCORD_BOT_COM")
                    .response { _, _, result ->
                        result.component2()?.apply {
                            MLOG.elog("Failed to POST to discordbotlist.com")
                            MLOG.elog(response.responseMessage)
                        }
                    }
                //https://discord.bots.gg
                "https://discord.bots.gg/api/v1/bots/${SELF.id}/stats".httpPost(
                    listOf("guildCount" to shard.guilds.size,
                        "shardCount" to JDA_SHARD_MNGR.shards.size, "shardId" to i
                        ,"Content-Type" to "application/json"
                    ))
                    .apply { headers.clear() }
                    .header("Authorization" to KEY_DISCORD_BOTS_GG)
                    .response { _, _, result ->
                        result.component2()?.apply {
                            MLOG.elog("Failed to POST to discord.bots.gg")
                            MLOG.elog(response.responseMessage)
                        }
                    }
            }*/
            MLOG.slog("done")
            delay(30 * 60 * 1_000)
        }
    }
}

/**
 * Calls the update method for each Weebot to setup NickNames
 * changed during downtime and initialize transient variables.
 */
private fun startupWeebots() {
    MLOG.slog("Starting Weebots...")
    //Update the Weebots in the database after downtime.
    JDA_SHARD_MNGR.guilds.filterNot { DAO.WEEBOTS.contains(it.idLong) }
        .forEach {
            DAO.addBot(Weebot(it))
            askTracking(it)
        }
    DAO.GLOBAL_WEEBOT.startUp()
    DAO.WEEBOTS.values.forEach { bot -> bot.startUp() }
}

/**
 * Starts a [Job] that saves a database backup each interval.
 * Listens for a shutdown event to save the the main file
 */
@Synchronized
private fun saveTimer() = GlobalScope.launch {
    var i = 1
    try {
        while (ON) {
            DAO.backUp()
            STAT.saveJson(STAT_BK)
            if (i % 100 == 0) {
                MLOG.slog("Database & Stats back up: $i")
            }
            i++
            delay(SAVE_INTER * 1_000L)
        }
    } catch (e: InterruptedException) {
        e.printStackTrace()
        sendSMS(PHONE_JONO, "WEEBOT: Save Job Failed")
    }
}

/** Begin the shutdown sequence. Backup and save database.  */
fun shutdown(user: User? = null) {
    if (user != null)
        MLOG.elog("Shutdown signal received from ${user.name} (${user.id}).")

    MLOG.elog("\tShutting down Global Weebot Reminder pools...")
    //DAO.GLOBAL_WEEBOT.reminderPools.forEach { _, pool -> pool.shutdown() }
    MLOG.elog("\tBacking up database...")
    if (DAO.backUp() < 1)
        MLOG.elog("\t\tFailed to backup database!")
    else {
        MLOG.elog("\tSaving Database...")
        when (DAO.save()) {
            -1   -> MLOG.elog("\t\tCould not save backup due to file exception.")
            -2   -> MLOG.elog("\t\tCould not save backup due to corrupt Json.")
            else -> MLOG.elog("\t\tDatabase saved.")
        }
    }
    MLOG.elog("\tBacking up Statistics...")
    if (STAT.saveJson(STAT_BK) < 1)
        MLOG.elog("\t\tFailed to backup Statistics!")
    else {
        MLOG.elog("\tSaving Statistics...")
        when (STAT.saveJson(STAT_SAVE)) {
            -1   -> MLOG.elog("\t\tCould not save due to file exception.")
            -2   -> MLOG.elog("\t\tCould not save due to corrupt Json.")
            else -> MLOG.elog("\t\tStatistics saved.")
        }
    }

    MLOG.elog("\tClearing temp directories...")
    clearTempDirs()

    JDA_SHARD_MNGR.shutdown()
    JDA_SHARD_MNGR.statuses.forEach { _, status ->
        while (status != SHUTDOWN) {}
    }

    CACHED_POOL.close()

    MLOG.elog("Safely shutdown.")
    System.exit(0)
}
