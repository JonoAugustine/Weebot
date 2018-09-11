package com.ampro.weebot.util.io;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A Type Adapter to serialize {@link Command}.class objects
 */
public class CommandClassAdapter
        implements JsonSerializer<Class<? extends Command>>,
        JsonDeserializer<Class<? extends Command>> {

    private static final String CLASSNAME = "CLASSNAME";

    private static List<Command> commands;
    static { commands = Launcher.getCommands(); }

    @Override
    public Class<? extends Command> deserialize(JsonElement jsonElement, Type type,
                         JsonDeserializationContext context)
    throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
        String className = prim.getAsString();
        for (Command command : commands) {
            if (command.getClass().getName().equals(className)) {
                return command.getClass();
            }
        }
        return null;
    }

    @Override
    public JsonElement serialize(Class<? extends Command> jsonElement, Type type,
                                 JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(CLASSNAME, jsonElement.getName());
        return jsonObject;
    }

}
