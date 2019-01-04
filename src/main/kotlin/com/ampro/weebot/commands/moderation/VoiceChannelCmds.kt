/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.moderation

import com.ampro.weebot.WAITER
import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.moderation.VCGenerator.Settings
import com.ampro.weebot.commands.moderation.VCRoleManager.Limit.*
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.Emoji.Pencil
import com.ampro.weebot.util.Emoji.X_Red
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent
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
        PUBLIC,
        /** Don't make roles for any channel (turn it off) */
        NONE
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
    private fun limitSafe(voiceChannel: VoiceChannel) = when (limit) {
        ALL -> true
        NONE -> false
        PUBLIC -> voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                    ?.allowed?.contains(VOICE_CONNECT) ?: true
                || !(voiceChannel.getPermissionOverride(voiceChannel.guild.publicRole)
                ?.denied?.contains(VOICE_CONNECT) ?: true)
    }

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

}

/**
 * A Controller Command for the passive [VCRoleManager].
 * Can enable and change the VCRole limits
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdVoiceChannelRole : WeebotCommand("voicechannelrole",
    arrayOf("vcrc","vcr","vrc", "vcrole"), CAT_MOD, "[enable/disable] [limit] or [limit]",
    "A manager that creates, assigns, removes, and deletes VoiceChannel roles.",
    cooldown = 10, userPerms = arrayOf(MANAGE_ROLES),
    botPerms = arrayOf(MANAGE_ROLES)
) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Voice Channel Roles")
            .setDescription("Allow Weebot to create and automatically assign ")
            .addToDesc("roles to anyone in a voice channel. The roles will ")
            .addToDesc("have the same name as the channel and can be ")
            .addToDesc("@/mentioned by anyone. You can choose to restrict ")
            .addToDesc("which channels get roles by their publicity (all ")
            .addToDesc("channels or only channels public to @/everyone)")
            .addField("Arguments", "[enable/disable/on/off] [all/public]" +
                    "\n[all/public] (if already enabled)", false)
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
        if (args.isEmpty()) return
        STAT.track(this, bot, event.author)
        val vcp = bot.getPassive<VCRoleManager>()
        when (args[0].toUpperCase()) {
            "ENABLE", "ON" -> {
                if (vcp == null) {
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
            "DISABLE", "OFF" -> {
                if (vcp != null) {
                    vcp.clean(event.guild)
                    vcp.dead = true
                    event.reply("*VoiceChannelRoles are now disabled*")
                    return
                } else {
                    event.reply("*VoiceChannelRoles are not enabled.*")
                    return
                }
            }
            "SETLIMIT", "SL", "LIMIT" -> {
                if (vcp != null) {
                    vcp.limit = if (args.size > 1) {
                        try {
                            VCRoleManager.Limit.valueOf(args[1].toUpperCase())
                        } catch (e: Exception) {
                            event.reply("${args[1]} is not a valid restriction. ``w!help vcrole``")
                            return
                        }
                    } else {
                        //TODO
                        return
                    }
                } else {
                    //TODO
                }
            }
            ALL.name -> { //TODO

            }
            NONE.name -> {//TODO

            }
            PUBLIC.name -> {//TODO

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
 * Creates a temp [VoiceChannel] for a User after they join a designated"[baseChannel]".
 * The [VoiceChannel] is deleted on empty.
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class VCGenerator(baseChannel: VoiceChannel) : IPassive {
    var dead = false
    override fun dead() = dead

    /** Whether the generator is set to shutdown (block incoming requests then die)*/
    var inShutdown = false

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
        fun asEmbed(member: Member) : MessageEmbed {
            return makeEmbedBuilder("${member.effectiveName}'s Voice Channel Generator",
                description = """
                    **User limit:** ${if (limit == 0) "Infinity" else "$limit"}
                    **Channel name:** ${ if (name.isNotBlank()) name else nameGen(member)}
                """.trimIndent())
                .setColor(member.color ?: STD_GREEN)
                .build()
        }
    }

    /** User default settings for their generated channel */
    internal val userSettings = ConcurrentHashMap<Long, Settings>()

    /** The Guild Settings for default Generated Channels */
    internal val guildSettings = Settings(0, "{USER}'s Channel")

    /** The generator Channel */
    var baseId: Long = baseChannel.idLong

    /** All active generated [VoiceChannel]s mapped to user IDs */
    val genChannels = ConcurrentHashMap<Long, Long>()

    override fun accept(bot: Weebot, event: Event) {
        when (event) {
            is GuildVoiceJoinEvent -> {
                if (dead || inShutdown) return
                val channel = event.channelJoined
                val member = event.member

                if (channel.idLong == baseId
                        && !genChannels.containsKey(member.user.idLong)) {
                    val guild = event.guild
                    val base = guild.getVoiceChannelById(baseId)
                    val settings = userSettings
                        .getOrDefault(member.user.idLong, guildSettings)

                    guild.controller.createVoiceChannel(
                        nameGen(event.member, settings.name)
                    ).setUserlimit(settings.limit).apply {
                        val tCat = if (guildSettings.categoryID != null) {
                            guild.getCategoryById(guildSettings.categoryID!!) ?: base.parent
                        } else base.parent
                        if (tCat != null) setParent(tCat)
                    }.queue { vc ->
                        genChannels.put(event.member.user.idLong, vc.idLong)
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
                    }
                }
            }
            is GuildVoiceUpdateEvent -> {
                if (genChannels[event.member.user.idLong]?.equals(event.channelLeft.idLong) == true
                        && event.channelLeft.members.isEmpty()) {

                    genChannels.remove(event.member.user.idLong)
                    val name = event.channelLeft.name
                    event.channelLeft.delete().reason("Empty").queue {
                        bot.settings.sendLog("AutoGen Channel deleted: ``$name`` (Empty).")
                    }
                }
                if (genChannels.isEmpty() && inShutdown) dead = true
            }
        }
    }

    fun shutdown(guild: Guild) = genChannels.forEach {
        guild.getVoiceChannelById(it.value)?.delete()?.queueAfter(250, MILLISECONDS)
    }

    fun asEmbed(guild: Guild) : MessageEmbed {
        genChannels.removeIf { _, vcID ->
            !guild.voiceChannels.has { it.idLong == vcID }
        }
        return makeEmbedBuilder("${guild.name}'s Voice Channel Generator",
            description = """
                **VCGenerator Channel:** ${guild.getVoiceChannelById(baseId)?.name
                    ?: "Unknown! The channel may have been deleted."}
                ***Default Settings:***
                    **User limit:** ${if (guildSettings.limit == 0) "Infinity"
            else "${guildSettings.limit}"}
                    **Channel name:** ${guildSettings.name}
                """.trimIndent()).addField("Open Channels",
            if (genChannels.isEmpty()) "none" else genChannels.map
            { guild.getVoiceChannelById(it.value)?.name ?: "" }.joinToString(", "), false)
            .setColor(guild.roles[0].color ?: STD_GREEN)
            .build()
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
    arrayOf("vcg", "vcgenerator", "vcgen"), CAT_UNDER_CONSTRUCTION, "",
    "Creates a temp VoiceChannel for a User after joining a designated Voice Channel",
    guildOnly = true, children = arrayOf(SubCmdEnable(), SubCmdDisable(),
        SubCmdServerDefaults(), SubCmdBaseChannel(), SubCmdUserDefaults(),
        SubCmdManulTemp())) {

    /** Turn ON  */
    internal class SubCmdEnable : WeebotCommand("enable", arrayOf("on"), CAT_MOD, "", "",
                guildOnly = true, cooldown = 30, botPerms = arrayOf(MANAGE_CHANNEL),
                userPerms = arrayOf(MANAGE_CHANNEL)) {
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
                message.editMessage(e).queue({
                    event.reply(vcGenerator.asEmbed(event.guild))
                }) {
                    event.reply(e)
                    event.reply(vcGenerator.asEmbed(event.guild))
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
                SelectableEmbed(event.author,
                    makeEmbedBuilder("Voice Channel Generator", null, """
                        $Pencil to create a new Voice Channel
                        """.trimIndent()).build(),
                    listOf(Pencil to { _:Message, _:User ->
                        newChannel(message) {chooseChannel(message)}
                    })) {
                    it.editMessage("*Timed Out*").queue {
                        it.clearReactions().queue()
                    }}.displayOrDefault(message, event.channel)
            }

            var vcg: VCGenerator? = bot.getPassive()

            if (vcg != null) {
                if (vcg.inShutdown) {
                    vcg.shutdown(event.guild)
                    bot.passives.remove(vcg)
                } else {
                    event.respondThenDelete(
                        "*The Voice Channel Generator is already active.*", 5)
                    event.delete(5)
                    return
                }
            }

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
    internal class SubCmdDisable : WeebotCommand("disable", arrayOf("off"), CAT_MOD,
        "", "", guildOnly = true, cooldown = 30,  botPerms = arrayOf(MANAGE_CHANNEL),
        userPerms = arrayOf(MANAGE_CHANNEL)) {
        public override fun execute(event: CommandEvent) {
            val bot = getWeebotOrNew(event.guild)
            val vcg: VCGenerator? = bot.getPassive()
            if (vcg != null) {
                vcg.inShutdown = true
                event.reply("""Voice Channel Generator has been placed in shutdown;
                    |No more channels will be created, and all remaining channels will
                    |be closed on exit.
                """.trimMargin())
            } else {
                event.reply("*There is no Voice Channel Generator active.*")
            }
        }

    }

    /** Set Server Defaults */
    internal class SubCmdServerDefaults : WeebotCommand("def",
        arrayOf("serverdefaults", "sdef", "servdef"), CAT_MOD, "", "",
        userPerms = arrayOf(MANAGE_CHANNEL), guildOnly = true, cooldown = 30) {
        override fun execute(event: CommandEvent) {
            TODO("not implemented")
        }
    }

    /** Set Base Channel */
    internal class SubCmdBaseChannel : WeebotCommand("base", arrayOf("channel", "chan"),
        CAT_MOD, "", "", userPerms = arrayOf(MANAGE_CHANNEL), cooldown = 30,
        guildOnly = true) {
        override fun execute(event: CommandEvent) {
            TODO("not implemented")
        }
    }

    /** Set User Settings */
    internal class SubCmdUserDefaults : WeebotCommand("set", arrayOf("mydef"), CAT_UTIL,
        "", "", cooldown = 30, guildOnly = true) {
        override fun execute(event: CommandEvent) {
            val args = event.splitArgs()
            val settings = getWeebotOrNew(event.guild).getPassive<VCGenerator>()
                ?.userSettings?.getOrDefault(event.author.idLong, Settings())
                    ?: Settings()

            if (args.isEmpty()) {
                event.reply(settings.asEmbed(event.member))
                return
            }
            //set [-L userLimit] [channelName]
            TODO("not implemented")
        }
    }

    /** Manual Temp Channel */
    internal class SubCmdManulTemp : WeebotCommand("set", arrayOf("mydef"), CAT_UTIL,
        "", "", cooldown = 30, guildOnly = true) {
        override fun execute(event: CommandEvent) {
            //temp [-t hours] [-L userLimit] [name]
            TODO("not implemented")
        }
    }

    override fun execute(event: CommandEvent) {
        val vcGenerator = getWeebotOrNew(event.guild).getPassive<VCGenerator>()
        val embed: MessageEmbed = vcGenerator?.asEmbed(event.guild) ?: run {
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
            .addField("Set/See Server Defaults",
                """``def [-L userLimit] [channelName]``
                    *Need ${MANAGE_CHANNEL.getName()} permission.*""".trimIndent(),true)
            .addField("Set Generator Voice Channel",
                "``base``\n*Need ${MANAGE_CHANNEL.getName()} permission.*", true)
            .addField("Set/See Your Defaults","``set [-L userLimit] [channelName]``",true)
            .addField("Manually Create Temporary Voice Channel", """
                ``temp [-t hours] [-L userLimit] [name]``
                If ``hours`` is not set, the channel will delete on exit
                *Must have ${MANAGE_CHANNEL.getName()} permission.*
            """.trimIndent(), true)
            .build()
    }
}
