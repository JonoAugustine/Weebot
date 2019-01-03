/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import com.ampro.weebot.database.constants.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.annotations.SerializedName
import com.twilio.http.TwilioRestClient
import com.twilio.rest.api.v2010.account.Message
import com.twilio.rest.api.v2010.account.MessageCreator
import com.twilio.type.PhoneNumber

/** Wrapper class for APIs responses that only hold a URL string */
data class ApiLinkResponse(@SerializedName("link") val link: String = "")

inline fun <reified C: Any> String.get() : Result<C, FuelError> {
    return this.httpGet().responseObject<C>().third
}

/**
 * General setup steps to make API usage simpler.
 */
fun setupWebFuel() {
    FuelManager.instance.baseHeaders = mapOf("User-Agent" to "Mozilla/5.0")
    FuelManager.instance.baseParams  = listOf("Accept" to "application/json")
}


val TWILIO_CLIENT: TwilioRestClient? = TwilioRestClient.Builder(
    TWILIO_SID,
    TWILIO_TOKEN).build()

fun sendSMS(to: String, message: String): Message?
        = MessageCreator(PhoneNumber(to),
    TWILIO_NUMBER, message).create(TWILIO_CLIENT)

fun sendSMS(to: String, message: String, handler: (Message?) -> Unit): Message?
        = MessageCreator(PhoneNumber(to),
    TWILIO_NUMBER, message).create(TWILIO_CLIENT)
    .also { handler(it) }


