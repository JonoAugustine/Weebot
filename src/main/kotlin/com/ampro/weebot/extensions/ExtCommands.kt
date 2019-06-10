/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.ampro.weebot.CMD_CLIENT
import com.ampro.weebot.JDA_SHARD_MNGR
import com.ampro.weebot.MLOG
import com.ampro.weebot.SELF
import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.COMMANDS
import com.ampro.weebot.database.DISCORD_BOTLIST_API
import com.ampro.weebot.database.GLOBAL_WEEBOT
import com.ampro.weebot.database.allows
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.constants.DEV_IDS
import com.ampro.weebot.database.constants.isDev
import com.ampro.weebot.util.Emoji.Warning
import com.ampro.weebot.util.Emoji.WhiteCheckMark
import com.ampro.weebot.util.Emoji.X_Red
import com.ampro.weebot.util.NOW
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER
import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.command.CommandListener
import com.jagrosh.jdautilities.command.GuildSettingsManager
import jdk.nashorn.internal.ir.annotations.Ignore
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Game.listening
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.SECONDS
import java.util.function.BiConsumer
import java.util.function.Function

/** @return the string arguments of the message split into a [List]. Does NOT have the
 * command call in it */
fun CommandEvent.splitArgs(): List<String> = this.args.split("\\s+".toRegex())
    .filter { it.isNotBlank() }

/** @return The string used to invoke this command (i.e. the first string of the message */
fun CommandEvent.getInvocation(): String = this.message.contentStripped
    .removePrefix("w!").removePrefix("\\").split(" ")[0]

/**
 * Respond to the [CommandEvent] then delete the message after [delay] seconds
 */
fun CommandEvent.respondThenDelete(message: String, delay: Long = 10L,
                                   success: () -> Unit = {})
        = reply("*${message.trim()}*") {
    it.delete().reason(message).queueAfter(delay, SECONDS) {success()}
}

/**
 * Respond to the [CommandEvent] then delete the message after [delay] seconds
 */
fun CommandEvent.respondThenDelete(embed: MessageEmbed, delay: Long = 10L,
                                   success: () -> Unit = {})
        = reply(embed) { it.delete().queueAfter(delay, SECONDS) {success()} }


/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete (default 10)
 */
fun CommandEvent.respondThenDeleteBoth(reason: String, delay: Long = 10L) {
    this.reply("*${reason.trim()}*") { response ->
        if (this.privateChannel != null) {
            response.delete().reason(reason).queueAfter(delay, SECONDS)
        } else response.delete().reason(reason).queueAfter(delay, SECONDS) {
            event.message.delete().reason(reason).queueIgnore()
        }
    }
}

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete
 */
fun CommandEvent.respondThenDeleteBoth(reason: MessageEmbed, delay: Long = 10L) {
    this.reply(reason) { response ->
        if (this.privateChannel != null) {
            response.delete().queueAfter(delay, SECONDS)
        } else {
            event.message.delete().queueAfter(delay, SECONDS) {
                response.delete().queue()
            }
        }
    }
}

/**
 * Delete the [Message] and an event
 *
 * @param reason The message to send
 * @param delay The delay in seconds
 */
fun CommandEvent.delete(delay: Long = 0L) {
    if (!isFromType(PRIVATE)) {
        this.message.delete().queueAfter(delay, SECONDS)
    }
}


/**
 * A wrapper class for [Command] holding functions used by Weebots
 *
 * @param name Name of the Command
 * @param aliases Alternative names for the command
 * @property permaID The Command ID that wont change with Command-name changes
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
abstract class WeebotCommand(
    name: String,
    val permaID: String,
    val displayName: String?,
    aliases: Array<String>,
    category: Category,
    help: String,
    helpBiConsumer: BiConsumer<CommandEvent, Command>? = null,
    guildOnly: Boolean = false,
    ownerOnly: Boolean = false,
    hidden: Boolean = false,
    children: Array<out Command>? = emptyArray(),
    requiredRole: String? = null,
    cooldown: Int = 0,
    cooldownScope: CooldownScope = USER,
    userPerms: Array<Permission> = emptyArray(),
    botPerms: Array<Permission> = emptyArray()
) : Command() {

    init {
        super.name = name
        super.aliases = aliases
        super.category = category
        super.help = help
        super.helpBiConsumer = helpBiConsumer
        super.hidden = hidden
        super.guildOnly = guildOnly
        super.requiredRole = requiredRole
        super.ownerCommand = ownerOnly
        super.cooldown = cooldown
        super.cooldownScope = cooldownScope
        super.userPermissions = userPerms
        super.botPermissions = botPerms
        super.children = children
    }

    /**
     * A Builder class to make making Help [BiConsumer] embeds a lot simpler
     *
     * @param title The title of the embed
     * @param description
     * @param fields Each successive field in order
     * @param sendDM Whether to send it in a private DM (default = true)
     * @param thumbnailURL default = blank
     * @param imageURL default = blank
     * @author Jonathan Augustine
     * @since 2.0
     */
    open class HelpBiConsumerBuilder() {
        constructor(title: String) : this(title, true)
        constructor(title: String, description: String, withGuide: Boolean = true)
                : this(title, false) { this.description.append(description) }
        constructor(title: String, withGuide: Boolean) : this() {
            embedBuilder.setTitle(title)
            if (withGuide) embedBuilder.addField(guide)
        }

        companion object {
            val guide = Field("Guide",
                        "<required> , [optional], <[at][least][one]..>, /situational/",
                false)
        }

        var description = StringBuilder()
        var sendDM: Boolean = true

        val embedBuilder: EmbedBuilder = strdEmbedBuilder

        fun build(): BiConsumer<CommandEvent, Command> {
            return BiConsumer { event, _ ->
                embedBuilder.setDescription(description.toString())
                if (sendDM) event.replyInDm(embedBuilder.build())
                else event.reply(embedBuilder.build())
            }
        }

        fun build(success: (Message) -> Unit) : BiConsumer<CommandEvent, Command> {
            return BiConsumer { event, _ ->
                embedBuilder.setDescription(description.toString())
                if (sendDM) event.replyInDm(embedBuilder.build(), success)
                else event.reply(embedBuilder.build(), success)
            }
        }

        fun setTitle(title: String, url: String = "") : HelpBiConsumerBuilder {
            embedBuilder.setTitle(title, url)
            return this
        }

        fun setDescription(desc: String) : HelpBiConsumerBuilder {
            this.description = StringBuilder(desc)
            return this
        }

        /** Append a string to the description */
        fun addToDesc(string: String) : HelpBiConsumerBuilder {
            this.description.append(string)
            return this
        }

        /**
         * Add a list of command aliases
         * @param aliases
         * @return [HelpBiConsumerBuilder]
         */
        fun setAliases(aliases: Array<String>) : HelpBiConsumerBuilder {
            addField("Aliases", "``${aliases.joinToString(", ")}``")
            return this
        }

        fun sendDM(bool: Boolean) : HelpBiConsumerBuilder {
            this.sendDM = bool
            return this
        }

        fun addField(header: String, content: String, inline: Boolean = false)
                : HelpBiConsumerBuilder {
            embedBuilder.addField(Field(header, content, inline))
            return this
        }

        fun addField(field: Field) : HelpBiConsumerBuilder {
            embedBuilder.addField(field)
            return this
        }

        fun setThumbnail(url: String) : HelpBiConsumerBuilder {
            embedBuilder.setThumbnail(url)
            return this
        }

        fun setImage(url: String) : HelpBiConsumerBuilder {
            embedBuilder.setImage(url)
            return this
        }

        fun addBlankField(b: Boolean = false): HelpBiConsumerBuilder {
            embedBuilder.addBlankField(b)
            return this
        }
    }

    override fun execute(event: CommandEvent) = this.execute(event as WeebotCommandEvent)

    open fun execute(event: WeebotCommandEvent) {

    }

    override fun isAllowed(channel: TextChannel) =
        channel.guild.bot.settings.isAllowed(this, channel) && super.isAllowed(channel)

    fun getHelpBiConsumer(): BiConsumer<CommandEvent, Command>? = this.helpBiConsumer

}

class WeebotCommandEvent(event: MessageReceivedEvent, arg: String, val bot: Weebot)
    : CommandEvent(event, arg, CMD_CLIENT) {
    val argList get() = splitArgs()
    val isPrivate get() = event.isFromType(PRIVATE)
    val isTextChannel get() = event.isFromType(TEXT)
    override fun linkId(message: Message) = Unit
}

/**
 * A [CommandClient] implementation that seeks to fix the glaring issues of
 * the implementation given by jagrosh
 *
 * @author Jonathan Augustine
 * @since 2.2.0
 */
class WeebotCommandClient(
    val prefixes: List<String>,
    private val serverInvite: String,
    private val game: Game?,
    private val coroutinePool: ExecutorCoroutineDispatcher,
    helpWords: List<String>,
    private val helpConsumer: (WeebotCommandEvent) -> Unit
) : CommandClient, EventListener {

    val initTime: OffsetDateTime = NOW()

    private val commandIndexMap = ConcurrentHashMap<String, Int>()
    private val cooldowns = ConcurrentHashMap<String, OffsetDateTime>()

    private var totalGuilds: Int = 0

    val helpWords: List<String> = helpWords.map { it.toLowerCase() }

    init {
        COMMANDS.forEachIndexed { i, cmd ->
            if ((COMMANDS - cmd).any { it.permaID.equals(cmd.permaID, true) }) {
                MLOG.elog(this::class, "Command has conflicting PermaID")
                com.ampro.weebot.shutdown()
            }
            (cmd.aliases.map { it.toLowerCase() } + cmd.name.toLowerCase()).forEach {
                if (commandIndexMap.containsKey(it)) {
                    MLOG.elog(this::class, "Command added has a conflicting " +
                            "name or alias: other=${commandIndexMap[it]} vs this=$it")
                    com.ampro.weebot.shutdown()
                }
                commandIndexMap[it] = i
            }
        }
    }

    override fun shutdown() = coroutinePool.close()

    /* ************
        Cooldowns
     *************/

    override fun applyCooldown(name: String, seconds: Int) {
        try {
            val id = name.split("|").firstOrNull { it.startsWith("U", true) }
                ?.removeAll(Regex("[^\\d]"))?.toLong()
            if (id == null || !isDev(id))
                cooldowns[name] = NOW().plusSeconds(seconds.toLong())
        } catch (e: NumberFormatException) {
            cooldowns[name] = NOW().plusSeconds(seconds.toLong())
        }
    }

    override fun getRemainingCooldown(name: String): Int {
        cooldowns[name]?.also { cd ->
            val time = NOW().until(cd, ChronoUnit.SECONDS).toInt()
            if (time <= 0) {
                cooldowns.remove(name)
                return 0
            }
            return time
        }
        return 0
    }

    override fun getCooldown(name: String) : OffsetDateTime? = cooldowns[name]

    override fun cleanCooldowns() = cooldowns.filterValues { it.isBefore(NOW()) }
        .forEach { cooldowns.remove(it.key) }

    /* ***************
        Default Emoji
     *****************/

    override fun getSuccess() = WhiteCheckMark.unicode
    override fun getError()   = X_Red.unicode
    override fun getWarning() = Warning.unicode

    /* ************
         Owners
     *************/

    override fun getOwnerId() = DEV_IDS.first().toString()
    override fun getCoOwnerIds() = DEV_IDS.map { it.toString() }.toTypedArray()
    override fun getOwnerIdLong() = DEV_IDS.first()
    override fun getCoOwnerIdsLong() = DEV_IDS.toLongArray()

    /* ******************
        Command Parsing
     ********************/

    override fun onEvent(event: Event) {
        when (event) {
            is MessageReceivedEvent -> {
                if (event.author.isBot) return
                GlobalScope.launch(coroutinePool) { onMessageReceived(event) }
            }
            is GuildJoinEvent -> {
                //Weebot joins a guild
                if (event.guild.selfMember.joinDate.plusMinutes(10).isAfter(NOW())) {
                    sendStats()
                }
            }
            is GuildLeaveEvent -> sendStats()
            is ReadyEvent -> {
                JDA_SHARD_MNGR.setGame(
                    game ?: listening("$@${event.jda.selfUser.name}$helpWord"))
                sendStats()
            }
            is ShutdownEvent -> shutdown()
        }
    }

    private fun onMessageReceived(event: MessageReceivedEvent) {
        /** each string from raw content. the first arg will be the command name */
        var rawParts = event.splitArgsRaw().toMutableList()

        val settings = event.guild?.bot?.settings ?: GLOBAL_WEEBOT.settings

        when {
            //Check for @Mention
            rawParts[0].matches(REG_MENTION_USER)
                    && SELF.idLong == rawParts[0].removeAll("[^0-9]+").toLong() -> {
                rawParts = rawParts.subList(1)
            }
            // Check for guild specific prefixes
            settings.prefixes.isNotEmpty() -> {
                settings.prefixes.firstOrNull { rawParts[0].startsWith(it, true) }
                    ?.let { rawParts[0] = rawParts[0].removePrefix(it) } ?: return
            }
            // Check for default prefixes
            prefixes.isNotEmpty() -> {
                prefixes.firstOrNull { rawParts[0].startsWith(it, true) }
                    ?.let { rawParts[0] = rawParts[0].removePrefix(it) } ?: return
            }
            // lest a command was not called properly or at all
            else -> return
        }

        if (rawParts.isEmpty()) return
        val cmdCall: String = rawParts[0].toLowerCase()
        val args = if (rawParts.size == 1) ""
        else rawParts.subList(1).joinToString(" ")

        val wce = WeebotCommandEvent(event, args, event.guild?.bot ?: GLOBAL_WEEBOT)
        if (helpWords.contains(cmdCall.toLowerCase())) {
            helpConsumer(wce)
        } else if (event.isFromType(PRIVATE) || event.textChannel.canTalk()) {
            commandIndexMap[cmdCall]
                ?.takeIf { COMMANDS[it] allows event.author }
                ?.let { COMMANDS[it].run(wce) }
        }

    }

    private fun sendStats() {
        //discordbots.org
        MLOG.slog(this::class, "Sending Stats to discordbots.org...")
        DISCORD_BOTLIST_API.setStats(JDA_SHARD_MNGR.shards.map { it.guilds.size })
        //discordbotlist.com
        /*MLOG.slog("Sending Stats to discordbotlist.com && discord.bots.gg...")
        JDA_SHARD_MNGR.shards.forEachIndexed { i, shard ->
            "https://discordbotlist.com/api/bots/${SELF.id}/stats".httpPost(
                listOf("shard_id" to i, "guilds" to shard.guilds.size,
                    "users" to shard.users.size))
                .apply { headers.clear() }
                .header("Authorization" to "Bot $KEY_DISCORD_BOT_COM")
                .response { _, _, result ->
                    result.component2()?.apply {
                        MLOG.elog("Failed to POST to discordbotlist.com")
                        MLOG.elog(response.responseMessage)
                    }
                }
            //https://discord.bots.gg
            "https://discord.bots.gg/api/v1/bots/${SELF.id}/stats".httpPost(
                listOf("guildCount" to shard.guilds.size,
                    "shardCount" to JDA_SHARD_MNGR.shards.size, "shardId" to i
                    ,"Content-Type" to "application/json"
                ))
                .apply { headers.clear() }
                .header("Authorization" to KEY_DISCORD_BOTS_GG)
                .response { _, _, result ->
                    result.component2()?.apply {
                        MLOG.elog("Failed to POST to discord.bots.gg")
                        MLOG.elog(response.responseMessage)
                    }
                }
        }*/
    }

    //Unused Overrides
    override fun getPrefix() = try { prefixes[0] } catch (e: Exception) { null }
    override fun getAltPrefix() = try { prefixes[1] } catch (e: Exception) { null }
    override fun getHelpWord() = helpWords.first()
    override fun getServerInvite() = serverInvite
    override fun getStartTime() = initTime
    override fun addCommand(command: Command) = Unit
    override fun addCommand(command: Command, index: Int) = Unit
    override fun removeCommand(name: String?) = Unit
    @Ignore override fun getCommands(): MutableList<Command>? = null
    /**
     * same thing as [getPrefix]
     * @return A never-null prefix
     */
    override fun getTextualPrefix() = prefix
    override fun getTotalGuilds() = totalGuilds
    @Suppress("UNCHECKED_CAST")
    override fun <M : GuildSettingsManager<*>?> getSettingsManager() = null
    override fun addAnnotatedModule(module: Any) = Unit
    override fun addAnnotatedModule(module: Any, mapFunction: Function<Command, Int>) = Unit
    override fun getScheduleExecutor(): ScheduledExecutorService?  = null
    @Suppress("UNCHECKED_CAST")
    override fun <S : Any?> getSettingsFor(guild: Guild) = null
    override fun setListener(listener: CommandListener) = Unit
    override fun getListener() = null
    override fun usesLinkedDeletion() = false
    override fun getCommandUses(command: Command): Int = -1
    override fun getCommandUses(name: String) : Int = -1

}
