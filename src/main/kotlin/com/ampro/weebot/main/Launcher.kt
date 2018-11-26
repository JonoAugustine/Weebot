/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.main

import com.ampro.weebot.contants.jdaDevLogIn
import com.ampro.weebot.util.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import java.util.concurrent.Executors
import javax.security.auth.login.LoginException

//JDA connection Client
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
    JDA_CLIENT = jdaDevLogIn()

    setUpDatabase()
    startupWeebots()
    startSaveTimer(.5)

    addListeners()
    console.start()
    MLOG.elog("Initialization Complete!\n\n")

}
