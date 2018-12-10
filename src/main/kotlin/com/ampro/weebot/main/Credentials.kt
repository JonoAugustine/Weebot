/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.main

import com.twilio.type.PhoneNumber
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import javax.security.auth.login.LoginException


/* *****************
        API Keys
 *******************/

const val API_CAT_TOKEN  = "949c9e4b-02af-42a9-a38b-3b90ac927ccf" //https://thecatapi.com/
const val TWILIO_SID     = "ACd4d05126a5cef2d7d3df831aa564f1f8"
const val TWILIO_TOKEN   = "bd906c9dad4f39c2c2e662f38095bc00"
val TWILIO_NUMBER  = PhoneNumber("+17084773268")

/* *********************
      Weebot & Tobeew
 **********************/

val LINK_INVITEBOT = "https://discordapp.com/api/oauth2/authorize?client_id=437851896263213056&permissions=500296919&scope=bot"

val LINK_INVITE_TESTBOT = "https://discordapp" +
        ".com/oauth2/authorize?client_id=444323732010303488&permissions=8&scope=bot"

private const val TOKEN_WBT =
        "NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk"
private const val TOKEN_TEST = "NDQ0MzIzNzMyMDEwMzAzNDg4.DdaQyQ.ztloAQmeuUffaC-DC9zE-LFwPq4"

/**
 * Initiates `Launcher` data and connects to Weebot API.
 * Builds Asynchronously in a separate thread to let main thread work on
 * other setup processes.
 *
 * @throws LoginException
 * @throws InterruptedException
 */
@Throws(LoginException::class, InterruptedException::class)
fun jdaLogIn() : JDA {
    MLOG.slog("Logging in to Weebot JDA TWILIO_CLIENT...")
    return JDABuilder(AccountType.BOT).setToken(TOKEN_WBT)
        .setGame(Game.playing("@Weebot help")).setCorePoolSize(10)
        .build().awaitReady()
}

/**
 * Initiates `Launcher` data and connects to Weebot TestBuild API.
 * Builds Asynchronously in a separate thread to let main thread work on
 * other setup processes.
 *
 * @throws LoginException
 * @throws InterruptedException
 */
@Throws(LoginException::class, InterruptedException::class)
fun jdaDevLogIn() : JDA {
    MLOG.slog("Logging in to TestBot JDA TWILIO_CLIENT...")
    return JDABuilder(AccountType.BOT).setToken(TOKEN_TEST)
        .setGame(Game.playing("Genocide")).setCorePoolSize(10)
            .build().awaitReady()
}

/**
 * Does not build
 */
@Throws(LoginException::class, InterruptedException::class)
fun jdaShardLogIn() : DefaultShardManagerBuilder {
    MLOG.slog("Logging in to Weebot JDA shards...")
    return DefaultShardManagerBuilder().setToken(TOKEN_WBT)
            .setShardsTotal(-1).setGame(Game.playing("@Weebot help"))
            .setCorePoolSize(50)
}

/**
 * Does not build
 */
@Throws(LoginException::class, InterruptedException::class)
fun jdaDevShardLogIn() : DefaultShardManagerBuilder {
    MLOG.slog("Logging in to TestBot JDA shards...")
    return DefaultShardManagerBuilder().setToken(TOKEN_TEST)
            .setShardsTotal(1)
            .setGame(Game.playing("Genocide")).setCorePoolSize(10)
}
