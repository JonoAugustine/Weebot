/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.strifeExtensions

import com.serebit.strife.BotBuilder
import com.serebit.strife.BotFeature


inline fun <reified F : BotFeature> BotBuilder.getFeature(): F? =
    features.values.firstOrNull { it is F } as? F
