/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.commands.`fun`.games

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.extensions.WeebotCommand
import com.ampro.weebot.extensions.WeebotCommandEvent
import java.io.BufferedReader
import java.io.InputStreamReader


//TODO
class TempCmdCheckReadMe : WeebotCommand("rpg", null, emptyArray(),
        CAT_DEV, "", ownerOnly = true) {
    override fun execute(event: WeebotCommandEvent) {
        val input = javaClass.getResourceAsStream("/readme_rpg.md")
        val reader = BufferedReader(InputStreamReader(input))
        val sb = StringBuilder()
        try {
            reader.use {
                sb.append(it.readLine()).ap
            }
        } catch (e :Exception) {}
    }
}
