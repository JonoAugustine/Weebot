package com.ampro.weebot.util.io;

import com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.OptionPosition;
import com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Position;
import com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.StockPostion;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * JSON class adapter for {@link Position} subclasses.
 *
 * @author Jonathan Augustine
 * @since 1.1.0
 */
public class PositionClassAdapter implements JsonSerializer<Class<? extends Position>>,
        JsonDeserializer<Class<? extends Position>> {

    private static final String CLASSNAME = "CLASSNAME";
    private static final String DATA = "DATA";

    @Override
    public Class<? extends Position> deserialize(JsonElement jsonElement, Type type,
                                                 JsonDeserializationContext context)
    throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (OptionPosition.class.getName().equals(jsonObject.get(CLASSNAME))) {
            return context.deserialize(jsonObject.get(DATA), OptionPosition.class);
        } else if (StockPostion.class.getName().equals(jsonObject.get(CLASSNAME))) {
            return context.deserialize(jsonObject.get(DATA), StockPostion.class);
        }
        return null;
    }

    @Override
    public JsonElement serialize(Class<? extends Position> jsonElement, Type type,
                                 JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(CLASSNAME, jsonElement.getName());
        jsonObject.add(DATA, context.serialize(jsonElement));
        return jsonObject;
    }

}
