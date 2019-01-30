/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.database

import com.ampro.weebot.Weebot
import com.ampro.weebot.commands.`fun`.games.cardgame.CAHDeck
import com.ampro.weebot.commands.developer.Suggestion
import com.ampro.weebot.database.constants.NL_GUILD_ID
import com.ampro.weebot.util.slog
import com.mongodb.*
import com.mongodb.MongoCredential.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.async.*
import org.litote.kmongo.eq

const val MONGO_HOST = "den1.mongo1.gear.host:27001"
const val MONGO_USER = "weebotmongo"
const val MONGO_PASS = "Vy51?VWUt3J-"

class DatabaseAccess {

    init { runBlocking {
        KMongo.createClient(MongoClientSettings.builder()
            .credential(createCredential(MONGO_USER, MONGO_USER, MONGO_PASS.toCharArray()))
            .applicationName("Weebot_Mongo")
            .retryWrites(true).build())
        val client = KMongo.createClient(
            "mongodb://$MONGO_USER:$MONGO_PASS@$MONGO_HOST/$MONGO_USER"
        )
        client.startSession { result, _ ->
            val database = client.getDatabase("weebotmongo")
            val col = database.getCollection<Weebot>()
            col.insertOne(Weebot(NL_GUILD_ID), {_,_->})
            col.findOne(Weebot::guildID eq NL_GUILD_ID) { s, f ->
                f?.printStackTrace() ?: s?.settings?.nickname
            }
            database.listCollectionNames().forEach({
                slog(it)
            }, { _, _ ->})
        }
        delay(3_000)

        }
    }

}


fun main() {
    val dao = DatabaseAccess()

}
