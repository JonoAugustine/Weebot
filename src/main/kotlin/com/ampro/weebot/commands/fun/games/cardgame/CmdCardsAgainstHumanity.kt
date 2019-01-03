/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`.games.cardgame

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.`fun`.games.*
import com.ampro.weebot.database.*
import com.ampro.weebot.MLOG
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

const val CAH_MIN_PLAYERS   = 3
/**
 * The Maximum number of players
 * (this is based on the number of usable emotes for [BlackCard] reactions
 */
const val CAH_MAX_PLAYERS   = 51
const val HAND_SIZE_MAX     = 11
const val HAND_SIZE_DEF     = 5
const val HAND_SIZE_MIN     = 4

val CAH_IDGEN   = IdGenerator(7)

val DIR_CAH             = File(DIR_RES, "CAH")
val FILE_C_BLACK        = File(DIR_CAH, "CAH_C_BLACK")
val FILE_C_WHITE        = File(DIR_CAH, "CAH_C_WHITE")
val FILE_D_STRD_JSON    = File(DIR_CAH, "CAH_D_STRD.json")
val FILE_D_CUST_JSON    = File(DIR_CAH, "CAH_D_CUSTOM.json")

val C_BLACK_BASE: MutableList<BlackCard> = mutableListOf()
val C_WHITE_BASE: MutableList<WhiteCard> = mutableListOf()
val DECK_STRD = loadJson<CAHDeck>(FILE_D_STRD_JSON) ?: run {
    //Load white cards
    try {
        Files.readAllLines(FILE_C_WHITE.toPath())
            .forEach { C_WHITE_BASE.add(WhiteCard(it)) }
    } catch (e: IOException) {
        MLOG.elog("Loading CAH WhiteCards - FAILED\n${e.message}")
        System.exit(-1)
    }

    //Load black cards
    try {
        Files.readAllLines(FILE_C_BLACK.toPath())
            .forEach {
                val pick = it.count { char -> char == '_' }
                C_BLACK_BASE.add(BlackCard(
                    it.replace("_", "____").removeAll("[.\\d]*(<.\\d>)[.\\d]*"), //todo clear html
                    if (pick == 0) 1 else pick
                ))
            }
    } catch (e: IOException) {
        MLOG.elog("Loading CAH BlackCards - FAILED\n${e.message}")
        System.exit(-1)
    }

    MLOG.slog("[CAH] Init Load of base cards complete!")
    val c = CAHDeck("Official", blackCards = C_BLACK_BASE, whiteCards = C_WHITE_BASE)
    c.saveJson(FILE_D_STRD_JSON.apply { createNewFile() })
    c
}
val DECK_CUST = loadJson<ConcurrentHashMap<Long, MutableList<CAHDeck>>>(FILE_D_CUST_JSON)
        ?: run { MLOG.elog("Failed to load Custom CAH Decks"); System.exit(-1) }

/**
 * A class used to hold information about a guild's CAH info like
 * leaderboards, blocked cards, and custom decks.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
data class CahGuildInfo(val guildId: Long) {
    val leaderBoard: LeaderBoard = LeaderBoard()
}

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class WhiteCard(val text: String) : Card() {
    val id: String = CAH_IDGEN.next()
}

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class BlackCard(val text: String, val pick: Int)
    : Card() {
    val id: String = CAH_IDGEN.next()
    companion object {
        val MIN_BLANKS = HAND_SIZE_MIN
        val MAX_BLANKS = HAND_SIZE_MAX
    }
}

fun <T: Any> Collection<CAHDeck>.collect(params: (CAHDeck) -> List<T>)
        : Collection<T> {
    val list = mutableListOf<T>()
    forEach { list.addAll(params(it)) }
    return list
}

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class CAHDeck(var name: String, val authorID: Long = -1L,
                   val restriction: Restriction = Restriction(),
                   val blackCards: MutableList<BlackCard> = mutableListOf(),
                   val whiteCards: MutableList<WhiteCard> = mutableListOf()) {
    val id: String = CAH_IDGEN.next()
}

/**
 * The Cards Against Humanity Player. Has a hand of [WhiteCard]s and won [BlackCard]s
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CAHPlayer(user: User) : Player(user) {

    /**
     * TODO update at each round
     *
     * @author
     * @since 2.0
     */
    inner class CardHand : IPassive {
        var dead: Boolean = false
        override fun dead() = dead

        override fun accept(bot: Weebot, event: Event) {
            TODO()
        }

        fun update() {

        }
    }

    /**
     * Currently held cards This is visible to the [Player] in their
     * privateChannel
     * Do not expose this to the guild channel, since that
     * would show the hand to all members and that's dumb.
     */
    val hand: MutableList<WhiteCard> = mutableListOf()

    var playedCards: MutableList<WhiteCard> = mutableListOf()

    /** [BlackCard]s won  */
    val cardsWon: MutableList<BlackCard> = mutableListOf()

    companion object {
        /** Compares by cards won */
        val scoreComparator = Comparator<CAHPlayer> { a, b ->
            a.cardsWon.size - b.cardsWon.size
        }
    }

}

enum class GameState {
    /** Players are choosing white cards  */
    CHOOSING,
    /** Czar is reading played white cards  */
    READING
}

/**
 * A game of Cards Against Humanity.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CardsAgainstHumanity(guild: Guild, author: User,
                           /** The hosting channel */
                           val channel: TextChannel,
                           decks: List<CAHDeck> = listOf(DECK_STRD),
                           val handSize: Int = HAND_SIZE_DEF)
    : CardGame<CAHPlayer>(guild.idLong, author.idLong) {

    /**
     * @since 2.0
     */
    inner class BlackCardPassive : IPassive {
        var dead = false
        override fun dead() = dead

        var message: Message? = null

        val bcEmbed = strdEmbedBuilder.setAuthor("Cards Against ${getGuild(guildID)?.name}").setTitle("")!!

        /** Sends the Black Card Message or Updates the last */
        fun sendOrUpdate() {
            message?.editMessage("")?.queue {

            } ?: channel.sendMessage("").queue {

            }
        }
        /**
         * Watch for reactions & stuff
         */
        override fun accept(bot: Weebot, event: Event) {
            //TODO add a reaction that reports the card for wrong pick count
            //and then edits the message with new card
        }

    }

    val activeCardsBlack = mutableListOf<BlackCard>()
    val activeCardsWhite = mutableListOf<WhiteCard>()
    val usedCardsWhite = mutableListOf<WhiteCard>()
    val usedCardsBlack = mutableListOf<BlackCard>()

    val blackCard: BlackCardPassive = BlackCardPassive()

    var round: Int = 1

    /** The current Czar  */
    //val czar: CAHPlayer
    //val czarIterator: Iterator<Long>


    init {
        activeCardsWhite.addAll(decks.collect{ it.whiteCards })
        activeCardsBlack.addAll(decks.collect { it.blackCards })
    }


    /**
     * Deal cards to a player
     * @param player The player to deal cards to.
     * @return False if the player already has a full hand.
     */
    override fun dealCards(player: CAHPlayer): Boolean {
        return true
    }

    /**
     * Start the game of CAH.
     *
     * @return {@code false} if there are less than 3 players.
     */
    override fun startGame(): Boolean {
        return true
    }

    override fun endGame(): Boolean {
        TODO("not implemented")
    }

    /**
     * Add a user to the game, wrapping the [User] in a new [Player]
     * implementation.
     * @param user The user to add.
     * @return false if the user could not be added.
     */
    override fun addUser(user: User): Boolean {
        return true
    }

}

/**
 * Run Cards Against Humanity games, and make custom cards. <br> Users can:<br> start a
 * game of CAH <br> make custom Card Decks <br> make custom White Cards <br> make custom
 * Black Cards <br> play against the bot
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdCardsAgainstHumanity : WeebotCommand("cardsagainsthumanity", arrayOf("cah"),
    CAT_UNDER_CONSTRUCTION, "<command> [arguments]",
    "Start a game of CardsAgainstHumanity or make custom cards.", guildOnly = true
) {

    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Cards Against Weebot",
            "*You can purchase Cards Against Humanity for yourself to support the creators* "
                    + "[`here`](https://www.cardsagainsthumanity.com/)\n\n" +
                    "Play a game of Cards Against Humanity and make custom decks " +
                    "with user-made cards. Weebot uses all official decks (as of december " +
                    "2018).\n\nTo play a game, you first need to setup and have at least" +
                    "3 players join, then you can start. Cards will be given to each player" +
                    "in a private message, then a Card Czar will be chosen at random." +
                    "A Black Card will be sent to the channel the game was started in," +
                    "once each player has played their card(s) (by reacting to their " +
                    "private chat hand or sending a ``play`` command) the Czar can choose" +
                    "a winner by reacting to the Black Card (up to $CAH_MAX_PLAYERS) or " +
                    "using the ``pick`` command. Each round new cards will be delt " +
                    "to each player, a new Czar will be chosen (in order), and a new" +
                    "Black Card will be sent until a winner is reached or the game is" +
                    "cancelled.\n\nHAVE FUN!")
            .addField("Game Setup", "setup [+bot] [hand_size] [deck1 deck2...] "
                    + "\n**Join Game**\njoin\n**+bot** adds Weebot to the game")
            .addField("Start Game (Requires $CAH_MIN_PLAYERS players)", "start")
            .addField("Add Weebot to the Game", "+bot\n*Aliases*: addbot, invitebot")
            .addField("Play Your Card(s)", "White Cards can be played by reacting to "
                    + "the private message or sending a command to the server channel\n"
                    + "play <cardNum1> /cardNum2/...\n*Alias*: use")
            .addField("Card Czar Pick Winning Card(s)", "React to the Black Card message"
                    + "or use ``pick <card_set_num>``")
            .addField("Get your cards re-sent", "myhand")
            .addField("See the current game's scores","scores")
            .addField("See server leaderboard", "leaderboard\n*Aliases:* LB, top, ranks")
            .addBlankField()
            .addField("Make a Custom Deck", "cah makedeck <deck_name>", false)
            .addField("Make a Custom White Card",
                "cah mkwc <deck_name> <card text>\n*Aliases*: makewhitecard, makewc",
                false)
            .addField("Make a Custom Black Card",
                "cah mkbc <deck_name> <numberOfBlanks> <card text>\n*Aliases*: "
                        + "makeblackcard, makebc", false)
            .addField("View all Custom Decks", "cah alldecks", false)
            .addField("View a Custom Deck's Cards",
                "cah view <deck_name>\n*Alias*: seedeck", false)
            .addField("Lock a Custom Deck to One or More Roles", "cah lock <deck_name>",
                false)
            .addField("Get a Custom Deck as a Text File", "cah deckfile <deck_name>",
                false)
            .addField("Remove Custom Deck **", "cah remove <deck_number>", false)
            .addField("Remove Custom White Card ","rmwc <deck_name> <card_number>",
                false)
            .addField("Remove Custom Black Card ","rmbc <deck_name> <card_number>",
                false)
            .addField("Under Construction ",
                "Commands marked '**' are still under construction", false)
            .setThumbnail("https://cardsagainsthumanity.com/v8/images/social-3f4a4c57.png")
            .build()
    }

}
