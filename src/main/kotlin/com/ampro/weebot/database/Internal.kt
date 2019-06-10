/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.database

import com.ampro.weebot.extensions.WeebotCommand
import java.time.OffsetDateTime
import kotlin.reflect.KClass

data class UserData(
    val _id: String,
    val blacklists: HashSet<KClass<out WeebotCommand>> = hashSetOf(),
    val tracked: Boolean = false,
    var status: PremiumStatus? = null
) {
    data class PremiumStatus(var initDate: OffsetDateTime, val on: Boolean = true)
}
