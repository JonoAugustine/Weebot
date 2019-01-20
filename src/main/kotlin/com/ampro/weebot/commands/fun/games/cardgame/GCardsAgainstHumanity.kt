/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`.games.cardgame

import com.ampro.weebot.*
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.`fun`.games.*
import com.ampro.weebot.commands.`fun`.games.WinCondition.ROUNDS
import com.ampro.weebot.commands.`fun`.games.WinCondition.WINS
import com.ampro.weebot.commands.`fun`.games.cardgame.GameState.CHOOSING
import com.ampro.weebot.commands.`fun`.games.cardgame.GameState.READING
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_SHARD
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.Message.MentionType.*
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.*

internal const val LINK_CAH = "https://cardsagainsthumanity.com/"
internal const val LINK_CAH_THUMBNAIL
        = "https://cardsagainsthumanity.com/v8/images/social-3f4a4c57.png"

internal const val MIN_PLAYERS   = 3
internal const val MAX_PLAYERS   = 25
internal const val HAND_SIZE_MAX = 10
/** Default Hand Size */
internal const val HAND_SIZE_DEF = 5
internal const val HAND_SIZE_MIN = 4

internal val CAH_IDGEN   = IdGenerator(7, "CAH:")

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
        MLOG.elog(CardsAgainstHumanity::class, "Loading CAH WhiteCards - FAILED\n${e.message}")
        System.exit(-1)
    }

    //Load black cards
    try {
        Files.readAllLines(FILE_C_BLACK.toPath())
            .forEach {
                val pick = it.count { char -> char == '_' }
                C_BLACK_BASE.add(BlackCard(
                    it.replace(Regex("_+"), "[____]").replace("<i>", "*")
                        .replace("</i>","* ").replace(Regex("</?br>"), "\n"),
                    if (pick == 0) 1 else pick))
            }
    } catch (e: IOException) {
        MLOG.elog(CardsAgainstHumanity::class, "Loading CAH BlackCards - FAILED\n${e.message}")
        System.exit(-1)
    }

    MLOG.slog(CardsAgainstHumanity::class, "[CAH] Init Load of base cards complete!")
    val c = CAHDeck("Official", blackCards = C_BLACK_BASE, whiteCards = C_WHITE_BASE, public = true)
    c.saveJson(FILE_D_STRD_JSON.apply { createNewFile() })
    return@run c
}
/**[Guild.getIdLong] -> [MutableList]<[CAHDeck]>*/
internal val DECK_CUST: ConcurrentHashMap<Long, MutableList<CAHDeck>>
        = loadJson<ConcurrentHashMap<Long, MutableList<CAHDeck>>>(FILE_D_CUST_JSON)
        ?: run {
    MLOG.elog(CardsAgainstHumanity::class, "Failed to load Custom CAH Decks"); System.exit(-1);
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
    /** All decks created by the guild (not saved to dao file) */
    val decks: MutableList<CAHDeck> get() = DECK_CUST.getOrPut(guildId) {mutableListOf()}
    /** Saved public decks not made by the guild */
    val favoritedDecks: MutableList<String> = mutableListOf()
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
}

fun <T: Any> Collection<CAHDeck>.collect(params: (CAHDeck) -> List<T>)
        : Collection<T> {
    val list = mutableListOf<T>()
    forEach { list.addAll(params(it)) }
    return list
}

/**
 * @param name
 * @param authorID
 * @param public
 * @param init
 * @param writeRestrictions
 * @param blackCards
 * @param whiteCards
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
data class CAHDeck(var name: String, val authorID: Long = -1L,
                   var public: Boolean = false, val init: OffsetDateTime = NOW(),
                   val writeRestrictions: Restriction = Restriction(),
                   val blackCards: MutableList<BlackCard> = mutableListOf(),
                   val whiteCards: MutableList<WhiteCard> = mutableListOf()) {
    val id: String = CAH_IDGEN.next()
    /**Number of times used publically*/
    var popularity: Int = 0

    /**
     * @return an [EmbedBuilder] set to:
     * Title: [name]
     * Description: [id] \n [authorID] \n [init] \n [public] \n [popularity]
     */
    internal fun asEmbed() = makeEmbedBuilder(name, null, """
        ID: ``$id``
        Author: ${getUser(authorID)?.asMention ?: "*Unknown User*"}
        Created ${init.format(WKDAY_MONTH_YEAR)}
        ${if (public) "Public for all Weebot users" else ""}
        ${if (public && popularity > 0) "Times Used: $popularity" else ""}
        ${if (blackCards.isNotEmpty()) "Black Cards: ${blackCards.size}" else ""}
        ${if (whiteCards.isNotEmpty()) "White Cards: ${whiteCards.size}" else ""}
        """.trimIndent())

}

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

    /** Compare by [cardsWon] */
    override fun compareTo(other: Player)
            = this.cardsWon.size - (other as CAHPlayer).cardsWon.size

}

/**
 * Passive for watching and updating all [CAHPlayer.hand] menus
 *
 * @author Jonathan Augustine
 * @since 2.2.0
 */
internal class PlayerHandPassive(@Transient private val player: CAHPlayer,
                                 @Transient internal val cah: CardsAgainstHumanity?)
    : IPassive {
    var dead = false
    override fun dead() = dead

    @Transient var message: Message? = null

    /**
     * Send the player's hand and current game info
     */
    internal fun send() {
        if (cah == null) {
            dead = true
            return
        }
        val eb = makeEmbedBuilder(cah.title, LINK_CAH)
            .addField("Czar: ${cah.name(cah.czar.user)}", "", true)
            .addField("Black Card (Pick ${cah.blackCard.pick})", cah.blackCard.text,false)
            .setDescription("Choose ${cah.blackCard.pick} card(s) then " +
                    "$WhiteCheckMark to confirm the selection or $X_Red to " +
                    "clear the selection")
            .setThumbnail(LINK_CAH_THUMBNAIL)
        val sb = StringBuilder()
        player.hand.forEachIndexed { i, wc -> sb.append("``${i+1})`` ${wc.text}\n") }
        eb.addField("Your Cards", sb.toString(), false)

        val u = getUser(player.user.idLong)
        u?.openPrivateChannel()?.queue { pmc ->
            eb.build().send(pmc, {
                message?.delete()?.queueIgnore()
                message = it
                it.reactWith(EmojiNumbers.subList(0, player.hand.size)
                        + WhiteCheckMark + X_Red)
            })
        }
    }

    override fun accept(bot: Weebot, event: Event) {
        if (cah == null) {
            dead = true
            return
        }
        when {
            !cah.isRunning || cah.gameState == READING -> return
            event !is MessageReactionAddEvent  -> return
            event.privateChannel == null       -> return
            event.user.id != player.user.id    -> return
            event.messageId != message?.id     -> return
            else -> Unit
        }

        val emoji = event.reactionEmote.toEmoji() ?: return

        //Check for Selection
        val index = EmojiNumbers.indexOf(emoji)
        when {
            index != -1 -> player.playedCards.add(player.hand[index])
            emoji == X_Red -> {
                player.playedCards.clear()
                event.user.openPrivateChannel().queue {
                    it.sendMessage("Played Cards cleared.")
                        .queue { m -> m.delete().queueAfter(5, SECONDS) }
                }
            }
            emoji == WhiteCheckMark -> event.user.openPrivateChannel().queue { pmc ->
                when {
                    player == cah.czar -> pmc.sendMessage(
                        "The Czar cannot play their own cards!")
                        .queue { it.delete().queueAfter(5, SECONDS) }

                    player.playedCards.size == cah.blackCard.pick -> pmc.sendMessage(
                        "Cards submitted, may the memes be with you...")
                        .queue { it.delete().queueAfter(5, SECONDS) }

                    player.playedCards.size < cah.blackCard.pick -> pmc.sendMessage(
                        "You need ${cah.blackCard.pick - player.playedCards.size
                        } more card(s).")
                        .queue { it.delete() .queueAfter(5, SECONDS) }

                    player.playedCards.size > cah.blackCard.pick -> pmc.sendMessage(
                        "You need ${player.playedCards.size - cah.blackCard.pick
                        } fewer card(s). Hit $X_Red and reselect.")
                        .queue { it.delete().queueAfter(5, SECONDS) }
                }
                synchronized(cah) {
                    if (cah.playerList.filterNot { it.user.isBot || it == cah.czar }
                                .all { it.playedCards.size == cah.blackCard.pick }
                            && cah.gameState == CHOOSING) {
                        cah.gameState = READING
                        cah.sendBlackCard()
                    }
                }
            }
        }

    }

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
                           channel: TextChannel,
                           val decks: List<CAHDeck> = listOf(DECK_STRD),
                           val handSize: Int = HAND_SIZE_DEF,
                           val winCondition: WinCondition = WINS(7))
    : CardGame<CAHPlayer>(guild, author) {

    private val guildName = guild.name
    internal val channelID = channel.idLong
    val channel: TextChannel get() = getGuild(guildID)!!.getTextChannelById(channelID)!!
    val name: (User) -> String = {
        getGuild(guildID)?.getMember(it)?.effectiveName?: it.name
    }

    private val activeCardsBlack = mutableListOf<BlackCard>()
    private val activeCardsWhite = mutableListOf<WhiteCard>()

    private val usedCardsWhite = mutableListOf<WhiteCard>()
    private val usedCardsBlack = mutableListOf<BlackCard>()

    /**The current [BlackCard] being played on */
    lateinit var blackCard: BlackCard

    private var blackCardMessage: Message? = null

    @Transient
    private val playerHandMenus = ConcurrentHashMap<CAHPlayer, PlayerHandPassive>()

    /** The current Czar  */
    internal lateinit var czar: CAHPlayer

    internal lateinit var gameState: GameState

    /** A round is finished when each player has been Czar once */
    private var round: Int = 1

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

    val title: String get() = (if ("Cards Against $guildName".length > EMBED_MAX_TITLE)
        CMD_CAH.displayName!! else "Cards Against $guildName") + " | round $round"

    /**
     * Send the black card to [channel]
     */
    fun sendBlackCard() {
        if (!isRunning) return
        val baseEmbed: () -> EmbedBuilder = {
            makeEmbedBuilder(title, LINK_CAH).apply {
                setAuthor("Czar: ${name(czar.user)}", czar.user.avatarUrl)
                setDescription("""**Black Card | Pick ${blackCard.pick}:**
                    ${blackCard.text}""".trimIndent())
                setThumbnail(LINK_CAH_THUMBNAIL)
                if (gameState == CHOOSING) addField(howToPlayField)
            }
        }

        if (gameState == CHOOSING) { //If this is the CHOOSING embed
            /** Loads new [blackCard] and removes it from [activeCardsBlack] */
            fun reloadBlackCard() {
                this.activeCardsBlack.remove(this.blackCard)
                this.blackCard = this.activeCardsBlack.random()
                blackCardMessage?.delete()?.queueAfter(250, MILLISECONDS)
                sendBlackCard()
                sendWhiteCards(*playerList.toTypedArray())
            }
            val e = baseEmbed().addField("Guide", """
                $ArrowsCounterclockwise to load a new Black Card
                $Warning to report this Black Card and load a new one
            """.trimIndent(), true).build()
            SelectableEmbed(czar.user, false, e, listOf(
                ArrowsCounterclockwise to cc@{  m, _ ->
                    if (!isRunning) return@cc
                    this.usedCardsBlack.add(this.blackCard)
                    reloadBlackCard()
                    m.delete().queueAfter(250, MILLISECONDS)
                }, Warning to { m, _ ->
                    try {
                        blackCard.reports++
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
        } else { //If this is the Reading Embed
            //Give the bots cards
            this.playerList.filter { it.user.isBot }.forEach {
                while (it.playedCards.size < blackCard.pick)
                    it.playedCards.add(it.hand.random())
            }
            val embed = baseEmbed().addField("Czar, choose your victor!",
                "${czar.user.asMention}, " +
                        "Select a victor by reacting with the corresponding number.",
                true)
                .build()
            val playersIn = playerList.filterNot { it.playedCards.isEmpty() || it == czar}
                .map { p ->
                //TODO Check formatting
                p.playedCards.joinToString("\n") { it.text } to { _: Int, _: Message ->
                    p.cardsWon.add(this.blackCard)
                    this.channel.sendMessage(makeEmbedBuilder(title, LINK_CAH,
                        "LeaderBoard:\n${leaderBoard()}")
                        .setAuthor("${name(p.user)} Won This Turn! $Tada$Tada").build()
                    ).queue { nextTurn() }
                }
            }.shuffled()
            val sp = SelectablePaginator(setOf(czar.user), emptySet(), -1, MINUTES, embed,
                playersIn, itemsPerPage = -1, singleUse = true) {
                blackCardMessage = it
            }

            if (blackCardMessage != null) blackCardMessage!!.clearReactions().queue({
                sp.displayOrDefault(blackCardMessage, channel)
            }, { sp.displayOrDefault(blackCardMessage, channel) })
            else sp.displayOrDefault(blackCardMessage, channel)
        }

    }

    /**
     * Sends each [players] their [CAHPlayer.hand]. This does NOT deal cards.
     * @param players
     */
    fun sendWhiteCards(vararg players: CAHPlayer) {
        if (!isRunning) return
        players.filterNot { it.user.isBot }.forEach { p ->
            playerHandMenus.getOrPut(p) {
                val php = PlayerHandPassive(p, this@CardsAgainstHumanity)
                DAO.GLOBAL_WEEBOT.addUserPassive(p.user, php)
                return@getOrPut php
            }.send()
        }
    }

    /**
     * Clears Played cards then deal new cards to [player]
     *
     * @param player The player to deal cards to.
     * @return False if the player already has a full hand.
     */
    override fun dealCards(player: CAHPlayer): Boolean {
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
        sendWhiteCards(*playerList.toTypedArray())
        gameState = CHOOSING
        sendBlackCard()
    }

    /** @return true if the Czar was reset (start a new round) */
    private fun nextCzar() : Boolean {
        val list: List<CAHPlayer> = this.playerList.filterNot { it.user.isBot }
        val i = list.indexOf(czar)
        return if (i + 1 in 0 until list.size) {
            czar = list[i + 1]
            false
        } else {
            czar = list.first()
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
            if (this.playerList.filterNot { it.user.isBot }.size < 2)
                return false
            //Deal Cards to players
            this.playerList.forEach { dealCards(it) }
            //Set the First Czar
            czar = this.playerList.filterNot { it.user.isBot }.random()
            //Set the first black card
            blackCard = activeCardsBlack.random()
            gameState = CHOOSING
            isRunning = true
            sendBlackCard()
            sendWhiteCards(*playerList.toTypedArray())
            true
        } else false
    }

    override fun endGame(): Boolean {
        if (!decks.all { it == DECK_STRD }) decks.filter(CAHDeck::public)
            .forEach { it.popularity++ }
        val sortedList = playerList.sortedByDescending { it.cardsWon.size }
        val winner = sortedList.first()
        val bot = getWeebotOrNew(guildID)
        bot.games.remove(this)

        val cgi = bot.cahGuildInfo

        cgi.leaderBoard.addUser(winner.user, winner.cardsWon.size)

        makeEmbedBuilder(title, LINK_CAH, """And the winner is.....
            ${winner.user.asMention}!!! $Tada$Tada$Tada""".trimIndent())
            .setThumbnail(winner.user.avatarUrl)
            .addField("Cards Against Humanity Leaderboard", cgi.leaderBoard.get()
                .joinToString("\n") {
                    getGuild(guildID)!!.getMemberById(it.first)?.effectiveName
                            ?: "Unknown Member"
                }, false).build()
            .send(channel)
        isRunning = false
        blackCardMessage = null
        playerHandMenus.forEach { it.value.dead = true }
        return true
    }

    /** @return The [playerList] sorted by score each player on a new line */
    private fun leaderBoard() : String {
        return playerList.sortedByDescending { it.cardsWon.size }
            .joinToString("\n") { "*${name(it.user)}:* ${it.cardsWon.size}" }
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
    "Play a game of CardsAgainstHumanity or make custom cards.", guildOnly = true,
    children = arrayOf(SubCmdDeck(), SubCmdSeeReports())
) {

    //Setup, Join, & leaderboard
    override fun execute(event: WeebotCommandEvent) {
        if (event.splitArgs().isEmpty()) return
        val liveGames = event.bot.getRunningGames<CardsAgainstHumanity>()

        //Setup, Join, & leaderboard
        when {
            //setup [hand_size] [default] [deck_id1 deck_id2...] [@bots...]
            event.argList[0].matches("(?i)set(up)?") -> {
                TODO(event)
                //TODO selectableEmbed, +/- for hand size, paginator for decks
            }
            //join the game at any time
            event.argList[0].matches("(?i)j(oin)?") -> TODO(event)
            //start
            event.argList[0].matches("(?i)sta?rt") -> {
                TODO(event)
                STAT.track(this, event.bot, event.author, event.creationTime)
            }
            //Get your cards re-sent
            event.argList[0].matches("(?i)((my)?hand|(re)?send)") -> TODO(event)
            //See the current game's scores
            event.argList[0].matches("(?i)scores?") -> TODO(event)
            //"See server leaderboard", "``leaderboard``\n*Aliases:* LB, top",
            event.argList[0].matches("(?i)setup") -> TODO(event)
            event.argList[0].matches("(?i)setup") -> TODO(event)
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Cards Against Weebot",
            "*You can purchase Cards Against Humanity for yourself to support the creators* "
                    + "[`here`]($LINK_CAH)\n\n" +
                    "Play a game of Cards Against Humanity and make custom decks " +
                    "with user-made cards. Weebot uses all official decks (as of " +
                    "December 2018).\n\nTo play a game, you first need to setup " +
                    "and have at least 3 players join, then you can start. Cards will " +
                    "be given to each player in a private message, then a Card Czar " +
                    "will be chosen at random. A Black Card will be sent to the channel" +
                    " the game was started in, once each player has played their card" +
                    "(s) by reacting to their private chat hand. The Czar can choose " +
                    "a winner by reacting to the Black Card. Each round new cards will " +
                    "be dealt to each player, a new Czar will be chosen (in order), and " +
                    "a new Black Card will be sent until a winner is reached or the " +
                    "game is cancelled.\n\nHAVE FUN!")
            .addField("Game Setup", """``setup [hand_size] [deck1 deck2...]``
                Any bot can be added to the game by ``@mentioning`` them
                ``join`` to join the game at any time
                ``start`` to start the game (*Requires $MIN_PLAYERS players*)
                """.trimIndent())
            .addField("Play Your Card(s)", "White Cards can be played by reacting to "
                    + "the private message", true)
            .addField("Czar Pick Winning Card(s)", "React to the Black Card message",true)
            .addField("Get your cards re-sent", "``myhand``", true)
            .addField("See the current game's scores","``scores``", true)
            .addField("See server leaderboard", "``leaderboard``\n*Aliases:* LB, top", true)
            .addField("Make a Custom Deck", "``deck make [-public] <deck_name>``", true)
            .addField("View Custom Decks",
                """``deck see [-public or -all] [deckNames/IDs...]``
                    |``-public/-p`` to see a list of public, user-made decks
                    |``-all/-a`` to see decks made in this server and public ones
                    |*By default, only decks in this server will be shown*
                    |""".trimMargin(), true)
            .setThumbnail(LINK_CAH_THUMBNAIL)
            .build()
    }

}

/**
 * Sub command for [CAHDeck] actions
 * @author Jonathan Augustine
 * @since 2.2.1
 */
internal class SubCmdDeck : WeebotCommand("deck", null, arrayOf("decks"), CAT_GAME,
    "","", guildOnly = true, cooldown = 60, cooldownScope = USER_SHARD) {
    override fun execute(event: WeebotCommandEvent) {
        //make [-public] <deck_name>
        when {
            event.argList.isEmpty() -> {
                event.respondThenDelete("Use ``make`` or ``view`` to do something.", 5)
            }
            event.argList[0].matches("(?i)m(ake)?") -> {
                if (event.argList.size == 1) { //Use waiter to get name
                    event.respondThenDelete("What is the name of the new deck?", 360)
                    WAITER.waitForEvent(MessageReceivedEvent::class.java, check@{
                        when (validName(it, it.message.contentDisplay, event.bot)) {
                            0 -> {
                                event.respondThenDelete(
                                    "Name cannot contain emotes, please try again")
                                false
                            }
                            -1 -> {
                                event.respondThenDelete("Name cannot have mentions, " +
                                        "please try again")
                                false
                            }
                            -2 -> {
                                event.respondThenDelete("Name cannot be a number, " +
                                        "please try again")
                                false
                            }
                            -3 -> {
                                event.respondThenDelete("Name is too long, please try again")
                                false
                            }
                            -4 -> {
                                event.respondThenDelete("There is already a deck " +
                                        "by that name in this server, please try again")
                                false
                            }
                            else -> it.isValidUser(event.guild, setOf(event.author),
                                channel = event.channel)
                        }
                    },
                        {
                        event.respondThenDelete(
                            "Allow all Weebot users to use this deck? (yes/no)", 360)
                        WAITER.waitForEvent(MessageReceivedEvent::class.java, check@{e2 ->
                            if (e2.isValidUser(event.guild, setOf(event.author),
                                        channel = event.channel)) {
                                if (!e2.message.contentDisplay
                                            .matchesAny(REG_YES, REG_NO)) {
                                    event.respondThenDelete("Please say ``yes`` or ``no``")
                                    false
                                } else true
                            } else false
                        }, { e2 ->
                            val pub = e2.message.contentDisplay.matches(REG_YES)
                            val deck = CAHDeck(it.message.contentDisplay,
                                it.author.idLong, pub, event.creationTime)
                            event.bot.cahGuildInfo.decks.add(deck)
                            sendNewDeck(event, deck)
                            STAT.track(this, event.bot, event.author, event.creationTime)
                        }, 5, MINUTES, {
                            event.respondThenDelete("Timed out. Please try again.")
                        })
                    }, 5, MINUTES, {
                        event.respondThenDelete("Timed out. Please try again.")
                    })
                } else {
                    var nameStart = 1
                    if (event.argList[1].matches(REG_HYPHEN+"p(ub(lic)?)?")) nameStart++
                    if (nameStart >= event.argList.size) {
                        return event.respondThenDelete("No name provided.", 30)
                    }
                    val name = event.argList.subList(nameStart).joinToString(" ")

                    if (validName(event.event, name, event.bot) == 0) {
                        return event.respondThenDelete(
                            "Name cannot contain emotes, " + "please try again", 30)
                    } else if (validName(event.event, name, event.bot) == -1) {
                        return event.respondThenDelete("Name cannot have mentions, " +
                                "please try again", 30)
                    } else if (validName(event.event, name, event.bot) == -2) {
                        return event.respondThenDelete("Name cannot be a number, " +
                                "please try again", 30)
                    } else if (validName(event.event, name, event.bot) == -3) {
                        return event.respondThenDelete("Name is too long, please try " +
                                "again", 30)
                    } else if (validName(event.event, name, event.bot) == -4) {
                        return event.respondThenDelete("There is already a deck "
                                + "by that name in this server, please try again", 30)
                    } else if (event.bot.cahGuildInfo.decks.has{
                                it.name.equals(name, true)})
                        return event.respondThenDelete("There is already a deck " +
                                "in this server by that name. Please try again.", 30)

                    val deck = CAHDeck(name, event.author.idLong, nameStart == 2,
                        event.creationTime)
                    event.bot.cahGuildInfo.decks.add(deck)
                    sendNewDeck(event, deck)
                    STAT.track(this, event.bot, event.author, event.creationTime)
                }
            }
            event.argList[0].matches("(?i)(v(iew)?|se*)") -> {
                TODO(event)
                //see/view [-public or -all] [deckNames/IDs...]
                //*By default, only decks in this server will be shown*
            }
        }
    }

    private fun sendDeck(event: WeebotCommandEvent, deck: CAHDeck, embed: EmbedBuilder?) {
        if (event.bot.cahGuildInfo.decks.none { it.id == deck.id })
            return sendForeignDeck(event, deck)

        val e = (embed ?: deck.asEmbed()).addField("Actions", """
            $BlackLargeSquare to write a Black Card
            $B to view all Black Cards
            $WhiteLargeSquare to write a White Card
            $W to view all White Cards
            ${if(event.author.`is`(deck.authorID)) "$Lock to change write restrictions"
            else ""}""".trimIndent(), false).build()

        val items = mutableListOf(
            BlackLargeSquare to { m: Message, u: User ->
                TODO(event)
            }, B to { m: Message, u: User ->
                TODO(event)
            }, WhiteLargeSquare to { m: Message, u: User ->
                TODO(event)
            }, W to { m: Message, u: User ->
                TODO(event)
            }
        )
        if (event.author.`is`(deck.authorID)) {
            items.add(Lock to { m: Message, u: User ->
                TODO(event)
            })
        }

        val writers = mutableSetOf(event.author)
        val writerRoles = mutableSetOf<Role>()
        if (deck.writeRestrictions.isRestricted()) {
            writers.addAll(deck.writeRestrictions.allowedUsers.mapNotNull {
                event.guild.getMemberById(it).user
            })
            writerRoles.addAll(deck.writeRestrictions.allowedRoles.mapNotNull {
                event.guild.getRoleById(it)
            })
        }

        SelectableEmbed(writers, writerRoles, 5, MINUTES, false, e, items) {
            it.clearReactions().queue()
        }.display(event.channel)

    }

    private fun sendForeignDeck(event: WeebotCommandEvent, deck: CAHDeck) {
        TODO(event)
    }

    /** Builds an embed for [CAHDeck] creation then uses [sendDeck] to send */
    private fun sendNewDeck(event: WeebotCommandEvent, deck: CAHDeck) {
        sendDeck(event, deck, deck.asEmbed().addField("What Next?", """
            Right now this deck is empty $FrowningFace, but you can start adding cards
            to this deck by reacting to this message or by using this command:
            ```css
            cah deck view ${deck.id.removePrefix(CAH_IDGEN.prefix)}
            ```
            """.trimIndent(), false).setThumbnail(event.author.avatarUrl)
            .setAuthor("New Deck Created!!"))

    }

    /**
     * @return 1 if valid
     *         0 if contained emotes
     *        -1 if contains mentions
     *        -2 if it's a number
     *        -3 if invalid length
     *        -4 if duplicate name
     */
    private fun validName(event: MessageReceivedEvent, name: String, weebot: Weebot)
            : Int {
        return when {
            event.message.emotes.isNotEmpty() -> 0
            event.message.getMentions(USER, ROLE, CHANNEL, EMOTE, HERE, EVERYONE)
                .isNotEmpty() -> -1
            name.length > EMBED_MAX_TITLE -> -3
            weebot.cahGuildInfo.decks.any { it.name.equals(name, true) } -> -4
            else -> try {
                name.toDouble()
                -2
            } catch (e: NumberFormatException) { 1 }
        }
    }
}

/**
 * A [WeebotCommand] to inspect any [BlackCard] or [WhiteCard] with any reports.
 *
 * @author Jonathan Augustine
 * @since 2.2.1
 */
internal class SubCmdSeeReports : WeebotCommand("reports", null, arrayOf("report", "rep"),
    CAT_DEV, "", "", ownerOnly = true, hidden = true) {
    override fun execute(event: WeebotCommandEvent) {
        val whiteCards = (DECK_CUST.map { it.value }.flatten() + DECK_STRD)
            .map { it.whiteCards }.flatten().filter { it.reports > 0 }
            .sortedByDescending { it.reports }.map {
                "[R: ${it.reports}] W:``${it.id}`` ~ ${it.text}" to { _:Int, m:Message ->
                    TODO(event)
                }
            }

        val blackCards = (DECK_CUST.map { it.value }.flatten() + DECK_STRD)
            .map { it.blackCards }.flatten().filter { it.reports > 0 }
            .sortedByDescending { it.reports }.map {
                "``[R: ${it.reports}] B:${it.id} - Picks ${it.pick}`` ~ ${it.text}" to
                        { _:Int, m:Message ->
                    TODO(event)
                }
            }

        SelectablePaginator(setOf(event.author), timeout = -1, itemsPerPage = -1,
            title = "Reported CAH Cards",
            description = "All CAH cards with at least ``1`` report.",
            items = blackCards + whiteCards).display(event.channel)

    }
}
