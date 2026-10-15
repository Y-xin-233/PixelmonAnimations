package cn.xin_233.animations.mixin;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import static org.junit.Assert.*;

public class AttackMixinTargetTest {
    @Test
    public void installedPixelmonHasOneAnimationListReadInEachSelectedMethod() throws Exception {
        String wrapper = "Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;";
        Map<String, Integer> matches = new HashMap<>();
        matches.put("playAnimation(" + wrapper + wrapper + ")V", 0);
        matches.put("use(" + wrapper + wrapper
                + "Lcom/pixelmonmod/pixelmon/battles/controller/log/MoveResults;"
                + "Lcom/pixelmonmod/pixelmon/battles/attacks/ZMove;)Z", 0);
        try (InputStream input = getClass().getResourceAsStream("/com/pixelmonmod/pixelmon/battles/attacks/Attack.class")) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            for (MethodNode method : node.methods) {
                String key = method.name + method.desc;
                if (!matches.containsKey(key)) {
                    continue;
                }
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() == Opcodes.GETFIELD && instruction instanceof FieldInsnNode) {
                        FieldInsnNode field = (FieldInsnNode) instruction;
                        if (field.owner.equals("com/pixelmonmod/pixelmon/battles/attacks/AttackBase")
                                && field.name.equals("animations") && field.desc.equals("Ljava/util/ArrayList;")) {
                            matches.put(key, matches.get(key) + 1);
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, Integer> entry : matches.entrySet()) {
            assertEquals(entry.getKey(), 1, entry.getValue().intValue());
        }
    }
}
