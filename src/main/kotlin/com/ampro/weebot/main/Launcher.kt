/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.main

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CMD_HELP
import com.ampro.weebot.commands.commands
import com.ampro.weebot.commands.utilitycommands.remThreadPool
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.listeners.EventDispatcher
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import kotlinx.coroutines.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game.playing
import net.dv8tion.jda.core.entities.SelfUser
import net.dv8tion.jda.core.entities.User
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.logging.Level.CONFIG
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


/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 *
 * @param args_
 * @throws LoginException
 * @throws RateLimitedException/
 * @throws InterruptedException
 */
fun main(args_: Array<String>) {
    runBlocking {
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
        val weebot = (args_.isEmpty() || args_[0].matches(reg_wbot))
                || (FILE_CONFIG.exists() && FILE_CONFIG.readLines()[0].matches(reg_wbot))

        val alt = if (weebot) "\\" else "t\\"


        //COMMAND CLIENT
        CMD_CLIENT = CommandClientBuilder().setOwnerId(DEV_IDS[0].toString())
            .setCoOwnerIds(DEV_IDS[1].toString())
            .setGuildSettingsManager { getWeebotOrNew(it.idLong).settings }
            .setAlternativePrefix(alt)
            //.setGame(listening("@Weebot help"))
            .setGame(playing("Weebot 2.0 Kotlin!"))
            .addCommands(commands)
            .setEmojis(heavy_check_mark.unicode, Warning.unicode, X_Red.unicode)
            .setServerInvite(LINK_INVITEBOT)
            .setHelpConsumer { event ->
                //If the only argument is the command invoke
                val args = event.splitArgs()
                if (args.isEmpty()) {
                    CMD_HELP.execute(event)
                } else {
                    commands.forEach { cmd ->
                        if (cmd.isCommandFor(args[0]) && (!cmd.isHidden || event.isOwner)) {
                            if (cmd.getHelpBiConsumer() != null) {
                                cmd.getHelpBiConsumer().accept(event, cmd)
                                return@setHelpConsumer
                            } else if (!cmd.help.isNullOrBlank()) {
                                event.reply("*${cmd.help}*")
                                return@setHelpConsumer
                            } else {
                                event.reply("*Help is currently unavailable for this command." +
                                        " You can use ``@Weebot sugg`` to send feedback to the" +
                                        " Developers and remind them they have a job to do!* $Rage")
                                return@setHelpConsumer
                            }
                        }
                    }
                }
            }
            .setDiscordBotsKey(BOTSONDISCORD_KEY)
            //.setDiscordBotListKey(BOTLIST_KEY)
            .build()

        //LOGIN & LISTENERS
        JDA_SHARD_MNGR = if (weebot) {
            jdaShardLogIn().addEventListeners(CMD_CLIENT).build()
        } else {
            jdaDevShardLogIn().addEventListeners(CMD_CLIENT).build()
        }

        //WAIT FOR SHARD CONNECT
        MLOG.slog("Shard connected! ${measureTimeMillis {
            while (JDA_SHARD_MNGR.shards[0].status != JDA.Status.CONNECTED) {
                MLOG.slog("Waiting for shard to connect...")
                Thread.sleep(500)
            }
        } / 1_000} seconds")

        //DATABASE
        setUpDatabase()
        //Stats
        setUpStatistics()

        JDA_SHARD_MNGR.addEventListener(EventDispatcher(), WAITER)

        genSetup.joinAll() //Ensure all gensetup is finished

        //SET SELF AND AVATAR URL
        SELF = JDA_SHARD_MNGR.shards[0].selfUser
        weebotAvatar = SELF.avatarUrl

        startupWeebots()

        MLOG.slog("Starting Save Job...")
        SAVE_JOB = saveTimer()

        MLOG.slog("Launch Complete!\n\n")

        JDA_SHARD_MNGR.getTextChannelById(BOT_DEV_CHAT).sendMessage("ONLINE!")
            .queueAfter(850, MILLISECONDS)
    }
}

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
        JDA_SHARD_MNGR.guilds.forEach { tdao.addBot(Weebot(it)) }
        tdao.save()
        MLOG.slog("\tDatabase created and saved to file.")
        DAO = tdao
    } else {
        MLOG.slog("\tDatabase located. Updating registered Guilds.")
        DAO = tdao
        //Update the Weebots in the database after downtime.
        JDA_SHARD_MNGR.guilds.forEach { DAO.addBot(Weebot(it)) }
    }
    DAO.updatePremiumUsers()
    MLOG.slog("\tBacking up database.")
    DAO.backUp()
    MLOG.slog("\t...DONE")
    MLOG.slog("...DONE")
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
 *
 * @param min The delay in minuets between saves.
 */
@Synchronized
private fun saveTimer() = GlobalScope.launch {
    var i = 1
    try {
        while (ON) {
            DAO.backUp()
            STAT.saveJson(STAT_SAVE)
            if (i % 50 == 0) {
                MLOG.slog("Database back up: $i")
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

    MLOG.elog("\tClearing temp directories...")
    clearTempDirs()

    JDA_SHARD_MNGR.shutdown()
    JDA_SHARD_MNGR.statuses.forEach { _, status ->
        while (status != JDA.Status.SHUTDOWN) {}
    }

    remThreadPool.close()
    CACHED_POOL.close()

    MLOG.elog("Safely shutdown.")
    System.exit(0)
}

const val GENERIC_ERR_MESG = "*Sorry, I tripped over my shoelaces. Please try that " +
        "again later*"
