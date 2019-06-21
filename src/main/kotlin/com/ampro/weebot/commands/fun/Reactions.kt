/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.GENERIC_ERR_MSG
import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.bot
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.database.track
import com.ampro.weebot.extensions.CLR_GREEN
import com.ampro.weebot.extensions.EMBED_MAX_DESCRIPTION
import com.ampro.weebot.extensions.EMBED_MAX_TITLE
import com.ampro.weebot.extensions.TODO
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import com.ampro.weebot.extensions.creationTime
import com.ampro.weebot.extensions.makeEmbedBuilder
import com.ampro.weebot.extensions.matches
import com.ampro.weebot.extensions.matchesAny
import com.ampro.weebot.extensions.replace
import com.ampro.weebot.extensions.respondThenDeleteBoth
import com.ampro.weebot.extensions.send
import com.ampro.weebot.extensions.splitArgs
import com.ampro.weebot.extensions.strdEmbedBuilder
import com.ampro.weebot.extensions.subList
import com.ampro.weebot.util.Emoji.ArrowUp
import com.ampro.weebot.util.Emoji.H
import com.ampro.weebot.util.Emoji.I_lowercase
import com.ampro.weebot.util.Emoji.S
import com.ampro.weebot.util.Emoji.T
import com.ampro.weebot.util.Emoji.Zero
import com.ampro.weebot.util.EmojiNumbers
import com.ampro.weebot.util.REG_DISABLE
import com.ampro.weebot.util.REG_ENABLE
import com.ampro.weebot.util.REG_OFF
import com.ampro.weebot.util.REG_ON
import com.ampro.weebot.util.digi
import com.ampro.weebot.util.reactWith
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS


/**
 * A [WeebotCommand] with associated [IPassive] for replacing a single-emote-only
 * message with a larger version within an embed.
 *
 * @author Jonathan Augustine
 * @since 2.2.1
 */
class CmdBiggifyEmoji : WeebotCommand(
    "biggify", "BIGEMOJI", "Emote Biggify", arrayOf("ebig", "bigemote"),
    CAT_FUN, "Replace single-emote-messages with a bigger image",
    guildOnly = true, cooldown = 30,
    userPerms = arrayOf(ADMINISTRATOR), botPerms = arrayOf(MESSAGE_MANAGE)
) {

    /**
     * @author Jonathan Augustine
     * @since 2.2.1
     */
    class Biggifyer(var invoke: Char? = null) : IPassive {
        private var dead = false
        override fun dead() = dead

        private fun Emote.mentioRegex() = Regex(
            "${if (invoke != null) "$invoke\\s+" else ""}<[A-Za-z]*:$name:$id>")

        override fun accept(bot: Weebot, event: Event) {
            if (event !is GuildMessageReceivedEvent) return
            val emotes = event.message.emotes.filterNotNull()
            if (emotes.size != 1) return
            val das = emotes.single()
            val raw = event.message.contentRaw
            if (!raw.matches(das.mentioRegex())) return
            event.message.delete().queueAfter(250, MILLISECONDS)
            strdEmbedBuilder.setAuthor(event.member.effectiveName, event.author.avatarUrl)
                .setColor(event.member.color ?: CLR_GREEN).setImage(das.imageUrl)
                .build().send(event.channel)
        }

    }

    override fun execute(event: WeebotCommandEvent) {
        var biggifyer = event.bot.getPassive<Biggifyer>()
        if (event.argList.isEmpty()) return event.respondThenDeleteBoth("""
            The Emote Biggifyer is ${if (biggifyer == null) "off" else "on"}.
            Use ``biggify <on/off>`` to change the setting.""".trimIndent())

        when {
            event.argList[0].matchesAny(REG_ON, REG_ENABLE) -> {
                if (biggifyer != null) return event.respondThenDeleteBoth(
                    "The Biggifyer is already active.")
                val invoke: Char? = if (event.argList.size == 2) {
                    if (event.argList[1].length > 1) {
                        return event.respondThenDeleteBoth(
                            "Prefix must be ``1`` character (``g``, ``\\``, ``+``, etc)",
                            30)
                    } else event.argList[1][0]
                } else null
                biggifyer = Biggifyer(invoke)
                event.bot.add(biggifyer)
                if (invoke == null) {
                    event.replySuccess("Biggifyer Activated!")
                } else {
                    event.reply(makeEmbedBuilder("Biggifyer Activated!", null, """
                        Any custom emote can now be BIGGENED by using the prefix
                        ``$invoke:emoteName:``""".trimIndent()).build())
                }
            }
            event.argList[0].matchesAny(REG_OFF, REG_DISABLE) -> {
                if (biggifyer == null) return event.respondThenDeleteBoth(
                    "The Biggifyer is already off.")
                return if (event.bot.passives.remove(biggifyer)) {
                    event.replySuccess("Biggifyer deactivated.")
                } else event.replyError(GENERIC_ERR_MSG)
            }
            event.argList[0].matches("(?i)-{0,2}p(ref(ix)?)?") -> {
                if (biggifyer == null) return event.respondThenDeleteBoth(
                    "No Biggifyer active.")
                if (event.argList.size == 1) {
                    return event.reply(
                        "The prefix is currently ``${biggifyer.invoke ?: "not set"}``")
                } else if (event.argList[1].length > 1) {
                    if (event.argList[1].matchesAny(REG_OFF, REG_DISABLE)) {
                        biggifyer.invoke = null
                        event.replySuccess("Prefix removed. AutoBigging activated.")
                    } else return event.respondThenDeleteBoth(
                        "Must be ``1`` character (``g``, ``\\``, ``+``, etc)", 30)
                } else {
                    biggifyer.invoke = event.argList[1][0]
                    event.replySuccess("Biggifyer prefix set to ``${biggifyer.invoke}``")
                }
            }
            else -> return
        }
        track(this, event.bot, event.author, event.creationTime)
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Emote Biggifyer", false)
            .setDescription("Automatically (or manually) have weebot replace " +
                "any *custom* emote with a bigger version of the same emote.")
            .addField("Commands", """
                **Enable**
                ``on [prefix]``
                if the prefix is set, then put the prefix before the emote, e.g.``b:emoteName:``
                otherwise, any message of a single emote will be **mBIggened**
                **Change/remove the prefix**
                ``pref <1_char or off>`` | ``off`` to set to autoBIggify
                **Disable**
                ``off``
            """.trimIndent()).build()
    }

}


/**
 * Adds a reaction to a message on unsigned request.
 *
 * @author Jonathan Augustine
 * @since 2.2.1
 */
class CmdReacter : WeebotCommand(
    "reacter", "REACTER", null, arrayOf("reac", "mrc", "reactor"),
    CAT_FUN, "React with predefined emotes to any message.",
    cooldown = 10, guildOnly = true, userPerms = arrayOf(MESSAGE_ADD_REACTION),
    botPerms = arrayOf(MESSAGE_ADD_REACTION)
) {

    /**
     * @author Jonathan Augustine
     * @since 2.0
     */
    class Reactor(var invoke: Regex, var reaction: List<String>) : IPassive {
        var dead = false
        override fun dead() = dead

        /**
         * Get the first message in the list that isn't "^[invoke]".
         *
         * @param messages An message iterable
         * @param startDex The index to start searching at
         *
         * @return The first message that doesnt match \^(t+h+i+s+|t+h+a+t+) or null
         */
        fun getNonThis(messages: List<Message>) = messages.firstOrNull {
            !it.contentDisplay.matches(invoke)
        }

        override fun accept(bot: Weebot, event: Event) {
            if (event !is GuildMessageReceivedEvent) return
            val args = event.message.contentDisplay.toLowerCase().split(Regex("\\s+"))
            args.forEachIndexed { i, it ->
                if (it.matches(invoke)) {
                    event.channel.getHistoryBefore(event.message, 100).queue { his ->
                        val dex = try {
                            args[i + 1].toInt() - 1
                        } catch (e: Exception) {
                            0
                        }
                        (getNonThis(his.retrievedHistory.subList(dex))
                            ?: his.retrievedHistory[0]).reactWith(reaction)
                    }
                    return
                }
            }
        }
    }

    override fun execute(event: WeebotCommandEvent) {
        val args = event.splitArgs()
        val bot = event.guild.bot
        val reactors = bot.getPassives<Reactor>()

        if (args.isEmpty()) {
            return event.reply(strdEmbedBuilder.setTitle("This-Reactor").setDescription(
                "A passive reaction watcher triggered ").appendDescription(
                "by \'^this\' or \'^that\' with an ").appendDescription(
                "optional number indicating how many ").appendDescription(
                "messages up to react to. ``^this 4``").build())
        }

        track(this, bot, event.author, event.creationTime)

        //on <invoke> <r e a c t i o n e m o t e s>
        when {
            args[0].toLowerCase().matchesAny(REG_ON, REG_ENABLE) -> {
                event.message.emotes
                TODO(event)
            }
            args[0].toLowerCase().matchesAny(REG_OFF, REG_DISABLE) -> {
                TODO(event)
            }
            else -> {
                event.respondThenDeleteBoth("Sorry, I had trouble understanding " +
                    "${args[0]}. Try using " + "``on`` or ``off``.", 20)
            }
        }

    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Message Reacter")
            .setDescription("Have me react to a message with any emote on request.")
            .addToDesc("\nEx.) use ``^this`` to react with ``^THiS`` to a message.")
            .setAliases(aliases)
            .build { it.reactWith(ArrowUp, T, H, I_lowercase, S) }
    }

}

/**
 * Adds a reaction THiS to a message. Can also be used to activate an [IPassive]
 * watcher for "^this" or "^that" messages.
 *
 * TODO: Generalize this command
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdThis : WeebotCommand(
    "^this", "REACTTHIS", null, arrayOf("^that"),
    CAT_FUN, "React with \"THiS\" to a message or enable an auto-reactor for This",
    cooldown = 10, guildOnly = true, userPerms = arrayOf(MESSAGE_ADD_REACTION),
    botPerms = arrayOf(MESSAGE_ADD_REACTION),
    helpBiConsumer = HelpBiConsumerBuilder("^This Reactor")
        .setDescription(
            "Have me react with *\"THiS\"* to a message whenever someone types ")
        .addToDesc("\"^this\" or \"^that\".")
        .addField("Arguments", "[on/off]").addField("Aliases", "^that")
        .build { it.reactWith(ArrowUp, T, H, I_lowercase, S) }
) {

    /**
     * Watcher for "^this" or "^that" messages.
     *
     * @author Jonathan Augustine
     * @since 2.0
     */
    class ThisReactor : IPassive {
        companion object {
            val THIS_REG = Regex("\\^(t+h+i+s+|t+h+a+t+)")
        }

        var dead = false
        override fun dead() = dead

        /**
         * Get the first message in the list that isn't "^this".
         *
         * @param messages An message iterable
         * @param startDex The index to start searching at
         *
         * @return The first message that doesnt match \^(t+h+i+s+|t+h+a+t+) or null
         */
        fun getNonThis(messages: List<Message>, startDex: Int): Message? {
            for (i in startDex until messages.size) {
                if (!messages[i].contentDisplay.matches(THIS_REG)) {
                    return messages[i]
                }
            }
            return null
        }

        override fun accept(bot: Weebot, event: Event) {
            if (event !is GuildMessageReceivedEvent) return
            val args = event.message.contentDisplay.toLowerCase().split(Regex("\\s+"))
            args.forEachIndexed { i, it ->
                if (it.matches(Regex("\\^(t+h+i+s+|t+h+a+t+)"))) {
                    event.channel.getHistoryBefore(event.message, 100).queue {
                        val dex = try {
                            args[i + 1].toInt() - 1
                        } catch (e: Exception) {
                            0
                        }
                        (getNonThis(it.retrievedHistory, dex)
                            ?: it.retrievedHistory[0]).reactWith(ArrowUp, T, H,
                            I_lowercase, S)
                    }
                    return
                }
            }
        }
    }

    override fun execute(event: WeebotCommandEvent) {
        val args = event.splitArgs()
        val bot = event.guild.bot
        val reactor = bot.getPassive<ThisReactor>()
        track(this, event.guild.bot, event.author, event.creationTime)

        //Check for ThisReactor enabling
        // \thisreactor on
        if (args.isEmpty()) {
            event.reply(strdEmbedBuilder.setTitle("This-Reactor").setDescription(
                "A passive reaction watcher triggered ").appendDescription(
                "by \'^this\' or \'^that\' with an ").appendDescription(
                "optional number indicating how many ").appendDescription(
                "messages up to react to. ``^this 4``").build())
        } else {
            when {
                args[0].toLowerCase().matches(Regex("^(on|enable)$")) -> {
                    if (reactor == null) {
                        bot.passives.add(ThisReactor())
                        event.reply(strdEmbedBuilder
                            .setTitle("Your ^This Reactor is now active!")
                            .setDescription("send ``^this`` or ``^that``")
                            .appendDescription(" to react to a message. You can ")
                            .appendDescription("add a number to indicate how many ")
                            .appendDescription("messages up to react to.\nHAVE FUN!")
                            .build()
                        ) { it.reactWith(ArrowUp, T, H, I_lowercase, S) }
                        return
                    } else {
                        event.respondThenDeleteBoth("The ^this reactor is already active")
                        return
                    }
                }
                args[0].toLowerCase().matches(Regex("^(off|disable)$")) -> {
                    if (reactor != null) {
                        bot.passives.remove(reactor)
                        event.reply(strdEmbedBuilder
                            .setTitle("Your ^This Reactor is now deactivated!")
                            .setDescription("Use ``\\^this on`` to turn it back ")
                            .appendDescription("on at any time.")
                            .build())
                        return
                    }
                }
                else -> {
                    event.respondThenDeleteBoth(
                        "Sorry, I had trouble understanding ${args[0]}. Try using " +
                            "``on`` or ``off``.", 30)
                }
            }
        }

    }

}

/**
 * Converts a sentence to letter/number emojis
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class CmdEmojify : WeebotCommand(
    "emojify", "EMOJIFY", "Emojify", arrayOf(),
    CAT_FUN, "Turn any sentence into Emoji", cooldown = 5
) {

    private val numberSet = digi.mapIndexed { i, _ ->
        Regex("(?i)$i") to (listOf(Zero) + EmojiNumbers)[i].unicode
    }.toTypedArray()

    override fun execute(event: WeebotCommandEvent) {
        if (event.args.isNullOrBlank()) return
        if (event.args.length * 6 > EMBED_MAX_DESCRIPTION)
            return event.respondThenDeleteBoth("Too long", 5)

        val s = event.args.replace(Regex("\\s"), "\t")
            .replace(Regex("(?i)[A-z]")) {
                ":regional_indicator_${it.value.toLowerCase()}:"
            }.replace(*numberSet)

        event.reply(s)
    }
}

/**
 * Sends a General Kenobi Hello There gif.
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdHelloThere : WeebotCommand(
    "hellothere", "KENOBI", "Hello There", arrayOf("droppingin"),
    CAT_FUN, "*GENERAL KENOBI!*", cooldown = 360,
    userPerms = arrayOf(MESSAGE_EMBED_LINKS), botPerms = arrayOf(MESSAGE_EMBED_LINKS)
) {
    companion object {
        val HELLO_THERE_GIFS = listOf(
            "https://media.giphy.com/media/Nx0rz3jtxtEre/giphy.gif",
            "https://media1.tenor.com/images/4735f34b4dd3b86333341fa17b203004/tenor.gif?itemid=8729471")
    }

    override fun execute(event: WeebotCommandEvent) {
        track(this, event.guild.bot, event.author, event.creationTime)
        val e = strdEmbedBuilder.apply {
            val sb = StringBuilder()
            event.message.mentionedUsers.forEach {
                if (sb.length + it.asMention.length > EMBED_MAX_TITLE) {
                    return@forEach
                }
                sb.append("${it.asMention} ")
            }
            setDescription("Hello There $sb")
        }.setAuthor(event.member.effectiveName)
            .setImage(HELLO_THERE_GIFS.random()).build()

        event.reply(e) { event.message.delete().queueAfter(1, SECONDS) }
    }

}
