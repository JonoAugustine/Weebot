/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.database

import com.ampro.weebot.*
import com.ampro.weebot.commands.developer.CmdSuggestion
import com.ampro.weebot.commands.developer.Suggestion
import com.ampro.weebot.database.constants.CLIENT_WBT
import com.ampro.weebot.database.constants.KEY_DISCORD_BOTS_ORG
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.jagrosh.jdautilities.command.Command
import kotlinx.coroutines.Job
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.discordbots.api.client.DiscordBotListAPI
import java.io.*
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.reflect.KClass

/* ************************
     Discord Bot List API
 *************************/

val DISCORD_BOTLIST_API: DiscordBotListAPI = DiscordBotListAPI.Builder()
    .token(KEY_DISCORD_BOTS_ORG).botId(CLIENT_WBT.toString()).build()

lateinit var BOT_LIST_API_UPDATERS: Job

infix fun User.hasVoted(handler: (Boolean, Throwable) -> Boolean) {
    DISCORD_BOTLIST_API.hasVoted(this.id).handleAsync(handler)
}

/* ************************
        Database
 *************************/

lateinit var DAO : Dao
lateinit var STAT: Statistics

/**
 * Get a guild matching the ID given.
 *
 * @param id long ID
 * @return requested Guild <br></br> null if not found.
 */
fun getGuild(id: Long): Guild? {
    return JDA_SHARD_MNGR.guilds.find { it.idLong == id }
}

fun getUser(id: Long): User? { return JDA_SHARD_MNGR.getUserById(id) }

fun getWeebot(guildID: Long): Weebot? { return DAO.WEEBOTS[guildID] }

fun getWeebotOrNew(guild: Guild) = getWeebotOrNew(guild.idLong)

fun getWeebotOrNew(guildID: Long) : Weebot {
    return getWeebot(guildID) ?: run {
        val b = Weebot(guildID)
        DAO.addBot(b)
        b
    }
}

val Guild.bot: Weebot
    get() = getWeebotOrNew(this)

infix fun User.isBlocked(klass: Class<out WeebotCommand>)
        = DAO.blockedUsers[klass]?.contains(idLong) == true

infix fun WeebotCommand.blocks(user: User)
        = DAO.blockedUsers[this.javaClass]?.contains(user.idLong) == true

/**
 * A class to track the bot's usage.
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
data class Statistics(val initTime: OffsetDateTime = NOW()) {

    /**
     * A data class to hold tracked information about the state of a Weebot.
     *
     * @param settings The weebot's current settings
     * @param init The init time of the bot (for finding age)
     * @param passivesEnabled The number of [IPassive]s enabled
     * @param disabledCommands A list of [Command]s disabled by this bot
     * @param guildSize The size of the host giuld
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class WeebotInfo(weebot: Weebot) {
        val settings: WeebotSettings = weebot.settings
        val init: OffsetDateTime        = weebot.initDate
        val guildSize: Int              = getGuild(weebot.guildID)?.size ?: -1
        val percentHuman: Double        = run {
            val guild = getGuild(weebot.guildID)
            guild?.trueSize?.div(guild.size.toDouble()) ?: 1.0
        }
    }

    /**
     * A data class to hold tracked information about a User.
     *
     * @param mutualGuilds How many guilds does this user share with Weebot
     * @param
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    data class UserInfo(val mutualGuilds: Int) {
        constructor(user: User) : this(user.mutualGuilds.size)
    }

    /**
     * A Unit of a [Command]'s usage, with information about the guild, invoking
     * user and corresponding Weebot.
     *
     * @param guildID
     * @param weebotInfo
     * @param userInfo The [UserInfo] of the invoker User
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    data class CommandUsageEvent(val guildID: Long, val weebotInfo: WeebotInfo,
                                 val userInfo: UserInfo, val time: OffsetDateTime?)

    /**
     * A map of Command names to their useage statistics
     */
    val commandUsage: ConcurrentHashMap<KClass<out WeebotCommand>,
            MutableList<CommandUsageEvent>> = ConcurrentHashMap()

    /**
     * @param command
     * @param weebot
     * @param user
     */
    fun track(command: WeebotCommand, weebot: Weebot, user: User, time: OffsetDateTime) {
        if (/*TODO weebot.settings.trackingEnabled &&*/ weebot !is GlobalWeebot) {
            commandUsage.getOrPut(command::class) { mutableListOf() }
                .add(CommandUsageEvent(weebot.guildID, WeebotInfo(weebot),
                    UserInfo(user), time))
        }
    }

}

fun List<Statistics.CommandUsageEvent>.summarize() : String {
    val sizes = map { it.weebotInfo.guildSize }.sorted()
    val medSize: Double = if (sizes.size % 2 == 0)
        (sizes[sizes.size/2] + sizes[sizes.size/2 - 1])/2.0
    else sizes[sizes.size/2].toDouble()

    val pHumans = map { it.weebotInfo.percentHuman }.sorted()
    val medPercentHuman: Double = ceil((if (pHumans.size % 2 == 0)
        (pHumans[pHumans.size/2] + pHumans[pHumans.size/2 - 1])/2.0
    else pHumans[sizes.size/2]) * 100.0)

    val ages = map { it.weebotInfo.init.until(NOW(), ChronoUnit.MINUTES) }
    val medAge: Long = if (ages.size % 2 == 0)
        (ages[ages.size/2] + ages[ages.size/2 - 1])/2
    else ages[sizes.size/2]

    val hours = mapNotNull { it.time?.hour }
    val medHours = if (hours.size % 2 == 0)
        (hours[hours.size/2] + hours[hours.size/2 - 1])/2
    else hours[hours.size/2]

    return """
        Data Set Size: $size
        Median Size: $medSize
        Median Time (hr): $medHours
        Median Percent Human: $medPercentHuman
        Median Age: ${(medAge * 60).formatTime()}
        """.trimIndent()
}

data class PremiumUser(val userId: Long) { val joinDate = NOW() }

/**
 * A database for storing all the information about the Weebot program
 * between downtime.
 *
 * TODO: Put each Guild/Weebot into it's own file?
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class Dao {

    @SerializedName("initTime")
    val initTime: String = NOW_STR_FILE

    /**
     * List of all suggestions given through
     * [com.ampro.weebot.commands.developer.WeebotSuggestionCommand]
     */
    val suggestions = mutableListOf<Suggestion>()

    val GLOBAL_WEEBOT = GlobalWeebot()

    /** All Weebots currently in circulation, mapped to their Guild's ID  */
    val WEEBOTS = ConcurrentHashMap<Long, Weebot>()

    private val PREMIUM_USERS = ConcurrentHashMap<Long, PremiumUser>()

    val blockedUsers = ConcurrentHashMap<Class<out WeebotCommand>, MutableList<Long>>(
        mapOf(CmdSuggestion::class.java to mutableListOf(383656411789524995))
    )

    /** Build an empty `Database`. */
    init {
        WEEBOTS.putIfAbsent(-1L, GLOBAL_WEEBOT)
    }

    /**
     * Save the Database to file in the format:
     * `database.wbot`
     *
     * @return 1 if the file was saved.
     * -1 if an IO error occurred.
     * -2 if a Gson exception was thrown
     */
    @Synchronized
    fun save(): Int {
        if (!corruptBackupReadCheck()) return -2
        try {
            if (!DIR_DAO.mkdir()) {
                //System.err.println("\tDirectory not created.");
            }
            if (!DAO_SAVE.createNewFile()) {
                //System.err.println("\tFile not created");
            }
        } catch (e: IOException) {
            System.err.println("IOException while creating Database file.")
            e.printStackTrace()
            return -1
        }

        try {
            FileWriter(DAO_SAVE).use { writer -> GSON.toJson(this, writer) }
        } catch (e: FileNotFoundException) {
            System.err.println("File not found while writing gson to file.")
            e.printStackTrace()
            return -1
        } catch (e: IOException) {
            System.err.println("IOException while writing gson to file.")
            e.printStackTrace()
            return -1
        }

        return 1
    }

    /**
     * Save a backup of the database.
     * @param database
     * @return -1 if any error prevented the database to be backed up.
     */
    @Synchronized
    fun backUp(): Int {
        try {
            if (!DIR_DAO.exists() && DIR_DAO.mkdirs()) {
                MLOG.elog(Dao::class, "Failed to generate database dir!")
                return -1
            } else if (!DAO_BKUP.exists() && !DAO_BKUP.createNewFile()) {
                MLOG.elog(Dao::class, "Failed to generate database backup!")
                return -1
            }
        } catch (e: IOException) {
            MLOG.elog(Dao::class, "IOException while creating Database backup file.")
            e.printStackTrace()
            return -1
        }

        if (!corruptBackupWriteCheck(this)) return -1
        try {
            FileWriter(DAO_BKUP).use { writer: FileWriter ->
                GSON.toJson(this, writer)
                return@use 1
            }
        } catch (e: FileNotFoundException) {
            MLOG.elog(Dao::class, "File not found while writing gson backup to file.")
            e.printStackTrace()
            return -1
        } catch (e: IOException) {
            MLOG.elog(Dao::class, "IOException while writing gson backup to file.")
            e.printStackTrace()
            return -1
        }
        return 1
    }

    /**
     * Adds a new `Weebot` to the `Database`.
     * Does nothing if the link already exists.
     *
     * @param bot The bot
     * @return True if the bot was added
     */
    fun addBot(bot: Weebot) = this.WEEBOTS.putIfAbsent(bot.guildID, bot) == null

    /**
     * Remove a Weebot from the database.
     * @param bot Weebot to remove.
     * @return The removed Weebot.
     */
    @Synchronized
    fun removeBot(bot: Weebot): Weebot? = this.WEEBOTS.remove(bot.guildID)

    /**
     * Remove a bot from the database.
     *
     * @param id The hosting Guild ID
     * @return The removed Weebot.
     */
    @Synchronized
    fun removeBot(id: Long): Weebot? = this.WEEBOTS.remove(id)

    @Synchronized
    fun isPremium(user: User): Boolean = PREMIUM_USERS.contains(user.idLong)

    @Synchronized
    fun isPremium(userId: Long?): Boolean = PREMIUM_USERS.contains(userId)

    @Synchronized
    fun premiumUsers() = PREMIUM_USERS.toMap()

    @Synchronized
    fun updatePremiumUsers() { TODO() }

    @Synchronized
    fun addPremiumUser(user: User,
                       status: PremiumUser = PremiumUser(user.idLong)): Boolean {
        return PREMIUM_USERS.putIfAbsent(user.idLong, status) == null
    }

    @Synchronized
    fun removePremiumUser(user: User) = PREMIUM_USERS.remove(user.idLong) != null

    override fun toString(): String {
        val out = StringBuilder("[")
        this.WEEBOTS.forEach { key, bot ->
            out.append("[" + key + "," + bot.settings.nickname + "]")
        }
        return out.append("]").toString()
    }

}

/**
 * Loads database. If main database does not match the backup, return the backup.
 * @return Database in format database.wbot
 * null if database not found.
 */
@Synchronized
fun loadDao(): Dao? {
    val f = "[Database Manager]"
    var out: Dao? = null
    var bk: Dao? = null
    try {
        FileReader(DAO_SAVE).use { reader: FileReader ->
            out = GSON.fromJson(reader, Dao::class.java)
        }
    } catch (e: FileNotFoundException) {
        System.err.println(
                f + "Unable to locate database.wbot.\n\tAttempting to loadDao backup file..."
        )
    } catch (e: IOException) {
        System.err.println(f + "IOException while reading gson from file.")
        e.printStackTrace()
    }

    try {
        FileReader(DAO_BKUP).use { bKreader: FileReader ->
            bk = GSON.fromJson(bKreader, Dao::class.java)
        }
    } catch (e: FileNotFoundException) {
        System.err.println("$f\t\tUnable to locate databseBK.wbot.")
        //e.printStackTrace();
        //e2.printStackTrace();
    } catch (e: Exception) {
        System.err.println("$f\tException while reading gson from backup file.")
        e.printStackTrace()
    }

    return if (out === bk) out else if (bk == null) out else bk

}

/**
 * Attempt to loadDao the backup file. If any [Gson] exceptions are thrown
 * while the file is found, return false.
 *
 * @return `false` if gson fails to loadDao the backup.
 */
private fun corruptBackupReadCheck(): Boolean = try {
    FileReader(DAO_BKBK).use { reader: FileReader ->
        GSON.fromJson(reader, Dao::class.java)
    }
    true
} catch (e: JsonSyntaxException) {
    e.printStackTrace()
    false
} catch (e: IOException) {
    true
}

/**
 * Attempt to loadDao the backup file. If any [Gson] exceptions are thrown
 * while the file is found, return false.
 *
 * @return `false` if gson fails to loadDao the backup.
 */
private fun corruptBackupWriteCheck(dao: Dao): Boolean = try {
    FileWriter(DAO_BKBK).use { writer: FileWriter -> GSON.toJson(dao, writer) }
    true
} catch (e: JsonSyntaxException) {
    e.printStackTrace()
    false
} catch (e: IOException) {
    DAO_BKBK.delete()
    true
}

