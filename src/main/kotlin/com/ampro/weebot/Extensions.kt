/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot

import com.serebit.strife.BotBuilder
import com.serebit.strife.BotFeature
import com.serebit.strife.entities.Message
import java.util.regex.Pattern

object Extensions {
    object RegexShorthand {
        /** ignore case shortcut */
        const val ic = "(?i)"
    }
}

operator fun Regex.plus(pattern: String) = (this.pattern + pattern).toRegex()

val Message.args get() = content.split(' ')

inline fun <reified F : BotFeature> BotBuilder.getFeature(): F? =
    features.values.firstOrNull { it is F } as? F
