package com.ampro.weebot.contants

import com.ampro.weebot.MLOG
import com.ampro.weebot.jda.JDABuilder
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game
import javax.security.auth.login.LoginException

private const val TOKEN_WBT =
        "NDM3ODUxODk2MjYzMjEzMDU2.DcN_lA.Etf9Q9wuk1YCUnUox0IbIon1dUk"
private const val TOKEN_TEST =
        "NDQ0MzIzNzMyMDEwMzAzNDg4.DdaQyQ.ztloAQmeuUffaC-DC9zE-LFwPq4"

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
    MLOG.elog("Logging in to Weebot JDA client...")
    return JDABuilder(AccountType.BOT).setToken(TOKEN_WBT)
        .setGame(Game.playing("@Weebot help")).setCorePoolSize(10)
        .buildBlocking(JDA.Status.CONNECTED)!!
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
    MLOG.elog("Logging in to TestBot JDA client...")
    return JDABuilder(AccountType.BOT).setToken(TOKEN_TEST)
        .setGame(Game.playing("Genocide")).setCorePoolSize(10)
        .buildBlocking(JDA.Status.CONNECTED)!!
}
