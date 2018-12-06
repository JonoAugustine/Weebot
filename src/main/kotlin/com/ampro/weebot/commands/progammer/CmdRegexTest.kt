/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.progammer

import com.ampro.weebot.commands.CAT_PROG
import com.ampro.weebot.commands.WeebotCommand
import com.ampro.weebot.commands.splitArgs
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.ampro.weebot.util.Emoji
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.MessageEmbed
import java.lang.Exception
import java.util.*
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
class CmdRegexTest : WeebotCommand(
    "regex", arrayOf("regtest", "RegexTest"), CAT_PROG, "<regex> <word> [words...]",
    "Test a Regex against one or more strings", cooldown = 10) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Regex Tester")
            .setDescription("Test a Regex against one or more strings.")
            .setThumbnail("https://i1.wp.com/digitalfortress" +
                    ".tech/wp-content/uploads/2018/05/regex1.png?fit=526%2C526&ssl=1")
            .addField("Arguments", "<regex> <word> [words...]", false)
            .addField("Aliases", "$name, ${aliases[0]}, ${aliases[1]}", false)
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
        } else { Emoji.FrowningFace.unicode }}")
        .build()

    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        if (args.size < 2) return
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
