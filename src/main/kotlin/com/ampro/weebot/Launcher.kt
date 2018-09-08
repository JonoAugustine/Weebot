package com.ampro.weebot

/**
 * Builds single JDA connection, instances of all Commands, and Database.
 *
 * @author Jonathan Augustine
 * @copyright Aquatic Mastery Productions 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import com.ampro.weebot.bot.GlobalWeebot
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.developer.ShutdownCommand
import com.ampro.weebot.commands.getCommand
import com.ampro.weebot.contants.jdaDevLogIn
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.Dao
import com.ampro.weebot.database.loadDao
import com.ampro.weebot.listener.EventDispatcher
import com.ampro.weebot.util.*
import com.ampro.weebot.util.io.FileManager
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.exceptions.RateLimitedException
import java.util.*
import java.util.concurrent.Executors
import javax.security.auth.login.LoginException


internal val console = Thread {
    MLOG.elog("[Launcher#console] Starting console listener...")
    val scanner = Scanner(System.`in`)
    val c = getCommand(ShutdownCommand::class) as ShutdownCommand
    while (true) {
        if (scanner.hasNext())
            if (c.isCommandFor(scanner.nextLine())) { shutdown() }
    }
}

internal lateinit var saveTimer: Thread

lateinit var JDA_CLIENT: JDA

/**
 * Get a guild matching the ID given.
 *
 * @param id long ID
 * @return requested Guild <br></br> null if not found.
 */
fun getGuild(id: Long): Guild? = JDA_CLIENT.guilds.find { it.idLong == id }

lateinit var GLOBAL_WEEBOT: GlobalWeebot

val CACHED_POOL =  Executors.newCachedThreadPool().asCoroutineDispatcher()
val FIXED_POOL = newFixedThreadPoolContext(100, "FixedPool")

val MLOG = FileLogger("Launcher $NOW_FILE")

/**
 * Put bot online, setup listeners, and get full list of servers (Guilds)
 *
 * @param args
 * @throws LoginException
 * @throws RateLimitedException
 * @throws InterruptedException
 */
@Throws(LoginException::class, InterruptedException::class)
fun main(args: Array<String>) {
    MLOG.elog("Building Directories...")
    if (!buildDirs()) {
        MLOG.elog("\tFAILED!")
        System.exit(-1)
    }
    //Debug
    //RestAction.setPassContext(true) // enable context by default
    //RestAction.DEFAULT_FAILURE = Throwable::printStackTrace

    //JDA_CLIENT = jdaLogIn()
    JDA_CLIENT = jdaDevLogIn()
    Launcher.setJDA(JDA_CLIENT)

    setUpDatabase()
    startupWeebots()
    startSaveTimer(.5)

    addListeners()
    console.start()
    MLOG.elog("Initialization Complete!\n\n")

}


/**
 * Adds event listeners to the JDA.
 */
private fun addListeners() {
    MLOG.elog("Adding Listeners to JDA Client...")
    JDA_CLIENT.addEventListener(EventDispatcher(), GLOBAL_WEEBOT)
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
        JDA_CLIENT.guilds.forEach { tdao.addBot(Weebot(it)) }
        tdao.save()
        MLOG.elog("\tDatabase created and saved to file.")
        DAO = tdao
    } else {
        MLOG.elog("\tDatabase located. Updating registered Guilds.")
        DAO = Dao()
        GLOBAL_WEEBOT = DAO.GLOBAL_WEEBOT
        updateGuilds()
    }
    MLOG.elog("\tBacking up database.")
    DAO.backUp()
}

/**
 * Update the Weebots in the database after downtime.
 * **This is only called once on startup**
 */
private fun updateGuilds() = JDA_CLIENT.guilds.forEach { DAO.addBot(Weebot(it)) }

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
 */
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

/** Begin the shutdown sequence. Backup and save database.  */
fun shutdown() {
    MLOG.elog(" Shutdown signal received.")
    MLOG.elog("\tClearing registered event listeners...")
    for (o in JDA_CLIENT.registeredListeners) {
        JDA_CLIENT.removeEventListener(o)
    }
    MLOG.elog("\tStopping save timer thread...")
    saveTimer.interrupt()
    MLOG.elog("\tShutting down Global Weebot Reminder pools...")
    DAO.GLOBAL_WEEBOT.reminderPools.forEach { _, pool -> pool.shutdown() }
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
    FileManager.clearTempDirs()

    MLOG.elog("Safely shutdown.")

    JDA_CLIENT.shutdown()
    while (JDA_CLIENT.status != JDA.Status.SHUTDOWN) {}
    FIXED_POOL.close()
    System.exit(0)

}
