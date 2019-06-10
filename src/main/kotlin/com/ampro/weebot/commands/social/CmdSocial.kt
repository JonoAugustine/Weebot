package com.ampro.weebot.commands.social

import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.CAT_SOC
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.social.GuildSocialSettings.CurrencySettings
import com.ampro.weebot.commands.social.GuildSocialSettings.LevelSettings
import com.ampro.weebot.commands.social.GuildSocialSettings.ProfileSettings
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.track
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.util.NOW
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.Permission.MESSAGE_READ
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

//TODO Still like most of it damn

/**
 *
 * @param userID The ID of the [User]
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class UserProfile(val userID: Long) {
    val initDate = NOW()

    /* ******************
            Currency
     ********************/

    /** How much Currency the user has */
    var wallet: Double = 0.0

    /** A history of the user's currency transactions */
    val transactionHistory = mutableListOf<String>()

    /* ********************
            Levels/Ranks
     *********************/

    /** The amount of experience points the user has */
    var exp: Long = 0L

    /** The current [LevelRank] of the user (nullable) */
    var rank: LevelRank? = null

}

/**
 * A data class representing a Level/Rank, with a required
 *
 * @param name The name of the rank
 * @param expReq The experience points required to obtain this level
 * @param roleID The ID of a linked guild Role (nullable)
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
data class LevelRank(var name: String, var expReq: Long, var roleID: Long?)

/**
 * A data class of a guild's social settings: [CurrencySettings], [LevelSettings],
 * and [ProfileSettings].
 *
 * @param guildID The ID of the host Guild
 * @param currency The [CurrencySettings] of the guild (nullable)
 * @param levels The [LevelSettings] of the guild (nullable)
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
data class GuildSocialSettings(val guildID: Long, var profiles: ProfileSettings?,
                               var currency: CurrencySettings?, var levels: LevelSettings?) {

    /**
     * @param autoCreate Whether user's should be given a [UserProfile] on join.
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    inner class ProfileSettings(var autoCreate: Boolean) : IPassive {
        var dead = false
        override fun dead() = dead

        override fun accept(bot: Weebot, event: Event) {
            if (event is GuildMemberJoinEvent && autoCreate) {
                userProfiles[event.user.idLong] = UserProfile(event.user.idLong)
            } else if (event is GuildMemberLeaveEvent) {
                userProfiles.remove(event.user.idLong)
            }
        }
    }

    /**
     * A class holding settings for a guild's social Currency
     *
     * @param name The name of the Currency
     * @param cooldown How long between new messages to wait b4 giving reward
     * @param rewardRange The range from which to get a random reward from
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    inner class CurrencySettings(var name: String, var cooldown: Long = 180L,
                                var rewardRange: IntRange = IntRange(1, 5))
        : IPassive {

        var dead: Boolean = false
        override fun dead() = dead

        /** A map of userID -> time of last message sent */
        val coolDowns = ConcurrentHashMap<Long, OffsetDateTime>()

        override fun accept(bot: Weebot, event: Event) {
            if (event !is GuildMessageReceivedEvent) return

        }

    }

    /**
     * A class with settings for a guild's leveling/ranks system.
     *
     * @param levels an ordered list of [LevelRank]s
     * @param cooldown How long between new messages to wait b4 giving reward
     * @param rewardRange The range from which to get a random reward from
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    inner class LevelSettings(val levels: MutableList<LevelRank>, var cooldown: Long,
                             var rewardRange: IntRange)

    val userProfiles = ConcurrentHashMap<Long, UserProfile>()
}


class CmdSocial : WeebotCommand(
    "social", "SOCIAL", null, arrayOf(),
    CAT_SOC, "Control Social features like Currency, Profiles, & Levels",
    HelpBiConsumerBuilder("Social Features", false)
        .setDescription("Control Social features like Currency, Profiles, & Levels")
        .build(),
    true, cooldown = 30,
    userPerms = arrayOf(ADMINISTRATOR), botPerms = arrayOf(MESSAGE_READ)
) {
    override fun execute(event: CommandEvent) {
        //TODO
        track(this, event.guild.bot, event.author, event.creationTime)
    }
}
