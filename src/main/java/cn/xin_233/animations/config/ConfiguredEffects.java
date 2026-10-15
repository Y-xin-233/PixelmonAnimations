package cn.xin_233.animations.config;

import com.google.gson.JsonObject;

public final class ConfiguredEffects {
    private ConfiguredEffects() {
    }

    public static ConfiguredEffect parse(JsonObject object) {
        String renderer = ConfigJson.string(object, "renderer");
        switch (renderer) {
            case "native_particle":
                return NativeParticleEffect.parse(object);
            case "fbx":
                return FbxEffect.parse(object);
            default:
                throw new IllegalArgumentException("Unsupported renderer: " + renderer);
        }
    }
}
