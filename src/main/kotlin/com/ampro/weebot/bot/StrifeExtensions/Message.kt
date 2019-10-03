/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.StrifeExtensions

import com.serebit.strife.entities.Message

val Message.args get() = content.split(' ')
