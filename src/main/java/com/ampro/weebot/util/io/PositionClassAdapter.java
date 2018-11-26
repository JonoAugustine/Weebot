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
public class PositionClassAdapter<T extends Position>
        implements JsonSerializer<T>,
        JsonDeserializer<T> {

    private static final String CLASSNAME = "CLASSNAME";
    private static final String DATA = "DATA";

    @Override
    public T deserialize(JsonElement jsonElement, Type type,
                         JsonDeserializationContext context)
    throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String klName = jsonObject.get(CLASSNAME).getAsString();
        if (OptionPosition.class.getName().equals(klName)) {
            return context.deserialize(jsonObject.get(DATA), OptionPosition.class);
        } else if (StockPostion.class.getName().equals(klName)) {
            return context.deserialize(jsonObject.get(DATA), StockPostion.class);
        }
        return null;
    }

    @Override
    public JsonElement serialize(T jsonElement, Type type,
                                 JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(CLASSNAME, jsonElement.getClass().getName());
        jsonObject.add(DATA, context.serialize(jsonElement));
        return jsonObject;
    }

}
