package com.ampro.weebot.database

import com.ampro.weebot.bot.GlobalWeebot
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.developer.WeebotSuggestionCommand.Suggestion
import com.ampro.weebot.database.DatabaseManager.DIR_DBS
import com.ampro.weebot.util.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.dv8tion.jda.core.entities.User
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** Array of registered developer Discord IDs  */
internal val DEV_IDS = mutableListOf(
    139167730237571072L /*JONO*/, 186130584693637131L /*DERNST*/
)

/**
 * Removes a developer by ID.
 *
 * @param id long user ID
 * @return The removed id
 */
@Synchronized
fun removeDeveloper(id: Long): Long = DEV_IDS.removeAt(DEV_IDS.indexOf(id))

/**
 * Check if user ID matches a Developer ID.
 *
 * @param id long ID to check
 * @return true if the user ID is a dev.
 */
fun checkDevID(id: Long): Boolean = Database.getDevelopers().contains(id)


/**
 * TODO
 */
data class PremiumUser(val ID: Long)

lateinit var DAO : Dao

data class Dao(var initTime: String = NOW_FILE) {

    /**
     * Map of all suggestions given through
     * [com.ampro.weebot.commands.developer.WeebotSuggestionCommand]
     */
    val SUGGESTIONS = ConcurrentHashMap<Long, MutableList<Suggestion>>()

    val GLOBAL_WEEBOT = GlobalWeebot()

    /** All Weebots currently in circulation, mapped to their Guild's ID  */
    val WEEBOTS = ConcurrentHashMap<Long, Weebot>()

    private val PREMIUM_USERS = ConcurrentHashMap<Long, PremiumUser>()

    /** Build an empty `Database`. */
    init {
        WEEBOTS.putIfAbsent(0L, GLOBAL_WEEBOT)
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
            if (!DIR_DBS.mkdir()) {
                //System.err.println("\tDirectory not created.");
            }
            if (!SAVE.createNewFile()) {
                //System.err.println("\tFile not created");
            }
        } catch (e: IOException) {
            System.err.println("IOException while creating Database file.")
            e.printStackTrace()
            return -1
        }

        try {
            FileWriter(SAVE).use { writer -> GSON.toJson(this, writer) }
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
            if (!DIR_DBS.exists() && DIR_DBS.mkdirs()) {
                Logger.derr("[Database Manager] Failed to generate database dir!")
                return -1
            } else if (!BKUP.exists() && !BKUP.createNewFile()) {
                Logger.derr("[Database Manager] Failed to generate database backup!")
                return -1
            }
        } catch (e: IOException) {
            Logger.derr("IOException while creating Database backup file.")
            e.printStackTrace()
            return -1
        }

        if (!corruptBackupWriteCheck(this)) return -1
        try {
            FileWriter(BKUP).use { writer ->
                GSON.toJson(this, writer)
                return 1
            }
        } catch (e: FileNotFoundException) {
            Logger.derr("File not found while writing gson backup to file.")
            e.printStackTrace()
            return -1
        } catch (e: IOException) {
            Logger.derr("IOException while writing gson backup to file.")
            e.printStackTrace()
            return -1
        }

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
    fun premiumUsers() = Collections.unmodifiableMap(PREMIUM_USERS)

    @Synchronized
    fun addPremiumUser(user: User,
                       status: PremiumUser = PremiumUser(user.idLong)): Boolean {
        return PREMIUM_USERS.putIfAbsent(user.idLong, status) == null
    }

    @Synchronized
    fun removePremiumUser(user: User) = PREMIUM_USERS.remove(user.idLong) != null

    @Synchronized
    fun addSuggestion(suggestion: Suggestion) {
        SUGGESTIONS.putIfAbsent(suggestion.authorID, mutableListOf(suggestion))
            ?.add(suggestion)
    }

    @Synchronized
    fun removeSuggestion(suggestion: Suggestion) = SUGGESTIONS.remove(suggestion.authorID)

    /**
     * Remove all suggestions from the given user
     *
     * @return A list of the removed suggestions
     */
    @Synchronized
    fun clearUserSuggestions(user: User): List<Suggestion> =
            SUGGESTIONS.remove(user.idLong) ?: listOf()

    override fun toString(): String {
        var out = ""
        out += "["

        for ((key, bot) in this.WEEBOTS) {

            out + ("[" + key + "," + bot.nickname + "]")
        }

        out += "]"

        return out
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
        FileReader(SAVE).use { reader ->
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
        FileReader(BKUP).use { bKreader ->
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
    FileReader(BKBK).use { reader ->
        GSON.fromJson(reader, Database::class.java)
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
    FileWriter(BKBK).use { writer -> GSON.toJson(dao, writer) }
    true
} catch (e: JsonSyntaxException) {
    e.printStackTrace()
    false
} catch (e: IOException) {
    BKBK.delete()
    true
}


