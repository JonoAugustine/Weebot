/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.site

import com.ampro.weebot.bots
import com.ampro.weebot.logger
import io.kweb.Kweb
import io.kweb.dom.element.creation.tags.h1
import io.kweb.dom.element.creation.tags.p
import io.kweb.dom.element.new
import io.kweb.state.KVar
import io.kweb.state.render.toVar

val port = 6900

fun initKweb() {
    logger.info("Serving KWeb at http://localhost:$port")
    Kweb(port) {
        doc.body.new {
            h1().text("Weebot Site")
            p().text(KVar("${bots.size}"))
        }
    }
}
