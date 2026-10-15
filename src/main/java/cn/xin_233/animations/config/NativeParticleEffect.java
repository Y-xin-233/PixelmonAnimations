package cn.xin_233.animations.config;

import cn.xin_233.animations.animation.PacketSafeParticleData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pixelmonmod.pixelmon.battles.attacks.animations.AttackAnimationData;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackBall;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackBeam;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackBuff;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackExplosion;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackRadial;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackRain;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackStatChange;
import com.pixelmonmod.pixelmon.client.particle.particles.AttackEffect.EnumParticleTexture;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class NativeParticleEffect implements ConfiguredEffect {
    private static final Gson GSON = new Gson();
    private final String id;
    private final Class<? extends AttackAnimationData> dataClass;
    private final String dataJson;

    private NativeParticleEffect(String id, Class<? extends AttackAnimationData> dataClass, JsonObject data) {
        this.id = id;
        this.dataClass = dataClass;
        this.dataJson = GSON.toJson(data);
    }

    public String getId() {
        return id;
    }

    public AttackAnimationData createData() {
        // Pixelmon fills missing defaults and some systems mutate data while rendering.
        return GSON.fromJson(dataJson, dataClass);
    }

    public static NativeParticleEffect parse(JsonObject object) {
        ConfigJson.integer(object, "format", 1, 1, 1);
        String id = ConfigJson.id(object, "id");
        String renderer = ConfigJson.string(object, "renderer");
        if (!"native_particle".equals(renderer)) {
            throw new IllegalArgumentException("Unsupported renderer: " + renderer);
        }
        String type = ConfigJson.string(object, "type").toLowerCase(Locale.ROOT);
        List<String> fields = new ArrayList<>(Arrays.asList("format", "id", "renderer", "type",
                "power", "speed", "rgb", "texture", "lifetimeTicks", "scale"));
        Class<? extends AttackAnimationData> dataClass;
        switch (type) {
            case "ball":
                dataClass = AttackBall.BallData.class;
                break;
            case "beam":
                dataClass = AttackBeam.BeamData.class;
                fields.addAll(Arrays.asList("segments", "variation", "inverted"));
                ConfigJson.integer(object, "segments", 15, 1, 255);
                ConfigJson.number(object, "variation", 0, 1);
                ConfigJson.bool(object, "inverted", false);
                break;
            case "buff":
                dataClass = AttackBuff.BuffData.class;
                fields.add("atTarget");
                ConfigJson.bool(object, "atTarget", true);
                break;
            case "explosion":
                dataClass = AttackExplosion.ExplosionData.class;
                fields.add("atTarget");
                ConfigJson.bool(object, "atTarget", false);
                break;
            case "tube":
                dataClass = PacketSafeParticleData.Tube.class;
                fields.addAll(Arrays.asList("segments", "height", "radius", "conical", "narrowness", "apexRadius"));
                ConfigJson.integer(object, "segments", 13, 2, 127);
                ConfigJson.number(object, "height", 0.01F, 64);
                ConfigJson.number(object, "radius", 0.01F, 64);
                ConfigJson.number(object, "narrowness", 0.01F, 64);
                ConfigJson.number(object, "apexRadius", 0, 64);
                ConfigJson.bool(object, "conical", true);
                break;
            case "radial":
                dataClass = AttackRadial.RadialData.class;
                fields.addAll(Arrays.asList("totalPoints", "radius"));
                ConfigJson.integer(object, "totalPoints", 36, 1, 255);
                ConfigJson.integer(object, "radius", -1, 1, 255);
                break;
            case "rain":
                dataClass = AttackRain.RainData.class;
                fields.addAll(Arrays.asList("durationTicks", "maxHorizontalDisplacement", "heightAboveTarget"));
                ConfigJson.integer(object, "durationTicks", 0, 1, 127);
                ConfigJson.integer(object, "heightAboveTarget", 2, 0, 127);
                ConfigJson.number(object, "maxHorizontalDisplacement", 0, 64);
                break;
            case "rise":
                dataClass = PacketSafeParticleData.Rise.class;
                fields.addAll(Arrays.asList("durationTicks", "particleAmount", "startHeight", "acceleration",
                        "endHeight", "radius", "sparkle"));
                ConfigJson.integer(object, "durationTicks", 50, 1, 127);
                ConfigJson.integer(object, "particleAmount", 15, 1, 127);
                ConfigJson.integer(object, "startHeight", 0, -128, 127);
                ConfigJson.number(object, "acceleration", -10, 10);
                ConfigJson.number(object, "endHeight", 0.01F, 64);
                ConfigJson.number(object, "radius", 0.01F, 64);
                ConfigJson.bool(object, "sparkle", false);
                // Pixelmon replaces particleAmount whenever power is omitted. Keep the explicit amount.
                if (object.has("particleAmount") && !object.has("power")) {
                    object.addProperty("power", 40);
                }
                break;
            case "stat_change":
                dataClass = AttackStatChange.StatChangeData.class;
                fields.addAll(Arrays.asList("stat", "stages"));
                if (object.has("stat")) {
                    StatsType stat = ConfigJson.enumeration(StatsType.class, object, "stat");
                    if (stat == StatsType.None || stat == StatsType.HP) {
                        throw new IllegalArgumentException("stat must be a battle stat");
                    }
                }
                if (ConfigJson.integer(object, "stages", 1, -6, 6) == 0) {
                    throw new IllegalArgumentException("stages cannot be zero");
                }
                break;
            case "lightning":
                throw new IllegalArgumentException("Pixelmon 8.4.3 lightning emits no particles; use beam instead");
            default:
                throw new IllegalArgumentException("Unsupported native particle type: " + type);
        }
        ConfigJson.fields(object, fields.toArray(new String[0]));
        ConfigJson.integer(object, "power", -1, 1, 32767);
        ConfigJson.integer(object, "lifetimeTicks", -1, 0, 255);
        ConfigJson.number(object, "speed", 0.001F, 64);
        ConfigJson.number(object, "scale", 0.001F, 64);
        if (object.has("rgb")) {
            if (!object.get("rgb").isJsonArray() || object.getAsJsonArray("rgb").size() != 3) {
                throw new IllegalArgumentException("rgb must contain exactly three integers");
            }
            JsonArray rgb = object.getAsJsonArray("rgb");
            for (int i = 0; i < 3; i++) {
                ConfigJson.integer(rgb.get(i), "rgb[" + i + "]", 0, 255);
            }
        }
        if (object.has("texture")) {
            ConfigJson.enumeration(EnumParticleTexture.class, object, "texture");
        }
        JsonObject data = new JsonObject();
        for (String field : fields) {
            if (!Arrays.asList("format", "id", "renderer", "type").contains(field) && object.has(field)) {
                data.add(field, object.get(field));
            }
        }
        return new NativeParticleEffect(id, dataClass, data);
    }
}
