package cn.xin_233.animations.client;

import cn.xin_233.animations.Main;
import cn.xin_233.animations.config.ConfiguredEffect;
import cn.xin_233.animations.config.FbxEffect;
import cn.xin_233.animations.network.FbxEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class FbxEffectClient {
    // These are independent effect instances. They are never attached to or
    // used as replacements for Pixelmon's Pokemon renderers.
    private static final Logger LOGGER = Main.LOGGER;
    private static final List<FbxEffectInstance> ACTIVE = new ArrayList<>();
    private static final FbxModelCache MODELS = new FbxModelCache();

    private FbxEffectClient() {
    }

    public static void receive(final FbxEffectPacket packet) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().world == null
                    || Minecraft.getMinecraft().world.provider.getDimension() != packet.dimension) {
                return;
            }
            ConfiguredEffect configured = Main.getConfigs().findEffect(packet.effectId);
            if (!(configured instanceof FbxEffect)) {
                LOGGER.warn("Received unknown or non-FBX effect {}", packet.effectId);
                return;
            }
            FbxEffect effect = (FbxEffect) configured;
            try {
                ACTIVE.add(new FbxEffectInstance(MODELS.get(effect), packet));
            } catch (RuntimeException exception) {
                LOGGER.error("Cannot start FBX effect {}", packet.effectId, exception);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END || Minecraft.getMinecraft().world == null) {
            return;
        }
        int dimension = Minecraft.getMinecraft().world.provider.getDimension();
        Iterator<FbxEffectInstance> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            FbxEffectInstance instance = iterator.next();
            if (instance.dimension() != dimension || !instance.tick()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (Minecraft.getMinecraft().world == null) {
            return;
        }
        int dimension = Minecraft.getMinecraft().world.provider.getDimension();
        double viewerX = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double viewerY = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double viewerZ = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
        for (FbxEffectInstance instance : new ArrayList<>(ACTIVE)) {
            if (instance.dimension() == dimension) {
                instance.render(event.getPartialTicks(), viewerX, viewerY, viewerZ);
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
        MODELS.clear();
    }
}
