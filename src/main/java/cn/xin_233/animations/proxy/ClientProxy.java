package cn.xin_233.animations.proxy;

import cn.xin_233.animations.client.FbxEffectClient;
import cn.xin_233.animations.network.FbxEffectPacket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(FbxEffectClient.class);
    }

    @Override
    public void handleFbxEffect(FbxEffectPacket packet) {
        FbxEffectClient.receive(packet);
    }
}
