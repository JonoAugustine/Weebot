/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.main

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.Dao
import com.ampro.weebot.database.constants.BOT_DEV_CHAT
import com.ampro.weebot.database.constants.DEV_IDS
import com.ampro.weebot.database.constants.jdaDevShardLogIn
import com.ampro.weebot.database.loadDao
import com.ampro.weebot.util.*
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import kotlinx.coroutines.asCoroutineDispatcher
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.SelfUser
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.Executors
import javax.security.auth.login.LoginException
import kotlin.system.measureTimeMillis

//JDA connection shard Client
lateinit var JDA_SHARD_MNGR: ShardManager
val WAITER: EventWaiter = EventWaiter()

val CACHED_POOL =  Executors.newCachedThreadPool().asCoroutineDispatcher()
//val FIXED_POOL = newFixedThreadPoolContext(100, "FixedPool")

/** Main Logger */
lateinit var MLOG: FileLogger

/** The bot's selfuser from */
lateinit var SELF: SelfUser

//TODO lateinit var GLOBAL_WEEBOT: GlobalWeebot

/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 * @param args
 * @throws LoginException
 * @throws RateLimitedException
 * @throws InterruptedException
 */
fun main(args: Array<String>) = run {
    slog("Launching...")
    slog("\tBuilding Directories...")
    if (!buildDirs()) {
        elog("\tFAILED: Build Dir | shutting down...")
        System.exit(-1)
    }
    slog("\t...DONE")
    slog("Initializing Main Logger")
    MLOG = FileLogger("Launcher $NOW_FILE")
    slog("...DONE\n\n")

    //Debug
    //RestAction.setPassContext(true) // enable context by default
    //RestAction.DEFAULT_FAILURE = Throwable::printStackTrace

    //JDA_CLIENT = jdaLogIn()
    //JDA_CLIENT = jdaDevLogIn()

    JDA_SHARD_MNGR = jdaDevShardLogIn().build()

    setUpDatabase()

    val cmdClientBuilder = CommandClientBuilder().setOwnerId(DEV_IDS[0].toString())
        .setCoOwnerIds(DEV_IDS[1].toString())
        .setGuildSettingsManager { DAO.WEEBOTS[it.idLong]?.settings }.setPrefix("\\")
        .setAlternativePrefix("w!")
        .setGame(Game.of(Game.GameType.LISTENING, "@Weebot help"))
        .addCommands(CMD_SHUTDOWN, CDM_PING, COM_GUILDLIST, CMD_ABOUT, CMD_SUGG,
                CMD_INVITEBOT, CDM_VCR)

    JDA_SHARD_MNGR.addEventListener(cmdClientBuilder.build())

    MLOG.slog("Shard connected! ${measureTimeMillis {
        while (JDA_SHARD_MNGR.shards[0].status != JDA.Status.CONNECTED) {
            MLOG.slog("Waiting for shard to connect...")
            Thread.sleep(500)
        }
    } / 1_000} seconds")

    SELF = JDA_SHARD_MNGR.shards[0].selfUser

    startupWeebots()
    //startSaveTimer(.5)

    //console.start()

    MLOG.slog("Launch Complete!\n\n")

    JDA_SHARD_MNGR.getTextChannelById(BOT_DEV_CHAT).sendMessage("ONLINE!").queue()
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
        //GLOBAL_WEEBOT = DAO.GLOBAL_WEEBOT
        updateGuilds()
    }
    MLOG.slog("\tBacking up database.")
    DAO.backUp()
    MLOG.slog("\t...DONE")
    MLOG.slog("...DONE")
}

/**
 * Update the Weebots in the database after downtime.
 * **This is only called once on startup**
 */
private fun updateGuilds() = JDA_SHARD_MNGR.guilds.forEach { DAO.addBot(Weebot(it)) }

/**
 * Calls the update method for each Weebot to setup NickNames
 * changed during downtime and initialize transient variables.
 */
private fun startupWeebots() {
    MLOG.slog("Starting Weebots...")
    DAO.WEEBOTS.forEach { _, bot -> bot.startup() }
}

/**
 * Starts a thread that saves a database backup each interval.
 * <br></br> Listens for a shutdown event to save the the main file
 * @param min The delay in minuets between saves.
 *
@Synchronized
private fun startSaveTimer(min: Double) {
    saveTimer = Thread {
        var i = 1
        val time = Math.round(1000 * 60 * min)
        try {
            while (true) {
                if (JDA_CLIENT.status == JDA.Status.SHUTDOWN)
                    break
                if (JDA_CLIENT.status != JDA.Status.CONNECTED)
                    continue
                DAO.backUp()
                if (i % 10 == 0) {
                    MLOG.elog("Database back up: $i")
                }
                i++
                Thread.sleep(time)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    saveTimer.name = "Save Timer"
    saveTimer.start()
}
*/

/** Begin the shutdown sequence. Backup and save database.  */
fun shutdown(user: User) {
    MLOG.elog("Shutdown signal received from ${user.name} (${user.id}).")
    MLOG.elog("\tClearing registered event listeners...")

    MLOG.elog("\tStopping save timer thread...")
    //saveTimer.interrupt()
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

    //FIXED_POOL.close()
    CACHED_POOL.close()

    MLOG.elog("Safely shutdown.")
    System.exit(0)
}



