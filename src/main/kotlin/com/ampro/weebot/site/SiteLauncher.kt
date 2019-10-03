/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.site

import com.ampro.weebot.logger
import io.kweb.Kweb
import io.kweb.dom.element.creation.tags.h1
import io.kweb.dom.element.new

val port = 6900

fun initKweb() {
    logger.info("Serving KWeb at http://localhost:$port")
    Kweb(port) {
        doc.body.new {
            h1().text("Weebot Site")
        }
    }
}
