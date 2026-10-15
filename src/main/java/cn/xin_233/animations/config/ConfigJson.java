package cn.xin_233.animations.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ConfigJson {
    private ConfigJson() {
    }

    static void fields(JsonObject object, String... allowed) {
        Set<String> names = new HashSet<>(Arrays.asList(allowed));
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!names.contains(entry.getKey())) {
                throw new IllegalArgumentException("Unknown field: " + entry.getKey());
            }
            if (entry.getValue().isJsonNull()) {
                throw new IllegalArgumentException("Field cannot be null: " + entry.getKey());
            }
        }
    }

    static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must be a non-empty string");
        }
        return value.getAsString().trim();
    }

    static String id(JsonObject object, String name) {
        String value = string(object, name);
        if (!value.matches("[a-z0-9][a-z0-9_./-]*") || value.contains("..")) {
            throw new IllegalArgumentException(name + " must be a lowercase effect ID without '..'");
        }
        return value;
    }

    static boolean bool(JsonObject object, String name, boolean fallback) {
        if (!object.has(name)) {
            return fallback;
        }
        JsonElement value = object.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(name + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    static int integer(JsonElement value, String name, int min, int max) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        try {
            int result = new BigDecimal(value.getAsString()).intValueExact();
            if (result < min || result > max) {
                throw new IllegalArgumentException(name + " must be within " + min + ".." + max);
            }
            return result;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    static int integer(JsonObject object, String name, int fallback, int min, int max) {
        return object.has(name) ? integer(object.get(name), name, min, max) : fallback;
    }

    static void number(JsonObject object, String name, float min, float max) {
        if (!object.has(name)) {
            return;
        }
        JsonElement value = object.get(name);
        if (!(value instanceof JsonPrimitive) || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be a number");
        }
        float result = value.getAsFloat();
        if (!Float.isFinite(result) || result < min || result > max) {
            throw new IllegalArgumentException(name + " must be within " + min + ".." + max);
        }
    }

    static float decimal(JsonObject object, String name, float fallback, float min, float max) {
        number(object, name, min, max);
        return object.has(name) ? object.get(name).getAsFloat() : fallback;
    }

    static <T extends Enum<T>> T enumeration(Class<T> type, JsonObject object, String name) {
        String value = string(object, name);
        for (T candidate : type.getEnumConstants()) {
            if (candidate.name().equalsIgnoreCase(value)) {
                object.addProperty(name, candidate.name());
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown " + name + ": " + value);
    }
}
