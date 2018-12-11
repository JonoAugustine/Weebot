/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

import com.ampro.weebot.util.NOW
import com.jagrosh.jdautilities.command.*
import com.jagrosh.jdautilities.commons.utils.FixedSizeCache
import com.jagrosh.jdautilities.commons.utils.SafeIdUtil
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.requests.Requester
import net.dv8tion.jda.core.utils.Checks
import okhttp3.*
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer
import java.util.function.Function

class WeebotClientBuilder : CommandClientBuilder() {
    fun build(): CommandClient {
        return WeebotClient()
    }


}

/**
 * An implementation of [CommandClient][com.jagrosh.jdautilities.command.CommandClient] to be used by a bot.
 *
 *
 * This is a listener usable with [JDA][net.dv8tion.jda.core.JDA], as it implements
 * [EventListener][net.dv8tion.jda.core.hooks.EventListener] in order to catch and use different kinds of
 * [Event][net.dv8tion.jda.core.events.Event]s. The primary usage of this is where the CommandClient implementation
 * takes [MessageReceivedEvent][net.dv8tion.jda.core.events.message.MessageReceivedEvent]s, and automatically
 * processes arguments, and provide them to a [Command][com.jagrosh.jdautilities.command.Command] for
 * running and execution.
 *
 * @author John Grosh (jagrosh)
 * @author Jonathan Augustine
 */
class WeebotClient(val ownerId: String?,
                   val coOwnerIds: Array<String>?,
                   var textPrefix: String?, altprefix: String?, val game: Game?,
                   val status: OnlineStatus?, val serverInvite: String?,
                   success: String?, warning: String?, error: String?,
                   val carbonKey: String?, val botsKey: String?,
                   val botsOrgKey: String?, commands: ArrayList<Command>,
                   val useHelp: Boolean, val shutdownAutomatically: Boolean,
                   helpConsumer: Consumer<CommandEvent>?, helpWord: String?,
                   executor: ScheduledExecutorService?, linkedCacheSize: Int,
                   val compiler: AnnotatedModuleCompiler,
                   val manager: GuildSettingsManager<*>?)
    : EventListener {

    val start: OffsetDateTime
    val prefix: String?
    val altprefix: String?
    val commandIndex: HashMap<String, Int>
    val commands: ArrayList<Command>
    val success: String?
    val warning: String?
    val error: String?
    val cooldowns: HashMap<String, OffsetDateTime>
    val uses: HashMap<String, Int>
    val linkMap: FixedSizeCache<Long, Set<Message>>?
    val helpConsumer: Consumer<CommandEvent>?
    val helpWord: String?
    val executor: ScheduledExecutorService?
    var listener: CommandListener? = null
    var totalGuilds: Int = 0

    init {
        Checks.check(ownerId != null,
            "Owner ID was set null or not set! Please provide an User ID to register as the owner!")

        if (!SafeIdUtil.checkId(ownerId)) LOG.warn(String.format(
            "The provided Owner ID (%s) was found unsafe! Make sure ID is a non-negative long!",
            ownerId))

        if (coOwnerIds != null) {
            for (coOwnerId in coOwnerIds) {
                if (!SafeIdUtil.checkId(coOwnerId)) LOG.warn(String.format(
                    "The provided CoOwner ID (%s) was found unsafe! Make sure ID is a non-negative long!",
                    coOwnerId))
            }
        }

        this.start = OffsetDateTime.now()
        this.prefix = if (textPrefix == null || textPrefix!!.isEmpty()) DEFAULT_PREFIX else textPrefix
        this.altprefix = if (altprefix == null || altprefix.isEmpty()) null else altprefix
        this.success = success ?: ""
        this.warning = warning ?: ""
        this.error = error ?: ""
        this.commandIndex = HashMap()
        this.commands = ArrayList()
        this.cooldowns = HashMap()
        this.uses = HashMap()
        this.linkMap = if (linkedCacheSize > 0) FixedSizeCache(linkedCacheSize) else null
        this.helpWord = helpWord ?: "help"
        this.executor = executor ?: Executors.newSingleThreadScheduledExecutor()
        this.helpConsumer = helpConsumer ?: Consumer { event: CommandEvent ->
            val builder = StringBuilder(
                "**" + event.getSelfUser().getName() + "** commands:\n")
            var category: Command.Category? = null
            for (command in commands) {
                if (!command.isHidden && (!command.isOwnerCommand || event.isOwner())) {
                    if (category != command.category) {
                        category = command.category
                        builder.append("\n\n  __")
                            .append(
                                if (category == null) "No Category" else category.name)
                            .append("__:\n")
                    }
                    builder.append("\n`").append(textPrefix)
                        .append(if (textPrefix == null) " " else "").append(command.name)
                        .append(
                            if (command.arguments == null) "`" else " " + command.arguments + "`")
                        .append(" - ").append(command.help)
                }
            }
            val owner = event.getJDA().getUserById(ownerId)
            if (owner != null) {
                builder.append("\n\nFor additional help, contact **")
                    .append(owner!!.getName()).append("**#")
                    .append(owner!!.getDiscriminator())
                if (serverInvite != null) builder.append(" or join ").append(serverInvite)
            }
            event.replyInDm(builder.toString(), { unused ->
                if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
            }, { t ->
                event.replyWarning(
                    "Help cannot be sent because you are blocking Direct Messages.")
            })
        }

        // Load commands
        for (command in commands) {
            addCommand(command)
        }
    }

    fun getCommands(): List<Command> {
        return commands
    }

    fun getStartTime(): OffsetDateTime {
        return start
    }

    fun getCooldown(name: String): OffsetDateTime? {
        return cooldowns[name]
    }

    fun getRemainingCooldown(name: String): Int {
        if (cooldowns.containsKey(name)) {
            val time = OffsetDateTime.now().until(cooldowns[name], ChronoUnit.SECONDS)
                .toInt()
            if (time <= 0) {
                cooldowns.remove(name)
                return 0
            }
            return time
        }
        return 0
    }

    fun applyCooldown(name: String, seconds: Int) {
        cooldowns[name] = OffsetDateTime.now().plusSeconds(seconds.toLong())
    }

    fun cleanCooldowns() {
        cooldowns.filter { it.value.isBefore(NOW) }
            .keys.forEach { cooldowns.remove(it) }
    }

    fun getCommandUses(command: Command): Int {
        return getCommandUses(command.name)
    }

    fun getCommandUses(name: String): Int {
        return (uses as java.util.Map<String, Int>).getOrDefault(name, 0)
    }

    fun addCommand(command: Command) {
        addCommand(command, commands.size)
    }

    fun addCommand(command: Command, index: Int) {
        if (index > commands.size || index < 0) throw ArrayIndexOutOfBoundsException(
            "Index specified is invalid: [" + index + "/" + commands.size + "]")
        val name = command.name
        synchronized(commandIndex) {
            if (commandIndex.containsKey(name)) throw IllegalArgumentException(
                "Command added has a name or alias that has already been indexed: \"$name\"!")
            for (alias in command.aliases) {
                if (commandIndex.containsKey(alias)) throw IllegalArgumentException(
                    "Command added has a name or alias that has already been indexed: \"$alias\"!")
                commandIndex[alias] = index
            }
            commandIndex[name] = index
            if (index < commands.size) {
                commandIndex.filter { it.value > index }
                    .keys.forEach { commandIndex[it] = commandIndex[it]!! + 1 }
            }
        }
        commands.add(index, command)
    }

    fun removeCommand(name: String) {
        if (!commandIndex.containsKey(name)) throw IllegalArgumentException(
            "Name provided is not indexed: \"$name\"!")
        val targetIndex = commandIndex.remove(name)!!
        if (commandIndex.containsValue(targetIndex)) {
            commandIndex.filter { it.value == targetIndex }
                .keys.forEach { commandIndex.remove(it) }
        }
        commandIndex.filter { it.value > targetIndex }
            .keys.forEach { commandIndex[it] = commandIndex[it]!! - 1 }
        commands.removeAt(targetIndex)
    }

    fun addAnnotatedModule(module: Any) {
        compiler.compile(module).forEach(Consumer { this.addCommand(it) })
    }

    fun addAnnotatedModule(module: Any, mapFunction: Function<Command, Int>) {
        compiler.compile(module)
            .forEach { command -> addCommand(command, mapFunction.apply(command)) }
    }

    fun getCoOwnerIdsLong(): LongArray? {
        // Thought about using java.util.Arrays#setAll(T[], IntFunction<T>)
        // here, but as it turns out it's actually the same thing as this but
        // it throws an error if null. Go figure.
        if (coOwnerIds == null) return null
        val ids = LongArray(coOwnerIds.size)
        for (i in ids.indices) ids[i] = java.lang.Long.parseLong(coOwnerIds[i])
        return ids
    }


    fun getScheduleExecutor(): ScheduledExecutorService? {
        return executor
    }



    fun getAltPrefix(): String? {
        return altprefix
    }

    fun getTextualPrefix(): String? {
        return textPrefix
    }

    fun usesLinkedDeletion(): Boolean {
        return linkMap != null
    }

    fun <S> getSettingsFor(guild: Guild): S? {
        return if (manager == null) null else manager.getSettings(guild) as S?
    }

    fun <M : GuildSettingsManager<*>> getSettingsManager(): M? {
        return manager as M?
    }

    fun shutdown() {
        val manager = getSettingsManager<GuildSettingsManager<*>>()
        manager?.shutdown()
        executor!!.shutdown()
    }

    override fun onEvent(event: Event) {
        if (event is MessageReceivedEvent) onMessageReceived(event)
        else if (event is GuildMessageDeleteEvent && usesLinkedDeletion()) onMessageDelete(
            event)
        else if (event is GuildJoinEvent) {
            if (event.guild.selfMember.joinDate.plusMinutes(10).isAfter(
                        OffsetDateTime.now())) sendStats(event.getJDA())
        } else if (event is GuildLeaveEvent) sendStats(event.getJDA())
        else if (event is ReadyEvent) onReady(event)
        else if (event is ShutdownEvent) {
            if (shutdownAutomatically) shutdown()
        }
    }

    fun onReady(event: ReadyEvent) {
        if (!event.jda.selfUser.isBot) {
            LOG.error("JDA-Utilities does not support CLIENT accounts.")
            event.jda.shutdown()
            return
        }
        textPrefix = if (prefix == DEFAULT_PREFIX) "@" + event.jda.selfUser.name + " " else prefix
        event.jda.presence.setPresence(status ?: OnlineStatus.ONLINE,
            if (game == null) null else if ("default" == game.name) Game.playing(
                "Type $textPrefix$helpWord") else game)

        // Start SettingsManager if necessary
        val manager = getSettingsManager<GuildSettingsManager<*>>()
        manager?.init()

        sendStats(event.jda)
    }

    fun onMessageReceived(event: MessageReceivedEvent) {
        // Return if it's a bot
        if (event.author.isBot) return

        var parts: Array<String>? = null
        val rawContent = event.message.contentRaw

        val settings = if (event.isFromType(ChannelType.TEXT)) provideSettings(
            event.guild) else null

        // Check for prefix or alternate prefix (@mention cases)
        if (prefix == DEFAULT_PREFIX || altprefix != null && altprefix == DEFAULT_PREFIX) {
            if (rawContent.startsWith(
                        "<@" + event.jda.selfUser.id + ">") || rawContent.startsWith(
                        "<@!" + event.jda.selfUser.id + ">")) {
                parts = splitOnPrefixLength(rawContent,
                    rawContent.indexOf(">") + 1)
            }
        }
        // Check for prefix
        if (parts == null && rawContent.toLowerCase().startsWith(
                    prefix!!.toLowerCase())) parts = splitOnPrefixLength(
            rawContent, prefix.length)
        // Check for alternate prefix
        if (parts == null && altprefix != null && rawContent.toLowerCase().startsWith(
                    altprefix.toLowerCase())) parts = splitOnPrefixLength(
            rawContent, altprefix.length)
        // Check for guild specific prefixes
        if (parts == null && settings != null) {
            val prefixes = settings.prefixes
            if (prefixes != null) {
                for (prefix in prefixes) {
                    if (parts == null && rawContent.toLowerCase().startsWith(
                                prefix.toLowerCase())) parts = splitOnPrefixLength(
                        rawContent, prefix.length)
                }
            }
        }

        if (parts != null)
        //starts with valid prefix
        {
            if (useHelp && parts[0].equals(helpWord!!, ignoreCase = true)) {
                val cevent = CommandEvent(event, if (parts[1] == null) "" else parts[1],
                    this)
                if (listener != null) listener!!.onCommand(cevent, null)
                helpConsumer!!.accept(cevent) // Fire help consumer
                if (listener != null) listener!!.onCompletedCommand(cevent, null)
                return  // Help Consumer is done
            } else if (event.isFromType(
                        ChannelType.PRIVATE) || event.textChannel.canTalk()) {
                val name = parts[0]
                val args = if (parts[1] == null) "" else parts[1]
                val command: Command? // this will be null if it's not a command
                if (commands.size < INDEX_LIMIT + 1) command = commands.stream()
                    .filter { cmd -> cmd.isCommandFor(name) }.findAny().orElse(null)
                else {
                    synchronized(commandIndex) {
                        val i = (commandIndex as java.util.Map<String, Int>).getOrDefault(
                            name.toLowerCase(), -1)
                        command = if (i != -1) commands[i] else null
                    }
                }

                if (command != null) {
                    val cevent = CommandEvent(event, args, this)

                    if (listener != null) listener!!.onCommand(cevent, command)
                    uses[command.name] = (uses as java.util.Map<String, Int>).getOrDefault(
                        command.name, 0) + 1
                    command.run(cevent)
                    return  // Command is done
                }
            }
        }

        if (listener != null) listener!!.onNonCommandMessage(event)
    }

    fun sendStats(jda: JDA) {
        val client = (jda as JDAImpl).httpClient

        if (carbonKey != null) {
            val bodyBuilder = FormBody.Builder().add("key", carbonKey)
                .add("servercount", Integer.toString(jda.getGuilds().size))

            if (jda.getShardInfo() != null) {
                bodyBuilder.add("shard_id", Integer.toString(jda.getShardInfo().shardId))
                    .add("shard_count", Integer.toString(jda.getShardInfo().shardTotal))
            }

            val builder = Request.Builder().post(bodyBuilder.build())
                .url("https://www.carbonitex.net/discord/data/botdata.php")

            client.newCall(builder.build()).enqueue(object : Callback {
                fun onResponse(call: Call, response: Response) {
                    LOG.info("Successfully send information to carbonitex.net")
                    response.close()
                }

                fun onFailure(call: Call, e: IOException) {
                    LOG.error("Failed to send information to carbonitex.net ", e)
                }
            })
        }

        // Both bots.discord.pw and discordbots.org use the same JSON body
        // structure for POST requests to their stats APIs, so we reuse the same
        // JSON for both
        val body = JSONObject().put("server_count", jda.getGuilds().size)
        if (jda.getShardInfo() != null) {
            body.put("shard_id", jda.getShardInfo().shardId)
                .put("shard_count", jda.getShardInfo().shardTotal)
        }

        if (botsOrgKey != null) {
            val builder = Request.Builder()
                .post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
                .url(
                    "https://discordbots.org/api/bots/" + jda.getSelfUser().id + "/stats")
                .header("Authorization", botsOrgKey)
                .header("Content-Type", "application/json")

            client.newCall(builder.build()).enqueue(object : Callback {
                fun onResponse(call: Call, response: Response) {
                    LOG.info("Successfully send information to discordbots.org")
                    response.close()
                }

                fun onFailure(call: Call, e: IOException) {
                    LOG.error("Failed to send information to discordbots.org ", e)
                }
            })
        }

        if (botsKey != null) {
            val builder = Request.Builder()
                .post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
                .url(
                    "https://bots.discord.pw/api/bots/" + jda.getSelfUser().id + "/stats")
                .header("Authorization", botsKey)
                .header("Content-Type", "application/json")

            client.newCall(builder.build()).enqueue(object : Callback {
                fun onResponse(call: Call, response: Response) {
                    LOG.info("Successfully send information to bots.discord.pw")
                    response.close()
                }

                fun onFailure(call: Call, e: IOException) {
                    LOG.error("Failed to send information to bots.discord.pw ", e)
                }
            })

            if (jda.getShardInfo() == null) {
                this.totalGuilds = jda.getGuilds().size
            } else {
                val b = Request.Builder().get()
                    .url(
                        "https://bots.discord.pw/api/bots/" + jda.getSelfUser().id + "/stats")
                    .header("Authorization", botsKey)
                    .header("Content-Type", "application/json")

                client.newCall(b.build()).enqueue(object : Callback {
                    @Throws(IOException::class)
                    fun onResponse(call: Call, response: Response) {
                        try {
                            response.body()!!.charStream().use { reader ->
                                val array = JSONObject(JSONTokener(reader)).getJSONArray(
                                    "stats")
                                var total = 0
                                for (i in 0 until array.length()) total += array.getJSONObject(
                                    i).getInt("server_count")
                                totalGuilds = total
                            }
                        } finally {
                            // Close the response
                            response.close()
                        }
                    }

                    fun onFailure(call: Call, e: IOException) {
                        LOG.error(
                            "Failed to retrieve bot shard information from bots.discord.pw ",
                            e)
                    }
                })

                // Good thing to keep in mind:
                // We used to make the request above by blocking the thread and waiting for DBots
                // to respond. For the future (should we succeed in not blocking that as well),
                // let's not do this again, okay?

                /*try(Reader reader = client.newCall(new Request.Builder()
                        .get().url("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
                        .header("Authorization", botsKey)
                        .header("Content-Type", "application/json")
                        .build()).execute().body().charStream()) {
                    JSONArray array = new JSONObject(new JSONTokener(reader)).getJSONArray("stats");
                    int total = 0;
                    for (int i = 0; i < array.length(); i++)
                        total += array.getJSONObject(i).getInt("server_count");
                    this.totalGuilds = total;
                } catch (Exception e) {
                    LOG.error("Failed to retrieve bot shard information from bots.discord.pw ", e);
                }*/
            }
        }
    }

    fun onMessageDelete(event: GuildMessageDeleteEvent) {
        // We don't need to cover whether or not this client usesLinkedDeletion() because
        // that is checked in onEvent(Event) before this is even called.
        synchronized(linkMap) {
            if (linkMap!!.contains(event.messageIdLong)) {
                val messages = linkMap.get(event.messageIdLong)
                if (messages.size > 1 && event.guild.selfMember.hasPermission(
                            event.channel,
                            Permission.MESSAGE_MANAGE)) event.channel.deleteMessages(
                    messages).queue({ unused -> }, { ignored -> })
                else if (messages.size > 0) messages.forEach { m ->
                    m.delete()
                        .queue({ unused -> }, { ignored -> })
                }
            }
        }
    }

    fun provideSettings(guild: Guild): GuildSettingsProvider? {
        val settings = getSettingsFor<Any>(guild)
        return if (settings != null && settings is GuildSettingsProvider) settings
        else null
    }

    /**
     * **DO NOT USE THIS!**
     *
     *
     * This is a method necessary for linking a bot's response messages
     * to their corresponding call message ID.
     * <br></br>**Using this anywhere in your code can and will break your bot.**
     *
     * @param  callId
     * The ID of the call Message
     * @param  message
     * The Message to link to the ID
     */
    fun linkIds(callId: Long, message: Message) {
        // We don't use linked deletion, so we don't do anything.
        if (!usesLinkedDeletion()) return

        synchronized(linkMap) {
            var stored: MutableSet<Message>? = linkMap!!.get(callId)
            if (stored != null) stored.add(message)
            else {
                stored = HashSet()
                stored.add(message)
                linkMap.add(callId, stored)
            }
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(CommandClient::class.java)
        val INDEX_LIMIT = 20
        val DEFAULT_PREFIX = "@mention"

        fun splitOnPrefixLength(rawContent: String, length: Int): Array<String> {
            return Arrays.copyOf(
                rawContent.substring(length).trim { it <= ' ' }.split("\\s+".toRegex(),
                    2).toTypedArray(), 2)
        }
    }
}

