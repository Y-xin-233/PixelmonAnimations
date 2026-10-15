package cn.xin_233.animations.bootstrap;

import java.util.Collections;
import java.util.List;
import zone.rong.mixinbooter.ILateMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

@MixinLoader
@SuppressWarnings("deprecation")
public final class LateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.pixelmonanimations.json");
    }
}
