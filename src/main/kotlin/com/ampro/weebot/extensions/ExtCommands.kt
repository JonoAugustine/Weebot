/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.ampro.weebot.database.getWeebotOrNew
import com.jagrosh.jdautilities.command.*
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import java.time.OffsetDateTime
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
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete (default 10)
 */
fun CommandEvent.respondThenDelete(reason: String, delay: Long = 10L) {
    this.reply("*$reason*") { response ->
        if (this.privateChannel != null) {
            response.delete().reason(reason).queueAfter(delay, SECONDS)
        } else {
            event.message.delete().reason(reason).queueAfter(delay, SECONDS) {
                response.delete().reason(reason).queue()
            }
        }
    }
}

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete
 */
fun CommandEvent.respondThenDelete(reason: MessageEmbed, delay: Long = 10L) {
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
 * @param
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
abstract class WeebotCommand(name: String, val displayName: String?,
                             aliases: Array<String>, category: Category,
                             arguments: String, help: String,
                             helpBiConsumer: BiConsumer<CommandEvent, Command>? = null,
                             guildOnly: Boolean = false, ownerOnly: Boolean = false,
                             hidden: Boolean = false, useTopicTags: Boolean = true,
                             children: Array<out Command>? = emptyArray(),
                             requiredRole: String? = null, cooldown: Int = 0,
                             cooldownScope: CooldownScope = USER,
                             userPerms: Array<Permission> = emptyArray(),
                             botPerms: Array<Permission> = emptyArray()
) : Command() {

    init {
        super.name = name
        super.aliases = aliases
        super.help = help
        super.helpBiConsumer = helpBiConsumer
        super.category = category
        super.arguments = arguments
        super.guildOnly = guildOnly
        super.requiredRole = requiredRole
        super.ownerCommand = ownerOnly
        super.cooldown = cooldown
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
        constructor(title: String) : this() { embedBuilder.setTitle(title).addField(
            guide) }
        constructor(title: String, description: String) : this(title) {
            this.description.append(description)
        }
        constructor(title: String, withGuide: Boolean) : this() {
            embedBuilder.setTitle(title)
            if (withGuide) embedBuilder.addField(
                guide) else return
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

    override fun isAllowed(channel: TextChannel): Boolean {
        return getWeebotOrNew(channel.guild).settings.isAllowed(this, channel)
                && super.isAllowed(channel)
    }

    fun getHelpBiConsumer(): BiConsumer<CommandEvent, Command>? = this.helpBiConsumer

}

/**
 * A [CommandClient] implementation that seeks to fix the glaring issues of
 * the implementation given by jagrosh
 *
 * @author Jonathan Augustine
 * @since 2.2.0
 */
class WeebotCommandClient : CommandClient {
    /**
     * Gets the ID(s) of any CoOwners of this bot as a String Array.
     *
     * @return The String ID(s) of any CoOwners of this bot
     */
    override fun getCoOwnerIds(): Array<String> {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Applies the specified cooldown with the provided name.
     *
     * @param  name
     * The cooldown name
     * @param  seconds
     * The time to make the cooldown last
     */
    override fun applyCooldown(name: String?, seconds: Int) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Returns the type of [GuildSettingsManager][com.jagrosh.jdautilities.command.GuildSettingsManager],
     * the same type of one provided when building this CommandClient, or `null` if one was not provided there.
     *
     *
     * This is good if you want to use non-abstract methods specific to your implementation.
     *
     * @param  <M>
     * The type of the GuildSettingsManager
     *
     * @return The GuildSettingsManager, or `null` if one was not provided when building this CommandClient.
    </M> */
    override fun <M : GuildSettingsManager<*>?> getSettingsManager(): M {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the error emoji.
     *
     * @return The error emoji
     */
    override fun getError(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Compiles the provided [Object][java.lang.Object] annotated with [ ] into a [ List][java.util.List] of [Command][com.jagrosh.jdautilities.command.Command]s and adds them to this CommandClient in
     * the order they are listed.
     *
     *
     * This is done through the [ AnnotatedModuleCompiler][AnnotatedModuleCompiler] provided when building this CommandClient.
     *
     * @param  module
     * An object annotated with JDACommand.Module to compile into commands to be added.
     *
     * @throws java.lang.IllegalArgumentException
     * If the Command provided has a name or alias that has already been registered
     */
    override fun addAnnotatedModule(module: Any?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Compiles the provided [Object][java.lang.Object] annotated with [ ] into a [ List][java.util.List] of [Command][com.jagrosh.jdautilities.command.Command]s and adds them to this CommandClient via
     * the [Function][java.util.function.Function] provided.
     *
     *
     * This is done through the [ AnnotatedModuleCompiler][AnnotatedModuleCompiler] provided when building this CommandClient.
     *
     *
     * The Function will [apply][java.util.function.Function.apply] each [ ] in the compiled list and request an `int` in return.
     * <br></br>Using this `int`, the command provided will be applied to the CommandClient via [ ][CommandClient.addCommand].
     *
     * @param  module
     * An object annotated with JDACommand.Module to compile into commands to be added.
     * @param  mapFunction
     * The Function to get indexes for each compiled Command with when adding them to the CommandClient.
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     * If `index < 0` or `index > size()`
     * @throws java.lang.IllegalArgumentException
     * If the Command provided has a name or alias that has already been registered to an index
     */
    override fun addAnnotatedModule(module: Any?, mapFunction: Function<Command, Int>?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the invite to the bot's support server.
     *
     * @return A possibly-null server invite
     */
    override fun getServerInvite(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the Client's alternate prefix.
     *
     * @return A possibly-null alternate prefix
     */
    override fun getAltPrefix(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the [ScheduledExecutorService][java.util.concurrent.ScheduledExecutorService] held by this client.
     *
     *
     * This is used for methods such as [ CommandEvent#async(Runnable)][com.jagrosh.jdautilities.command.CommandEvent.async] run code asynchronously.
     *
     * @return The ScheduledExecutorService held by this client.
     */
    override fun getScheduleExecutor(): ScheduledExecutorService {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Cleans up expired cooldowns to reduce memory.
     */
    override fun cleanCooldowns() {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the warning emoji.
     *
     * @return The warning emoji
     */
    override fun getWarning(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Adds a single [Command][com.jagrosh.jdautilities.command.Command] to this CommandClient's
     * registered Commands.
     *
     *
     * For CommandClient's containing 20 commands or less, command calls by users will have the bot iterate
     * through the entire [ArrayList][java.util.ArrayList] to find the command called. As expected, this
     * can get fairly hefty if a bot has a lot of Commands registered to it.
     *
     *
     * To prevent delay a CommandClient that has more that 20 Commands registered to it will begin to use
     * **indexed calls**.
     * <br></br>Indexed calls use a [HashMap][java.util.HashMap] which links their
     * [name][com.jagrosh.jdautilities.command.Command.name] and their
     * [aliases][com.jagrosh.jdautilities.command.Command.aliases] to the index that which they
     * are located at in the ArrayList they are stored.
     *
     *
     * This means that all insertion and removal of Commands must reorganize the index maintained by the HashMap.
     * <br></br>For this particular insertion, the Command provided is inserted at the end of the index, meaning it will
     * become the "rightmost" Command in the ArrayList.
     *
     * @param  command
     * The Command to add
     *
     * @throws java.lang.IllegalArgumentException
     * If the Command provided has a name or alias that has already been registered
     */
    override fun addCommand(command: Command?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Adds a single [Command][com.jagrosh.jdautilities.command.Command] to this CommandClient's
     * registered Commands at the specified index.
     *
     *
     * For CommandClient's containing 20 commands or less, command calls by users will have the bot iterate
     * through the entire [ArrayList][java.util.ArrayList] to find the command called. As expected, this
     * can get fairly hefty if a bot has a lot of Commands registered to it.
     *
     *
     * To prevent delay a CommandClient that has more that 20 Commands registered to it will begin to use
     * **indexed calls**.
     * <br></br>Indexed calls use a [HashMap][java.util.HashMap] which links their
     * [name][com.jagrosh.jdautilities.command.Command.name] and their
     * [aliases][com.jagrosh.jdautilities.command.Command.aliases] to the index that which they
     * are located at in the ArrayList they are stored.
     *
     *
     * This means that all insertion and removal of Commands must reorganize the index maintained by the HashMap.
     * <br></br>For this particular insertion, the Command provided is inserted at the index specified, meaning it will
     * become the Command located at that index in the ArrayList. This will shift the Command previously located at
     * that index as well as any located at greater indices, right one index (`size()+1`).
     *
     * @param  command
     * The Command to add
     * @param  index
     * The index to add the Command at (must follow the specifications `0<=index<=size()`)
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     * If `index < 0` or `index > size()`
     * @throws java.lang.IllegalArgumentException
     * If the Command provided has a name or alias that has already been registered to an index
     */
    override fun addCommand(command: Command?, index: Int) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Returns an Object of the type parameter that should contain settings relating to the specified
     * [Guild][net.dv8tion.jda.core.entities.Guild].
     *
     *
     * The returning object for this is specified via provision of a
     * [GuildSettingsManager][com.jagrosh.jdautilities.command.GuildSettingsManager] to
     * [ CommandClientBuilder#setGuildSettingsManager(GuildSettingsManager)][com.jagrosh.jdautilities.command.CommandClientBuilder.setGuildSettingsManager], more specifically
     * [ GuildSettingsManager#getSettings(Guild)][GuildSettingsManager.getSettings].
     *
     * @param  <S>
     * The type of settings the GuildSettingsManager provides
     * @param  guild
     * The Guild to get Settings for
     *
     * @return The settings object for the Guild, specified in
     * [         GuildSettingsManager#getSettings(Guild)][com.jagrosh.jdautilities.command.GuildSettingsManager.getSettings], can be `null` if the implementation
     * allows it.
    </S> */
    override fun <S : Any?> getSettingsFor(guild: Guild?): S {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Sets the [CommandListener][com.jagrosh.jdautilities.command.CommandListener] to catch
     * command-related events thrown by this [CommandClient][com.jagrosh.jdautilities.command.CommandClient].
     *
     * @param  listener
     * The CommandListener
     */
    override fun setListener(listener: CommandListener?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Returns the list of registered [Command][com.jagrosh.jdautilities.command.Command]s
     * during this session.
     *
     * @return A never-null List of Commands registered during this session
     */
    override fun getCommands(): MutableList<Command> {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the time this [CommandClient][com.jagrosh.jdautilities.command.CommandClient]
     * implementation was created.
     *
     * @return The start time of this CommandClient implementation
     */
    override fun getStartTime(): OffsetDateTime {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the ID(s) of any CoOwners of this bot as a `long` Array.
     *
     * @return The `long` ID(s) of any CoOwners of this bot
     */
    override fun getCoOwnerIdsLong(): LongArray {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Returns the current [CommandListener][com.jagrosh.jdautilities.command.CommandListener].
     *
     * @return A possibly-null CommandListener
     */
    override fun getListener(): CommandListener {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the success emoji.
     *
     * @return The success emoji
     */
    override fun getSuccess(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the number of uses for the provide [Command][com.jagrosh.jdautilities.command.Command]
     * during this session, or `0` if the command is not registered to this CommandClient.
     *
     * @param  command
     * The Command
     *
     * @return The number of uses for the Command
     */
    override fun getCommandUses(command: Command?): Int {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the number of uses for a [Command][com.jagrosh.jdautilities.command.Command]
     * during this session matching the provided String name, or `0` if there is no Command
     * with the name.
     *
     *
     * **NOTE:** this method **WILL NOT** get uses for a command if an
     * [alias][com.jagrosh.jdautilities.command.Command.aliases] is provided! Also note that
     * [child commands][com.jagrosh.jdautilities.command.Command.children] **ARE NOT**
     * tracked and providing names or effective names of child commands will return `0`.
     *
     * @param  name
     * The name of the Command
     *
     * @return The number of uses for the Command, or `0` if the name does not match with a Command
     */
    override fun getCommandUses(name: String?): Int {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the [OffsetDateTime][java.time.OffsetDateTime] that the specified cooldown expires.
     *
     * @param  name
     * The cooldown name
     *
     * @return The expiration time, or null if the cooldown does not exist
     */
    override fun getCooldown(name: String?): OffsetDateTime {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the ID of the owner of this bot as a `long`.
     *
     * @return The `long` ID of the owner of the bot
     */
    override fun getOwnerIdLong(): Long {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the word used to invoke a help DM.
     *
     * @return The help word
     */
    override fun getHelpWord(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Shuts down internals of the Command Client, such as the threadpool and guild settings manager
     */
    override fun shutdown() {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the ID of the owner of this bot as a String.
     *
     * @return The String ID of the owner of the bot
     */
    override fun getOwnerId(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Removes a single [Command][com.jagrosh.jdautilities.command.Command] from this CommandClient's
     * registered Commands at the index linked to the provided name/alias.
     *
     *
     * For CommandClient's containing 20 commands or less, command calls by users will have the bot iterate
     * through the entire [ArrayList][java.util.ArrayList] to find the command called. As expected, this
     * can get fairly hefty if a bot has a lot of Commands registered to it.
     *
     *
     * To prevent delay a CommandClient that has more that 20 Commands registered to it will begin to use
     * **indexed calls**.
     * <br></br>Indexed calls use a [HashMap][java.util.HashMap] which links their
     * [name][com.jagrosh.jdautilities.command.Command.name] and their
     * [aliases][com.jagrosh.jdautilities.command.Command.aliases] to the index that which they
     * are located at in the ArrayList they are stored.
     *
     *
     * This means that all insertion and removal of Commands must reorganize the index maintained by the HashMap.
     * <br></br>For this particular removal, the Command removed is that of the corresponding index retrieved by the name
     * provided. This will shift any Commands located at greater indices, left one index (`size()-1`).
     *
     * @param  name
     * The name or an alias of the Command to remove
     *
     * @throws java.lang.IllegalArgumentException
     * If the name provided was not registered to an index
     */
    override fun removeCommand(name: String?) {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets whether this CommandClient uses linked deletion.
     *
     *
     * Linking calls is the basic principle of pairing bot responses with their calling
     * [Message][net.dv8tion.jda.core.entities.Message]s.
     * <br></br>Using this with a basic function such as deletion, this causes bots to delete their
     * Messages as a response to the calling Message being deleted.
     *
     * @return `true` if the bot uses linked deletion, `false` otherwise.
     *
     * @see com.jagrosh.jdautilities.command.CommandClientBuilder.setLinkedCacheSize
     */
    override fun usesLinkedDeletion(): Boolean {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the Client's prefix.
     *
     * @return A possibly-null prefix
     */
    override fun getPrefix(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Returns the visual representation of the bot's prefix.
     *
     *
     * This is the same as [com.jagrosh.jdautilities.command.CommandClient.getPrefix] unless the prefix is the default,
     * in which case it appears as @Botname.
     *
     * @return A never-null prefix
     */
    override fun getTextualPrefix(): String {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets an a recently updated count of all the [Guild][net.dv8tion.jda.core.entities.Guild]s
     * the bot is connected to on all shards.
     *
     *
     * **NOTE:** This may not always or should not be assumed accurate! Any time
     * a shard joins or leaves a guild it will update the number retrieved by this method
     * but will not update when other shards join or leave guilds. This means that shards
     * will not always retrieve the same value. For instance:
     *
     *  * 1) Shard A joins 10 Guilds
     *  * 2) Shard B invokes this method
     *  * 3) Shard A invokes this method
     *
     * The number retrieved by Shard B will be that of the number retrieved by Shard A,
     * minus 10 guilds because Shard B hasn't updated and accounted for those 10 guilds
     * on Shard A.
     *
     *
     * **This feature requires a Discord Bots API Key to be set!**
     * <br></br>To set your Discord Bots API Key, you'll have to retrieve it from the
     * [Discord Bots](http://bots.discord.pw/) website.
     *
     * @return A recently updated count of all the Guilds the bot is connected to on
     * all shards.
     */
    override fun getTotalGuilds(): Int {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Gets the remaining number of seconds on the specified cooldown.
     *
     * @param  name
     * The cooldown name
     *
     * @return The number of seconds remaining
     */
    override fun getRemainingCooldown(name: String?): Int {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
