/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.util

import com.ampro.weebot.MLOG
import com.ampro.weebot.commands.COMMANDS
import com.google.gson.*
import com.jagrosh.jdautilities.command.Command
import java.lang.Exception
import java.lang.reflect.Type
import kotlin.reflect.KClass

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
        for (command in COMMANDS) {
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


/**
 * A Type Adapter to serialize [Command].class objects
 */
class CommandKClassAdapter : JsonSerializer<KClass<out Command>>,
                            JsonDeserializer<KClass<out Command>> {

    @Throws(JsonParseException::class)
    override fun deserialize(jsonElement: JsonElement, type: Type,
                             context: JsonDeserializationContext): KClass<out Command>? {
        val jsonObject = jsonElement.asJsonObject
        val prim = jsonObject.get(CLASSNAME) as JsonPrimitive
        val className = prim.asString
        for (command in COMMANDS) {
            if (command::class.qualifiedName == className) {
                return command::class
            }
        }
        return null
    }

    override fun serialize(jsonElement: KClass<out Command>, type: Type,
                           context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty(CLASSNAME, jsonElement::class.qualifiedName)
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
        try {
            jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement))
        } catch (e: Exception) {
            MLOG.elog(this::class, jsonElement.javaClass.name)
        }
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
