package cn.xin_233.animations.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Locale;

/** An independent effect asset, never a replacement for a Pokemon model. */
public final class FbxEffect implements ConfiguredEffect {
    private final String id;
    public final String model;
    public final String animation;
    public final String texture;
    public final boolean atUser;
    public final boolean followEntity;
    public final boolean loop;
    public final boolean additive;
    public final int durationTicks;
    public final float scale;
    public final float speed;
    public final float alpha;
    public final float offsetX, offsetY, offsetZ;
    public final float rotationX, rotationY, rotationZ;

    private FbxEffect(JsonObject json) {
        id = ConfigJson.id(json, "id");
        model = assetPath(ConfigJson.string(json, "model"));
        if (!model.toLowerCase(Locale.ROOT).endsWith(".fbx")) {
            throw new IllegalArgumentException("model must name an FBX file");
        }
        animation = ConfigJson.string(json, "animation");
        texture = json.has("texture") ? assetPath(ConfigJson.string(json, "texture")) : null;
        if (texture != null && !texture.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg)")) {
            throw new IllegalArgumentException("texture must be PNG or JPEG");
        }
        String anchor = json.has("anchor") ? ConfigJson.string(json, "anchor") : "target";
        if (!"user".equals(anchor) && !"target".equals(anchor)) {
            throw new IllegalArgumentException("anchor must be user or target");
        }
        atUser = "user".equals(anchor);
        followEntity = ConfigJson.bool(json, "followEntity", true);
        loop = ConfigJson.bool(json, "loop", false);
        String blend = json.has("blend") ? ConfigJson.string(json, "blend") : "alpha";
        if (!"alpha".equals(blend) && !"additive".equals(blend)) {
            throw new IllegalArgumentException("blend must be alpha or additive");
        }
        additive = "additive".equals(blend);
        durationTicks = ConfigJson.integer(json, "durationTicks", 30, 1, 1200);
        scale = ConfigJson.decimal(json, "scale", 1, 0.0001F, 100);
        speed = ConfigJson.decimal(json, "speed", 1, 0.01F, 16);
        alpha = ConfigJson.decimal(json, "alpha", 1, 0, 1);
        float[] offset = vector(json, "offset", -64, 64);
        offsetX = offset[0];
        offsetY = offset[1];
        offsetZ = offset[2];
        float[] rotation = vector(json, "rotation", -360, 360);
        rotationX = rotation[0];
        rotationY = rotation[1];
        rotationZ = rotation[2];
    }

    public static FbxEffect parse(JsonObject json) {
        ConfigJson.fields(json, "format", "id", "renderer", "model", "animation", "texture", "anchor",
                "followEntity", "loop", "durationTicks", "scale", "speed", "alpha", "offset", "rotation", "blend");
        ConfigJson.integer(json, "format", 1, 1, 1);
        if (!"fbx".equals(ConfigJson.string(json, "renderer"))) {
            throw new IllegalArgumentException("Expected renderer fbx");
        }
        return new FbxEffect(json);
    }

    public static String assetPath(String value) {
        if (value.length() > 512 || value.startsWith("/") || value.contains("\\")
                || value.contains(":") || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Asset path must be relative to effects, using '/'");
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                    || segment.endsWith(".") || segment.endsWith(" ")) {
                throw new IllegalArgumentException("Invalid asset path segment");
            }
        }
        return value;
    }

    private static float[] vector(JsonObject json, String name, float min, float max) {
        if (!json.has(name)) {
            return new float[3];
        }
        if (!json.get(name).isJsonArray() || json.getAsJsonArray(name).size() != 3) {
            throw new IllegalArgumentException(name + " must contain exactly three numbers");
        }
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            JsonObject component = new JsonObject();
            component.add(name, json.getAsJsonArray(name).get(i));
            result[i] = ConfigJson.decimal(component, name, 0, min, max);
        }
        return result;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getDurationTicks() {
        return durationTicks;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("format", 1);
        json.addProperty("id", id);
        json.addProperty("renderer", "fbx");
        json.addProperty("model", model);
        json.addProperty("animation", animation);
        if (texture != null) {
            json.addProperty("texture", texture);
        }
        json.addProperty("anchor", atUser ? "user" : "target");
        json.addProperty("followEntity", followEntity);
        json.addProperty("loop", loop);
        json.addProperty("durationTicks", durationTicks);
        json.addProperty("scale", scale);
        json.addProperty("speed", speed);
        json.addProperty("alpha", alpha);
        json.addProperty("blend", additive ? "additive" : "alpha");
        json.add("offset", array(offsetX, offsetY, offsetZ));
        json.add("rotation", array(rotationX, rotationY, rotationZ));
        return json;
    }

    private static JsonArray array(float x, float y, float z) {
        JsonArray values = new JsonArray();
        values.add(x);
        values.add(y);
        values.add(z);
        return values;
    }
}
