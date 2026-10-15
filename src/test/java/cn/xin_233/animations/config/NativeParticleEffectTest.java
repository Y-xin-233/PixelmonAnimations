package cn.xin_233.animations.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pixelmonmod.pixelmon.battles.attacks.animations.AttackAnimationData;
import com.pixelmonmod.pixelmon.battles.attacks.animations.StandardParticleAnimationData;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackBeam;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackRise;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackTube;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.EnumEffectType;
import com.pixelmonmod.pixelmon.enums.EnumType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class NativeParticleEffectTest {
    static JsonObject effect(String type) {
        JsonObject json = new JsonObject();
        json.addProperty("format", 1);
        json.addProperty("id", "test");
        json.addProperty("renderer", "native_particle");
        json.addProperty("type", type);
        return json;
    }

    private static AttackAnimationData roundTrip(AttackAnimationData data) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            data.writeToByteBuffer(buffer);
            AttackAnimationData decoded = data.getEffectEnum().dataSupplier.get().readFromByteBuffer(buffer);
            assertEquals("Native decoder must consume the entire payload", 0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }

    @Test
    public void allSupportedTypesUseNativeCompatiblePayloads() {
        for (String type : Arrays.asList("ball", "beam", "buff", "tube", "explosion", "radial", "rain", "rise", "stat_change")) {
            JsonObject json = effect(type);
            json.addProperty("power", 80);
            json.addProperty("speed", 0.7F);
            json.addProperty("scale", 0.4F);
            json.addProperty("texture", "normal");
            json.add("rgb", new JsonParser().parse("[0,128,255]"));
            StandardParticleAnimationData data = (StandardParticleAnimationData) NativeParticleEffect.parse(json).createData();
            StandardParticleAnimationData decoded = (StandardParticleAnimationData) roundTrip(data);
            assertEquals(type.toUpperCase(), decoded.getEffectEnum().name());
            assertEquals(80, decoded.power);
            assertEquals(0.7F, decoded.speed, 0);
            assertEquals(0.4F, decoded.scale, 0);
            assertArrayEquals(new int[]{0, 128, 255}, decoded.rgb);
            assertEquals(data.texture, decoded.texture);
        }
    }

    @Test
    public void byteBoundariesSurviveNativeDecoding() {
        JsonObject json = effect("beam");
        json.addProperty("segments", 255);
        json.addProperty("lifetimeTicks", 255);
        json.addProperty("power", 32767);
        AttackBeam.BeamData data = (AttackBeam.BeamData) roundTrip(NativeParticleEffect.parse(json).createData());
        assertEquals(255, data.segments);
        assertEquals(255, data.lifetimeTicks);
        assertEquals(32767, data.power);
    }

    @Test
    public void tubeRadiusOneAndZeroApexSurviveNativeDecoding() {
        JsonObject json = effect("tube");
        json.addProperty("radius", 1);
        json.addProperty("apexRadius", 0);
        json.addProperty("segments", 127);
        AttackTube.TubeData decoded = (AttackTube.TubeData) roundTrip(NativeParticleEffect.parse(json).createData());
        assertEquals(1, decoded.radius, 0);
        assertEquals(0, decoded.apexRadius, 0);
        assertEquals(127, decoded.segments);
    }

    @Test
    public void riseAmountThirteenIsPreservedAfterDecodingAndInitialization() {
        JsonObject json = effect("rise");
        json.addProperty("particleAmount", 13);
        json.addProperty("durationTicks", 127);
        AttackRise.RiseData data = (AttackRise.RiseData) roundTrip(NativeParticleEffect.parse(json).createData());
        data.initFromAttack(null, 90, EnumType.Electric);
        assertEquals(13, data.particleAmount);
        assertEquals(127, data.durationTicks);
    }

    @Test
    public void eachPlaybackReceivesIndependentDataAndColorArrays() {
        JsonObject json = effect("beam");
        json.add("rgb", new JsonParser().parse("[1,2,3]"));
        NativeParticleEffect config = NativeParticleEffect.parse(json);
        StandardParticleAnimationData first = (StandardParticleAnimationData) config.createData();
        first.power = 500;
        first.rgb[0] = 200;
        StandardParticleAnimationData second = (StandardParticleAnimationData) config.createData();
        assertNotSame(first, second);
        assertEquals(-1, second.power);
        assertEquals(1, second.rgb[0]);
    }

    @Test
    public void omittedValuesRetainPixelmonDefaults() {
        AttackBeam.BeamData data = (AttackBeam.BeamData) NativeParticleEffect.parse(effect("beam")).createData();
        assertEquals(-1, data.speed, 0);
        assertEquals(-1, data.variation, 0);
        assertNull(data.rgb);
        data.initFromAttack(null, 90, EnumType.Electric);
        assertEquals(90, data.power);
        assertEquals(0.25F, data.variation, 0);
        assertNotNull(data.rgb);
        assertEquals(EnumEffectType.BEAM, data.getEffectEnum());
    }

    @Test
    public void rejectsOverflowAndInvalidCommonFields() {
        for (String fragment : Arrays.asList("\"power\":32768", "\"power\":0", "\"speed\":0",
                "\"scale\":-1", "\"lifetimeTicks\":256", "\"rgb\":[0,256,0]", "\"rgb\":[1,2]",
                "\"texture\":\"bad_texture\"", "\"speed\":1e100", "\"segments\":1.5",
                "\"segments\":256", "\"segments\":\"15\"", "\"inverted\":\"false\"")) {
            JsonObject json = effect("beam");
            JsonObject fields = new JsonParser().parse("{" + fragment + "}").getAsJsonObject();
            fields.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
            assertThrows(fragment, IllegalArgumentException.class, () -> NativeParticleEffect.parse(json));
        }
    }

    @Test
    public void rejectsSignedByteOverflowAndUnsupportedTypes() {
        JsonObject tube = effect("tube");
        tube.addProperty("segments", 128);
        assertThrows(IllegalArgumentException.class, () -> NativeParticleEffect.parse(tube));
        JsonObject rise = effect("rise");
        rise.addProperty("durationTicks", 128);
        assertThrows(IllegalArgumentException.class, () -> NativeParticleEffect.parse(rise));
        assertThrows(IllegalArgumentException.class, () -> NativeParticleEffect.parse(effect("lightning")));
        JsonObject fbx = effect("beam");
        fbx.addProperty("renderer", "fbx");
        assertThrows(IllegalArgumentException.class, () -> NativeParticleEffect.parse(fbx));
        JsonObject misspelled = effect("beam");
        misspelled.addProperty("segements", 15);
        assertThrows(IllegalArgumentException.class, () -> NativeParticleEffect.parse(misspelled));
    }
}
