package cn.xin_233.animations.animation;

import cn.xin_233.animations.Main;
import cn.xin_233.animations.config.ConfiguredEffect;
import cn.xin_233.animations.config.FbxEffect;
import cn.xin_233.animations.config.NativeParticleEffect;
import cn.xin_233.animations.config.SkillAnimationConfig;
import com.pixelmonmod.pixelmon.api.attackAnimations.AttackAnimation;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

public final class ConfiguredAttackAnimation extends AttackAnimation {
    private final SkillAnimationConfig config;
    private int nextEvent;

    public ConfiguredAttackAnimation(SkillAnimationConfig config) {
        this.config = config;
    }

    @Override
    public AttackAnimation instantiate(PixelmonWrapper user, PixelmonWrapper target, Attack attack) {
        ConfiguredAttackAnimation animation = new ConfiguredAttackAnimation(config);
        animation.initialize(user, target, attack);
        animation.tickAnimation(++animation.ticks);
        return animation;
    }

    @Override
    public boolean tickAnimation(int tick) {
        while (nextEvent < config.events.size() && config.events.get(nextEvent).tick <= tick) {
            SkillAnimationConfig.Event event = config.events.get(nextEvent++);
            try {
                ConfiguredEffect effect = event.effect;
                if (effect instanceof NativeParticleEffect) {
                    sendBattleEffect(((NativeParticleEffect) effect).createData(),
                            event.groundedStart, event.groundedEnd);
                } else if (effect instanceof FbxEffect) {
                    Main.sendFbxEffect(this, (FbxEffect) effect);
                } else {
                    throw new IllegalArgumentException("Unsupported configured effect: " + effect.getId());
                }
            } catch (RuntimeException exception) {
                Main.LOGGER.error("Cannot play effect {} at tick {}", event.effect.getId(), tick, exception);
            }
        }
        return nextEvent == config.events.size() && tick >= config.durationTicks;
    }

    @Override
    public boolean usedOncePerTurn() {
        return config.oncePerTurn;
    }
}
