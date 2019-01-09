/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`.games

import com.ampro.weebot.database.constants.DEV_IDS
import net.dv8tion.jda.core.entities.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * File containing abstract classes and global variables used for Weebot Games.
 */

/** Don't let some things be modified while the game is running  */
class ModificationWhileRunningException : Exception {
    /** Constructor with message  */
    constructor(err: String) : super(err)

    companion object { private val serialVersionUID = 1549072265432776147L }
}

/**
 * A class representing a leaderboard
 *
 * @param scoredUsers A map of user IDs -> scores
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
open class LeaderBoard(protected val scoredUsers: MutableList<Pair<Long, Int>>
                       = mutableListOf()) {
    fun addUser(user: User, score: Int = 0) {
        scoredUsers.add(Pair(user.idLong, score))
    }
    fun get() = scoredUsers.sortedBy { it.second }
    operator fun get(int: Int) = scoredUsers[int]
    operator fun get(user: User) = scoredUsers.firstOrNull { user.idLong == it.first }
}

/**
 * Base Wrapper Class for Members currently involved in a Webot `Game`
 *
 * @param user The [User] wrapped by this player object
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
abstract class Player(/** User this Player is wrapped around*/ val user: User)
    : Comparable<Player> {

    /**
     * Send a private message to the `Player`.
     * @param message String to send
     */
    private fun privateMessage(message: String) {
        this.user.openPrivateChannel().queue { it.sendMessage(message).queue() }
    }

    /**
     * Send a private message to the [Player].
     *
     * @param message The message.
     * @param consumer Lambda
     */
    fun privateMessage(message: String, consumer: Consumer<Message>) {
        this.user.openPrivateChannel().queue {
            it.sendMessage(message).queue { m -> consumer.accept(m) }
        }
    }

    /**
     * Send a private embed message to the player.
     * @param embed The MessageEmbed to send.
     * @param consumer Message Consumer
     */
    fun privateMessage(embed: MessageEmbed, consumer: Consumer<Message>) {
        this.user.openPrivateChannel().queue {
            it.sendMessage(embed).queue { m -> consumer.accept(m) }
        }
    }

    override fun compareTo(other: Player): Int {
        return (this.user.idLong - other.user.idLong).toInt()
    }

}

/**
 * Under what conditions should the game end?
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
enum class WinCondition(var score: Int) {
    /** End Game by Max Rounds */ ROUNDS(-1),
    /** End Game by Max Wins */ WINS(-1);
    operator fun invoke(score: Int) : WinCondition {
        this.score = score
        return this
    }
}

/**
 * Basis of a Weebot Game.
 * Must be connected to a single type of Player.
 *
 * @param P A class that extends [Player]
 */
abstract class Game<P : Player> (guild: Guild, author: User) {
    /** The ID of the hosting bot  */
    val guildID: Long = guild.idLong
    /** User ID of the User who started the game. */
    val authorID: Long = author.idLong
    /** List of all the Players */
    val players: ConcurrentHashMap<Long, P> = ConcurrentHashMap()
    /** Is the game currently running? Starts false */
    var isRunning: Boolean = false

    protected val playerList = mutableListOf<P>()

    init {
        this.addUser(author)
    }


    /**
     * Create a game with an initial set of `Player`s
     *
     * @param bot Weebot hosting game
     * @param players `Players` to add to the game
     */
    protected constructor(guild: Guild, author: User, vararg players: P)
            : this (guild, author) {
        players.forEach { addPlayer(it) }
        this.addUser(author)
    }

    //Some Very important but vague methods to implement in child.
    abstract fun startGame(): Boolean

    abstract fun endGame(): Boolean

    /**
     * Add a user to the game, wrapping the [User] in a new [Player] implementation.
     *
     * @param user The user to add.
     * @return false if the user could not be added.
     */
    abstract fun addUser(user: User) : Boolean

    /**
     * Adds a player to the [players] map and the [playerList]
     *
     * @return false if the player is already in the game
     */
    protected open fun addPlayer(player: P) = if (this.players[player.user.idLong] == null) {
        this.players[player.user.idLong] = player
        this.playerList.add(player)
        true
    } else false

    /**
     * Get a player.
     * @param user The user who's player to get.
     * @return The player or null if no player is found.
     */
    operator fun get(user: User) : P? = this.players[user.idLong]

    /** @return true if the game has a dev as a player */
    fun hasDev() = players.count { DEV_IDS.contains(it.key) } != 0

}
