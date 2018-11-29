/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import com.ampro.weebot.commands.IPassive
import com.ampro.weebot.commands.commands
import com.google.gson.*
import com.jagrosh.jdautilities.command.Command
import java.lang.reflect.Type

private val CLASSNAME = "CLASSNAME"
private val DATA = "DATA"

/**
 * A Type Adapter to serialize [Command].class objects
 */
class CommandClassAdapter : JsonSerializer<Class<out Command>>,
                            JsonDeserializer<Class<out Command>> {

    @Throws(JsonParseException::class)
    override fun deserialize(jsonElement: JsonElement, type: Type,
                             context: JsonDeserializationContext): Class<out Command>? {
        val jsonObject = jsonElement.asJsonObject
        val prim = jsonObject.get(CLASSNAME) as JsonPrimitive
        val className = prim.asString
        for (command in commands) {
            if (command.javaClass.name == className) {
                return command.javaClass
            }
        }
        return null
    }

    override fun serialize(jsonElement: Class<out Command>, type: Type,
                           context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty(CLASSNAME, jsonElement.name)
        return jsonObject
    }

}


class InterfaceAdapter<T: Any> : JsonSerializer<T>, JsonDeserializer<T> {

    @Throws(JsonParseException::class)
    override fun deserialize(jsonElement: JsonElement, type: Type,
                             jsonDeserializationContext: JsonDeserializationContext): T {

        val jsonObject = jsonElement.asJsonObject
        val prim = jsonObject.get(CLASSNAME) as JsonPrimitive
        val className = prim.asString
        val klass = getObjectClass(className)
        return jsonDeserializationContext.deserialize(jsonObject.get(DATA), klass)
    }

    override fun serialize(jsonElement: T, type: Type,
                           jsonSerializationContext: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty(CLASSNAME, jsonElement.javaClass.name)
        jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement))
        return jsonObject
    }

    /****** Helper method to get the className of the object to be deserialized  */
    fun getObjectClass(className: String): Class<*> {
        try {
            return Class.forName(className)
        } catch (e: ClassNotFoundException) {
            //e.printStackTrace();
            throw JsonParseException(e.message)
        }
    }

}
