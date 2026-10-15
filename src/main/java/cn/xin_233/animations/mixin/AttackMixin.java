package cn.xin_233.animations.mixin;

import cn.xin_233.animations.Main;
import com.pixelmonmod.pixelmon.api.attackAnimations.AttackAnimation;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Attack.class, remap = false)
public abstract class AttackMixin {
    @Redirect(
            method = {
                    "playAnimation(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V",
                    "use(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/log/MoveResults;Lcom/pixelmonmod/pixelmon/battles/attacks/ZMove;)Z"
            },
            at = @At(value = "FIELD", target = "Lcom/pixelmonmod/pixelmon/battles/attacks/AttackBase;animations:Ljava/util/ArrayList;", opcode = 180),
            require = 2,
            remap = false
    )
    private ArrayList<AttackAnimation> pixelmonanimations$resolveAnimations(AttackBase move) {
        return Main.getConfigs().resolve(move, move.animations);
    }
}
