/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.Poolcounts.cachedPoolCount
import com.ampro.weebot.Poolcounts.cmdPoolCount
import com.ampro.weebot.api.javalin
import com.ampro.weebot.commands.CMD_HELP
import com.ampro.weebot.commands.COMMANDS
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.LINK_HQTWITCH
import com.ampro.weebot.commands.developer.CLIENT_TWTICH_PUB
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.bots
import com.ampro.weebot.database.constants.BOT_DEV_CHAT
import com.ampro.weebot.database.constants.LINK_INVITEBOT
import com.ampro.weebot.database.constants.NL_GUILD
import com.ampro.weebot.database.constants.PHONE_JONO
import com.ampro.weebot.database.constants.TWITCH_HQR_ID
import com.ampro.weebot.database.constants.jdaDevShardLogIn
import com.ampro.weebot.database.constants.jdaShardLogIn
import com.ampro.weebot.database.save
import com.ampro.weebot.extensions.WeebotCommandClient
import com.ampro.weebot.extensions.delete
import com.ampro.weebot.extensions.get
import com.ampro.weebot.extensions.matches
import com.ampro.weebot.extensions.queueIgnore
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.extensions.weebotAvatar
import com.ampro.weebot.util.Emoji.Rage
import com.ampro.weebot.util.FileLogger
import com.ampro.weebot.util.buildDirs
import com.ampro.weebot.util.clearTempDirs
import com.ampro.weebot.util.elog
import com.ampro.weebot.util.sendSMS
import com.ampro.weebot.util.setUpWebFuel
import com.ampro.weebot.util.slog
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA.Status.CONNECTED
import net.dv8tion.jda.core.JDA.Status.SHUTDOWN
import net.dv8tion.jda.core.entities.Game.GameType.STREAMING
import net.dv8tion.jda.core.entities.Game.listening
import net.dv8tion.jda.core.entities.Game.playing
import net.dv8tion.jda.core.entities.Game.streaming
import net.dv8tion.jda.core.entities.Game.watching
import net.dv8tion.jda.core.entities.SelfUser
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import javax.security.auth.login.LoginException
import kotlin.system.measureTimeMillis


private lateinit var SAVER_JOB: Job
val SAVE_JOBS = mutableListOf<() -> Unit>({ bots.forEach { runBlocking { it.save() } }})
/** How ofter to asve to file in Seconds */
private const val SAVE_INTER = 120

private object Poolcounts {
    var cachedPoolCount = 0
    var cmdPoolCount = 0
}
val CACHED_POOL = ThreadPoolExecutor(8, 100, 10, MINUTES, SynchronousQueue()) { r ->
    MLOG.slog(ThreadFactory::class, "Cached Thread Created: ${++cachedPoolCount}")
    Thread(r, "CachedPool-$cachedPoolCount")
}.asCoroutineDispatcher()
val CMD_POOL = newFixedThreadPool(25) { r ->
    MLOG.slog(ThreadFactory::class, "Command Thread Created: ${++cmdPoolCount}")
    Thread(r, "CommandPool-$cmdPoolCount")
}.asCoroutineDispatcher()

/** Main Logger */
lateinit var MLOG: FileLogger

var ON = true
//JDA connection shard Client
lateinit var JDA_SHARD_MNGR: ShardManager
lateinit var CMD_CLIENT: WeebotCommandClient
val WAITER: EventWaiter = EventWaiter()
/** The bot's selfuser from */
lateinit var SELF: SelfUser

/** *Sorry, I tripped over my shoelaces. Please try that again later* */
const val GENERIC_ERR_MSG = "*Sorry, I tripped over my shoelaces. Please try that again later*"

val games = listOf(listening("@Weebot Help"), playing("with wires"),
    watching("Humans Poop"), listening("your thoughts"), playing("Weebot 2.3.0"),
    listening("crying enemies"), watching("HQR's life fall apart"))

/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 *
 * @param args_
 * @throws LoginException
 * @throws InterruptedException
 */
fun main(args_: Array<String>) = runBlocking<Unit> {
    slog("Launching...")
    slog("\tBuilding Directories...")
    if (!buildDirs()) {
        elog("\tFAILED: Build Dir | shutting down...")
        System.exit(-1)
    }
    slog("Initializing Main Logger\n\n")
    //LOGIN & LISTENERS
    val weebot = (args_.isNotEmpty() && args_[0].matches("(?i)(t|tobeew|test)")).not()

    MLOG = FileLogger("Weebot", true, !weebot)

    /** Setup for random methods and stuff that is needed before launch */
    val genSetup = listOf(
        launch { setUpWebFuel() },
        launch { javalin.start(System.getenv("PORT")?.toInt() ?: 6900) }
    )

    //Debug
    //RestAction.setPassContext(true) // enable context by default
    //RestAction.DEFAULT_FAILURE = Consumer(Throwable::printStackTrace)

    val alt = if (weebot) "\\" else "t\\"

    //COMMAND CLIENT
    CMD_CLIENT = WeebotCommandClient(listOf(alt), LINK_INVITEBOT, games.random(),
        CMD_POOL, (CMD_HELP.aliases + CMD_HELP.name).toList()) { event ->
        val args = event.splitArgs()
        event.delete()
        if (args.isEmpty()) CMD_HELP.execute(event)
        else COMMANDS.forEach { cmd ->
            if (cmd.isCommandFor(args[0]) && (!cmd.isHidden || event.isOwner)) {
                if (cmd.getHelpBiConsumer() != null) {
                    cmd.getHelpBiConsumer()!!.accept(event, cmd)
                } else if (!cmd.help.isNullOrBlank()) {
                    event.reply("*${cmd.help}*")
                } else {
                    event.reply("*Help is currently unavailable for this " +
                        "command. You can use ``@Weebot sugg`` to "
                        + "send feedback to the Developers and remind "
                        + "them they have a job to do!* $Rage")
                }
            }
        }
    }

    //LOGIN & LISTENERS
    JDA_SHARD_MNGR = (if (weebot) jdaShardLogIn() else jdaDevShardLogIn())
        .addEventListeners(CMD_CLIENT, WAITER)
        .build().apply { addEventListener(EventDispatcher) }
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
    MLOG.slog(null,
        "All ${JDA_SHARD_MNGR.shards.size} Shards connected! ${measureTimeMillis {
            MLOG.slog(null,
                "Waiting for all ${JDA_SHARD_MNGR.shards.size} shards to connect...")
            while (JDA_SHARD_MNGR.shards.any { it.status != CONNECTED }) Thread.sleep(250)
        } / 1_000} seconds")

    //SET SELF AND AVATAR URL
    SELF = JDA_SHARD_MNGR.shards[0].selfUser
    weebotAvatar = SELF.avatarUrl
    JDA_SHARD_MNGR.shards

    genSetup.joinAll() //Ensure all gen-setup is finished

    SAVER_JOB = GlobalScope.launch(CACHED_POOL) {
        var i = 0
        try {
            MLOG.slog(null, "Starting Save Job...")
            while (ON) {
                i++
                for (j in 0 until SAVE_JOBS.size) Runnable(SAVE_JOBS[j]).run()
                if (i % 100 == 0) MLOG.slog(null, "Save Cycle: $i")
                delay(SAVE_INTER * 1_000L)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            sendSMS(PHONE_JONO, "WEEBOT: Save Job Failed")
        }
    }

    MLOG.slog(null, "Launch Complete!\n\n")

    //Send online confirm message
    JDA_SHARD_MNGR.getTextChannelById(BOT_DEV_CHAT).sendMessage("ONLINE!")
        .queueAfter(250, MILLISECONDS) { it.delete().queueIgnore(30) }

    NL_GUILD!!.bot.add(object : IPassive {
        override fun dead(): Boolean = false
        override fun accept(bot: Weebot, event: Event) {
            if (event is GuildMessageReactionAddEvent) {

            }
        }
    })

    //Watch HQStream
    launch(CACHED_POOL) {
        while (ON) {
            MLOG.slog(null, "Checking HQRegent Twitch Live Stream")
            CLIENT_TWTICH_PUB.helix.getStreams(
                "", "", null, 1, null, null, null, listOf(127416768), null
            ).observe().subscribe { streamList ->
                streamList.streams.firstOrNull { it.userId == TWITCH_HQR_ID }?.also {
                    JDA_SHARD_MNGR.setGame(streaming(it.title, LINK_HQTWITCH))
                    MLOG.slog(null, "\nOnline. Set watching")
                }
            }
            delay(20 * 60 * 1_000)
            MLOG.slog(null, "Checking HQRegent Twitch Live Stream")
            CLIENT_TWTICH_PUB.helix.getStreams(
                "", "", null, 1, null, null, null, listOf(127416768), null
            ).observe().subscribe { streamList ->
                if (streamList.streams.none { it.userId == TWITCH_HQR_ID }
                    && JDA_SHARD_MNGR[0].presence.game == STREAMING) {
                    JDA_SHARD_MNGR.setGame(games.random())
                    MLOG.slog(null, "\nOffline. Set random")
                }
            }
            delay(20 * 60 * 1_000)
        }
    }
    //Watch HQTwitter
    /*launch(CACHED_POOL) {
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

    }*/
}


/**
 * Calls the update method for each Weebot to setup NickNames
 * changed during downtime and initialize transient variables.
 */
private fun startupWeebots() {
    MLOG.slog(null, "Starting Weebots...")
    (JDA_SHARD_MNGR.guilds.map { it.bot } + GlobalWeebot).forEach { it.startUp() }
}



/** Begin the shutdown sequence. Backup and save database.  */
fun shutdown(user: User? = null) {
    MLOG.elog(null, "Shutdown signal received ${
    if (user != null) "from " + user.name + " (${user.id})." else ""}")

    MLOG.elog(null, "\tClearing temp directories...")
    clearTempDirs()
    SAVE_JOBS.forEach { it() }

    JDA_SHARD_MNGR.shutdown()
    JDA_SHARD_MNGR.statuses.forEach { (_, status) ->
        while (status != SHUTDOWN) {
        }
    }

    CACHED_POOL.close()

    MLOG.elog(null, "Safely shutdown.")
    System.exit(0)
}
