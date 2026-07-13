package io.github.projectunified.craftconfig.gson.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Some utilities for Gson
 */
public final class GsonUtil {
    private GsonUtil() {
    }

    /**
     * Parse a string to {@link JsonElement}
     *
     * @param string the string
     * @return the {@link JsonElement}
     */
    public static JsonElement parse(String string) {
        return JsonParser.parseString(string);
    }

    /**
     * Parse a reader to {@link JsonElement}
     *
     * @param reader the reader
     * @return the {@link JsonElement}
     */
    public static JsonElement parse(Reader reader) {
        return JsonParser.parseReader(reader);
    }

    /**
     * Parse a reader to {@link JsonElement}
     *
     * @param reader the reader
     * @return the {@link JsonElement}
     */
    public static JsonElement parse(JsonReader reader) {
        return JsonParser.parseReader(reader);
    }

    /**
     * Get the elements from a {@link JsonArray}
     *
     * @param array the array
     * @return the elements
     */
    public static List<JsonElement> getElements(JsonArray array) {
        List<JsonElement> elements = new ArrayList<>();
        for (JsonElement element : array) {
            elements.add(element);
        }
        return elements;
    }

    /**
     * Check if the {@link JsonArray} is empty
     *
     * @param array the array
     * @return true if it is empty
     */
    public static boolean isEmpty(JsonArray array) {
        return array == null || array.size() == 0;
    }

    /**
     * Get the elements from a {@link JsonObject}
     *
     * @param object the object
     * @return the elements
     */
    public static Map<String, JsonElement> getElements(JsonObject object) {
        Map<String, JsonElement> elements = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            elements.put(entry.getKey(), entry.getValue());
        }
        return elements;
    }

    /**
     * Normalize the {@link JsonElement} to the object
     *
     * @param element the element
     * @param deep    whether to normalize the nested elements
     * @return the normalized object
     */
    public static Object normalize(JsonElement element, boolean deep) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = element.getAsJsonPrimitive();
            if (jsonPrimitive.isBoolean()) {
                return jsonPrimitive.getAsBoolean();
            } else if (jsonPrimitive.isNumber()) {
                return jsonPrimitive.getAsNumber();
            } else if (jsonPrimitive.isString()) {
                return jsonPrimitive.getAsString();
            }
        } else if (element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            if (deep) {
                List<Object> list = new ArrayList<>();
                for (JsonElement jsonElement : jsonArray) {
                    list.add(normalize(jsonElement, true));
                }
                return list;
            } else {
                return getElements(jsonArray);
            }
        } else if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            if (deep) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    map.put(entry.getKey(), normalize(entry.getValue(), true));
                }
                return map;
            } else {
                return getElements(jsonObject);
            }
        } else if (element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }
}
