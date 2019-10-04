/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.site

import com.ampro.weebot.logger
import com.sun.deploy.Environment
import io.kweb.Kweb
import io.kweb.dom.element.creation.tags.h1
import io.kweb.dom.element.new

fun initKweb() {
    Kweb(System.getenv("PORT")!!.toInt()) {
        doc.body.new {
            h1().text("Weebot Site")
        }
    }
}
