package gg.auroramc.collections.hooks;

import gg.auroramc.collections.AuroraCollections;

public interface Hook {
    void hook(AuroraCollections plugin);

    default void hookAtStartUp(AuroraCollections plugin) {
    }
}
