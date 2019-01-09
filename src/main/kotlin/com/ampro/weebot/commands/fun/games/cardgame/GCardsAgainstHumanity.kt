/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`.games.cardgame

import com.ampro.weebot.MLOG
import com.ampro.weebot.commands.CAT_UNDER_CONSTRUCTION
import com.ampro.weebot.commands.CMD_CAH
import com.ampro.weebot.commands.`fun`.games.*
import com.ampro.weebot.commands.`fun`.games.WinCondition.ROUNDS
import com.ampro.weebot.commands.`fun`.games.WinCondition.WINS
import com.ampro.weebot.commands.`fun`.games.cardgame.GameState.CHOOSING
import com.ampro.weebot.database.getGuild
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.*

internal const val LINK_CAH = "https://cardsagainsthumanity.com/"
internal const val LINK_CAH_THUMBNAIL
        = "https://cardsagainsthumanity.com/v8/images/social-3f4a4c57.png"

internal const val MIN_PLAYERS   = 3
/**
 * The Maximum number of players
 * (this is based on the number of usable emotes for [BlackCard] reactions
 */
internal const val MAX_PLAYERS   = 25 //TODO this is a really artificial limit
internal const val HAND_SIZE_MAX     = 10
/** Default Hand Size */
internal const val HAND_SIZE_DEF     = 5
internal const val HAND_SIZE_MIN     = 4

internal val CAH_IDGEN   = IdGenerator(7)

val DIR_CAH             = File(DIR_RES, "CAH")
val FILE_C_BLACK        = File(DIR_CAH, "CAH_C_BLACK")
val FILE_C_WHITE        = File(DIR_CAH, "CAH_C_WHITE")
val FILE_D_STRD_JSON    = File(DIR_CAH, "CAH_D_STRD.json")
val FILE_D_CUST_JSON    = File(DIR_CAH, "CAH_D_CUSTOM.json")

internal val C_BLACK_BASE: MutableList<BlackCard> = mutableListOf()
internal val C_WHITE_BASE: MutableList<WhiteCard> = mutableListOf()
internal val DECK_STRD = loadJson<CAHDeck>(FILE_D_STRD_JSON) ?: run {
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
                    it.replace(Regex("_+"), "[___]").replace("<i>", "*")
                        .replace("</i>","* ").replace(Regex("</?br>"), "\n"),
                    if (pick == 0) 1 else pick))
            }
    } catch (e: IOException) {
        MLOG.elog("Loading CAH BlackCards - FAILED\n${e.message}")
        System.exit(-1)
    }

    MLOG.slog("[CAH] Init Load of base cards complete!")
    val c = CAHDeck("Official", blackCards = C_BLACK_BASE, whiteCards = C_WHITE_BASE, public = true)
    c.saveJson(FILE_D_STRD_JSON.apply { createNewFile() })
    return@run c
}
/**[Guild.getIdLong] -> [MutableList]<[CAHDeck]>*/
internal val DECK_CUST: ConcurrentHashMap<Long, MutableList<CAHDeck>> = loadJson<ConcurrentHashMap<Long,
        MutableList<CAHDeck>>>(FILE_D_CUST_JSON) ?: run {
    MLOG.elog("Failed to load Custom CAH Decks"); System.exit(-1);
    return@run ConcurrentHashMap<Long, MutableList<CAHDeck>>()
}

/**
 * A class used to hold information about a guild's CAH info like
 * leaderboards, blocked cards, and custom decks.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
data class CahGuildInfo(val guildId: Long) {
    val leaderBoard: LeaderBoard = LeaderBoard()
    val decks: MutableList<CAHDeck> get() = DECK_CUST[guildId] ?: mutableListOf()
}

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class WhiteCard(val text: String, val id: String = CAH_IDGEN.next()) : Card() {
    /** How many times this card has been reported by end users */
    var reports: Int = 0
}

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class BlackCard(val text: String, val pick: Int, val id: String = CAH_IDGEN.next())
    : Card() {
    /** How many times this card has been reported by end users */
    var reports: Int = 0
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
                   val whiteCards: MutableList<WhiteCard> = mutableListOf(),
                   val id: String = CAH_IDGEN.next(), var public: Boolean = false)

/**
 * The Cards Against Humanity Player. Has a hand of [WhiteCard]s and won [BlackCard]s
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CAHPlayer(user: User) : Comparable<Player>, Player(user) {

    /**
     * Currently held cards This is visible to the [Player] in their [PrivateChannel].
     * Do not expose this to the guild channel, since that would show the hand to all
     * members and that's dumb.
     */
    val hand = mutableListOf<WhiteCard>()
    /** Cards set to be played for thhe current [BlackCard] */
    var playedCards = mutableListOf<WhiteCard>()
    /** [BlackCard]s won  */
    val cardsWon = mutableListOf<BlackCard>()
    /** The private [Message] sent to the [Player] */
    var handMessage: Message? = null

    /** Compare by [cardsWon] */
    override fun compareTo(other: Player)
            = this.cardsWon.size - (other as CAHPlayer).cardsWon.size

}

/** At what point in the game cycle is the game in */
enum class GameState {
    /** Players are choosing white cards */ CHOOSING,
    /** Czar is reading played white cards */ READING
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
                           val handSize: Int = HAND_SIZE_DEF,
                           val winCondition: WinCondition = WINS(7))
    : CardGame<CAHPlayer>(guild, author) {

    val guildName = guild.name
    val name: (User) -> String = {
        getGuild(guildID)?.getMember(it)?.effectiveName?: it.name
    }

    val activeCardsBlack = mutableListOf<BlackCard>()
    val activeCardsWhite = mutableListOf<WhiteCard>()

    val usedCardsWhite = mutableListOf<WhiteCard>()
    val usedCardsBlack = mutableListOf<BlackCard>()

    /**The current [BlackCard] being played on */
    lateinit var blackCard: BlackCard

    var blackCardMessage: Message? = null
    val playerHandMessages: ConcurrentHashMap<CAHPlayer, Message> = ConcurrentHashMap()

    /** The current Czar  */
    lateinit var czar: CAHPlayer

    lateinit var gameState: GameState

    /** A round is finished when each player has been Czar once */
    var round: Int = 1

    init {
        //Collect all the cards for this game's deck
        activeCardsWhite.addAll(decks.collect{ it.whiteCards })
        activeCardsBlack.addAll(decks.collect { it.blackCards })
    }

    companion object {
        val howToPlayField = Field("How to Play:",
            "Select your cards from the private chat I sent you by reacting with the " +
                    "cards you want to play. Use ``cah resend`` or ``cah myhand`` " +
                    "to have the message resent.)", true)
    }

    /**
     * Send the black card to [channel]
     */
    fun sendBlackCard() {
        val title = (if ("Cards Against $guildName".length > EMBED_MAX_TITLE)
            CMD_CAH.displayName!! else "Cards Against $guildName") + " | round $round"

        val baseEmbed: () -> EmbedBuilder = {makeEmbedBuilder(title, LINK_CAH)
            .setAuthor("Czar: ${name(czar.user)}", czar.user.avatarUrl)
            .addField("Black Card (Pick ${blackCard.pick})", blackCard.text, false)
            .addField(howToPlayField).setThumbnail(LINK_CAH_THUMBNAIL)}

        if (gameState == CHOOSING) { //If this is the CHOOSING embed
            /** Loads new [blackCard] and removes it from [activeCardsBlack] */
            fun reloadBlackCard() {
                this.activeCardsBlack.remove(this.blackCard)
                this.blackCard = this.activeCardsBlack.random()
                blackCardMessage?.delete()?.queueAfter(250, MILLISECONDS)
                sendBlackCard()
            }
            val e = baseEmbed().addField("Guide", """
                $ArrowsCounterclockwise to load a new Black Card
                $Warning to report this Black Card and load a new one
            """.trimIndent(), true).build()
            SelectableEmbed(czar.user, false, e, listOf(
                ArrowsCounterclockwise to {  m, _ ->
                    this.usedCardsBlack.add(this.blackCard)
                    reloadBlackCard()
                    m.delete().queueAfter(250, MILLISECONDS)
                }, Warning to { m, _ ->
                    try {
                        reloadBlackCard()
                        channel.sendMessage(
                            "Card Reported, thank you for helping our moderation efforts!")
                            .queue { it.delete().queueAfter(5, SECONDS) }
                        m.delete().queueAfter(250, MILLISECONDS)
                    } catch (e: NoSuchElementException) {
                        channel.sendMessage(
                            "You have removed all the Black Cards from the deck..."
                        ).queue { endGame() }
                    }
                }), 2) { blackCardMessage = it }.display(channel)
        } else { //If this is the Choosing Embed
            val embed = baseEmbed().addField("Czar, choose your victor!",
                "Select a victor by reacting with the corresponding number.", true)
                .build()
            val playersIn = playerList.filter { it.playedCards.isNotEmpty() }.map { p ->
                //TODO Check formatting
                p.playedCards.joinToString("\n") { it.text } to { _: Int, _: Message ->
                    p.cardsWon.add(this.blackCard)
                    this.channel.sendMessage(makeEmbedBuilder(title, LINK_CAH,
                        "LeaderBoard:\n${leaderBoard()}")
                        .setAuthor("${name(p.user)} Won This Round! $Tada$Tada").build()
                    ).queue()
                    nextTurn()
                }
            }.shuffled()
            if (playersIn.isEmpty()) TODO("This should not happen")
            SelectablePaginator(setOf(czar.user), emptySet(), -1, MINUTES, embed,
                playersIn, singleUse = true) {blackCardMessage = it}.display(channel)
        }

    }

    /**
     * If [player] is null: Sends each [CAHPlayer.hand]
     * if [player] is not null: Sends the selected player their [CAHPlayer.hand]
     */
    fun sendWhiteCards(player: CAHPlayer? = null) {
        TODO()
    }

    /**
     * Clears Played cards then deal new cards to [player]
     *
     * @param player The player to deal cards to.
     * @return False if the player already has a full hand.
     */
    override fun dealCards(player: CAHPlayer) : Boolean {
        if (player.hand.size == this.handSize) return false
        this.activeCardsWhite.removeAll(player.playedCards)
        this.usedCardsWhite.addAll(player.playedCards)
        player.hand.removeAll(player.playedCards)
        player.playedCards.clear()
        while (player.hand.size < this.handSize) {
            val randCard = this.activeCardsWhite.random()
            this.activeCardsWhite.remove(randCard)
            this.usedCardsWhite.add(randCard)
            player.hand.add(randCard)
        }
        return true
    }

    /**
     * Clear player [CAHPlayer.playedCards] and [dealCards].
     * Removes the [blackCard] from [activeCardsBlack] and adds it to [usedCardsBlack]
     * [sendWhiteCards] [sendBlackCard]
     */
    private fun nextTurn() {
        if (nextCzar()) round++
        when (winCondition) {
            ROUNDS -> if (this.round >= winCondition.score) endGame()
            WINS -> {
                val winners = players.filterValues { it.cardsWon.size >= winCondition.score }
                if (winners.isNotEmpty()) endGame()
            }
        }

        this.activeCardsBlack.remove(this.blackCard)
        this.usedCardsBlack.add(this.blackCard)
        if (activeCardsBlack.isEmpty()) {
            activeCardsBlack.addAll(usedCardsBlack)
            usedCardsBlack.clear()
        }
        this.blackCard = activeCardsBlack.random()

        if (activeCardsWhite.isEmpty()) {
            activeCardsWhite.addAll(usedCardsWhite)
            usedCardsWhite.clear()
        }
        this.playerList.forEach { dealCards(it) }
        sendBlackCard()
        sendWhiteCards()
    }

    /** @return true if the Czar was reset (start a new round) */
    private fun nextCzar() : Boolean {
        val i = this.playerList.indexOf(czar)
        return if (i + 1 in 0 until this.playerList.size) {
            czar = this.playerList[i + 1]
            false
        } else {
            czar = this.playerList.first()
            true
        }
    }

    /**
     * Add a user to the game, wrapping the [User] in a new [Player]
     * implementation.
     * @param user The user to add.
     * @return false if the user is already in the game
     */
    override fun addUser(user: User): Boolean {
        val newPlayer = CAHPlayer(user)
        if (!super.addPlayer(newPlayer)) return false
        if (this.isRunning && this.gameState == CHOOSING) {
            dealCards(newPlayer)
            sendWhiteCards(newPlayer)
        }
        return true
    }

    /**
     * Start the game of CAH.
     *
     * @return false if there are less than 3 players.
     */
    override fun startGame(): Boolean {
        return if (this.players.size in MIN_PLAYERS..MAX_PLAYERS) {
            //Deal Cards to players
            this.playerList.forEach { dealCards(it) }
            //Set the First Czar
            czar = this.playerList.random()
            //Set the first black card
            blackCard = activeCardsBlack.random()
            gameState = CHOOSING
            isRunning = true
            sendBlackCard()
            sendWhiteCards()
            true
        } else false
    }

    override fun endGame(): Boolean {
        TODO("not implemented")
    }

    /** @return The [playerList] sorted by score each player on a new line */
    fun leaderBoard() : String {
        return playerList.sortedBy { it.cardsWon.size }.joinToString {
            "*${name(it.user)}:* ${it.cardsWon.size}\n"
        }
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
class CmdCardsAgainstHumanity : WeebotCommand("cah", "Cards Against Humanity",
    arrayOf("cardsagainsthumanity"), CAT_UNDER_CONSTRUCTION, "<command> [arguments]",
    "Start a game of CardsAgainstHumanity or make custom cards.", guildOnly = true
) {

    override fun execute(event: CommandEvent) {
        val c = CardsAgainstHumanity(event.guild, event.author, event.textChannel)
            .apply {
                event.message.mentionedUsers.forEach { addUser(it) }
            }.startGame()

        TODO(event)
        //STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Cards Against Weebot",
            "*You can purchase Cards Against Humanity for yourself to support the creators* "
                    + "[`here`]($LINK_CAH)\n\n" +
                    "Play a game of Cards Against Humanity and make custom decks " +
                    "with user-made cards. Weebot uses all official decks (as of december " +
                    "2018).\n\nTo play a game, you first need to setup and have at least" +
                    "3 players join, then you can start. Cards will be given to each player" +
                    "in a private message, then a Card Czar will be chosen at random." +
                    "A Black Card will be sent to the channel the game was started in," +
                    "once each player has played their card(s) (by reacting to their " +
                    "private chat hand or sending a ``play`` command) the Czar can choose" +
                    "a winner by reacting to the Black Card (up to $MAX_PLAYERS) or " +
                    "using the ``pick`` command. Each round new cards will be delt " +
                    "to each player, a new Czar will be chosen (in order), and a new" +
                    "Black Card will be sent until a winner is reached or the game is " +
                    "cancelled.\n\nHAVE FUN!")
            .addField("Game Setup", "setup [+bot] [hand_size] [deck1 deck2...] "
                    + "\n**Join Game**\njoin\n**+bot** adds Weebot to the game")
            .addField("Start Game (Requires $MIN_PLAYERS players)", "start")
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
            .setThumbnail(LINK_CAH_THUMBNAIL)
            .build()
    }

}
