package com.ampro.weebot.util.io;

import com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.OptionPosition;
import com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.StockPostion;
import com.google.gson.*;

import java.lang.reflect.Type;

public class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    private static final String CLASSNAME = "CLASSNAME";
    private static final String DATA = "DATA";

    @Override
    public T deserialize(JsonElement jsonElement, Type type,
                         JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        System.out.println("IA: " + jsonObject.get(CLASSNAME));

        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
        String className = prim.getAsString();
        Class klass = getObjectClass(className);
        return context.deserialize(jsonObject.get(DATA), klass);
    }

    @Override
    public JsonElement serialize(T jsonElement, Type type,
                                 JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(CLASSNAME, jsonElement.getClass().getName());
        jsonObject.add(DATA, context.serialize(jsonElement));
        return jsonObject;
    }

    /****** Helper method to get the className of the object to be deserialized *****/
    public Class getObjectClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
            throw new JsonParseException(e.getMessage());
        }
    }

}

