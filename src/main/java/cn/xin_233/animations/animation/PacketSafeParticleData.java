package cn.xin_233.animations.animation;

import com.pixelmonmod.pixelmon.battles.attacks.animations.StandardParticleAnimationData;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackBall;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackRise;
import com.pixelmonmod.pixelmon.battles.attacks.animations.particles.AttackTube;
import io.netty.buffer.ByteBuf;

/** Preserves configured values where Pixelmon's writer and reader disagree about defaults. */
public final class PacketSafeParticleData {
    private PacketSafeParticleData() {
    }

    private static void writeStandard(ByteBuf buffer, StandardParticleAnimationData data) {
        AttackBall.BallData standard = new AttackBall.BallData();
        standard.power = data.power;
        standard.speed = data.speed;
        standard.rgb = data.rgb;
        standard.texture = data.texture;
        standard.lifetimeTicks = data.lifetimeTicks;
        standard.scale = data.scale;
        standard.writeToByteBuffer(buffer);
    }

    public static final class Tube extends AttackTube.TubeData {
        @Override
        public void writeToByteBuffer(ByteBuf buffer) {
            writeStandard(buffer, this);
            buffer.writeBoolean(true).writeByte(segments);
            buffer.writeBoolean(true).writeFloat(height);
            buffer.writeBoolean(true).writeFloat(radius);
            buffer.writeBoolean(conical);
            if (conical) {
                buffer.writeBoolean(true).writeFloat(narrowness);
                buffer.writeBoolean(true).writeFloat(apexRadius);
            }
        }
    }

    public static final class Rise extends AttackRise.RiseData {
        @Override
        public void writeToByteBuffer(ByteBuf buffer) {
            writeStandard(buffer, this);
            buffer.writeByte(durationTicks).writeByte(startHeight).writeFloat(endHeight);
            buffer.writeBoolean(true).writeByte(particleAmount);
            buffer.writeBoolean(true).writeFloat(acceleration);
            buffer.writeBoolean(true).writeFloat(radius);
            buffer.writeBoolean(sparkle);
        }
    }
}
