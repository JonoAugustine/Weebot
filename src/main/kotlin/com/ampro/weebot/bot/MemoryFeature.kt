/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot

import com.ampro.weebot.*
import com.ampro.weebot.Cache.bots
import com.ampro.weebot.bot.strifeExtensions.getFeature
import com.serebit.strife.BotBuilder
import com.serebit.strife.BotFeature
import com.serebit.strife.entities.Guild
import com.serebit.strife.entities.User
import com.serebit.strife.events.GuildCreateEvent
import com.serebit.strife.events.GuildDeleteEvent
import com.serebit.strife.onEvent
import kotlinx.serialization.Serializable

/** A [Memory] is used by the [MemoryFeature] to store any custom information associated with a [Guild] or [User]. */
interface Memory

/**
 * A [MemoryType] is useful for debugging and stats collection.
 * @property name The name of the [MemoryType]
 */
@Serializable
sealed class MemoryType(val name: String) {
    /** A [User] MemoryType. */
    @Serializable
    object User : MemoryType("user")

    /** A [Message] MemoryType. */
    @Serializable
    object Message : MemoryType("note")

    /** A [Guild] MemoryType. */
    @Serializable
    object Guild : MemoryType("guild")

    /** A [GuildTextChannel] MemoryType. */
    @Serializable
    object GuildTextChannel : MemoryType("text channel (guild)")

    /** A [GuildVoiceChannel] MemoryType. */
    @Serializable
    object GuildVoiceChannel : MemoryType("voice channel (guild)")

    /** A [DmChannel] MemoryType. */
    @Serializable
    object DmChannel : MemoryType("text channel (dm)")

    /** A MemoryType for any custom types. */
    @Serializable
    open class Other(name: String) : MemoryType(name)

    /** Returns the [name]. */
    override fun toString() = name
}

/** The [MemoryFeature] applies the logic of creating, storing, and removing [memories][Memory]. */
object MemoryFeature : BotFeature {

    const val FEATURE_NAME: String = "memory"

    override val name = FEATURE_NAME

    /** Use this lambda to determine how memories are created. */
    val memoryFunction: BotBuilder.() -> Unit = {
        onEvent<GuildCreateEvent> { bot(guild.id) }
        onEvent<GuildDeleteEvent> { bot(guildID).delete() }
    }

    /** Get the [memory][M] at the given [key]. */
    fun getMemory(key: Long): Weebot? = bot(key)

    override fun installTo(scope: BotBuilder) = memoryFunction(scope)

}

/**
 * Retrieve the [Memory] of type [M] associated with the given [id] and run the lambda [scope] with the memory.
 */
suspend fun BotBuilder.memory(id: Long?, scope: suspend Weebot.() -> Unit) =
    id?.let {
        getFeature<MemoryFeature>()?.getMemory(it)?.apply { modify(scope) }
    }

/**
 * Manually remove a [Memory] of type [M] from the given [key].
 * Returns the forgotten memory or `null` if not found.
 */
fun BotBuilder.forget(key: Long): Weebot? = getFeature<MemoryFeature>()
    ?.run { forget(key) } ?: error("BotMemory feature not installed.")
