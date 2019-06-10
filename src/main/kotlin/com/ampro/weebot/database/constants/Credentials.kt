/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.database.constants

import com.ampro.weebot.MLOG
import com.twilio.type.PhoneNumber
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import javax.security.auth.login.LoginException

const val TWITCH_HQR_ID = 127416768L

/* *****************
        API Keys
 *******************/

const val API_CAT_TOKEN  = "949c9e4b-02af-42a9-a38b-3b90ac927ccf" //https://thecatapi.com/
const val TWILIO_SID     = "ACd4d05126a5cef2d7d3df831aa564f1f8"
const val TWILIO_TOKEN   = "bd906c9dad4f39c2c2e662f38095bc00"
val TWILIO_NUMBER  = PhoneNumber("+17084773268")

/* Reddit */
const val REDDIT_ID = "wt50r_Mqf1zs_A"
const val REDDIT_SEC = "WRLTL6bEdgtSELdw_3a79QoRAA0"
const val REDDIT_USR = "Ventus_Aurelius"
const val REDDIT_PAS = "G3lassenheit"

/** Twitter */
const val KEY_TWITTER_API = "RX7LWmYZ4HjWZVdTbTrUzgz5e"
const val KEY_TWITTER_API_SEC = "oGnGZ5h3CEiiZRsPEyOVayABH8pZY9d9QVq3IUGBgmApyYTyDA"
const val TOKEN_TWITTER = "1059304575956606976-pD8d1K3B2T3g7W5TlXvycDoo2gUgDV"
const val TOKEN_TWITTER_SEC = "y9lswGdRAI3gRiZ0GRdl20HRobngTpDapSj3nNT7kMU0m"
/* *********************
      SQL
 **********************

val ds = MysqlDataSource().apply {
    serverName = "den1.mysql4.gear.host"
    databaseName = "weebotdb"
    user = "weebotdb"
    password = "kjshdo&A(*Sdba76y879as"
}
val configuration = KotlinConfiguration(dataSource = ds, model = )
val data = KotlinEntityDataStore(configuration)
*/

/* *********************
      Weebot & Tobeew
 **********************/

/** Key for https://discordbotlist.com/bots/437851896263213056 */
const val KEY_DISCORD_BOT_COM = "6f499481ace429583babc6704c457a7b037f001a2f57df3ebc5b4ff383088b05 "
/** Key for https://discordbots.org/bot/437851896263213056 */
const val KEY_DISCORD_BOTS_ORG     = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjQzNzg1MTg5NjI2MzIxMzA1NiIsImJvdCI6dHJ1ZSwiaWF0IjoxNTQ2OTI4NzI5fQ.wnbwJc1D4pzFNEo7hbHOT_0jFi-6vFcbtO6PRQW_jzY"
const val KEY_DISCORD_BOTS_GG = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcGkiOnRydWUsImlkIjoiMTM5MTY3NzMwMjM3NTcxMDcyIiwiaWF0IjoxNTQ2OTI3NTMyfQ.200ymKmtqiRNFubFUT-yxLJ_O60jxMM1S3v7GAujt5c"
val KEY_BOTS_ON_DISCORD_XYZ = {TODO()}


const val LINK_DISCORD_BOTS = "https://discordbots.org/bot/437851896263213056"
const val LINK_DISCORD_BOTS_LIST = "https://discordbotlist.com/bots/437851896263213056"

val LINK_INVITEBOT = "https://discordapp.com/api/oauth2/authorize?client_id=437851896263213056&permissions=500296919&scope=bot"

val LINK_INVITE_TESTBOT = "https://discordapp.com/oauth2/authorize?client_id=444323732010303488&permissions=8&scope=bot"

private const val TOKEN_WBT =
        "NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk"
const val CLIENT_WBT = 437851896263213056
private const val TOKEN_TEST = "UAE-i4FFQakMds56XakTKI8TyMCFn2jg"

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
    MLOG.slog(null, "Logging in to Weebot JDA TWILIO_CLIENT...")
    return JDABuilder(AccountType.BOT).setToken(
        TOKEN_WBT)
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
    MLOG.slog(null, "Logging in to TestBot JDA TWILIO_CLIENT...")
    return JDABuilder(AccountType.BOT).setToken(
        TOKEN_TEST)
        .setGame(Game.playing("Genocide")).setCorePoolSize(10)
            .build().awaitReady()
}

/**
 * Does not build
 */
@Throws(LoginException::class, InterruptedException::class)
fun jdaShardLogIn() : DefaultShardManagerBuilder {
    MLOG.slog(null, "Logging in to Weebot JDA shards...")
    return DefaultShardManagerBuilder().setToken(TOKEN_WBT)
        .setShardsTotal(-1).setCorePoolSize(50)
}

/**
 * Does not build
 */
@Throws(LoginException::class, InterruptedException::class)
fun jdaDevShardLogIn() : DefaultShardManagerBuilder {
    MLOG.slog(null, "Logging in to TestBot JDA shards...")
    return DefaultShardManagerBuilder().setToken(TOKEN_TEST)
            .setShardsTotal(-1).setCorePoolSize(10)
}
