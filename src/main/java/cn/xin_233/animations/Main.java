package cn.xin_233.animations;

import cn.xin_233.animations.command.ReloadAnimationsCommand;
import cn.xin_233.animations.animation.FbxEffectSender;
import cn.xin_233.animations.config.AnimationConfigManager;
import cn.xin_233.animations.config.FbxEffect;
import cn.xin_233.animations.network.FbxEffectPacket;
import cn.xin_233.animations.proxy.CommonProxy;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION,
        dependencies = "required-after:pixelmon;required-after:mixinbooter@[11.18,)",
        acceptableRemoteVersions = "*")
public class Main
{
    public static final String MODID = "pixelmonanimations";
    public static final String NAME = "PixelmonAnimations";
    public static final String VERSION = "1.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);
    private static final AnimationConfigManager CONFIGS = new AnimationConfigManager(LOGGER);
    public static final SimpleNetworkWrapper NETWORK =
            NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    @SidedProxy(clientSide = "cn.xin_233.animations.proxy.ClientProxy",
            serverSide = "cn.xin_233.animations.proxy.CommonProxy")
    public static CommonProxy PROXY;

    public static AnimationConfigManager getConfigs()
    {
        return CONFIGS;
    }

    public static void sendFbxEffect(com.pixelmonmod.pixelmon.api.attackAnimations.AttackAnimation animation,
                                     FbxEffect effect)
    {
        FbxEffectSender.send(animation, effect);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        CONFIGS.initialize(event.getModConfigurationDirectory().toPath().toAbsolutePath()
                .getParent().resolve(MODID));
        CONFIGS.reload();
        NETWORK.registerMessage(FbxEffectPacket.Handler.class, FbxEffectPacket.class, 0,
                net.minecraftforge.fml.relauncher.Side.CLIENT);
        PROXY.preInit();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        CONFIGS.reload();
        event.registerServerCommand(new ReloadAnimationsCommand(CONFIGS));
    }
}
