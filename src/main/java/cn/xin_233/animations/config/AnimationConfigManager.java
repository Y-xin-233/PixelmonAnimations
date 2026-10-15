package cn.xin_233.animations.config;

import cn.xin_233.animations.animation.ConfiguredAttackAnimation;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.pixelmonmod.pixelmon.api.attackAnimations.AttackAnimation;
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;

public final class AnimationConfigManager {
    private static final class Snapshot {
        final Map<Integer, SkillAnimationConfig> byId;
        final Map<String, SkillAnimationConfig> byName;
        final Map<String, ConfiguredEffect> effects;
        final int skillCount;
        final int effectCount;

        Snapshot(Map<Integer, SkillAnimationConfig> byId, Map<String, SkillAnimationConfig> byName,
                 Map<String, ConfiguredEffect> effects, int skillCount, int effectCount) {
            this.byId = Collections.unmodifiableMap(byId);
            this.byName = Collections.unmodifiableMap(byName);
            this.effects = Collections.unmodifiableMap(effects);
            this.skillCount = skillCount;
            this.effectCount = effectCount;
        }
    }

    private final Logger logger;
    private Path root;
    private volatile Snapshot snapshot = new Snapshot(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, 0);

    public AnimationConfigManager(Logger logger) {
        this.logger = logger;
    }

    public synchronized void initialize(Path directory) {
        root = directory.toAbsolutePath().normalize();
        boolean firstRun = !Files.exists(root);
        try {
            Files.createDirectories(root.resolve("skills"));
            Files.createDirectories(root.resolve("effects"));
            Files.createDirectories(root.resolve("effects/models"));
            Files.createDirectories(root.resolve("effects/textures"));
            if (firstRun) {
                copyExample("skills/thunderbolt.json");
                copyExample("effects/electric_beam.json");
                copyExample("effects/electric_impact.json");
                copyExample("skills/thunderbolt_fbx.json");
                copyExample("effects/thunderbolt_pikachu.json");
                copyExample("effects/models/thunderbolt_pikachu.fbx");
            }
            logger.info("Animation directory: {}", root);
        } catch (IOException exception) {
            logger.error("Cannot initialize animation directory {}", root, exception);
        }
    }

    private void copyExample(String relative) throws IOException {
        try (InputStream input = AnimationConfigManager.class.getResourceAsStream("/examples/" + relative)) {
            if (input == null) {
                throw new IOException("Missing bundled example: " + relative);
            }
            Files.copy(input, root.resolve(relative));
        }
    }

    public synchronized boolean reload() {
        if (root == null) {
            return false;
        }
        Map<String, ConfiguredEffect> effects = new HashMap<>();
        Map<Integer, SkillAnimationConfig> byId = new HashMap<>();
        Map<String, SkillAnimationConfig> byName = new HashMap<>();
        int skillCount = 0;
        Path currentFile = root;
        try {
            for (Path path : jsonFiles(root.resolve("effects"))) {
                currentFile = path;
                ConfiguredEffect effect = ConfiguredEffects.parse(readObject(path));
                if (effects.putIfAbsent(effect.getId(), effect) != null) {
                    throw new IllegalArgumentException("Duplicate effect ID: " + effect.getId());
                }
            }
            for (Path path : jsonFiles(root.resolve("skills"))) {
                currentFile = path;
                SkillAnimationConfig skill = SkillAnimationConfig.parse(readObject(path), effects);
                if (skill == null) {
                    continue;
                }
                if (skill.moveId != null && byId.putIfAbsent(skill.moveId, skill) != null) {
                    throw new IllegalArgumentException("Duplicate move ID: " + skill.moveId);
                }
                if (skill.nameKey() != null && byName.putIfAbsent(skill.nameKey(), skill) != null) {
                    throw new IllegalArgumentException("Duplicate move name: " + skill.moveName);
                }
                skillCount++;
            }
            snapshot = new Snapshot(byId, byName, effects, skillCount, effects.size());
            logger.info("Loaded {} skills and {} effects", skillCount, effects.size());
            return true;
        } catch (IOException | RuntimeException exception) {
            logger.error("Animation reload failed at {}. Previous configuration retained: {}",
                    currentFile, exception.getMessage(), exception);
            return false;
        }
    }

    private static List<Path> jsonFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
    }

    private static com.google.gson.JsonObject readObject(Path path) throws IOException {
        try (Reader input = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             JsonReader reader = new JsonReader(input)) {
            JsonElement element = new JsonParser().parse(reader);
            if (!element.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("File must contain one JSON object");
            }
            return element.getAsJsonObject();
        }
    }

    public SkillAnimationConfig find(int moveId, String moveName) {
        Snapshot current = snapshot;
        SkillAnimationConfig skill = current.byId.get(moveId);
        // When an ID is supplied it is authoritative; moveName is descriptive only.
        if (skill != null || moveName == null) {
            return skill;
        }
        skill = current.byName.get(moveName.toLowerCase(Locale.ROOT));
        return skill != null && skill.moveId == null ? skill : null;
    }

    public ArrayList<AttackAnimation> resolve(AttackBase move, ArrayList<AttackAnimation> original) {
        SkillAnimationConfig skill = find(move.getAttackId(), move.getAttackName());
        if (skill == null) {
            return original;
        }
        ArrayList<AttackAnimation> result = new ArrayList<>();
        if (!skill.replaceNative && original != null) {
            result.addAll(original);
        }
        result.add(new ConfiguredAttackAnimation(skill));
        return result;
    }

    public int getSkillCount() {
        return snapshot.skillCount;
    }

    public int getEffectCount() {
        return snapshot.effectCount;
    }

    public ConfiguredEffect findEffect(String effectId) {
        return snapshot.effects.get(effectId);
    }

    public Path getEffectsDirectory() {
        return root.resolve("effects");
    }
}
