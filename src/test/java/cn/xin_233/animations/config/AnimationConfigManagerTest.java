package cn.xin_233.animations.config;

import cn.xin_233.animations.animation.ConfiguredAttackAnimation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pixelmonmod.pixelmon.api.attackAnimations.AttackAnimation;
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase;
import com.pixelmonmod.pixelmon.battles.attacks.animations.AttackAnimationStationary;
import com.pixelmonmod.pixelmon.enums.EnumType;
import com.pixelmonmod.pixelmon.enums.battle.AttackCategory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class AnimationConfigManagerTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();
    private AnimationConfigManager manager;
    private Path root;

    @Before
    public void setup() throws Exception {
        root = temporary.newFolder("game").toPath().resolve("pixelmonanimations");
        manager = new AnimationConfigManager(LogManager.getLogger("AnimationConfigManagerTest"));
        manager.initialize(root);
    }

    private JsonObject skill() {
        return new JsonParser().parse("{\"moveId\":122,\"moveName\":\"Thunderbolt\",\"durationTicks\":1,"
                + "\"events\":[{\"tick\":12,\"effect\":\"electric_impact\"},{\"tick\":0,\"effect\":\"electric_beam\"},"
                + "{\"tick\":0,\"effect\":\"electric_impact\"}]}").getAsJsonObject();
    }

    private void write(String relative, JsonObject json) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private AttackBase move() {
        return new AttackBase(EnumType.Electric, 90, AttackCategory.SPECIAL) {
            @Override
            public int getAttackId() {
                return 122;
            }

            @Override
            public String getAttackName() {
                return "Thunderbolt";
            }
        };
    }

    @Test
    public void unconfiguredMoveReturnsOriginalAnimationList() {
        assertTrue(manager.reload());
        ArrayList<AttackAnimation> original = new ArrayList<>();
        original.add(new AttackAnimationStationary());
        assertSame(original, manager.resolve(move(), original));
    }

    @Test
    public void replacementDoesNotMutatePixelmonAnimationList() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        ArrayList<AttackAnimation> original = new ArrayList<>();
        AttackAnimation nativeAnimation = new AttackAnimationStationary();
        original.add(nativeAnimation);
        ArrayList<AttackAnimation> resolved = manager.resolve(move(), original);
        assertNotSame(original, resolved);
        assertEquals(1, resolved.size());
        assertTrue(resolved.get(0) instanceof ConfiguredAttackAnimation);
        assertTrue(resolved.get(0).usedOncePerTurn());
        assertEquals(1, original.size());
        assertSame(nativeAnimation, original.get(0));
    }

    @Test
    public void appendKeepsNativeAnimationAndHonorsPlaybackLimitOption() throws Exception {
        JsonObject json = skill();
        json.addProperty("replaceNative", false);
        json.addProperty("oncePerTurn", false);
        write("skills/thunderbolt.json", json);
        assertTrue(manager.reload());
        ArrayList<AttackAnimation> original = new ArrayList<>();
        original.add(new AttackAnimationStationary());
        ArrayList<AttackAnimation> resolved = manager.resolve(move(), original);
        assertEquals(2, resolved.size());
        assertSame(original.get(0), resolved.get(0));
        assertTrue(resolved.get(1) instanceof ConfiguredAttackAnimation);
        assertFalse(resolved.get(1).usedOncePerTurn());
        assertEquals(1, original.size());
    }

    @Test
    public void createsSeparateDirectoriesAndDisabledExamples() {
        assertTrue(Files.isDirectory(root.resolve("skills")));
        assertTrue(Files.isDirectory(root.resolve("effects")));
        assertTrue(manager.reload());
        assertEquals(0, manager.getSkillCount());
        assertEquals(3, manager.getEffectCount());
        assertNull(manager.find(122, "Thunderbolt"));
    }

    @Test
    public void timelineSortsEventsPreservesSameTickOrderAndExtendsDuration() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        SkillAnimationConfig config = manager.find(122, "Thunderbolt");
        assertEquals(12, config.durationTicks);
        assertEquals(0, config.events.get(0).tick);
        assertEquals("electric_beam", config.events.get(0).effect.getId());
        assertEquals("electric_impact", config.events.get(1).effect.getId());
        assertEquals(12, config.events.get(2).tick);
        assertTrue(config.replaceNative);
        assertTrue(config.oncePerTurn);
    }

    @Test
    public void explicitIdIsAuthoritativeAndNameOnlyMatchingIgnoresCase() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        assertNotNull(manager.find(122, "Different name"));
        assertNull(manager.find(123, "Thunderbolt"));
        JsonObject nameOnly = skill();
        nameOnly.remove("moveId");
        write("skills/thunderbolt.json", nameOnly);
        assertTrue(manager.reload());
        assertNotNull(manager.find(123, "THUNDERBOLT"));
    }

    @Test
    public void invalidReloadKeepsPreviousSnapshotAndActiveTimeline() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        SkillAnimationConfig before = manager.find(122, "Thunderbolt");
        JsonObject invalid = skill();
        invalid.getAsJsonArray("events").get(0).getAsJsonObject().addProperty("effect", "missing");
        write("skills/thunderbolt.json", invalid);
        assertFalse(manager.reload());
        assertSame(before, manager.find(122, "Thunderbolt"));
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        assertNotSame(before, manager.find(122, "Thunderbolt"));
        assertEquals("electric_impact", before.events.get(2).effect.getId());
    }

    @Test
    public void duplicateMoveOrEffectIdentifiersRejectEntireReload() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        write("skills/subfolder/duplicate.json", skill());
        assertFalse(manager.reload());
        assertEquals(1, manager.getSkillCount());
        Files.delete(root.resolve("skills/subfolder/duplicate.json"));
        JsonObject duplicate = NativeParticleEffectTest.effect("ball");
        duplicate.addProperty("id", "electric_beam");
        write("effects/duplicate.json", duplicate);
        assertFalse(manager.reload());
        assertEquals(3, manager.getEffectCount());
    }

    @Test
    public void bundledFbxExampleResolvesAndCannotDuplicateEnabledThunderbolt() throws Exception {
        assertTrue(Files.isRegularFile(root.resolve("effects/models/thunderbolt_pikachu.fbx")));
        try (java.io.InputStream input = getClass().getResourceAsStream("/examples/skills/thunderbolt_fbx.json");
             java.io.Reader reader = new java.io.InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject fbxSkill = new JsonParser().parse(reader).getAsJsonObject();
            fbxSkill.addProperty("enabled", true);
            write("skills/thunderbolt_fbx.json", fbxSkill);
        }
        assertTrue(manager.reload());
        assertEquals(1, manager.getSkillCount());
        SkillAnimationConfig configured = manager.find(122, "Thunderbolt");
        assertEquals(30, configured.durationTicks);
        assertEquals(1, configured.events.size());
        FbxEffect effect = (FbxEffect) configured.events.get(0).effect;
        assertFalse(effect.atUser);
        assertTrue(effect.followEntity);
        assertEquals("Thunderbolt", effect.animation);
        assertTrue(Files.isRegularFile(manager.getEffectsDirectory().resolve(effect.model)));
        write("skills/thunderbolt.json", skill());
        assertFalse(manager.reload());
        assertSame(configured, manager.find(122, "Thunderbolt"));
    }

    @Test
    public void deletedConfigurationsDisappearAndExamplesAreNotRecreated() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        Files.delete(root.resolve("skills/thunderbolt.json"));
        manager.initialize(root);
        assertFalse(Files.exists(root.resolve("skills/thunderbolt.json")));
        assertTrue(manager.reload());
        assertNull(manager.find(122, "Thunderbolt"));
    }

    @Test
    public void malformedFileAndOutOfRangeTimelineKeepPreviousSnapshot() throws Exception {
        write("skills/thunderbolt.json", skill());
        assertTrue(manager.reload());
        Files.write(root.resolve("skills/thunderbolt.json"), "{broken".getBytes(StandardCharsets.UTF_8));
        assertFalse(manager.reload());
        JsonObject invalid = skill();
        invalid.getAsJsonArray("events").get(0).getAsJsonObject().addProperty("tick", -1);
        write("skills/thunderbolt.json", invalid);
        assertFalse(manager.reload());
        assertEquals(1, manager.getSkillCount());
    }

    @Test
    public void emptyEnabledTimelineCannotSuppressNativeAnimation() {
        JsonObject json = new JsonParser().parse("{\"moveId\":122,\"events\":[]}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class,
                () -> SkillAnimationConfig.parse(json, Collections.emptyMap()));
    }
}
