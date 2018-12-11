/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jdautilities.command

import java.util.ArrayList
import java.util.LinkedList
import java.util.function.Consumer

import com.jagrosh.jdautilities.command.impl.AnnotatedModuleCompilerImpl
import com.jagrosh.jdautilities.command.impl.CommandClientImpl
import java.util.concurrent.ScheduledExecutorService
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game

/**
 * A simple builder used to create a [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl].
 *
 *
 * Once built, add the [CommandClient][com.jagrosh.jdautilities.command.CommandClient] as an EventListener to
 * [JDA][net.dv8tion.jda.core.JDA] and it will automatically handle commands with ease!
 *
 * @author John Grosh (jagrosh)
 */
open class CommandClientBuilder {
    private var game = Game.playing("default")
    private var status = OnlineStatus.ONLINE
    private var ownerId: String? = null
    private var coOwnerIds: Array<String>? = null
    private var prefix: String? = null
    private var altprefix: String? = null
    private var serverInvite: String? = null
    private var success: String? = null
    private var warning: String? = null
    private var error: String? = null
    private var carbonKey: String? = null
    private var botsKey: String? = null
    private var botsOrgKey: String? = null
    private val commands = LinkedList<Command>()
    private var listener: CommandListener? = null
    private var useHelp = true
    private var shutdownAutomatically = true
    private var helpConsumer: Consumer<CommandEvent>? = null
    private var helpWord: String? = null
    private var executor: ScheduledExecutorService? = null
    private var linkedCacheSize = 0
    private var compiler: AnnotatedModuleCompiler = AnnotatedModuleCompilerImpl()
    private var manager: GuildSettingsManager<*>? = null

    /**
     * Builds a [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl]
     * with the provided settings.
     * <br></br>Once built, only the [CommandListener][com.jagrosh.jdautilities.command.CommandListener],
     * and [Command][com.jagrosh.jdautilities.command.Command]s can be changed.
     *
     * @return The CommandClient built.
     */
    open fun build(): CommandClient {
        val client = CommandClientImpl(ownerId, coOwnerIds, prefix, altprefix, game,
            status, serverInvite, success, warning, error, carbonKey, botsKey, botsOrgKey,
            ArrayList(commands), useHelp, shutdownAutomatically, helpConsumer, helpWord,
            executor, linkedCacheSize, compiler, manager)
        if (listener != null) client.listener = listener
        return client
    }

    /**
     * Sets the owner for the bot.
     * <br></br>Make sure to verify that the ID provided is ISnowflake compatible when setting this.
     * If it is not, this will warn the developer.
     *
     * @param  ownerId
     * The ID of the owner.
     *
     * @return This builder
     */
    fun setOwnerId(ownerId: String): CommandClientBuilder {
        this.ownerId = ownerId
        return this
    }

    /**
     * Sets the one or more CoOwners of the bot.
     * <br></br>Make sure to verify that all of the IDs provided are ISnowflake compatible when setting this.
     * If it is not, this will warn the developer which ones are not.
     *
     * @param  coOwnerIds
     * The ID(s) of the CoOwners
     *
     * @return This builder
     */
    fun setCoOwnerIds(vararg coOwnerIds: String): CommandClientBuilder {
        //this.coOwnerIds = coOwnerIds
        return this
    }

    /**
     * Sets the bot's prefix.
     * <br></br>If set null, empty, or not set at all, the bot will use a mention @Botname as a prefix.
     *
     * @param  prefix
     * The prefix for the bot to use
     *
     * @return This builder
     */
    fun setPrefix(prefix: String): CommandClientBuilder {
        this.prefix = prefix
        return this
    }

    /**
     * Sets the bot's alternative prefix.
     * <br></br>If set null, the bot will only use its primary prefix prefix.
     *
     * @param  prefix
     * The alternative prefix for the bot to use
     *
     * @return This builder
     */
    fun setAlternativePrefix(prefix: String): CommandClientBuilder {
        this.altprefix = prefix
        return this
    }

    /**
     * Sets whether the [CommandClient][com.jagrosh.jdautilities.command.CommandClient] will use
     * the builder to automatically create a help command or not.
     *
     * @param  useHelp
     * `false` to disable the help command builder, otherwise the CommandClient
     * will use either the default or one provided via [com.jagrosh.jdautilities.command.CommandClientBuilder.setHelpConsumer]}.
     *
     * @return This builder
     */
    fun useHelpBuilder(useHelp: Boolean): CommandClientBuilder {
        this.useHelp = useHelp
        return this
    }

    /**
     * Sets the consumer to run as the bot's help command.
     * <br></br>Setting it to `null` or not setting this at all will cause the bot to use
     * the default help builder.
     *
     * @param  helpConsumer
     * A consumer to accept a [CommandEvent][com.jagrosh.jdautilities.command.CommandEvent]
     * when a help command is called.
     *
     * @return This builder
     */
    fun setHelpConsumer(helpConsumer: Consumer<CommandEvent>): CommandClientBuilder {
        this.helpConsumer = helpConsumer
        return this
    }

    /**
     * Sets the word used to trigger the command list.
     * <br></br>Setting this to `null` or not setting this at all will set the help word
     * to `"help"`.
     *
     * @param  helpWord
     * The word to trigger the help command
     *
     * @return This builder
     */
    fun setHelpWord(helpWord: String): CommandClientBuilder {
        this.helpWord = helpWord
        return this
    }

    /**
     * Sets the bot's support server invite.
     *
     * @param  serverInvite
     * The support server invite
     *
     * @return This builder
     */
    fun setServerInvite(serverInvite: String): CommandClientBuilder {
        this.serverInvite = serverInvite
        return this
    }

    /**
     * Sets the emojis for success, warning, and failure.
     *
     * @param  success
     * Emoji for success
     * @param  warning
     * Emoji for warning
     * @param  error
     * Emoji for failure
     *
     * @return This builder
     */
    fun setEmojis(success: String, warning: String, error: String): CommandClientBuilder {
        this.success = success
        this.warning = warning
        this.error = error
        return this
    }

    /**
     * Sets the [Game][net.dv8tion.jda.core.entities.Game] to use when the bot is ready.
     * <br></br>Can be set to `null` for no game.
     *
     * @param  game
     * The Game to use when the bot is ready
     *
     * @return This builder
     */
    fun setGame(game: Game): CommandClientBuilder {
        this.game = game
        return this
    }

    /**
     * Sets the [Game][net.dv8tion.jda.core.entities.Game] the bot will use as the default:
     * 'Playing **Type [prefix]help**'
     *
     * @return This builder
     */
    fun useDefaultGame(): CommandClientBuilder {
        this.game = Game.playing("default")
        return this
    }

    /**
     * Sets the [OnlineStatus][net.dv8tion.jda.core.OnlineStatus] the bot will use once Ready
     * This defaults to ONLINE
     *
     * @param  status
     * The status to set
     *
     * @return This builder
     */
    fun setStatus(status: OnlineStatus): CommandClientBuilder {
        this.status = status
        return this
    }

    /**
     * Adds a [Command][com.jagrosh.jdautilities.command.Command] and registers it to the
     * [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl] for this session.
     *
     * @param  command
     * The command to add
     *
     * @return This builder
     */
    fun addCommand(command: Command): CommandClientBuilder {
        commands.add(command)
        return this
    }

    /**
     * Adds and registers multiple [Command][com.jagrosh.jdautilities.command.Command]s to the
     * [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl] for this session.
     * <br></br>This is the same as calling [com.jagrosh.jdautilities.command.CommandClientBuilder.addCommand] multiple times.
     *
     * @param  commands
     * The Commands to add
     *
     * @return This builder
     */
    fun addCommands(vararg commands: Command): CommandClientBuilder {
        for (command in commands) this.addCommand(command)
        return this
    }

    /**
     * Adds an annotated command module to the
     * [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl] for this session.
     *
     *
     * For more information on annotated command modules, see
     * [the annotation package][com.jagrosh.jdautilities.command.annotation] documentation.
     *
     * @param  module
     * The annotated command module to add
     *
     * @return This builder
     *
     * @see AnnotatedModuleCompiler
     *
     * @see com.jagrosh.jdautilities.command.annotation.JDACommand
     */
    fun addAnnotatedModule(module: Any): CommandClientBuilder {
        this.commands.addAll(compiler.compile(module))
        return this
    }

    /**
     * Adds multiple annotated command modules to the
     * [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl] for this session.
     * <br></br>This is the same as calling [com.jagrosh.jdautilities.command.CommandClientBuilder.addAnnotatedModule] multiple times.
     *
     *
     * For more information on annotated command modules, see
     * [the annotation package][com.jagrosh.jdautilities.command.annotation] documentation.
     *
     * @param  modules
     * The annotated command modules to add
     *
     * @return This builder
     *
     * @see AnnotatedModuleCompiler
     *
     * @see com.jagrosh.jdautilities.command.annotation.JDACommand
     */
    fun addAnnotatedModules(vararg modules: Any): CommandClientBuilder {
        for (command in modules) addAnnotatedModule(command)
        return this
    }

    /**
     * Sets the [AnnotatedModuleCompiler][com.jagrosh.jdautilities.command.AnnotatedModuleCompiler]
     * for this CommandClientBuilder.
     *
     *
     * If not set this will be the default implementation found [ ].
     *
     * @param  compiler
     * The AnnotatedModuleCompiler to use
     *
     * @return This builder
     *
     * @see AnnotatedModuleCompiler
     *
     * @see com.jagrosh.jdautilities.command.annotation.JDACommand
     */
    fun setAnnotatedCompiler(compiler: AnnotatedModuleCompiler): CommandClientBuilder {
        this.compiler = compiler
        return this
    }

    /**
     * Sets the [Carbonitex](https://www.carbonitex.net/discord/bots) key for this bot's listing.
     *
     *
     * When set, the [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl]
     * will automatically update it's Carbonitex listing with relevant information such as server count.
     *
     * @param  key
     * A Carbonitex key
     *
     * @return This builder
     */
    fun setCarbonitexKey(key: String): CommandClientBuilder {
        this.carbonKey = key
        return this
    }

    /**
     * Sets the [Discord Bots](http://bots.discord.pw/) API key for this bot's listing.
     *
     *
     * When set, the [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl]
     * will automatically update it's Discord Bots listing with relevant information such as server count.
     *
     * @param  key
     * A Discord Bots API key
     *
     * @return This builder
     */
    fun setDiscordBotsKey(key: String): CommandClientBuilder {
        this.botsKey = key
        return this
    }

    /**
     * Sets the [Discord Bot List](https://discordbots.org/) API key for this bot's listing.
     *
     *
     * When set, the [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl]
     * will automatically update it's Discord Bot List listing with relevant information such as server count.
     *
     * @param  key
     * A Discord Bot List API key
     *
     * @return This builder
     */
    fun setDiscordBotListKey(key: String): CommandClientBuilder {
        this.botsOrgKey = key
        return this
    }

    /**
     * Sets the [CommandListener][com.jagrosh.jdautilities.command.CommandListener] for the
     * [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl].
     *
     * @param  listener
     * The CommandListener for the CommandClientImpl
     *
     * @return This builder
     */
    fun setListener(listener: CommandListener): CommandClientBuilder {
        this.listener = listener
        return this
    }

    /**
     * Sets the [ScheduledExecutorService][java.util.concurrent.ScheduledExecutorService] for the
     * [CommandClientImpl][com.jagrosh.jdautilities.command.impl.CommandClientImpl].
     *
     * @param  executor
     * The ScheduledExecutorService for the CommandClientImpl
     *
     * @return This builder
     */
    fun setScheduleExecutor(executor: ScheduledExecutorService): CommandClientBuilder {
        this.executor = executor
        return this
    }

    /**
     * Sets the Command Client to shut down internals automatically when a
     * [ShutdownEvent][net.dv8tion.jda.core.events.ShutdownEvent] is received.
     *
     * @param shutdownAutomatically
     * `false` to disable calling the shutdown method when a ShutdownEvent is received
     * @return This builder
     */
    fun setShutdownAutomatically(shutdownAutomatically: Boolean): CommandClientBuilder {
        this.shutdownAutomatically = shutdownAutomatically
        return this
    }

    /**
     * Sets the internal size of the client's [FixedSizeCache][com.jagrosh.jdautilities.commons.utils.FixedSizeCache]
     * used for caching and pairing the bot's response [Message][net.dv8tion.jda.core.entities.Message]s with
     * the calling Message's ID.
     *
     *
     * Higher cache size means that decay of cache contents will most likely occur later, allowing the deletion of
     * responses when the call is deleted to last for a longer duration. However this also means larger memory usage.
     *
     *
     * Setting `0` or negative will cause the client to not use linked caching **at all**.
     *
     * @param  linkedCacheSize
     * The maximum number of paired responses that can be cached, or `<1` if the
     * built [CommandClient][com.jagrosh.jdautilities.command.CommandClient]
     * will not use linked caching.
     *
     * @return This builder
     */
    fun setLinkedCacheSize(linkedCacheSize: Int): CommandClientBuilder {
        this.linkedCacheSize = linkedCacheSize
        return this
    }

    /**
     * Sets the [GuildSettingsManager][com.jagrosh.jdautilities.command.GuildSettingsManager]
     * for the CommandClientImpl built using this builder.
     *
     * @param  manager
     * The GuildSettingsManager to set.
     *
     * @return This builder
     */
    fun setGuildSettingsManager(manager: GuildSettingsManager<*>): CommandClientBuilder {
        this.manager = manager
        return this
    }
}
