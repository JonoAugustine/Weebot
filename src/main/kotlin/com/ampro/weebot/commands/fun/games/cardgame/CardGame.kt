/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`.games.cardgame

import com.ampro.weebot.commands.`fun`.games.Game
import com.ampro.weebot.commands.`fun`.games.Player
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User

/**
 * A card used in a `CardGame`.
 * Currenty holds no information in abstract,
 * Just here for future expansion and Generics
 */
abstract class Card {
    /** Just a little backup case in case something goes REALLY wrong.  */
    protected class InvalidCardException : Exception {
        /** Constructor with message  */
        constructor(err: String) : super(err)

        companion object { private val serialVersionUID = 7546072265632776147L }
    }
}

/**
 * A [Game] that uses [Card]s.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
abstract class CardGame<P: Player>(guild: Guild, author: User) : Game<P>(guild, author) {

    /**
     * Deal cards to a player
     * @param player The player to deal cards to.
     * @return False if the player already has a full hand.
     */
    protected abstract fun dealCards(player: P): Boolean

}
