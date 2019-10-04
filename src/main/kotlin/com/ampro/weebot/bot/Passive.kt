/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.bot
import com.ampro.weebot.globalWeebot
import com.ampro.weebot.modify
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.Guild
import com.serebit.strife.events.Event
import com.serebit.strife.events.GuildEvent
import com.serebit.strife.onAnyEvent

interface Passive {
    var active: Boolean
    suspend fun predicate(event: Event, bot: Weebot): Boolean = true
    suspend fun consume(event: Event, bot: Weebot)
}

/** Map of GuildID to passives. Global passives are at `0` */
val passives = mutableMapOf<Long, MutableList<Passive>>()

fun <P : Passive> addPassive(guildID: Long, passive: P) {
    passives.getOrPut(guildID, { mutableListOf() }).add(passive)
}

inline fun <reified P : Passive> getPassives(guildID: Long): List<P>? {
    return passives[guildID]?.filterIsInstance<P>()
}

fun <P : Passive> Guild.add(passive: P) {
    addPassive(id, passive)
}

inline fun <reified P : Passive> Guild.getAll() = getPassives<P>(id)

fun BotBuilder.passives() {
    onAnyEvent {
        passives[0]
            ?.filter { it.active }
            ?.filter { it.predicate(this, globalWeebot) }
            ?.forEach { it.consume(this, globalWeebot) }
        if (this is GuildEvent) {
            passives.forEach { (id, list) ->
                list
                    .filter { it.active }
                    .filter { it.predicate(this, bot(id)) }
                    .forEach {
                        bot(id).modify { it.consume(this@onAnyEvent, this) }
                    }
            }
        }
    }
}
