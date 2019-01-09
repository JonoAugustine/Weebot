/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.WAITER
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.moderation.VCRoleManager.Limit.ALL
import com.ampro.weebot.commands.moderation.VCRoleManager.Limit.PUBLIC
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES

/* ***************
    VC Roles
 *****************/

/**
 * The [IPassive] manager that creates, assigns, removes, and deletes
 * VoiceChannel roles.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class VCRoleManager(var limit: Limit = ALL) : IPassive {

    /** Defines which voice channels will have roles made for them */
    enum class Limit {
        /** Make roles for all voice channels */
        ALL,
        /** Only make roles for voice channels open to @everyone*/
        PUBLIC
    }

    /** Whether the [IPassive] is set to be destroyed */
    var dead = false
    override fun dead() = dead

    /** [VoiceChannel.getIdLong] mapped to [Role.getIdLong] */
    private val genRoles = ConcurrentHashMap<Long, Long>()

    /** Deletes all roles created for [VCRoleManager] */
    fun clean(guild: Guild) {
        genRoles.values.forEach { guild.getRoleById(it)?.delete()?.queue() }
        genRoles.clear()
    }

    /**
     * Check if the voice channel allows VCRoles.
     *
     * @param voiceChannel The [VoiceChannel] to check
     */
    private fun limitSafe(voiceChannel: VoiceChannel) = if (limit == ALL) true
    else voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)?.allowed?.contains(VOICE_CONNECT) != false
            || voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)?.denied?.contains(VOICE_CONNECT) == false

    /**
     * When a user joins a voice channel, assign the Member a role named after
     * the VoiceChannel. If the role does not exist, it is made. When there are
     * no members in the voice channel, the role is deleted.
     */
    override fun accept(bot: Weebot, event: Event) {
        when (event) {
            is GuildVoiceJoinEvent -> {
                val guild = event.guild
                val channel = event.channelJoined
                if (!limitSafe(channel)) return
                val controller = guild.controller
                //Check the voice channel for existing roles
                genRoles[event.channelJoined.idLong]?.also {
                    guild.getRoleById(it)?.also { role ->
                        controller.addSingleRoleToMember(event.member, role)
                        return
                    }
                }
                guild.roles.find { it.name.equals(channel.name, true) }?.also { role ->
                    controller.addRolesToMember(event.member, role).queue()
                    genRoles[event.channelJoined.idLong] = role.idLong
                } ?: guild.controller.createRole().setName(channel.name)
                    .setHoisted(false).setMentionable(true)
                    .setPermissions(guild.publicRole.permissions).queue({
                        controller.addRolesToMember(event.member, it).queue()
                        genRoles[event.channelJoined.idLong] = it.idLong
                    }, {
                        bot.settings.sendLog(
                            """Failed to assign VoiceChannel Role: ${channel.name}
                            |To Member: ${event.member.effectiveName}
                            """.trimMargin())
                    })
            }
            is GuildVoiceUpdateEvent-> {
                val guild = event.guild
                val channel = event.channelLeft
                if (!limitSafe(channel)) return
                val controller = guild.controller
                genRoles[channel.idLong]?.also { ID ->
                    guild.getRoleById(ID)?.also { role ->
                        controller.removeSingleRoleFromMember(event.member, role)
                            .queue({}) {
                                bot.settings.sendLog("""Failed to Remove VoiceChannel Role: ${channel.name}
                                |From Member: ${event.member.effectiveName}
                                |""".trimMargin())
                            }
                        return
                    }
                }
                guild.roles.find {  it.name.equals(channel.name, true) }?.also { role ->
                    controller.removeSingleRoleFromMember(event.member, role).queue({
                        if (channel.members.isEmpty()) {
                            role.delete().reason("VCRoleManager").queue({}) {
                                bot.settings.sendLog("Failed to delete VoiceChannel Role: ${channel.name}")
                            }
                        }
                    }, {
                        bot.settings.sendLog(
                            """Failed to Remove VoiceChannel Role: ${channel.name}
                            |From Member: ${event.member.effectiveName}""".trimMargin())
                    })
                }
            }
        }

    }

    internal fun asEmbed(guild: Guild) : EmbedBuilder {
        val list = if (genRoles.isNotEmpty())
            """All live Voice Channel Roles in **${guild.name}:**
                ```css
                ${genRoles.map { guild.getRoleById(it.value)?.name ?: ""}
                .filterNot { it.isBlank() }.joinToString(", ")}
                ```""".trimIndent()
        else "No Roles Yet"
        return makeEmbedBuilder("Voice Channel Role Manager", null, """
            $list
        ${if (limit != ALL) "(Limited to **Public** Channels)" else ""}
        """.trimIndent())
    }

}

/**
 * A Controller Command for the passive [VCRoleManager].
 * Can enable and change the VCRole limits
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdVoiceChannelRole : WeebotCommand("voicechannelrole", "Voice Channel Roles",
    arrayOf("vcr", "vcrole"), CAT_UTIL, "[enable/disable] [limit] or [limit]",
    "A manager that creates, assigns, removes, and deletes VoiceChannel roles.",
    cooldown = 10, userPerms = arrayOf(MANAGE_ROLES),
    botPerms = arrayOf(MANAGE_ROLES)
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Voice Channel Roles")
            .setDescription("Allow me to create and automatically assign ")
            .addToDesc("roles to anyone in a voice channel. The roles will ")
            .addToDesc("have the same name as the channel and can be ")
            .addToDesc("@/mentioned by anyone. You can choose to restrict ")
            .addToDesc("which channels get roles by their publicity (all ")
            .addToDesc("channels or only channels public to @/everyone)")
            .addField("Enable/Disable", "<on/off> /limit/", true)
            .addField("Change Limit", """Changes the Channels that will have roles
                |``<all/public>``""".trimMargin(), true)
            .setAliases(aliases)
            .build()
    }

    companion object {
        val activatedEmbed: MessageEmbed
            get() = strdEmbedBuilder
                .setTitle("Voice Channel Roles Activated!")
                .setDescription("""
                |The next time someone joins a voice channel, they will
                |be assigned a Role with the same name of the channel that can
                |be mentioned by anyone.
            """.trimMargin()).build()
    }

    override fun execute(event: CommandEvent) {
        val bot = getWeebotOrNew(event.guild)
        val args = event.splitArgs()
        STAT.track(this, bot, event.author, event.creationTime)
        val vcRoleManager = bot.getPassive<VCRoleManager>()
        when {
            args.isEmpty() -> {
                vcRoleManager?.asEmbed(event.guild)?.build()?.also {
                    event.reply(it)
                } ?: event.respondThenDelete("No Voice Channel Role Manager active.")
            }
            args[0].matchesAny(REG_ON, REG_ENABLE) -> {
                if (vcRoleManager == null) {
                    val lim = if (args.size > 1) {
                        try {
                            VCRoleManager.Limit.valueOf(args[1].toUpperCase())
                        } catch (e: Exception) {
                            event.reply("${args[1]} is not a valid restriction. ``w!help vcrole``")
                            return
                        }
                    } else { ALL }
                    bot.add(VCRoleManager(lim))
                    event.reply(activatedEmbed)
                    return
                } else {
                    event.reply("*The VoiceChannelRole is already enabled.* ``w!help vcrole``")
                    return
                }
            }
            args[0].matchesAny(REG_OFF, REG_DISABLE) -> {
                if (vcRoleManager != null) {
                    vcRoleManager.clean(event.guild)
                    vcRoleManager.dead = true
                    event.reply("*VoiceChannelRoles are now disabled*")
                    return
                } else {
                    event.reply("*VoiceChannelRoles are not enabled.*")
                    return
                }
            }
            args[0].matches(REG_HYPHEN + "al+") -> {
                vcRoleManager?.also {
                    it.limit = ALL
                    event.reply("*Voice Channel Role Manager set to watch All Channels.*")
                } ?: event.respondThenDelete(
                    "There is no VCRole Manager active. Use ``vcr on``"
                )
            }
            args[0].matches(REG_HYPHEN + "pub(lic)?") -> {
                vcRoleManager?.also {
                    it.limit = PUBLIC
                    event.reply(
                        "*Voice Channel Role Manager set to only watch Public Channels.*")
                } ?: event.respondThenDelete(
                    "There is no VCRole Manager active. Use ``vcr on``"
                )
            }
            args[0].matches(REG_HYPHEN + "c(lear)?") -> {
                vcRoleManager?.apply {
                    clean(event.guild)
                    event.respondThenDelete("VCRoles Cleared")
                } ?: event.respondThenDelete(
                    "There is no VCRole Manager active. Use ``vcr on``"
                )
            }
        }

    }

}

/* *******************
    Personal Auto VC
 *********************/

/**
 * Creates an item list for a [SelectablePaginator]
 *
 * @param action
 */
internal fun <T> Iterable<T>.paginate(string: (T) -> String, action: (T) -> Unit)
        = map { string(it) to { _: Int, _: Message -> action(it) } }

internal fun nameGen(member: Member) = "${member.effectiveName}'s Channel"

internal fun nameGen(member: Member, format: String)
        = format.replace(Regex("(?i)(\\{U+S+E+R+})"), member.effectiveName)

/**
 * Creates a temp [VoiceChannel] for a User after they join a designated
 * "[VCGenerator.baseId]". The [VoiceChannel] is deleted on empty.
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class VCGenerator(baseChannel: Long) : IPassive {
    constructor(baseChannel: VoiceChannel) : this(baseChannel.idLong)

    private var dead = false
    override fun dead() = dead

    /** Whether the generator is set to shutdown (block incoming requests then die)*/
    internal var inShutdown = false

    /**
     * @param limit the max users that can join the channel 1-99 (0 if no limit)
     * @param name The name of the Generated [VoiceChannel]. 1-99 char
     *
     * @throws IllegalArgumentException if either param is not in the allowed range
     */
    internal class Settings(var limit: Int = 0, var name: String = "",
                                 var categoryID: Long? = null) {
        init {
            if (limit !in 0..99)
                throw IllegalArgumentException(limit.toString())
            if (name.length > 99)
                throw IllegalArgumentException(name)
        }
        fun asEmbed(member: Member) : EmbedBuilder {
            return makeEmbedBuilder("${member.effectiveName}'s Voice Channel Generator",
                description = """
                    **User limit:** ${if (limit == 0) "Infinity" else "$limit"}
                    **Channel name:** ${ if (name.isNotBlank()) name else nameGen(member)}
                """.trimIndent())
                .setColor(member.color ?: STD_GREEN)
        }
    }

    /** User default settings for their generated channel */
    internal val userSettings = ConcurrentHashMap<Long, Settings>()

    /** The Guild Settings for default Generated Channels */
    internal val guildSettings = Settings(0, "{USER}'s Channel")

    /** The generator Channel */
    var baseId: Long = baseChannel

    /** All active generated [VoiceChannel]s mapped to user IDs */
    internal val genChannels = ConcurrentHashMap<Long, Long>()

    override fun accept(bot: Weebot, event: Event) {
        fun onJoin(guild: Guild, member: Member, channel: VoiceChannel) : Boolean {
            if (dead || inShutdown) return false
            clean(guild)
            var r = false
            if (channel.idLong == baseId
                    && !genChannels.containsKey(member.user.idLong)) {
                val base = guild.getVoiceChannelById(baseId)
                val settings = userSettings
                    .getOrDefault(member.user.idLong, guildSettings)

                guild.controller.createVoiceChannel(nameGen(member, settings.name))
                    .setUserlimit(settings.limit).apply {
                        val tCat = if (guildSettings.categoryID != null) {
                            guild.getCategoryById(guildSettings.categoryID!!) ?: base.parent
                        } else base.parent
                        if (tCat != null) setParent(tCat)
                    }.queue({ vc ->
                        genChannels[member.user.idLong] = vc.idLong
                        bot.settings.sendLog("AutoGen Channel created: ``${vc.name}``.")
                        if (guild.selfMember hasPerm VOICE_MOVE_OTHERS) {
                            guild.controller.moveVoiceMember(member, vc as VoiceChannel)
                                .queueAfter(205, MILLISECONDS)
                        } else WAITER.waitForEvent(GuildVoiceJoinEvent::class.java,
                            { it.channelJoined.id == vc.id }, {}, 2L, MINUTES) {
                            //Timout Delete VC
                            vc.delete().reason("Empty").queue {
                                bot.settings.sendLog(
                                    "AutoGen Channel deleted: ``${vc.name}`` (Timed out).")
                            }
                        }
                    }, { bot.settings.sendLog("Failed to generate voice channel for: " +
                                member.asMention)})
                r = true
            }
            if (genChannels.isEmpty() && inShutdown) shutdown(guild)
            return r
        }
        fun onLeave(guild: Guild, member: Member, channel: VoiceChannel) : Boolean {
            clean(guild)
            var r = false
            if (genChannels.containsValue(channel.idLong) && channel.members.isEmpty()) {

                genChannels.remove(member.user.idLong)
                val name = channel.name
                channel.delete().reason("Empty").queue (
                    {bot.settings.sendLog("AutoGen Channel deleted: ``$name`` (Empty).")}
                    ,{})
                r = true
            }
            if (genChannels.isEmpty() && inShutdown) shutdown(guild)
            return r
        }
        when (event) {
            is GuildVoiceJoinEvent -> onJoin(event.guild, event.member, event.channelJoined)
            is GuildVoiceLeaveEvent -> onLeave(event.guild, event.member, event.channelLeft)
            is GuildVoiceMoveEvent -> {
                //The checks for ID are in the methods, so we just send it
                if (!onJoin(event.guild, event.member, event.channelJoined))
                    onLeave(event.guild, event.member, event.channelLeft)
            }
        }
    }

    fun shutdown(guild: Guild) {
        clean(guild, true)
        dead = true
    }

    /**
     * @param full If true, all channels will be removed from the guild
     */
    private fun clean(guild: Guild, full: Boolean = false) {
        genChannels.removeIf { _, id -> !guild.voiceChannels.has { it.idLong == id } }
        if (full) genChannels.forEach { entry ->
            guild.getVoiceChannelById(entry.value)?.delete()?.queueAfter(250, MILLISECONDS)
        }
    }

    fun asEmbed(guild: Guild) : EmbedBuilder {
            genChannels.removeIf { _, vcID ->
                !guild.voiceChannels.has { it.idLong == vcID }
            }
            val channel = guild.getVoiceChannelById(baseId)?.name
                    ?: "Unknown! The channel may have been deleted."
            val category = guild.getCategoryById(guildSettings.categoryID ?: -1)?.name
                    ?: guild.getVoiceChannelById(baseId)?.parent?.name ?: "Undefined"
            val limit = if (guildSettings.limit == 0) "Infinity" else "${guildSettings.limit}"

            val desc = """
            **VCGenerator Channel:** $channel
            **Channel Category:** $category
            **Default User limit:** $limit
            **Default Channel Name:** ${guildSettings.name}
        """.trimIndent()

            return makeEmbedBuilder("${guild.name}'s Voice Channel Generator",
                description = desc).addField("Open Channels",
                if (genChannels.isEmpty()) "none" else genChannels.map
                { guild.getVoiceChannelById(it.value)?.name ?: "" }.joinToString(", "), false)
                .setColor(guild.roles[0].color ?: STD_GREEN)
        }

}

/**
 * A [WeebotCommand] to moderate a [Guild]'s [VCGenerator] or create temporary
 * [VoiceChannel].
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class CmdVoiceChannelGenerator : WeebotCommand("voicechannelgenerator",
    "Voice Channel Generator", arrayOf("vcg", "vcgenerator", "vcgen"), CAT_UTIL, "",
    "Creates a temp VoiceChannel for a User after joining a designated Voice Channel",
    guildOnly = true, children = arrayOf(SubCmdEnable(), SubCmdDisable(),
        SubCmdServerSettings(), SubCmdUserDefaults(), SubCedManualTemp())) {

    /** Turn ON  */
    internal class SubCmdEnable : WeebotCommand("enable", null, arrayOf("on"),
        CAT_MOD, "", "",guildOnly = true, cooldown = 30,
        botPerms = arrayOf(MANAGE_CHANNEL), userPerms = arrayOf(MANAGE_CHANNEL)) {
        public override fun execute(event: CommandEvent) {
            val bot = getWeebotOrNew(event.guild)
            fun newChannel(message: Message, action: () -> Unit) {
                event.reply("Please enter a name for the new voice channel:") { sm ->
                    WAITER.waitForEvent(GuildMessageReceivedEvent::class.java, {
                        if (it.isValidUser(event.guild, setOf(
                                    event.author))) if (it.message.contentDisplay.length !in 1..99) {
                            event.reply("The name must be under 99 characters.")
                            false
                        } else true
                        else false
                    }, { receivedEvent ->
                        receivedEvent.guild.controller
                            .createVoiceChannel(receivedEvent.message.contentDisplay)
                            .queue {
                                sm.delete().queueAfter(250, MILLISECONDS)
                                receivedEvent.message.delete().queueAfter(250, MILLISECONDS)
                                val vcg = VCGenerator(it as VoiceChannel)
                                bot.add(vcg)
                                message.clearReactions().queue({ action() }, { action() })
                            }
                    })
                }
            }
            fun finish(message: Message, vcGenerator: VCGenerator) {
                val e = makeEmbedBuilder("VCGenerator Setup Complete!", description = """
                        Your Voice Channel Generator has been initialized!
                        Whenever you join ${event.guild.getVoiceChannelById(
                    vcGenerator.baseId)?.name}, a temporary VC will be created!
                    As soon as the channel is empty, it will be deleted.
                    (*If the channel is not joined within 1 minute, it will be deleted*)
                    """.trimIndent()).build()
                message.clearReactions().queueAfter(250, MILLISECONDS)
                message.editMessage(e).queue({
                    vcGenerator.asEmbed(event.guild).build()
                }) {
                    event.reply(e)
                    event.reply(vcGenerator.asEmbed(event.guild).build())
                }
            }
            fun chooseCategory(message: Message, vcg: VCGenerator) {
                SelectablePaginator(setOf(event.author), singleUse = true,
                    title = "VCGenerator Base Channel Chosen!", description = """
                        Next, I need you to specify a Category for the generated VCs.
                        Reacting with $X_Red to this message will set generate VCs in
                        the same category as the Generator Voice Channel.
                        """.trimIndent(),
                    items = event.guild.categories.paginate({ it.name }) {
                        vcg.guildSettings.categoryID = it.idLong
                        finish(message, vcg)
                    }, exitAction = { finish(message, vcg) }) {
                    finish(message, vcg)
                }.displayOrDefault(message, event.channel)
            }
            fun chooseChannel(message: Message) {
                SelectablePaginator(setOf(event.author), singleUse = true,
                    title = "Voice Channel Generator Enabled!", description = """
                            Next, I need you to specify a Generator Voice Channel.
                            When members join this channel, they will have a VC generated for them.
                            You can choose from the VC you already have or you can
                            generate a new VC by reacting with $X_Red to this message.
                        """.trimIndent(),
                    items = event.guild.voiceChannels.paginate({ it.name }) {
                        val vcg = VCGenerator(it); bot.add(vcg)
                        if (event.guild.categories.isNotEmpty())
                            chooseCategory(message, vcg)
                        else finish(message, vcg)
                    }, exitAction = {
                        newChannel(message) { chooseChannel(it) }
                    }, timeoutAction = {}).displayOrDefault(message, event.channel)
            }
            fun makeFirstChannel(message: Message) {
                SelectableEmbed(event.author, true,
                    makeEmbedBuilder("Voice Channel Generator", null, """
                        $Pencil to create a new Voice Channel
                        """.trimIndent()).build(),
                    listOf(Pencil to { _:Message, _:User ->
                        newChannel(message) {chooseChannel(message)}
                    })) {
                    it.editMessage("*Timed Out*").queue { m ->
                        m.clearReactions().queue()
                    }}.displayOrDefault(message, event.channel)
            }

            //Check for running instance
            bot.getPassive<VCGenerator>()?.also {
                if (it.inShutdown) {
                    it.shutdown(event.guild)
                    bot.passives.remove(it)
                } else {
                    event.respondThenDelete("*The Voice Channel Generator is already active.*", 5)
                    return
                }
            }

            STAT.track(this, bot, event.author, event.creationTime)

            //Build new VCG
            event.reply("*Getting Things Ready...*") { m ->
                //If there are no VCs, ask them to make one
                if (event.guild.voiceChannels.isEmpty()) {
                    m.editMessage("There are no Voice Channels!...please make one.")
                        .queue({makeFirstChannel(m)}, {makeFirstChannel(m)})
                }
                else chooseChannel(m)
            }
        }
    }

    /** Turn OFF */
    internal class SubCmdDisable : WeebotCommand("disable",null, arrayOf("off"), CAT_MOD,
        "", "", guildOnly = true, cooldown = 30,  botPerms = arrayOf(MANAGE_CHANNEL),
        userPerms = arrayOf(MANAGE_CHANNEL)) {
        public override fun execute(event: CommandEvent) {
            getWeebotOrNew(event.guild).also { bot ->
                bot.getPassive<VCGenerator>()?.also {
                    STAT.track(this, bot, event.author, event.creationTime)
                    it.shutdown(event.guild)
                    event.reply("""Voice Channel Generator has been placed in shutdown;
                |No more channels will be created, and all remaining channels will
                |be closed on exit.
                |""".trimMargin())
                } ?: event.respondThenDelete("There is no VCGenerator active", 5)
            }
        }
    }

    /** Set Server Defaults */
    internal class SubCmdServerSettings : WeebotCommand("def",null,
        arrayOf("serverdefaults", "sdef", "servdef"), CAT_MOD, "", "",
        userPerms = arrayOf(MANAGE_CHANNEL), guildOnly = true, cooldown = 30) {
        override fun execute(event: CommandEvent) {
            getWeebotOrNew(event.guild).also { bot ->
                bot.getPassive<VCGenerator>()?.also { vcg ->
                    STAT.track(this, bot, event.author, event.creationTime)
                    SelectableEmbed(event.author, false,
                        vcg.asEmbed(event.guild).addField("Guide", """
                        $Speaker to change the VG Generator Channel
                        $OpenFileFolder to change the Generator Category
                        $NoEntry to change the default User Limit
                        $Pencil to change the default Channel Name
                    """.trimIndent(), true).build(),
                        listOf(
                            Speaker to { m: Message, _: User ->
                                fun newChannel() = event.reply(
                                    "Please enter a name for the new voice channel:"
                                ) { sm ->
                                WAITER.waitForEvent(
                                    GuildMessageReceivedEvent::class.java, {
                                        if (it.isValidUser(event.guild, setOf(event.author))) if (it.message.contentDisplay.length !in 1..99) {
                                            event.reply("The name must be under 99 characters.")
                                            false
                                        } else true
                                        else false
                                    }, { receivedEvent ->
                                        receivedEvent.guild.controller
                                            .createVoiceChannel(receivedEvent.message.contentDisplay)
                                            .queue {
                                                sm.delete().queueAfter(250, MILLISECONDS)
                                                receivedEvent.message.delete()
                                                    .queueAfter(250, MILLISECONDS)
                                                vcg.baseId = it.idLong
                                            }
                                    })
                            }
                                fun chooseChannel(message: Message) {
                                SelectablePaginator(setOf(event.author), singleUse = true,
                                    title = "Select a Voice Channel", description = """
                                        When members join this channel, they will have a VC generated for them.
                                        You can choose from the VC you already have or you can
                                        generate a new VC by reacting with $X_Red to this message.
                                        """.trimIndent(),
                                    items = event.guild.voiceChannels.paginate(
                                        { it.name }) {
                                        vcg.baseId = it.idLong
                                        event.reply(
                                            "*VCGenerator generator channel set to ${it.name}*")
                                        message.delete().queue({}, {
                                            message.clearReactions().queue()
                                        })
                                    }, exitAction = {
                                        newChannel()
                                    }, timeoutAction = {}).displayOrDefault(message,
                                    event.channel)
                            }
                                m.clearReactions().queueAfter(250, MILLISECONDS)
                                if (event.guild.voiceChannels.isEmpty()) {
                                    event.respondThenDelete("There are no Voice Channels!")
                                    newChannel()
                                } else chooseChannel(m)
                        }, OpenFileFolder to { m: Message, _: User ->
                                if (event.guild.categories.isNotEmpty()) {
                                    SelectablePaginator(setOf(event.author),
                                        singleUse = true, title = "VCGenerator Category",
                                        description = """
                                        Next, I need you to specify a Category for the generated VCs.
                                        Reacting with $X_Red to this message will set generate VCs in
                                        the same category as the Generator Voice Channel.
                                        """.trimIndent(),
                                        items = event.guild.categories.paginate(
                                            { it.name }) {
                                            vcg.guildSettings.categoryID = it.idLong
                                            event.reply("*Category set to ${it.name}.*")
                                        }, exitAction = {
                                            vcg.guildSettings.categoryID = null
                                            event.reply("*Category will be automatic.*")
                                        }) {
                                        it.clearReactions().queueAfter(250, MILLISECONDS)
                                        it.editMessage("*Timed Out*").queue()
                                    }.displayOrDefault(m, event.channel)
                                } else {
                                    event.reply("There are no Categories. Please make " +
                                            "one then try again.")
                                }
                            }, NoEntry to { _: Message, _: User ->
                                event.reply("Enter a number, 0 to 99. 0 = no limit")
                                WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                                    { e ->
                                        e.isValidUser(event.guild,setOf(event.author))
                                                && try {
                                            if (e.message.contentDisplay.toInt() !in 0..99) {
                                                event.reply(
                                                    "Enter a number, 0 to 99. 0 = no limit")
                                                false
                                            } else true
                                        } catch (e: NumberFormatException) {
                                            event.reply(
                                                "Enter a number, 0 to 99. 0 = no limit")
                                            false
                                        }
                                    }, { e ->
                                        val i = e.message.contentDisplay.toInt()
                                        vcg.guildSettings.limit = i
                                        val m = if (i == 0) "no limit" else i.toString()
                                        event.reply("Default user limit set to $m")
                                    }, 1, MINUTES, { event.reply("*Timed Out*") })
                            }, Pencil to { _: Message, _: User ->
                                event.reply("""
                                    What would you like the new default name to be?
                                    ``{USER}`` will be replaced with the user's name.
                                    For example, ``{USER}'s room`` becomes ``Bill's Room``
                                """.trimIndent())
                                WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                                    { e ->
                                        e.isValidUser(event.guild, setOf(event.author))
                                        && if (e.message.contentDisplay.length !in 1..99) {
                                            event.reply(
                                                """The name must be under 99 characters (including spaces). Please try again.""")
                                            false
                                        }else true
                                    }, { e ->
                                        val n = e.message.contentDisplay
                                        vcg.guildSettings.name = n
                                        event.reply("Default name set to ``$n``")
                                    }, 1, MINUTES, { event.reply("*timed out*")})
                        })) { it.clearReactions().queueAfter(250, MILLISECONDS) }
                        .display(event.channel)

                } ?: event.reply("*No VCGenerator is active.*")
            }
        }
    }

    /** Set User Settings */
    internal class SubCmdUserDefaults : WeebotCommand("set",null, arrayOf("mydef", "my"),
        CAT_UTIL, "", "", cooldown = 30, guildOnly = true) {
        override fun execute(event: CommandEvent) {
            getWeebotOrNew(event.guild).also { bot ->
                STAT.track(this, bot, event.author, event.creationTime)
                bot.getPassive<VCGenerator>()?.also { vcg ->
                    var set = vcg.userSettings[event.author.idLong] ?: vcg.guildSettings
                    SelectableEmbed(event.author, false,
                        set.asEmbed(event.member).addField("Guide", """
                        $Pencil to change the default Channel Name
                        $NoEntry to change the default User Limit
                    """.trimIndent(), true).build(),listOf(
                            NoEntry to { _: Message, _: User ->
                                event.reply("Enter a number, 0 to 99. 0 = no limit")
                                WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                                    { e ->
                                        e.isValidUser(event.guild,setOf(event.author))
                                                && try {
                                            if (e.message.contentDisplay.toInt() !in 0..99) {
                                                event.reply(
                                                    "Enter a number, 0 to 99. 0 = no limit")
                                                false
                                            } else true
                                        } catch (e: NumberFormatException) {
                                            event.reply(
                                                "Enter a number, 0 to 99. 0 = no limit")
                                            false
                                        }
                                    }, { e ->
                                        val i = e.message.contentDisplay.toInt()
                                        if (set == vcg.guildSettings) {
                                            set = VCGenerator.Settings(i, set.name)
                                            vcg.userSettings[event.author.idLong] = set
                                        }
                                        else set.limit = i
                                        val m = if (i == 0) "no limit" else i.toString()
                                        event.reply("User limit set to $m")
                                    }, 1, MINUTES, { event.reply("*Timed Out*") })
                            }, Pencil to { _: Message, _: User ->
                                event.reply("""
                                    What would you like the new default name to be?
                                    ``{USER}`` will be replaced with the user's name.
                                    For example, ``{USER}'s room`` becomes ``${event
                                    .member.effectiveName}'s Room``
                                """.trimIndent())
                                WAITER.waitForEvent(GuildMessageReceivedEvent::class.java,
                                    { e ->
                                        e.isValidUser(event.guild, setOf(event.author))
                                                && if (e.message.contentDisplay.length !in 1..99) {
                                            event.reply(
                                                """The name must be under 99 characters (including spaces). Please try again.""")
                                            false
                                        }else true
                                    }, { e ->
                                        val n = e.message.contentDisplay
                                        if (set == vcg.guildSettings) {
                                            set = VCGenerator.Settings(set.limit, n)
                                            vcg.userSettings[event.author.idLong] = set
                                        }
                                        else set.name = n
                                        event.reply("Channel name set to ``$n``")
                                    }, 1, MINUTES, { event.reply("*timed out*")})
                            })) { it.clearReactions().queueAfter(250, MILLISECONDS) }
                        .display(event.channel)
                } ?: event.reply("*No VCGenerator is active.*")
            }
        }
    }

    /** Manual Temp Channel */
    internal class SubCedManualTemp : WeebotCommand("temp", null,arrayOf("manual"),
        CAT_UTIL, "", "", cooldown = 30, guildOnly = true,
        userPerms = arrayOf(MANAGE_CHANNEL), botPerms = arrayOf(MANAGE_CHANNEL)) {
        override fun execute(event: CommandEvent) {
            getWeebotOrNew(event.guild).also { bot ->
                STAT.track(this, bot, event.author, event.creationTime)
                val vcg = bot.getPassive() ?: VCGenerator(-1).apply {
                    bot.add(this)
                    inShutdown = true
                }
                //[-L userLimit] [-c category-name] [name]
                val args = event.splitArgs()
                var name: String = nameGen(event.member,
                    vcg.userSettings[event.author.idLong]?.name ?: vcg.guildSettings.name)
                var limit = 0
                var cat = event.textChannel.parent ?: vcg.guildSettings.categoryID
                            ?.let { event.guild.getCategoryById(it) }

                var nameIndex = 0

                val limitIndex = args.indexOfFirst {
                    it.matches(REG_HYPHEN + "(l(imit)?|u(sers?)?)")
                }
                if (limitIndex != -1) {
                    limit = try {
                        val i = args[limitIndex + 1].toInt()
                        if (i !in 0..99) throw NumberFormatException()
                        i
                    } catch (e: NumberFormatException) {
                        event.replyError(
                            "``${args[limitIndex + 1]}`` is not a number 0 to 99")
                        return
                    } catch (e: IndexOutOfBoundsException) {
                        event.replyError("User limit is not specified")
                        return
                    }
                    nameIndex += 2
                }

                val catIndex = args.indexOfFirst {
                    it.matches(REG_HYPHEN + "c(at(egory)?)?")
                }
                if (catIndex != -1) {
                    cat = try {
                        val cName = args[catIndex + 1].replace('-', ' ')
                        event.guild.getCategoriesByName(cName, true)[0]
                    } catch (e: IndexOutOfBoundsException) {
                        event.replyError("No Category could be found.")
                        return
                    }
                    nameIndex += 2
                }

                if (nameIndex in 0 until args.size) {
                    name = args.subList(nameIndex, args.size).joinToString(" ")
                } else if (name.isBlank()) {
                    name = nameGen(event.member)
                }
                val controller = event.guild.controller
                controller.createVoiceChannel(name).apply {
                    if (cat != null) setParent(cat)
                    setUserlimit(limit)
                }.queue({ vc ->
                    vcg.genChannels[event.member.user.idLong] = vc.idLong
                    bot.settings.sendLog("AutoGen Channel created: ``${vc.name}``.")
                    if (event.guild.selfMember hasPerm VOICE_MOVE_OTHERS && event.member.voiceState.inVoiceChannel()) {
                        controller.moveVoiceMember(event.member, vc as VoiceChannel)
                            .queueAfter(205, MILLISECONDS)
                    } else WAITER.waitForEvent(GuildVoiceJoinEvent::class.java,
                        { it.channelJoined.id == vc.id }, {}, 2L, MINUTES) {
                        //Timout Delete VC
                        vc.delete().reason("Empty").queue {
                            bot.settings.sendLog(
                                "AutoGen Channel deleted: ``${vc.name}`` (Timed out).")
                        }
                    }
                }, { bot.settings.sendLog("Failed to generate voice channel for: " +
                        event.member.asMention)
                })

            }

        }
    }

    override fun execute(event: CommandEvent) {
        val vcGenerator = getWeebotOrNew(event.guild).getPassive<VCGenerator>()
        val embed: MessageEmbed = vcGenerator?.asEmbed(event.guild)?.build() ?: run {
            event.reply("*No Voice Channel Generator active. Use ``vcg on`` to start!*")
            return
        }
        event.reply(embed)
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Voice Channel Generator", """
            Creates a temp VoiceChannel for a User after joining a designated Voice Channel.

            **Changing Settings**
            There are two settings for Generated Voice Channels: *(User) Limit* and *Name*
            When settings these, it is important to follow these guidelines.
            **User Limit** can be any number from 0 to 99 (0 means there is no limit).
            **Name** is the name of the generated channel and will replace ``{USER}``
            with the user's name. For example, ``{USER}'s room`` becomes ``Bill's Room``
        """.trimIndent())
            .setAliases(aliases)
            .addField("Enable/Disable Auto Generator",
                "``on/off``\n*Need ${MANAGE_CHANNEL.getName()} permission.*", true)
            .addField("Set/See Server Defaults","""``def``
                    *Need ${MANAGE_CHANNEL.getName()} permission.*""".trimIndent(),true)
            .addField("Set/See Your Defaults","``set`` or ``mydef``",true)
            .addField("Manually Create Temporary Voice Channel", """
                ``temp [-L userLimit] [-c category] [name]``
                *Must have ${MANAGE_CHANNEL.getName()} permission.*
            """.trimIndent(), true)
            .build()
    }
}
