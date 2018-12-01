/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.progammer

import com.ampro.weebot.commands.*
import com.ampro.weebot.database.constants.Emoji
import com.ampro.weebot.database.constants.strdEmbedBuilder
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.MessageEmbed.Field

/**
 * A Command that takes in a Regex string and Strings to test if it matches.
 * TODO Multiple words in 1 check
 * \regex <regex> <word> {words...}
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
class CmdRegexTest : WeebotCommand(
    "regex", arrayOf("regtest", "RegexTest"), CAT_PROG, "<regex> <word> [words...]",
    "Test a Regex against one or more strings", cooldown = 10) {

    init {
        helpBiConsumer = HelpBiConsumerBuilder().setTitle("Regex Tester")
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
        .setDescription("Testing regex\n```\n$regex\n``` against ``$strings``" +
            "\nFound ${matches.size} matches ${if (matches.isNotEmpty()) {
            "!\n```\n${kotlin.run{var s="";matches.forEach {s+="$it\n"};s}}```"
        } else { Emoji.FrowningFace.unicode }}")
        .build()

    override fun execute(event: CommandEvent) {
        val args = event.splitArgs()
        if (args.size < 2) return
        val regex = args[0].toRegex()
        val strings = args.subList(1, args.size)
        val matches = strings.filter { regex.matches(it) }
        event.reply(
            regexEmbedBuilder(event.member?.nickname ?: event.author.name, args[0],
                strings, matches))
    }

}
