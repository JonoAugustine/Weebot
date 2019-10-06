/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.commands

import com.ampro.weebot.bot.strifeExtensions.args
import com.ampro.weebot.bot.strifeExtensions.sendWEmbed
import com.ampro.weebot.util.Regecies
import com.ampro.weebot.util.plus
import com.serebit.strife.entities.UnicodeEmoji
import com.serebit.strife.entities.reply
import com.serebit.strife.entities.title
import com.serebit.strife.text.italic
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
object RegexTest : Command(
    "Regex",
    listOf("rtc", "match", "regtest", "regextest"),
    details = buildString {
        append("Test a Regex against one or more strings.\n")
        append("To test single Strings/words, simply seperate each word with ")
        append("a space. In order to test phrases, use the `-p` option ")
        append("and begin each new phrase with a new line, like this:\n")
        append("```css\n-p <regex> <phrase_1>\n[phrase_2]\n...\n```")
    },
    params = listOfParams(
        "-p" to true,
        "regex",
        "word/phrase",
        "words/phrases..." to true
    ),
    rateLimit = 10,
    predicate = {
        if (message.args[1].matches(Regecies.hyphen + "p(hrase)?$"))
            message.args.size > 3
        else true
    },
    action = {
        val args = message.args
        val regex: Regex
        val strings: List<String>
        try {
            if (args[1].matches(Regecies.hyphen + "p(hrase)?$")) {
                //\regex -phrase <regex> <phrase here>
                regex = args[2].toRegex()
                strings = message.content.substring(
                    message.content.indexOf(regex.pattern) +
                        regex.pattern.length
                ).split(Regex("\\n"))
            } else { //\regex <regex> word word word
                regex = args[1].toRegex()
                strings = args.subList(2, args.size).map(String::trim)
            }
            val matches = strings.filter { regex.matches(it) }
            message.sendWEmbed {
                title("Regex Results")
                description = buildString {
                    append("Testing regex\n```css\n$regex\n```")
                    append("against ").append(strings.size).append(" strings: ")
                    append("```css\n${strings.joinToString(", ")}\n```\n")
                    append("Found ${matches.size} matches")
                    if (matches.isNotEmpty()) {
                        append("!\n```css\n")
                        matches.forEach { append("$it\n") }
                        append("```")
                    } else append(UnicodeEmoji.Frowning)
                }
            }
        } catch (e: PatternSyntaxException) {
            message.reply("*${e.description}*")
        } catch (e: Exception) {
            message.reply(
                buildString {
                    append("Sorry, there was a problem processing your regex.")
                    append(" Please try again.")
                }.italic
            )
        }
    }
)

