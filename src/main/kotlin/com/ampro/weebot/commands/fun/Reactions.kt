/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.`fun`

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.commands.CAT_FUN
import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.database.STAT
import com.ampro.weebot.database.getWeebotOrNew
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Adds a reaction THiS to a message. Can also be used to activate an [IPassive]
 * watcher for "^this" or "^that" messages.
 *
 * TODO: Generalize this command
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdThis : WeebotCommand("^this", null ,arrayOf("^that"), CAT_FUN,
    "[on/off]", "React with \"THiS\" to a message or enable an auto-reactor for This",
    cooldown = 10, guildOnly = true,userPerms = arrayOf(MESSAGE_ADD_REACTION),
    botPerms = arrayOf(MESSAGE_ADD_REACTION),
        helpBiConsumer = HelpBiConsumerBuilder("^This Reactor")
            .setDescription("Have me react with *\"THiS\"* to a message whenever someone types ")
            .addToDesc("\"^this\" or \"^that\".")
            .addField("Arguments", "[on/off]").addField("Aliases", "^that")
            .build { it.reactWith(ArrowUp, T, H, I_lowercase, S) }
) {

    /**
     * Watcher for "^this" or "^that" messages.
     *
     * @author Jonathan Augstine
     * @since 2.0
     */
    class ThisReactor : IPassive {
        companion object { val THIS_REG = Regex("\\^(t+h+i+s+|t+h+a+t+)") }

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

    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        val bot = getWeebotOrNew(event.guild)
        val reactor = bot.getPassive<ThisReactor>()
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)

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
                        event.respondThenDelete("The ^this reactor is already active")
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
                    event.respondThenDelete(
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
class CmdEmojify : WeebotCommand("emojify", "Emojify", arrayOf(), CAT_FUN, "",
    "Turn any sentence into Emoji", cooldown = 5) {

    private val numberSet = digi.mapIndexed { i, _ ->
        Regex("(?i)$i") to (listOf(Zero) + EmojiNumbers)[i].unicode
    }.toTypedArray()

    override fun execute(event: CommandEvent) {
        if (event.args.isNullOrBlank()) return
        if (event.args.length * 6 > EMBED_MAX_DESCRIPTION)
            return event.respondThenDelete("Too long", 5)

        val s = event.args.replace(Regex("\\s"), "\t")
            .replace(Regex("(?i)[A-z]")) { ":regional_indicator_${it.value.toLowerCase()}:"
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
class CmdHelloThere : WeebotCommand("hellothere", "Hello There",arrayOf("droppingin"),
    CAT_FUN, "[@Member]", "*GENERAL KENOBI!*", cooldown = 360,
    userPerms = arrayOf(MESSAGE_EMBED_LINKS), botPerms = arrayOf(MESSAGE_EMBED_LINKS)
) {
    companion object {
        val HELLO_THERE_GIFS = listOf("https://media.giphy.com/media/Nx0rz3jtxtEre/giphy.gif",
        "https://media1.tenor.com/images/4735f34b4dd3b86333341fa17b203004/tenor.gif?itemid=8729471")
    }

    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
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
