package cn.xin_233.animations.animation;

import cn.xin_233.animations.Main;
import cn.xin_233.animations.config.FbxEffect;
import cn.xin_233.animations.network.FbxEffectPacket;
import com.pixelmonmod.pixelmon.api.attackAnimations.AttackAnimation;
import com.pixelmonmod.pixelmon.config.PixelmonConfig;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public final class FbxEffectSender {
    private FbxEffectSender() {
    }

    public static void send(AttackAnimation animation, FbxEffect effect) {
        FbxEffectPacket packet = new FbxEffectPacket(
                animation.dimension,
                effect.getId(),
                animation.startID,
                animation.endID,
                position(animation.userPos, animation.user),
                positionY(animation.userPos, animation.user),
                positionZ(animation.userPos, animation.user),
                position(animation.targetPos, animation.target),
                positionY(animation.targetPos, animation.target),
                positionZ(animation.targetPos, animation.target),
                effect.atUser,
                effect.followEntity,
                effect.loop,
                effect.durationTicks,
                effect.scale,
                effect.speed,
                effect.alpha,
                effect.additive,
                effect.offsetX,
                effect.offsetY,
                effect.offsetZ,
                effect.rotationX,
                effect.rotationY,
                effect.rotationZ);
        if (PixelmonConfig.onlyShowAttackEffectsToBattlers) {
            animation.user.bc.spectators.forEach(spectator ->
                    Main.NETWORK.sendTo(packet, spectator.getEntity()));
            animation.user.bc.getPlayers().forEach(participant -> {
                if (participant.player != null) {
                    Main.NETWORK.sendTo(packet, participant.player);
                }
            });
        } else {
            // Use Pixelmon's visibility setting and radius, centered on the
            // attacking Pokemon. The effect chooses its anchor on the client.
            double x = packet.userX;
            double y = packet.userY;
            double z = packet.userZ;
            Main.NETWORK.sendToAllAround(packet,
                    new TargetPoint(packet.dimension, x, y, z, PixelmonConfig.rangeToDisplayAttackAnimations));
        }
    }

    private static float position(float[] fallback, com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper wrapper) {
        return wrapper != null && wrapper.entity != null ? (float) wrapper.entity.posX : fallback[0];
    }

    private static float positionY(float[] fallback, com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper wrapper) {
        return wrapper != null && wrapper.entity != null ? (float) wrapper.entity.posY : fallback[1];
    }

    private static float positionZ(float[] fallback, com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper wrapper) {
        return wrapper != null && wrapper.entity != null ? (float) wrapper.entity.posZ : fallback[2];
    }
}
