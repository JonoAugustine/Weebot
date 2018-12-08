/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.main

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CMD_HELP
import com.ampro.weebot.commands.`fun`.games.cardgame.*
import com.ampro.weebot.commands.commands
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.BOT_DEV_CHAT
import com.ampro.weebot.database.constants.DEV_IDS
import com.ampro.weebot.extensions.addCommands
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.listeners.EventDispatcher
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import kotlinx.coroutines.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game.listening
import net.dv8tion.jda.core.entities.SelfUser
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.Executors
import javax.security.auth.login.LoginException
import kotlin.random.Random
import kotlin.system.measureTimeMillis

val RAND = Random(128487621469)

//JDA connection shard Client
lateinit var JDA_SHARD_MNGR: ShardManager
val WAITER: EventWaiter = EventWaiter()

val CACHED_POOL =  Executors.newCachedThreadPool().asCoroutineDispatcher()
//val FIXED_POOL = newFixedThreadPoolContext(100, "FixedPool")

/** Main Logger */
lateinit var MLOG: FileLogger

/** The bot's selfuser from */
lateinit var SELF: SelfUser

lateinit var CMD_CLIENT: CommandClient

/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 * TODO: https://www.twilio.com/blog/2017/05/send-and-receive-sms-messages-with-kotlin.html
 * TODO Learn how to use [Paginator]
 *
 * @param args_
 * @throws LoginException
 * @throws RateLimitedException/
 * @throws InterruptedException
 */
fun main(args_: Array<String>) = runBlocking {
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
    slog(DECK_CUST)
    //Debug
    //RestAction.setPassContext(true) // enable context by default
    //RestAction.DEFAULT_FAILURE = Throwable::printStackTrace

    //JDA_SHARD_MNGR = jdaShardLogIn().build()
    JDA_SHARD_MNGR = jdaDevShardLogIn().build()

    setUpDatabase()

    CMD_CLIENT = CommandClientBuilder().setOwnerId(DEV_IDS[0].toString())
        .setCoOwnerIds(DEV_IDS[1].toString())
        .setGuildSettingsManager { DAO.WEEBOTS[it.idLong]?.settings }
        //.setPrefix("\\")
        .setAlternativePrefix("\\")
        .setGame(listening("@Weebot help"))
        .addCommands(commands)
        .setEmojis(heavy_check_mark.unicode, Warning.unicode, X.unicode)
        .setHelpConsumer { event ->
            //If the only argument is the command invoke
            val args = event.splitArgs()
            if (args.isEmpty()) {
                CMD_HELP.execute(event)
            } else {
                commands.forEach { cmd ->
                    if (cmd.isCommandFor(args[0])) {
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
        .build()

    genSetup.joinAll() //Ensure all gensetup is finished

    JDA_SHARD_MNGR.apply {
        addEventListener(CMD_CLIENT)
        addEventListener(EventDispatcher())
    }

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
        //Update the Weebots in the database after downtime.
        JDA_SHARD_MNGR.guilds.forEach { DAO.addBot(Weebot(it)) }
    }
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



