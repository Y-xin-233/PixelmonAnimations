package cn.xin_233.animations.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class FbxEffectTest {
    private static JsonObject effect() {
        return new JsonParser().parse("{"
                + "\"format\":1,\"id\":\"thunder_fx\",\"renderer\":\"fbx\","
                + "\"model\":\"models/thunder_fx.fbx\",\"animation\":\"Take 001\","
                + "\"anchor\":\"user\",\"followEntity\":false,\"durationTicks\":30,"
                + "\"scale\":0.08,\"offset\":[0,1,0],\"rotation\":[0,90,0]"
                + "}").getAsJsonObject();
    }

    @Test
    public void parsesIndependentEffectAnchorAndAssetPath() {
        FbxEffect parsed = FbxEffect.parse(effect());
        assertEquals("thunder_fx", parsed.getId());
        assertEquals("models/thunder_fx.fbx", parsed.model);
        assertEquals("Take 001", parsed.animation);
        assertTrue(parsed.atUser);
        assertFalse(parsed.followEntity);
        assertEquals(30, parsed.durationTicks);
        assertEquals(0.08F, parsed.scale, 0);
        assertEquals(1.0F, parsed.offsetY, 0);
        assertEquals(90.0F, parsed.rotationY, 0);
    }

    @Test
    public void rejectsTraversalAndAbsolutePaths() {
        for (String path : new String[]{"../effect.fbx", "/effect.fbx", "C:/effect.fbx",
                "models/../effect.fbx", "models\\effect.fbx"}) {
            JsonObject json = effect();
            json.addProperty("model", path);
            assertThrows(path, IllegalArgumentException.class, () -> FbxEffect.parse(json));
        }
    }

    @Test
    public void effectDurationExtendsSkillTimeline() {
        FbxEffect fbx = FbxEffect.parse(effect());
        JsonObject skill = new JsonParser().parse("{\"moveId\":122,\"durationTicks\":1,"
                + "\"events\":[{\"tick\":5,\"effect\":\"thunder_fx\"}]}").getAsJsonObject();
        SkillAnimationConfig parsed = SkillAnimationConfig.parse(skill,
                Collections.singletonMap("thunder_fx", fbx));
        assertEquals(35, parsed.durationTicks);
    }
}
