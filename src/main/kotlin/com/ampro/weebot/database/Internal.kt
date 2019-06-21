/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.database

import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.extensions.WeebotCommand
import java.time.OffsetDateTime
import kotlin.reflect.KClass

data class UserData(
    val _id: String,
    var tracked: Boolean = false, // TODO
    var status: PremiumStatus? = null,
    val globalPassives: MutableList<IPassive> = mutableListOf(),
    //val reminders: MutableList<Reminder> = mutableListOf(),
    val blacklists: HashSet<KClass<out WeebotCommand>> = hashSetOf()
)

data class PremiumStatus(var initDate: OffsetDateTime, val on: Boolean = true)
