package cn.xin_233.animations.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkillAnimationConfig {
    public static final class Event {
        public final int tick;
        public final ConfiguredEffect effect;
        public final boolean groundedStart;
        public final boolean groundedEnd;

        private Event(int tick, ConfiguredEffect effect, boolean groundedStart, boolean groundedEnd) {
            this.tick = tick;
            this.effect = effect;
            this.groundedStart = groundedStart;
            this.groundedEnd = groundedEnd;
        }
    }

    public final Integer moveId;
    public final String moveName;
    public final boolean replaceNative;
    public final boolean oncePerTurn;
    public final int durationTicks;
    public final List<Event> events;

    private SkillAnimationConfig(Integer moveId, String moveName, boolean replaceNative, boolean oncePerTurn,
                                 int durationTicks, List<Event> events) {
        this.moveId = moveId;
        this.moveName = moveName;
        this.replaceNative = replaceNative;
        this.oncePerTurn = oncePerTurn;
        this.durationTicks = durationTicks;
        this.events = Collections.unmodifiableList(events);
    }

    public static SkillAnimationConfig parse(JsonObject object, Map<String, ? extends ConfiguredEffect> effects) {
        ConfigJson.fields(object, "format", "enabled", "moveId", "moveName", "replaceNative",
                "oncePerTurn", "durationTicks", "events");
        ConfigJson.integer(object, "format", 1, 1, 1);
        if (!ConfigJson.bool(object, "enabled", true)) {
            return null;
        }
        Integer moveId = object.has("moveId") ? ConfigJson.integer(object, "moveId", -1, 0, 32767) : null;
        String moveName = object.has("moveName") ? ConfigJson.string(object, "moveName") : null;
        if (moveId == null && moveName == null) {
            throw new IllegalArgumentException("A skill must specify moveId or English moveName");
        }
        boolean replaceNative = ConfigJson.bool(object, "replaceNative", true);
        boolean oncePerTurn = ConfigJson.bool(object, "oncePerTurn", true);
        int duration = ConfigJson.integer(object, "durationTicks", 0, 0, 1200);
        JsonElement eventArray = object.get("events");
        if (eventArray == null || !eventArray.isJsonArray() || eventArray.getAsJsonArray().size() == 0
                || eventArray.getAsJsonArray().size() > 256) {
            throw new IllegalArgumentException("events must contain 1..256 event objects");
        }
        List<Event> events = new ArrayList<>();
        for (JsonElement element : eventArray.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Each event must be an object");
            }
            JsonObject event = element.getAsJsonObject();
            ConfigJson.fields(event, "tick", "effect", "groundedStart", "groundedEnd");
            int tick = ConfigJson.integer(event, "tick", 0, 0, 1200);
            String effectId = ConfigJson.id(event, "effect");
            ConfiguredEffect effect = effects.get(effectId);
            if (effect == null) {
                throw new IllegalArgumentException("Unknown effect: " + effectId);
            }
            events.add(new Event(tick, effect, ConfigJson.bool(event, "groundedStart", false),
                    ConfigJson.bool(event, "groundedEnd", false)));
            int endTick = tick + effect.getDurationTicks();
            if (endTick > 1200) {
                throw new IllegalArgumentException("Event plus effect duration exceeds 1200 ticks");
            }
            duration = Math.max(duration, endTick);
        }
        events.sort(Comparator.comparingInt(event -> event.tick));
        return new SkillAnimationConfig(moveId, moveName, replaceNative, oncePerTurn, duration, events);
    }

    public String nameKey() {
        return moveName == null ? null : moveName.toLowerCase(Locale.ROOT);
    }
}
