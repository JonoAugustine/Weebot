/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot


import com.ampro.weebot.bot.Weebot
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

private val botStore by lazy { Shoebox<Weebot>(DIR.resolve("dao\\").toPath()) }

/** All current [Weebot] instances. */
val bots = botStore.entries.map { it.value }

val globalWeebot by lazy {
    botStore["-1"] ?: Weebot(-1, "").also { botStore["-1"] = it }
}

val Guild.bot get() = bot(id)

/** Retrieve a [Weebot] for the [guildID]. */
fun bot(guildID: Long): Weebot = botStore[guildID.toString()] ?: Weebot(
    guildID).also { botStore[guildID.toString()] = it }

fun Weebot.modify(modify: Weebot.() -> Unit) =
    botStore.modify(guildID.toString()) { this.apply(modify) }
