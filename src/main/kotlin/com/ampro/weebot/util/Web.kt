/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.annotations.SerializedName

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
