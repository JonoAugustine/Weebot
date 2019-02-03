/*
 * Copyright Aquatic Mastery Productions (c) 2019.
 */

package com.ampro.weebot.database

import com.ampro.weebot.util.slog
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.litote.kmongo.reactivestreams.forEach
import org.litote.kmongo.util.CollectionNameFormatter.defaultCollectionNameBuilder

const val MONGO_HOST = "den1.mongo1.gear.host:27002"
const val MONGO_USER = "weebotmongo"
const val MONGO_PASS = "Vy51?VWUt3J-"

class DatabaseAccess {
    val cs = "mongodb://Jonathan:eyXvUg8qu5suLQX@docdb-2019-01-28-08-08-59" +
            ".cluster-cgkysadvu3bu.us-east-2.docdb.amazonaws.com:27017" +
            "/?ssl_ca_certs=rds-combined-ca-bundle.pem&replicaSet=rs0"

    init {
        val mongo = MongoClients.create(cs)
        mongo.startSession().forEach { clientSession, throwable ->


        }
        mongo.listDatabaseNames().forEach { s, throwable ->
            throwable?.printStackTrace() ?: slog(s)
        }

    }

    private inline fun <reified T> MongoClient.getDatabase()
            = getDatabase(defaultCollectionNameBuilder(T::class))

}
