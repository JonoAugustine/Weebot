/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot


import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.stats.BotStatistic
import com.ampro.weebot.stats.SiteStatistic
import com.serebit.strife.entities.Guild
import io.kweb.shoebox.Shoebox
import java.io.File
import java.nio.file.Files

/** The root wbot file. */
val DIR = File("wbot")
    get() = when {
        field.exists() && field.isDirectory -> field
        else -> {
            Files.createDirectory(field.toPath())
            field
        }
    }

private val BOT = DIR.resolve("dao\\")
private val STAT = DIR.resolve("stat\\")
private val STAT_BOT = STAT.resolve("bot\\")
private val STAT_SITE = STAT.resolve("site\\")

private val botStore by lazy {
    Shoebox<Weebot>(BOT.toPath()).apply {
        onNew { (key), source ->
            logger.trace("Weebot added at $key. ($source)")
        }
        onChange { _, (key, _), source ->
            logger.trace("Weebot updated at $key. ($source)")
        }
        onRemove { (key), source ->
            logger.trace("Weebot removed from $key. ($source)")
        }
    }
}
private val statStore_bot by lazy {
    Shoebox<BotStatistic>(STAT_BOT.toPath())
}
private val statStore_site by lazy {
    Shoebox<SiteStatistic>(STAT_SITE.toPath())
}


/** All current [Weebot] instances. */
val bots get() = botStore.entries.map { it.value }

val globalWeebot by lazy {
    botStore["-1"] ?: Weebot(-1, "").also { botStore["-1"] = it }
}

val Guild.bot get() = bot(id)

/** Retrieve a [Weebot] for the [guildID]. */
fun bot(guildID: Long): Weebot = botStore[guildID.toString()]
    ?: Weebot(guildID).also { botStore[guildID.toString()] = it }

suspend fun Weebot.modify(modify: suspend Weebot.() -> Unit) =
    apply { modify(this) }
        .let { nb -> botStore.modify(guildID.toString()) { nb } }

fun remove(botID: Long) = botStore.remove(botID.toString())
