/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CMD_HELP
import com.ampro.weebot.commands.COMMANDS
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.github.kittinunf.fuel.httpPost
import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import kotlinx.coroutines.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA.Status.CONNECTED
import net.dv8tion.jda.core.JDA.Status.SHUTDOWN
import net.dv8tion.jda.core.entities.Game.playing
import net.dv8tion.jda.core.entities.SelfUser
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.security.auth.login.LoginException
import kotlin.system.measureTimeMillis

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
        //.setGame(listening("@Weebot help"))
        .setGame(playing("Weebot 2.1 Kotlin!")).addCommandsWithCheck(COMMANDS)
        .setEmojis(heavy_check_mark.unicode, Warning.unicode, X_Red.unicode)
        .setServerInvite(LINK_INVITEBOT).setHelpConsumer { event ->
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

    //LOGIN & LISTENERS
    JDA_SHARD_MNGR = if (weebot) {
        jdaShardLogIn().addEventListeners(CMD_CLIENT).build()
    } else {
        jdaDevShardLogIn().addEventListeners(CMD_CLIENT).build()
    }

    //WAIT FOR SHARD CONNECT
    MLOG.slog("All Shards connected! ${measureTimeMillis {
        while (JDA_SHARD_MNGR.shards.has { it.status != CONNECTED }) {
            MLOG.slog("Waiting for all shards to connect...")
            Thread.sleep(500)
        }
    } / 1_000} seconds")

    //SET SELF AND AVATAR URL
    SELF = JDA_SHARD_MNGR.shards[0].selfUser
    weebotAvatar = SELF.avatarUrl

    //DATABASE
    setUpDatabase()
    //Stats
    setUpStatistics()
    //Bot List website APIs
    setupBotListApis()

    JDA_SHARD_MNGR.addEventListener(EventDispatcher(), WAITER)

    genSetup.joinAll() //Ensure all gensetup is finished

    startupWeebots()

    MLOG.slog("Starting Save Job...")
    SAVE_JOB = saveTimer()

    MLOG.slog("Launch Complete!\n\n")

    JDA_SHARD_MNGR.getTextChannelById(BOT_DEV_CHAT).sendMessage("ONLINE!")
        .queueAfter(850, MILLISECONDS)
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
        JDA_SHARD_MNGR.guilds.filterNot { tdao.WEEBOTS.contains(it.idLong) }
            .forEach {
                tdao.addBot(Weebot(it))
                askTracking(it)
        }
        tdao.save()
        MLOG.slog("\tDatabase created and saved to file.")
        DAO = tdao
    } else {
        MLOG.slog("\tDatabase located. Updating registered Guilds.")
        DAO = tdao
        //Update the Weebots in the database after downtime.
        JDA_SHARD_MNGR.guilds.forEach { DAO.addBot(Weebot(it)) }
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
            DISCORD_BOTLIST_API.setStats(JDA_SHARD_MNGR.shards.map { it.guilds.size })
            //discordbotlist.com
            JDA_SHARD_MNGR.shards.forEachIndexed { i, shard ->
                "https://discordbotlist.com//api/bots/:${SELF.id}/stats"
                    .httpPost(listOf(
                        "shard_id" to i,
                        "guilds" to shard.guilds.size,
                        "users" to shard.users.size
                    ))
            }
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
