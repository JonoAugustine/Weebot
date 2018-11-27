/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.main

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.database.*
import com.ampro.weebot.main.constants.jdaDevShardLogIn
import com.ampro.weebot.util.*
import com.jagrosh.jdautilities.command.CommandClientBuilder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import java.util.concurrent.Executors
import javax.security.auth.login.LoginException

//JDA connection shard Client
lateinit var JDA_SHARD_MNGR: ShardManager

val CACHED_POOL =  Executors.newCachedThreadPool().asCoroutineDispatcher()
val FIXED_POOL = newFixedThreadPoolContext(100, "FixedPool")

/** Main Logger */
val MLOG = FileLogger("Launcher $NOW_FILE")

/** The bot's selfuser from */
val SELF = JDA_SHARD_MNGR.shards[0].selfUser

//TODO lateinit var GLOBAL_WEEBOT: GlobalWeebot

/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 * @param args
 * @throws LoginException
 * @throws RateLimitedException
 * @throws InterruptedException
 */
fun main(args: Array<String>) {
    MLOG.slog("Launching...")
    MLOG.slog("\tBuilding Directories...")
    if (!buildDirs()) {
        MLOG.elog("\tFAILED: Build Dir | shutting down...")
        System.exit(-1)
    }
    MLOG.slog("\t...DONE")

    //Debug
    //RestAction.setPassContext(true) // enable context by default
    //RestAction.DEFAULT_FAILURE = Throwable::printStackTrace

    //JDA_CLIENT = jdaLogIn()
    //JDA_CLIENT = jdaDevLogIn()

    val cmdClientBuilder = CommandClientBuilder()
            .setOwnerId(DEV_IDS[0].toString()).setCoOwnerIds(DEV_IDS[1].toString())
            .setGuildSettingsManager { DAO.WEEBOTS[it.idLong]?.settings }
            .setPrefix("\\").setAlternativePrefix("w!")
            //.setDiscordBotListKey()
            .setGame(Game.of(Game.GameType.LISTENING, "@Weebot help"))



    JDA_SHARD_MNGR = jdaDevShardLogIn().addEventListeners().build()

    JDA_SHARD_MNGR.statuses.forEach {_, status ->
        while (status != JDA.Status.CONNECTED) {}
    }

    setUpDatabase()
    startupWeebots()
    //startSaveTimer(.5)

    //console.start()

    MLOG.elog("Launch Complete!\n\n")

}

/**
 * Attempts to loadDao a database from file.
 * If a database could not be loaded, a new one is created.
 * Is called only once during setup.
 */
private fun setUpDatabase() {
    MLOG.elog("Setting up Database...")
    MLOG.elog("\tLoading database...")
    var tdao = loadDao()
    if (tdao == null) {
        MLOG.elog("\t\tUnable to loadDao database, creating new database.")
        tdao = Dao()
        MLOG.elog("\t\tLoading known Guilds")
        JDA_SHARD_MNGR.guilds.forEach { tdao.addBot(Weebot(it)) }
        tdao.save()
        MLOG.elog("\tDatabase created and saved to file.")
        DAO = tdao
    } else {
        MLOG.elog("\tDatabase located. Updating registered Guilds.")
        DAO = Dao()
        //GLOBAL_WEEBOT = DAO.GLOBAL_WEEBOT
        updateGuilds()
    }
    MLOG.elog("\tBacking up database.")
    DAO.backUp()
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
    MLOG.elog("Starting Weebots...")
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
fun shutdown() {
    MLOG.elog(" Shutdown signal received.")
    MLOG.elog("\tClearing registered event listeners...")
    JDA_SHARD_MNGR.removeEventListenerProvider {  }

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

    MLOG.elog("Safely shutdown.")

    JDA_SHARD_MNGR.shutdown()
    JDA_SHARD_MNGR.statuses.forEach {_, status ->
        while (status != JDA.Status.SHUTDOWN) {}
    }
    FIXED_POOL.close()
    CACHED_POOL.close()
    System.exit(0)
}

/**
 * Get a guild matching the ID given.
 *
 * @param id long ID
 * @return requested Guild <br></br> null if not found.
 */
fun getGuild(id: Long): Guild? = JDA_SHARD_MNGR.guilds.find { it.idLong == id }


