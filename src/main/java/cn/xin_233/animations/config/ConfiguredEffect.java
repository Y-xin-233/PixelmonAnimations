package cn.xin_233.animations.config;

public interface ConfiguredEffect {
    String getId();

    default int getDurationTicks() {
        return 0;
    }
}
