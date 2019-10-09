/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.bot
import com.ampro.weebot.globalWeebot
import com.ampro.weebot.modify
import com.serebit.strife.BotBuilder
import com.serebit.strife.entities.Guild
import com.serebit.strife.entities.User
import com.serebit.strife.events.Event
import com.serebit.strife.events.GuildEvent
import com.serebit.strife.onAnyEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface Passive {
    var active: Boolean
    suspend fun predicate(event: Event, bot: Weebot): Boolean = true
    suspend fun consume(event: Event, bot: Weebot)
}

/** Map of GuildID or User to passives. */
val passives = mutableMapOf<Long, MutableList<Passive>>()

fun <P : Passive> addPassive(id: Long, passive: P) {
    passives.getOrPut(id, { mutableListOf() }).add(passive)
    bot(id).passives.add(passive)
}

inline fun <reified P : Passive> getPassives(id: Long): List<P>? {
    return passives[id]?.filterIsInstance<P>()
}

fun <P : Passive> Guild.add(passive: P) {
    addPassive(id, passive)
}

fun <P : Passive> User.add(passive: P) {
    addPassive(id, passive)
}

inline fun <reified P : Passive> Guild.getAll() = getPassives<P>(id)
inline fun <reified P : Passive> User.getAll() = getPassives<P>(id)

fun BotBuilder.passives() {
    onAnyEvent {
        GlobalScope.launch {
            passives[0]?.retainAll { it.active }
            passives[0]
                ?.filter { it.active }
                ?.filter { it.predicate(this@onAnyEvent, globalWeebot) }
                ?.forEach { it.consume(this@onAnyEvent, globalWeebot) }
        }
        if (this is GuildEvent) {
            passives.forEach { (id, list) ->
                GlobalScope.launch {
                    list.retainAll { it.active }
                    list
                        .filter { it.active }
                        .filter {
                            it.predicate(
                                this@onAnyEvent, bot(this@onAnyEvent.guild.id)
                            )
                        }
                        .forEach {
                            bot(id).modify { it.consume(this@onAnyEvent, this) }
                        }
                }
            }
        }
    }
}
