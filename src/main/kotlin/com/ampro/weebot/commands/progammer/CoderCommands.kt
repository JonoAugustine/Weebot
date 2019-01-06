/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.progammer

import com.ampro.weebot.WAITER
import com.ampro.weebot.commands.CAT_PROG
import com.ampro.weebot.database.*
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.*
import com.ampro.weebot.util.Emoji.*
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.ChannelType.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import java.util.function.Predicate
import java.util.regex.PatternSyntaxException

/**
 * A Command that takes in a Regex string and Strings to test if it matches.
 *
 * \regex <regex> <word> {words...}
 * \regex phrase <regex> {phrase}
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdRegexTest : WeebotCommand("regex", arrayOf("regtest", "RegexTest"),
    CAT_PROG, "<regex> <word> [words...]","Test a Regex against one or more strings",
    cooldown = 10) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Regex Tester")
            .setDescription("Test a Regex against one or more strings.")
            .setThumbnail("https://i1.wp.com/digitalfortress" +
                    ".tech/wp-content/uploads/2018/05/regex1.png?fit=526%2C526&ssl=1")
            .addField("Arguments", "<regex> <word> [words...]", false)
            .setAliases(aliases)
            .build()
    }

    /**
     * Builds the embed to send.
     *
     * @param name The user's name
     * @param regex The submitted regex
     * @param strings The strings tested
     * @param matches The strings that matched
     *
     * @return An embed built very nicely with all the data
     */
    private fun regexEmbedBuilder(name: String, regex: String, strings: List<String>,
                                  matches: List<String>): MessageEmbed
            = strdEmbedBuilder.setTitle("$name's Regex Test")
        .setDescription("Testing regex\n```\n$regex\n``` against ${strings.size} " +
                "strings: ```$strings```" +
            "\nFound ${matches.size} matches ${if (matches.isNotEmpty()) {
            "!\n```\n${kotlin.run{var s="";matches.forEach {s+="$it\n"};s}}```"
        } else { FrowningFace.unicode }}")
        .build()

    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        if (args.size < 2) return
        STAT.track(this, getWeebotOrNew(event.guild), event.author)
        val regex: Regex
        val strings: List<String>
        try {
            if (args[0].toLowerCase() == "phrase") { //\regex phrase <phrase here>
                if (args.size < 3) return
                regex = args[1].toRegex()
                strings = listOf(args.subList(2, args.size).joinToString(" ", "", ""))
            } else { //\regex <regex> word word word
                regex = args[0].toRegex()
                strings = args.subList(1, args.size)
            }
        } catch (e: PatternSyntaxException) {
            event.reply("*${e.description}*")
            return
        } catch (e: Exception) {
            event.reply("*Sorry, there was a problem processing your regex. Please try " +
                    "again.*")
            return
        }
        event.reply(
            regexEmbedBuilder(event.member?.nickname ?: event.author.name,
                regex.toString() ,strings, strings.filter { regex.matches(it) })
        )
    }

}


/**
 * A [WeebotCommand] that allows an end user to send a [MessageEmbed].
 *
 * @author Jonathan Augustine
 * @since 2.1
 */
class CmdEmbedMaker : WeebotCommand("embedmaker",
    arrayOf("embedbuilder", "embed", "makeembed", "sendembed"), CAT_PROG,
    "", ".", cooldown = 30, cooldownScope = CooldownScope.USER_CHANNEL) {

    val MAX_FIELDS = 10

    override fun execute(event: CommandEvent) {
        if (event.channelType == TEXT) STAT.track(this, getWeebotOrNew(event.guild), event.author)

        fun waitThen(predicate: (MessageReceivedEvent) -> Boolean = {true},
                     action: (MessageReceivedEvent) -> Unit) {
            WAITER.waitForEvent(MessageReceivedEvent::class.java,
                { it.isValidUser(event.guild, setOf(event.author)) && predicate(it) },
                action, 5, MINUTES, { event.reply("*Timed Out.*") })
        }

        val eb = EmbedBuilder()

        val setTitle: (Message, User) -> Unit = { _: Message, _: User ->
            event.reply("Enter the Title (under $EMBED_MAX_TITLE char):"){
                it.delete().queueAfter(5, MINUTES)
            }
            waitThen ({
                if (it.message.contentDisplay.length !in 1..EMBED_MAX_TITLE) {
                    event.respondThenDelete("Must be under $EMBED_MAX_TITLE characters.")
                    false
                } else true
            }) { m2 ->
                eb.setTitle(m2.message.contentDisplay)
                event.reply("Title set") {
                    it.delete().queueIgnore(1)
                    if (event.isFromType(TEXT)) m2.message.delete().queueIgnore(1)
                }
            }
        }
        val setDescription = { _: Message, _: User ->
            event.reply("Enter the Description (under $EMBED_MAX_DESCRIPTION char):"){
                it.delete().queueAfter(5, MINUTES)
            }
            waitThen ({
                if (it.message.contentDisplay.length !in 1..EMBED_MAX_DESCRIPTION) {
                    event.respondThenDelete(
                        "Must be under $EMBED_MAX_DESCRIPTION characters."
                    )
                    false
                } else true
            }) { m2 ->
                eb.setDescription(m2.message.contentRaw)
                event.reply("Description set") {
                    it.delete().queueIgnore(1)
                    if (event.isFromType(TEXT)) m2.message.delete().queueIgnore(1)
                }
            }
        }
        val addField = addField@{ _: Message, _: User ->
            if (eb.fields.size >= MAX_FIELDS) {
                event.replyError("Max Fields ($MAX_FIELDS) reached.")
                return@addField
            }
            event.reply("Enter the Field Name (under $EMBED_MAX_FIELD_NAME char):"){
                it.delete().queueAfter(5, MINUTES)
            }
            waitThen ({
                if (it.message.contentDisplay.length !in 1..EMBED_MAX_FIELD_NAME) {
                    event.respondThenDelete(
                        "Must be under $EMBED_MAX_FIELD_NAME characters.")
                    false
                } else true
            }) { nameMsg ->
                event.reply("Enter the Field content (under $EMBED_MAX_FIELD_VAL char):"){
                    it.delete().queueAfter(5, MINUTES)
                }
                waitThen({ e3 ->
                    if (e3.message.contentDisplay.length !in 1..EMBED_MAX_FIELD_VAL) {
                        event.respondThenDelete("Must be under $EMBED_MAX_FIELD_VAL characters.")
                        false
                    } else true
                }) { contentMsg ->
                    event.reply("Should the field be inline?: (``yes``/``no``)"){
                        it.delete().queueAfter(5, MINUTES)
                    }
                    waitThen({ e4 ->
                        if (!e4.message.contentDisplay
                                    .matchesAny(REG_YES, REG_ENABLE, REG_NO, REG_DISABLE)) {
                            event.respondThenDelete("``yes`` or ``no``")
                            false
                        } else true
                    }) { inlineMsg ->
                        val inline = inlineMsg.message.contentDisplay.matchesAny(REG_YES, REG_ENABLE)
                        eb.addField(nameMsg.message.contentDisplay, contentMsg.message.contentRaw, inline)
                        event.reply("Field Added") {
                            it.delete().queueIgnore(1)
                            if (event.isFromType(TEXT)) {
                                nameMsg.message.delete().queueIgnore(1)
                                contentMsg.message.delete().queueIgnore(2)
                                inlineMsg.message.delete().queueIgnore(3)
                            }
                        }
                    }
                }
            }
        }
        val setThumbnailUrl = { _: Message, _: User ->
            event.reply("Send the Image file or the image URL: ") {
                it.delete().queueAfter(5, MINUTES)
            }
            waitThen ({
                it.message.attachments.isNotEmpty() || try {
                    URL(it.message.contentDisplay).toURI()
                    true
                } catch (e: Exception) {
                    event.respondThenDelete("Invalid URL.")
                    false
                }
            }) { m2 ->
                eb.setThumbnail(if (m2.message.attachments.isNotEmpty()) {
                    m2.message.attachments[0].proxyUrl
                } else m2.message.contentDisplay)

                event.reply("Thumbnail set") {
                    it.delete().queueIgnore(1)
                    if (event.isFromType(TEXT)) m2.message.delete().queueIgnore(1)
                }
            }
        }
        val setImageUrl = { _: Message, _: User ->
            event.reply("Send the Image file or the image URL :"){
                it.delete().queueAfter(5, MINUTES)
            }
            waitThen ({
                it.message.attachments.isNotEmpty() || try {
                    URL(it.message.contentDisplay).toURI()
                    true
                } catch (e: Exception) {
                    event.respondThenDelete("Invalid URL.")
                    false
                }
            }) { m2 ->
                eb.setImage(if (m2.message.attachments.isNotEmpty()) {
                    m2.message.attachments[0].proxyUrl
                } else m2.message.contentDisplay)

                event.reply("Image set") {
                    it.delete().queueIgnore(1)
                    if (event.isFromType(TEXT)) m2.message.delete().queueIgnore(1)
                }
            }
        }
        val send = { m: Message, _: User ->
            if (eb.isEmpty) {
                event.reply("The Embed is empty!"){
                    it.delete().queueAfter(10, SECONDS)
                }
            } else {
                eb.setAuthor(event.author.name, null, event.author.avatarUrl)
                    .setColor(event.member?.color ?: STD_GREEN)
                    .setFooter("Run by Weebot", weebotAvatar)
                    .setTimestamp(event.creationTime)
                m.delete().queueIgnore(1)
                event.delete(1)
                event.reply(eb.build())
            }
        }

        SelectableEmbed(event.author, false, makeEmbedBuilder("Embed Maker", null, """
            $T to set the title
            $D to set the description
            $F to add a field (max = 10)
            $H to set the thumbnail
            $P to set the image
            $IncomingEnvelope to send the embed
            """.trimIndent()).build(), listOf(T to setTitle, D to setDescription,
            F to addField, H to setThumbnailUrl, P to setImageUrl,
            IncomingEnvelope to send), timeout = 5L) {
            it.clearReactions().queueIgnore(1)
        }.display(event.channel)

    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Embed Builder")
            .setDescription("Make a message embed (Like this message) straight from discord!")
            .addField("This is a Field Title", "This is the field content")
            .addField("This field is inline", "So it shares space with the next", true)
            .addField("This field is also inline", "It wont take a full line", true)
            .setAliases(aliases)
            .build()
    }
}
