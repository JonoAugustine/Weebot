/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.bot.strifeExtensions

import com.serebit.strife.entities.User

infix fun User.`is`(id: Long) = this.id == id
