package cn.xin_233.animations.client;

import cn.xin_233.animations.Main;
import cn.xin_233.animations.config.FbxEffect;
import java.util.HashMap;
import java.util.Map;

final class FbxModelCache {
    private final Map<String, FbxModel> models = new HashMap<>();

    FbxModel get(FbxEffect effect) {
        FbxModel model = models.get(effect.getId());
        if (model == null) {
            model = FbxModel.load(Main.getConfigs().getEffectsDirectory(), effect);
            models.put(effect.getId(), model);
        }
        return model;
    }

    void clear() {
        models.clear();
    }
}
