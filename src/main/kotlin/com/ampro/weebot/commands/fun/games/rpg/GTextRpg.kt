/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.commands.`fun`.games

import com.ampro.weebot.commands.CAT_DEV
import com.ampro.weebot.extensions.*
import java.io.File


//TODO
class TempCmdCheckReadMe : WeebotCommand("rpg", "GAMERPG", null, emptyArray(),
        CAT_DEV, "", ownerOnly = true) {
    override fun execute(event: WeebotCommandEvent) {
        val sb = StringBuilder()
        File("READMORE/readme_rpg.md").useLines {
            strdPaginator.addItems(*it.toList().toTypedArray())
                .setItemsPerPage(15)
                .build().display(event.channel)
        }

    }
}
