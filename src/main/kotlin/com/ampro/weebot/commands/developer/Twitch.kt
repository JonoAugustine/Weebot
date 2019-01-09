/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.commands.developer

import com.github.twitch4j.TwitchClientBuilder.builder

val CLIENT_TWTICH_PUB = builder().withEnableHelix(true).build()
