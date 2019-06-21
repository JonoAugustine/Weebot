/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`.games.cardgame

import com.ampro.weebot.GENERIC_ERR_MSG
import com.ampro.weebot.GlobalWeebot
import com.ampro.weebot.MLOG
import com.ampro.weebot.Weebot
import com.ampro.weebot.cahInfo
import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.commands.CAT_UNDER_CONSTRUCTION
import com.ampro.weebot.commands.CMD_CAH
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.`fun`.games.LeaderBoard
import com.ampro.weebot.commands.`fun`.games.Player
import com.ampro.weebot.commands.`fun`.games.WinCondition
import com.ampro.weebot.commands.`fun`.games.WinCondition.ROUNDS
import com.ampro.weebot.commands.`fun`.games.WinCondition.WINS
import com.ampro.weebot.commands.`fun`.games.cardgame.GameState.CHOOSING
import com.ampro.weebot.commands.`fun`.games.cardgame.GameState.READING
import com.ampro.weebot.database.getCahInfoAll
import com.ampro.weebot.database.getGuild
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.database.save
import com.ampro.weebot.database.track
import com.ampro.weebot.database.user
import com.ampro.weebot.extensions.DynamicSelectablePaginator
import com.ampro.weebot.extensions.EMBED_MAX_TITLE
import com.ampro.weebot.extensions.Restriction
import com.ampro.weebot.extensions.SelectableEmbed
import com.ampro.weebot.extensions.SelectablePaginator
import com.ampro.weebot.extensions.TODO
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.`is`
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.delete
import com.ampro.weebot.extensions.hasPerm
import com.ampro.weebot.extensions.isAdmin
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.extensions.matches
import com.ampro.weebot.extensions.matchesAny
import com.ampro.weebot.extensions.plus
import com.ampro.weebot.extensions.queueIgnore
import com.ampro.weebot.extensions.respondThenDelete
import com.ampro.weebot.extensions.respondThenDeleteBoth
import com.ampro.weebot.extensions.send
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.extensions.strdPaginator
import com.ampro.weebot.extensions.subList
import com.ampro.weebot.extensions.unit
import com.ampro.weebot.extensions.waitForMessage
import com.ampro.weebot.util.DIR_RES
import com.ampro.weebot.util.Emoji
import com.ampro.weebot.util.Emoji.ArrowsCounterclockwise
import com.ampro.weebot.util.Emoji.B
import com.ampro.weebot.util.Emoji.Beginner
import com.ampro.weebot.util.Emoji.BlackLargeSquare
import com.ampro.weebot.util.Emoji.D
import com.ampro.weebot.util.Emoji.FastForward
import com.ampro.weebot.util.Emoji.FirstPlaceMedal
import com.ampro.weebot.util.Emoji.FrowningFace
import com.ampro.weebot.util.Emoji.GiftHeart
import com.ampro.weebot.util.Emoji.HeavyMinusSign
import com.ampro.weebot.util.Emoji.HeavyPlusSign
import com.ampro.weebot.util.Emoji.J
import com.ampro.weebot.util.Emoji.Lock
import com.ampro.weebot.util.Emoji.Pencil
import com.ampro.weebot.util.Emoji.Tada
import com.ampro.weebot.util.Emoji.Unlock
import com.ampro.weebot.util.Emoji.W
import com.ampro.weebot.util.Emoji.Warning
import com.ampro.weebot.util.Emoji.WhiteCheckMark
import com.ampro.weebot.util.Emoji.WhiteLargeSquare
import com.ampro.weebot.util.Emoji.X_Red
import com.ampro.weebot.util.EmojiNumbers
import com.ampro.weebot.util.IdGenerator
import com.ampro.weebot.util.NOW
import com.ampro.weebot.util.REG_HYPHEN
import com.ampro.weebot.util.REG_NO
import com.ampro.weebot.util.REG_YES
import com.ampro.weebot.util.WKDAY_MONTH_YEAR
import com.ampro.weebot.util.loadJson
import com.ampro.weebot.util.reactWith
import com.ampro.weebot.util.saveJson
import com.ampro.weebot.util.toEmoji
import com.google.gson.annotations.SerializedName
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER_SHARD
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.ISnowflake
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Message.MentionType.CHANNEL
import net.dv8tion.jda.core.entities.Message.MentionType.EMOTE
import net.dv8tion.jda.core.entities.Message.MentionType.EVERYONE
import net.dv8tion.jda.core.entities.Message.MentionType.HERE
import net.dv8tion.jda.core.entities.Message.MentionType.ROLE
import net.dv8tion.jda.core.entities.Message.MentionType.USER
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.entities.PrivateChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

private const val LINK_CAH = "https://cardsagainsthumanity.com/"
private const val LINK_CAH_THUMBNAIL = "https://cardsagainsthumanity.com/v8/images/social-3f4a4c57.png"

private const val MIN_PLAYERS = 3
private const val MAX_PLAYERS = 25
private const val HAND_SIZE_MAX = 10
/** Default Hand Size */
private const val HAND_SIZE_DEF = 5
private const val HAND_SIZE_MIN = 4

private val IDGEN_CAH = IdGenerator(7, "CAH")

val DIR_CAH = File(DIR_RES, "CAH")
val FILE_C_BLACK = File(DIR_CAH, "CAH_C_BLACK")
val FILE_C_WHITE = File(DIR_CAH, "CAH_C_WHITE")
val FILE_D_STRD_JSON = File(DIR_CAH, "CAH_D_STRD.json")
val FILE_D_CUST_JSON = File(DIR_CAH, "CAH_D_CUSTOM.json")

private val C_BLACK_BASE: MutableList<BlackCard> = mutableListOf()
private val C_WHITE_BASE: MutableList<WhiteCard> = mutableListOf()
private val DECK_STRD by lazy {
    loadJson<CAHDeck>(FILE_D_STRD_JSON) ?: run {
        //Load white cards
        try {
            Files.readAllLines(FILE_C_WHITE.toPath())
                .forEach { C_WHITE_BASE.add(WhiteCard(it)) }
        } catch (e: IOException) {
            MLOG.elog(CardsAgainstHumanity::class,
                "Loading CAH WhiteCards - FAILED\n${e.message}")
            System.exit(-1)
        }

        //Load black cards
        try {
            Files.readAllLines(FILE_C_BLACK.toPath()).forEach {
                val pick = it.count { char -> char == '_' }
                C_BLACK_BASE.add(BlackCard(
                    it.replace(Regex("_+"), "[____]").replace("<i>", "*").replace("</i>",
                        "* ").replace(Regex("</?br>"), "\n"), if (pick == 0) 1 else pick))
            }
        } catch (e: IOException) {
            MLOG.elog(CardsAgainstHumanity::class,
                "Loading CAH BlackCards - FAILED\n${e.message}")
            System.exit(-1)
        }

        MLOG.slog(CardsAgainstHumanity::class, "[CAH] Init Load of base cards complete!")
        val c = CAHDeck("Official", blackCards = C_BLACK_BASE, whiteCards = C_WHITE_BASE,
            public = true)
        c.saveJson(FILE_D_STRD_JSON.apply { createNewFile() }, true)
        return@run c
    }
}
/** All public CAH decks */
private val DECK_CUSTOM_PUB: List<CAHDeck> by lazy {
    getCahInfoAll()
        .flatMap {
            it.value.decks.filter { d ->
                d.public && (d.whiteCards + d.blackCards).isNotEmpty()
            }
        }
}

private fun <T : Card> Collection<CAHDeck>.collect(params: (CAHDeck) -> List<T>)
    : Collection<T> {
    val list = mutableListOf<T>()
    forEach { list.addAll(params(it)) }
    return list
}

/**
 * A class used to hold information about a guild's CAH info like
 * leaderboards, blocked cards, and custom decks.
 *
 * @param decks All decks created by the guild (not saved to dao file).
 * @param favoritedDecks Saved public deck IDs *not made by the guild*.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
data class CahInfo(
    val _id: String,
    val leaderBoard: LeaderBoard = LeaderBoard(),
    val decks: MutableList<CAHDeck> = mutableListOf(),
    val favoritedDecks: MutableList<String> = mutableListOf()
)

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class WhiteCard(val text: String) : Card() {
    val id: String = IDGEN_CAH.next()
    /** How many times this card has been reported by end users */
    var reports: Int = 0
}

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
data class BlackCard(val text: String, val pick: Int) : Card() {
    val id: String = IDGEN_CAH.next()
    /** How many times this card has been reported by end users */
    var reports: Int = 0
}


/**
 * @param name
 * @param authorID
 * @param public
 * @param init
 * @param wrtRestrct
 * @param blackCards
 * @param whiteCards
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
data class CAHDeck(var name: String, val authorID: Long = -1L,
                   var public: Boolean = false, val init: OffsetDateTime = NOW(),
                   @SerializedName("writeRestrictions")
                   val wrtRestrct: Restriction = Restriction(),
                   val blackCards: MutableList<BlackCard> = mutableListOf(),
                   val whiteCards: MutableList<WhiteCard> = mutableListOf()) {
    val id: String = IDGEN_CAH.next()
    /**Number of times used publically*/
    var popularity: Int = 0

    /**
     * @return an [EmbedBuilder] set to:
     * Title: [name]
     * Description: [id] \n [authorID] \n [init] \n [public] \n [popularity]
     */
    fun asEmbed(g: Guild) = makeEmbedBuilder(name, null, """
        ID: ``$id``
        Author: ${authorID.user?.asMention ?: "*Unknown User*"}
        Created ${init.format(WKDAY_MONTH_YEAR)}
        ${if (public) "Public for all Weebot users $GiftHeart" else ""}
        ${if (public && popularity > 0) "Times Used: $popularity" else ""}
        ${if (!wrtRestrct.isRestricted()) "Anyone in the original server can add cards"
    else """
            ${if (wrtRestrct.allowedRoles.isNotEmpty()) "Editors: " + wrtRestrct.allowedUsers.mapNotNull(
        g::getMemberById).joinToString { it.asMention } else ""}
            ${if (wrtRestrct.allowedRoles.isNotEmpty()) "Editor Roles: " + wrtRestrct.allowedRoles.mapNotNull(
        g::getRoleById).joinToString { it.asMention } else ""}
            """.trimIndent()}
        ${if (blackCards.isNotEmpty()) "Black Cards: ${blackCards.size}" else ""}
        ${if (whiteCards.isNotEmpty()) "White Cards: ${whiteCards.size}" else ""}
        """.trimIndent())

    fun matches(any: Any): Boolean {
        return when (any) {
            is CAHDeck -> this.matches(any.name)
            is String -> this.matches(any)
            else -> false
        }
    }

    /** @return `true` if [name] == [CAHDeck.name] (ignoring non-digit/alpha/space) */
    private fun matches(name: String): Boolean {
        val name1 = this.name.toLowerCase().replace(Regex("([^a-zA-Z0-9]|\\s+)"), "_")
        val name2 = name.toLowerCase().replace(Regex("([^a-zA-Z0-9]|\\s+)"), "_")
        return name1 == name2
    }

    /**
     * @param other
     * @return `true` if [other] has the same [CAHDeck.id]
     */
    override fun equals(other: Any?): Boolean {
        return other is CAHDeck && other.id == this.id
    }

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
    override fun compareTo(
        other: Player) = this.cardsWon.size - (other as CAHPlayer).cardsWon.size

}

/**
 * Passive for watching and updating all [CAHPlayer.hand] menus
 *
 * @author Jonathan Augustine
 * @since 2.2.0
 */
class PlayerHandPassive(@Transient private val player: CAHPlayer,
                                @Transient internal val cah: CardsAgainstHumanity?)
    : IPassive {
    var dead = false
    override fun dead() = dead

    @Transient
    var message: Message? = null

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
            .addField("Black Card (Pick ${cah.blackCard.pick})", cah.blackCard.text,
                false)
            .setDescription("Choose ${cah.blackCard.pick} card(s) then " +
                "$WhiteCheckMark to confirm the selection or $X_Red to " +
                "clear the selection")
            .setThumbnail(LINK_CAH_THUMBNAIL)
        val sb = StringBuilder()
        player.hand.forEachIndexed { i, wc -> sb.append("``${i + 1})`` ${wc.text}\n") }
        eb.addField("Your Cards", sb.toString(), false)

        val u = player.user.idLong.user?.openPrivateChannel()?.queue { pmc ->
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
            event !is MessageReactionAddEvent -> return
            event.privateChannel == null -> return
            event.user.id != player.user.id -> return
            event.messageId != message?.id -> return
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
                        .queue { it.delete().queueAfter(5, SECONDS) }

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
    /** Players are choosing white cards */
    CHOOSING,
    /** Czar is reading played white cards */
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
                           channel: TextChannel,
                           val decks: List<CAHDeck> = listOf(DECK_STRD),
                           val handSize: Int = HAND_SIZE_DEF,
                           val winCondition: WinCondition = WINS(5))
    : CardGame<CAHPlayer>(guild, author) {

    constructor(event: WeebotCommandEvent, decks: List<CAHDeck>, handSize: Int,
                winCondition: WinCondition)
        : this(event.guild, event.author, event.textChannel, decks, handSize,
        winCondition)

    private val guildName = guild.name
    internal val channelID = channel.idLong
    val channel: TextChannel get() = getGuild(guildID)!!.getTextChannelById(channelID)!!
    val name: (User) -> String = {
        getGuild(guildID)?.getMember(it)?.effectiveName ?: it.name
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
    lateinit var czar: CAHPlayer

    internal lateinit var gameState: GameState

    /** A round is finished when each player has been Czar once */
    private var round: Int = 1

    init {
        //Collect all the cards for this game's deck
        activeCardsWhite.addAll(decks.collect { it.whiteCards })
        activeCardsBlack.addAll(decks.collect { it.blackCards })
    }

    companion object {
        val howToPlayField = Field("How to Play:",
            "Select your cards from the private chat I sent you by reacting with the " +
                "cards you want to play. Use ``cah resend`` or ``cah myhand`` " +
                "to have the message resent.)", true)
    }

    val title: String
        get() = (if ("Cards Against $guildName".length > EMBED_MAX_TITLE)
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
                $ArrowsCounterclockwise to refresh (if the game freezes up)
                $FastForward to load a new Black Card
                $Warning to report this Black Card and load a new one
            """.trimIndent(), true).build()
            SelectableEmbed(czar.user, false, e, listOf(
                ArrowsCounterclockwise to reload@{ m, _ ->
                    if (!isRunning) return@reload
                    m.delete().queueAfter(250, MILLISECONDS)
                    sendBlackCard()
                    sendWhiteCards(*playerList.toTypedArray())
                }, FastForward to skip@{ m, _ ->
                    if (!isRunning) return@skip
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
            val playersIn = playerList.filterNot { it.playedCards.isEmpty() || it == czar }
                .map { p ->
                    p.playedCards.joinToString(
                        "\n") { it.text } to { _: Int, _: Message ->
                        p.cardsWon.add(this.blackCard)
                        this.channel.sendMessage(makeEmbedBuilder(title, LINK_CAH,
                            "LeaderBoard:\n${leaderBoard()}")
                            .setAuthor(
                                "${name(p.user)} Won This Turn! $Tada$Tada").build()
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
    private fun sendWhiteCards(vararg players: CAHPlayer) {
        if (!isRunning) return
        players.filterNot { it.user.isBot }.forEach { p ->
            playerHandMenus.getOrPut(p) {
                val php = PlayerHandPassive(p, this@CardsAgainstHumanity)
                GlobalWeebot { p.user.addPassive(php) }
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
    private fun nextCzar(): Boolean {
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
        return when {
            this.players.size !in MIN_PLAYERS..MAX_PLAYERS -> false
            this.playerList.filterNot { it.user.isBot }.size < 2 -> false
            //Deal Cards to players
            //Set the First Czar
            //Set the first black card
            else -> {
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
            }
        }

    }

    override fun endGame(): Boolean {
        if (!decks.all { it == DECK_STRD }) decks.filter(CAHDeck::public)
            .forEach { it.popularity++ }
        val sortedList = playerList.sortedByDescending { it.cardsWon.size }
        val winner = sortedList.first()
        val cahInfo = runBlocking {
            getWeebotOrNew(guildID)
                .apply { games.remove(this@CardsAgainstHumanity) }
                .let {
                    it.save()
                    it.cahInfo
                }
        }

        cahInfo.leaderBoard.addUser(winner.user, winner.cardsWon.size)

        makeEmbedBuilder(title, LINK_CAH, """And the winner is.....
            ${winner.user.asMention}!!! $Tada$Tada$Tada""".trimIndent())
            .setThumbnail(winner.user.avatarUrl)
            .addField("Cards Against Humanity Leaderboard", cahInfo.leaderBoard.get()
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
    private fun leaderBoard(): String {
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
class CmdCardsAgainstHumanity : WeebotCommand(
    "cah", "GAMECAH", "Cards Against Humanity",
    arrayOf("cardsagainsthumanity"), CAT_UNDER_CONSTRUCTION,
    "Play a game of CardsAgainstHumanity or make custom cards.",
    guildOnly = true, children = arrayOf(SubCmdDeck(), SubCmdSeeReports())
) {

    //Setup, Join, & leaderboard
    override fun execute(event: WeebotCommandEvent) {
        if (event.splitArgs().isEmpty()) return
        val liveGames = event.bot.getRunningGames<CardsAgainstHumanity>()
        val thisGame = liveGames.firstOrNull { event.channel.`is`(it.channelID) }

        //Setup, Join, & leaderboard
        when {
            //setup [@bots...]
            event.argList[0].matches("(?i)set(up)?") -> {
                if (thisGame != null) {
                    return event.respondThenDeleteBoth("There is already a game " +
                        "running in ${event.textChannel.asMention}. End that one " +
                        "before starting a new game.")
                }
                val decks = mutableListOf(DECK_STRD)
                var handSize = HAND_SIZE_DEF
                var winCond = WINS(5)
                val players = mutableListOf(event.author)
                players.addAll(event.message.mentionedUsers.filter { it.isBot })
                fun gameState(): MessageEmbed {
                    return makeEmbedBuilder("$displayName Setup", null, """
                        Hand Size: $handSize
                        Win Condition: ${winCond.score} ${winCond.name.toLowerCase()}
                        Decks: ${decks.joinToString { it.name }}
                        Players: ${players.joinToString { it.asMention }}

                        $HeavyPlusSign/$HeavyMinusSign to change hand size
                        $FirstPlaceMedal to change the win condition
                        $D to choose the decks to play with
                        $J to join/leave the game
                        ${if (players.filterNot { it.isBot }.size < MIN_PLAYERS) ""
                    else "$Beginner to start"}
                        """.trimIndent()).build()
                }
                SelectableEmbed(messageEmbed = gameState(), options = listOf(
                    HeavyPlusSign to plus@{ m, u ->
                        if (!u.`is`(event.author)) return@plus
                        if (handSize < HAND_SIZE_MAX) {
                            handSize++
                            m.editMessage(gameState()).queue()
                        } else event.respondThenDelete("Max hand size: $HAND_SIZE_MAX")
                    }, HeavyMinusSign to minus@{ m, u ->
                        if (!u.`is`(event.author)) return@minus
                        if (handSize > HAND_SIZE_MIN) {
                            handSize--
                            m.editMessage(gameState()).queue()
                        } else event.respondThenDelete("Min hand size: $HAND_SIZE_MIN")
                    }, FirstPlaceMedal to win@{ m, u ->
                        if (!u.`is`(event.author)) return@win
                        winCond = if (winCond == WINS) ROUNDS else WINS
                        event.respondThenDelete(
                            "Win Condition changed to ${winCond.name}. How many to win?:")
                        waitForMessage(event, 1, predicate = {
                            return@waitForMessage try {
                                it.message.contentDisplay.toInt()
                                true
                            } catch (e: NumberFormatException) {
                                event.respondThenDelete("Must be a number.")
                                it.message.delete().queue()
                                false
                            }
                        }) {
                            winCond(it.message.contentDisplay.toInt())
                            event.respondThenDelete(
                                "Win Condition set to ${winCond.score} ${winCond.name}")
                            it.message.delete().queue()
                            m.editMessage(gameState()).queue()
                        }
                    }, D to deck@{ m, u ->
                        if (!u.`is`(event.author)) return@deck
                        val fallBack = mutableListOf<CAHDeck>().apply { addAll(decks) }
                        decks.clear()
                        decks.add(DECK_STRD)
                        m.editMessage(gameState()).queue()
                        val items = (event.bot.cahInfo.decks + DECK_CUSTOM_PUB)
                            .distinctBy(CAHDeck::id).map {
                                "${it.name} (``${it.id}``)" to { _: Int, _: Message ->
                                    decks.add(it)
                                    m.editMessage(gameState()).queue()
                                }
                            }
                        SelectablePaginator(baseEmbed = makeEmbedBuilder(
                            "Select one or more decks", null, """
                                Select at least one deck to play with.
                                React with $X_Red to confirm.""".trimIndent())
                            .build(), items = items, itemsPerPage = -1, exitAction = {
                            //val wcS = decks.flatMap { it.whiteCards }.size
                            //val bcS = decks.flatMap { it.blackCards }.size
                            if (decks.isEmpty()) {
                                event.respondThenDelete(
                                    "you need to pick at least 1 deck")
                            } else {
                                it.delete().queue()
                                m.editMessage(gameState()).queue()
                            }
                        }, timeoutAction = {
                            decks.clear()
                            decks.addAll(fallBack)
                            it.delete().queue()
                            m.editMessage(gameState()).queue()
                        }).display(event.textChannel)
                    }, J to join@{ m, u ->
                        if (players.removeIf { it.id == u.id }) {
                            event.respondThenDelete("${u.asMention} left the game")
                            m.editMessage(gameState()).queue()
                        } else {
                            event.respondThenDelete("${u.asMention} joined the game!")
                            players.add(u)
                            m.editMessage(gameState()).queue()
                        }
                    }, Beginner to start@{ m, u ->
                        val cah = CardsAgainstHumanity(event, decks, handSize, winCond)
                        players.forEach { cah.addUser(it) }
                        if (!cah.startGame()) {
                            return@start event.respondThenDelete("The are either too " +
                                "few or too many players ($MIN_PLAYERS--$MAX_PLAYERS)")
                        }
                        event.bot.games.add(cah)
                        m.clearReactions().queue()
                    }
                )) { it.delete().queue() }.display(event.channel)
            }
            //join the game at any time
            event.argList[0].matches("(?i)j(oin)?") -> TODO(event)
            //start
            event.argList[0].matches("(?i)sta?rt") -> {
                TODO(event)
                track(this, event.bot, event.author, event.creationTime)
            }
            //Get your cards re-sent
            event.argList[0].matches("(?i)((my)?hand|(re)?send)") -> TODO(event)
            //See the current game's scores
            event.argList[0].matches("(?i)scores?") -> TODO(event)
            //TODO leave the current game add to docs
            event.argList[0].matches("(?i)setup") -> TODO(event)
            //End the current game by vote TODO add to docs
            event.argList[0].matches("(?i)setup") -> TODO(event)
            //"See server leaderboard", "``leaderboard``\n*Aliases:* LB, top",
            event.argList[0].matches("(?i)((l(eader)?b(oard)?)|top)") -> TODO(event)
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
            .addField("Game Setup", """``setup [@botPlayers...]``
                Any bot can be added to the game by ``@mentioning`` them
                ``join`` to join the game at any time""".trimIndent())
            .addField("Play Your Card(s)", "White Cards can be played by reacting to "
                + "the private message", true)
            .addField("Czar Pick Winning Card(s)", "React to the Black Card message",
                true)
            .addField("Get your cards re-sent", "``myhand``", true)
            .addField("See the current game's scores", "``scores``", true)
            .addField("See server leaderboard", "``leaderboard``\n*Aliases:* LB, top",
                true)
            .addField("Make a Deck", "``deck make [-public] <deck_name>``", true)
            .addField("View Custom Decks",
                """``deck see [-public or -all] [deck_names_no_spaces/IDs...]``
                    |``-public/-p`` to see a list of public, user-made decks
                    |``-all/-a`` to see decks made in this server and public ones
                    |*By default, only decks in this server will be shown*
                    |""".trimMargin(), true)
            .addField("Delete Deck or Card", "``deck delete <deck_id> /card_id/``", true)
            .setThumbnail(LINK_CAH_THUMBNAIL).build()
    }

}

/**
 * Sub command for [CAHDeck] actions
 * @author Jonathan Augustine
 * @since 2.2.1
 */
private class SubCmdDeck : WeebotCommand(
    "deck", "GAMECAHDECK", null, arrayOf("decks"),
    CAT_UNDER_CONSTRUCTION, "", guildOnly = true,
    cooldown = 60, cooldownScope = USER_SHARD
) {

    override fun execute(event: WeebotCommandEvent) {
        val gDecks = event.bot.cahInfo.decks
        when {
            event.argList.isEmpty() -> {
                event.respondThenDeleteBoth("Use ``make`` or ``view`` to do something.")
            }
            event.argList[0].matches("(?i)-{0,2}m(ake)?") -> {
                //make [-public] <deck_name>
                if (event.argList.size == 1) { //Use waiter to get name
                    event.respondThenDeleteBoth("What is the name of the new deck?", 360)
                    waitForMessage(event, 5, timeOutAction = {
                        event.respondThenDelete("Timed out. Please try again.")
                    }, predicate = check@{
                        when (validName(it, it.message.contentDisplay, event.bot)) {
                            0 -> {
                                event.respondThenDelete(
                                    "Name cannot contain emotes, please try again") {
                                    it.message.delete().queueIgnore(10)
                                }
                                false
                            }
                            -1 -> {
                                event.respondThenDelete("Name cannot have mentions, " +
                                    "please try again") {
                                    it.message.delete().queueIgnore(10)
                                }
                                false
                            }
                            -2 -> {
                                event.respondThenDelete("Name cannot be a number, " +
                                    "please try again") {
                                    it.message.delete().queueIgnore(10)
                                }
                                false
                            }
                            -3 -> {
                                event.respondThenDelete(
                                    "Name is too long, please try again") {
                                    it.message.delete().queueIgnore(10)
                                }
                                false
                            }
                            -4 -> {
                                event.respondThenDelete("There is already a deck " +
                                    "by that name in this server, please try again") {
                                    it.message.delete().queueIgnore(10)
                                }
                                false
                            }
                            else -> true
                        }
                    }) {
                        event.respondThenDelete(
                            "Allow all Weebot users to use this deck? (yes/no)", 360) {
                        }
                        waitForMessage(event, 5, timeOutAction = {
                            event.respondThenDelete("Timed out. Please try again.")
                        }, predicate = check@{ e2 ->
                            if (!e2.message.contentDisplay.matchesAny(REG_YES, REG_NO)) {
                                event.respondThenDelete("Please say ``yes`` or ``no``") {
                                    e2.message.delete().queueIgnore(10)
                                }
                                false
                            } else false
                        }) { e2 ->
                            val pub = e2.message.contentDisplay.matches(REG_YES)
                            val deck = CAHDeck(it.message.contentDisplay,
                                it.author.idLong, pub, event.creationTime)
                            gDecks.add(deck)
                            runBlocking { event.bot.cahInfo.save() }
                            sendNewDeck(event, deck)
                            track(this, event.bot, event.author, event.creationTime)
                        }
                    }
                } else {
                    var nameStart = 1
                    if (event.argList[1].matches(REG_HYPHEN + "p(ub(lic)?)?")) nameStart++
                    if (nameStart >= event.argList.size) {
                        return event.respondThenDeleteBoth("No name provided.", 30)
                    }
                    val name = event.argList.subList(nameStart).joinToString(" ")

                    when (validName(event.event, name, event.bot)) {
                        0 -> event.respondThenDeleteBoth(
                            "Name cannot contain emotes, " + "please try again", 30)
                        -1 -> event.respondThenDeleteBoth(
                            "Name cannot have mentions, " + "please try again", 30)
                        -2 -> event.respondThenDeleteBoth(
                            "Name cannot be a number, " + "please try again", 30)
                        -3 -> event.respondThenDeleteBoth(
                            "Name is too long, please try " + "again", 30)
                        -4 -> event.respondThenDeleteBoth(
                            "There is already a deck " + "by that name in this server, please try again",
                            30)
                        else -> {
                            if (gDecks.any { it.matches(name) })
                                return event.respondThenDeleteBoth(
                                    "There is already a deck "
                                        + "in this server by that name. Please try again.",
                                    30)
                            val deck = CAHDeck(name, event.author.idLong, nameStart == 2,
                                event.creationTime)
                            gDecks.add(deck)
                            sendNewDeck(event, deck)
                            track(this, event.bot, event.author, event.creationTime)
                        }
                    }

                }
            }
            event.argList[0].matches("(?i)-{0,2}(v(iew)?|se*)") -> {
                //view [-public or -all] [deck_names_no_space/IDs...]
                event.delete(1)
                var idDex = 1
                var decks: MutableList<CAHDeck> = when {
                    event.argList.size == 1 -> {
                        if (gDecks.isEmpty()) {
                            return event.respondThenDeleteBoth("There are no custom " +
                                "decks in ``${event.guild.name}``, you can " +
                                "make one with ``cah deck make`` and have fun!", 30)
                        } else gDecks
                    }
                    event.argList[1].matches(REG_HYPHEN + "p(ub(lic)?)?") -> {
                        idDex++
                        DECK_CUSTOM_PUB
                    }
                    event.argList[1].matches(REG_HYPHEN + "al*") -> {
                        idDex++
                        DECK_CUSTOM_PUB + gDecks
                    }
                    else -> {
                        if (gDecks.isEmpty()) {
                            return event.respondThenDeleteBoth("There are no custom " +
                                "decks in ``${event.guild.name}``, you can " +
                                "make one with ``cah deck make``", 30)
                        } else gDecks
                    }
                }.distinctBy { it.id }.toMutableList()
                if (idDex < event.argList.size) {
                    val idOrNames = event.argList.subList(idDex)
                        .map { it.toUpperCase().removePrefix(IDGEN_CAH.prefix) }
                    decks = decks.filter {
                        idOrNames.contains(it.id.removePrefix(IDGEN_CAH.prefix))
                            || idOrNames.any(it::matches)
                    }.toMutableList()
                }
                if (decks.isEmpty()) {
                    return event.respondThenDeleteBoth("I couldn't find any " +
                        "custom decks $FrowningFace. you can " +
                        "make one with ``cah deck make``", 30)
                } else if (decks.size == 1) return sendDeck(event, decks.first())
                SelectablePaginator(setOf(event.author), baseEmbed = makeEmbedBuilder(
                    "User Created Decks", null, "Select any deck to view or edit.")
                    .setThumbnail(LINK_CAH_THUMBNAIL).build(), itemsPerPage = -1,
                    bulkSkipNumber = decks.size / 5,
                    exitAction = { it.delete().queueIgnore(1) }, items = decks.map {
                        "${it.name} (${it.id.removePrefix(
                            IDGEN_CAH.prefix)})" to { _: Int, m: Message ->
                            sendDeck(event, it, null)
                        }
                    }).display(event.channel)
            }
            event.argList[0].matches("(?i)-{0,2}del+(ete*)?") -> {
                //delete <deck_id> /note_ids.../
                if (event.argList.size == 1) return event.respondThenDeleteBoth(
                    "No deck id provided. ``cah deck del <deck_id> /note_id/``")
                gDecks.firstOrNull {
                    it.id.removePrefix(IDGEN_CAH.prefix)
                        .equals(event.argList[1].removePrefix(IDGEN_CAH.prefix), true)
                }?.also { d: CAHDeck ->
                    if (event.argList.size == 2) {
                        if (!(event.author `is` d.authorID || event.member.isAdmin())) {
                            return event.respondThenDeleteBoth(
                                "You must be the deck author or an admin to do this.", 20)
                        }
                        event.respondThenDeleteBoth("Are you sure? (yes/no)", 40)
                        waitForMessage(event, 40, SECONDS, {
                            event.respondThenDelete("Timed out")
                        }, {
                            if (it.message.contentDisplay.matchesAny(REG_YES, REG_NO))
                                true
                            else {
                                event.respondThenDelete("Please say ``yes`` or ``no``")
                                false
                            }
                        }, {
                            gDecks.remove(d)
                            event.respondThenDelete("Deck Deleted")
                        })
                    } else if (!(event.author `is` d.authorID
                            || d.wrtRestrct.explicitlyAllows(event.member)
                            || event.member.isAdmin())) {
                        return event.respondThenDeleteBoth(
                            "You must be the deck author, editor, or an admin to do this.",
                            20)
                    }
                    d.whiteCards.firstOrNull {
                        it.id.removePrefix(IDGEN_CAH.prefix)
                            .equals(event.argList[2].removePrefix(IDGEN_CAH.prefix), true)
                    }?.also { wc ->
                        if (d.whiteCards.remove(wc)) {
                            event.reply("White Card ``${wc.id}`` deleted.")
                        } else event.replyError(GENERIC_ERR_MSG)
                    } ?: d.blackCards.firstOrNull {
                        it.id.equals(event.argList[1].removePrefix(IDGEN_CAH.prefix),
                            true)
                    }?.also { bc ->
                        if (d.blackCards.remove(bc)) {
                            event.reply("Black Card ``${bc.id}`` deleted.")
                        } else event.replyError(GENERIC_ERR_MSG)
                    } ?: event.replyError("Card not found")
                } ?: event.respondThenDeleteBoth(
                    "No deck with id ``${event.argList[1]}`` found")
            }
        }
    }

    private fun sendDeck(event: WeebotCommandEvent, deck: CAHDeck,
                         embed: EmbedBuilder? = null, message: Message? = null) {
        val foreign = event.bot.cahInfo.decks.none { it.id == deck.id }
        fun genEmbed() = if (foreign) deck.asEmbed(event.guild).addField("Actions", """
            ${if (event.author.`is`(deck.authorID)
            || deck.wrtRestrct.explicitlyAllows(event.member))
            """$BlackLargeSquare to write a Black Card
            $WhiteLargeSquare to write a White Card
            $Lock to change restrictions & publicity
            """.trimIndent() else ""}
            $B to view all Black Cards
            $W to view all White Cards
            """.trimIndent(), false).build()
        else (embed ?: deck.asEmbed(event.guild)).addField("Actions", """
            $BlackLargeSquare to write a Black Card
            $WhiteLargeSquare to write a White Card
            ${if (event.author.`is`(
                deck.authorID)) "$Lock to change restrictions & publicity"
        else ""}
            $B to view all Black Cards
            $W to view all White Cards
            ${if (event.author.`is`(
                deck.authorID)) "$Lock to change restrictions & publicity"
        else ""}""".trimIndent(), false).build()

        val e = genEmbed()

        val writers = mutableSetOf(event.author)
        val writerRoles = mutableSetOf<Role>()
        if (deck.wrtRestrct.isRestricted()) {
            writers.addAll(deck.wrtRestrct.allowedUsers.mapNotNull {
                event.guild.getMemberById(it).user
            })
            writerRoles.addAll(deck.wrtRestrct.allowedRoles.mapNotNull {
                event.guild.getRoleById(it)
            })
        }

        val items = mutableListOf<Pair<Emoji, (Message, User) -> Unit>>()
        if (event.author.`is`(deck.authorID)
            || deck.wrtRestrct.explicitlyAllows(event.member)
            || !(foreign || deck.wrtRestrct.isRestricted())) {
            items.addAll(listOf(BlackLargeSquare to { m: Message, _: User ->
                event.respondThenDelete(
                    "Write the text of the card, using ``[____]`` for blanks", 60)
                waitForMessage(event.guild, user = event.author, channel = event.channel,
                    predicate = {
                        when {
                            it.message.contentRaw.length > EMBED_MAX_TITLE -> {
                                event.respondThenDelete(
                                    "This card is too long, please try again.", 30) {
                                    it.message.delete().queueIgnore(30)
                                }
                                false
                            }
                            it.message.contentRaw.length < 10 -> {
                                event.respondThenDelete(
                                    "This card is too short, please try again.", 30) {
                                    it.message.delete().queueIgnore(30)
                                }
                                false
                            }
                            else -> true
                        }
                    }) { e1 ->
                    event.respondThenDelete(
                        "How many White Cards does this need?", 60)
                    waitForMessage(event.guild, user = event.author,
                        channel = event.channel,
                        predicate = {
                            try {
                                val i = it.message.contentRaw.toInt()
                                if (i in 1..HAND_SIZE_MAX) true else {
                                    throw NumberFormatException()
                                }
                            } catch (e: NumberFormatException) {
                                event.respondThenDelete(
                                    "Must be a number ``1`` to ``$HAND_SIZE_MAX``") {
                                    it.message.delete().queueIgnore(10)
                                }
                                false
                            }
                        },
                        action = { e2 ->
                            val b = BlackCard(e1.message.contentRaw
                                .replace(Regex("\\[(_{0,3}|_{5,})]"), "[____]"),
                                e2.message.contentRaw.toInt())
                            deck.blackCards.add(b)
                            event.respondThenDelete(
                                makeEmbedBuilder("New Black Card added",
                                    null, """*ID:* ``${b.id}``
                                *Text:* ${b.text}
                                *Pick:* ``${b.pick}``
                            """.trimIndent()).build(), 60)
                            m.editMessage(genEmbed()).queueIgnore()
                        })
                }
            }, WhiteLargeSquare to { m: Message, _: User ->
                event.respondThenDelete("Write the card:")
                waitForMessage(event, 3, predicate = {
                    when {
                        it.message.contentRaw.length > EMBED_MAX_TITLE -> {
                            event.respondThenDelete(
                                "This card is too long, please try again.", 30) {
                                it.message.delete().queueIgnore(30)
                            }
                            false
                        }
                        it.message.contentRaw.length < 10 -> {
                            event.respondThenDelete(
                                "This card is too short, please try again.", 30) {
                                it.message.delete().queueIgnore(30)
                            }
                            false
                        }
                        else -> true
                    }
                }) {
                    val w = WhiteCard(it.message.contentRaw)
                    deck.whiteCards.add(w)
                    event.respondThenDelete(makeEmbedBuilder("New WhiteCard!", null,
                        "``${w.id}``\n${w.text}").build(), 30) {
                        it.message.delete().queueIgnore(30)
                    }
                    m.editMessage(genEmbed()).queueIgnore()
                }
            }, Lock to { m: Message, _: User ->
                /*
                Publicity
                selectable paginator allowed members
                selectable paginator allowed roles
                allow any in guild to edit (clear restrictions)
                 */
                SelectableEmbed(writers, writerRoles, 4, MINUTES, false,
                    makeEmbedBuilder("Deck Settings", null, """
                        ${if (deck.public) Lock else GiftHeart} to change deck publicity
                        $Pencil to change allowed editors (roles & members)
                        ${if (deck.wrtRestrct.isRestricted())
                        "$Unlock to allow anyone to write" else ""}
                    """.trimIndent()).build(),
                    mutableListOf<Pair<Emoji, (Message, User) -> Unit>>(
                        (if (deck.public) Lock else GiftHeart) to lock@{ _, u ->
                            if (u.`is`(deck.authorID)
                                || event.guild.getMember(u)?.hasPerm(
                                    ADMINISTRATOR) == true) {
                                val st = "Deck:``${deck.name}`` has been set to "
                                deck.public = !deck.public
                                if (deck.public) {
                                    event.respondThenDelete(st + "public $GiftHeart")
                                } else event.respondThenDelete(st + "server-only $Lock")
                                m.editMessage(genEmbed()).queueIgnore()
                            } else event.respondThenDelete(
                                "Only the deck author or an Admin can make this change")
                        }, Pencil to edit@{ m2, u: User ->
                            if (!u.`is`(deck.authorID)
                                && event.guild.getMember(u)?.hasPerm(
                                    ADMINISTRATOR) != true)
                                return@edit event.respondThenDelete(
                                    "Only the deck author or an Admin can make this change")
                            m2.clearReactions().queue()
                            val chosen = mutableListOf<ISnowflake>()
                            val items2 = event.guild.roles.filterNot { r ->
                                event.guild.getMembersWithRoles(r).all { it.user.isBot }
                            }.map {
                                it.asMention to { _: Int, _: Message ->
                                    chosen.add(it).unit
                                }
                            } + event.guild.members.filterNot { it.user.isBot }.map {
                                it.asMention to { _: Int, _: Message ->
                                    chosen.add(it.user).unit
                                }
                            }
                            DynamicSelectablePaginator(setOf(u), items = items2,
                                itemsPerPage = -1, exitAction = {
                                    deck.wrtRestrct.allow(chosen)
                                    it.clearReactions().queue()
                                    m.delete().queue()
                                    sendDeck(event, deck, null, it)
                                }, embed = {
                                    chosen.sortByDescending { it is User }
                                    val s = chosen.joinToString(", ") {
                                        if (it is User) it.asMention
                                        else (it as Role).asMention
                                    }
                                    makeEmbedBuilder("Allowed Editors", null,
                                        "All roles and users allowed to add cards "
                                            + "to\n**${deck.name}**:\n$s\n" +
                                            "react with $X_Red to confirm.")
                                        .setThumbnail(LINK_CAH_THUMBNAIL)
                                }).displayOrDefault(m2, event.channel)
                        }).apply {
                        if (deck.wrtRestrct.isRestricted())
                            add(Unlock to open@{ _, u: User ->
                                if (u.`is`(deck.authorID)
                                    || event.guild.getMember(u)?.hasPerm(
                                        ADMINISTRATOR) == true) {
                                    deck.wrtRestrct.clear()
                                    event.respondThenDelete("All restrictions cleared. " +
                                        "Anyone in ${event.guild.name} can add cards.")
                                    m.editMessage(genEmbed()).queueIgnore()
                                } else event.respondThenDelete(
                                    "Only the deck author or an Admin can make this change")
                            })
                    }) {
                    it.delete().queue({}) { _ -> it.clearReactions().queue() }
                }.displayOrDefault(message, event.channel)
            }))
        }
        items.addAll(listOf(
            B to paginateBlackCards(event, deck), W to paginateWhiteCards(event, deck)))

        SelectableEmbed(writers, writerRoles, 5, MINUTES, false, e, items) {
            it.clearReactions().queue()
        }.displayOrDefault(message, event.channel)

    }

    private fun paginateBlackCards(event: WeebotCommandEvent,
                                   deck: CAHDeck) = { _: Message, _: User ->
        if (deck.blackCards.isEmpty()) {
            event.reply("There are no black cards in this deck yet!") {
                it.delete().queueIgnore(10)
            }
        } else strdPaginator.setItemsPerPage(6).setText("${deck.name} Black Cards")
            .useNumberedItems(true).addItems(*deck.blackCards.map {
                "${it.text} (Pick: ${it.pick}, ``${it.id}``)"
            }.toTypedArray()).setFinalAction { it.clearReactions().queueIgnore(1) }
            .build().display(event.channel)
    }

    private fun paginateWhiteCards(event: WeebotCommandEvent,
                                   deck: CAHDeck) = { _: Message, _: User ->
        if (deck.whiteCards.isEmpty()) {
            event.reply("There are no white cards in this deck yet!") {
                it.delete().queueIgnore(10)
            }
        } else strdPaginator.setItemsPerPage(6).setText("${deck.name} Black Cards")
            .useNumberedItems(true).addItems(*deck.whiteCards.map {
                "${it.text} (``${it.id}``)"
            }.toTypedArray()).setFinalAction { it.clearReactions().queueIgnore(1) }
            .build().display(event.channel)
    }

    /** Builds an embed for [CAHDeck] creation then uses [sendDeck] to send */
    private fun sendNewDeck(event: WeebotCommandEvent, deck: CAHDeck) {
        sendDeck(event, deck, deck.asEmbed(event.guild).addField("What Next?", """
            Right now this deck is empty $FrowningFace, but you can start adding cards
            to this deck by reacting to this message or by using this command:
            ```css
            cah deck view ${deck.id.removePrefix(IDGEN_CAH.prefix)}
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
            weebot.cahInfo.decks.any { it.matches(name) } -> -4
            else -> try {
                name.toDouble()
                -2
            } catch (e: NumberFormatException) {
                1
            }
        }
    }
}

/**
 * A [WeebotCommand] to inspect any [BlackCard] or [WhiteCard] with any reports.
 *
 * @author Jonathan Augustine
 * @since 2.2.1
 */
private class SubCmdSeeReports : WeebotCommand(
    "reports", "GAMECAHREPORTS", null, arrayOf("report", "rep"),
    CAT_DEV, "", ownerOnly = true, hidden = true
) {
    override fun execute(event: WeebotCommandEvent) {
        val whiteCards = (DECK_CUSTOM_PUB + DECK_STRD)
            .flatMap { it.whiteCards }.filter { it.reports > 0 }
            .sortedByDescending { it.reports }.map {
                "[R: ${it.reports}] W:``${it.id}`` ~ ${it.text}" to { _: Int, m: Message ->
                    TODO(event)
                }
            }
            .toList()

        val blackCards = (DECK_CUSTOM_PUB + DECK_STRD)
            .flatMap { it.blackCards }.filter { it.reports > 0 }
            .sortedByDescending { it.reports }.map {
                "``[R: ${it.reports}] B:${it.id} - Picks ${it.pick}`` ~ ${it.text}" to
                    { _: Int, m: Message ->
                        TODO(event)
                    }
            }

        if (blackCards.isEmpty() && whiteCards.isEmpty()) {
            return event.reply("No reports to view")
        }

        SelectablePaginator(setOf(event.author), timeout = -1, itemsPerPage = -1,
            title = "Reported CAH Cards",
            description = "All CAH cards with at least ``1`` report.",
            items = blackCards + whiteCards).display(event.channel)

    }
}
