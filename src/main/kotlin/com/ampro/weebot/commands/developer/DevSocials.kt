/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.commands.developer

import com.ampro.weebot.database.constants.*
import com.github.twitch4j.TwitchClientBuilder.builder
import twitter4j.conf.ConfigurationBuilder


val CLIENT_TWTICH_PUB = builder().withEnableHelix(true).build()

val TWITTER_CONFIG = ConfigurationBuilder().setDebugEnabled(true)
    .setJSONStoreEnabled(true)
    .setOAuthConsumerKey(KEY_TWITTER_API).setOAuthConsumerSecret(KEY_TWITTER_API_SEC)
    .setOAuthAccessToken(TOKEN_TWITTER).setOAuthAccessTokenSecret(TOKEN_TWITTER_SEC)
    .build()
