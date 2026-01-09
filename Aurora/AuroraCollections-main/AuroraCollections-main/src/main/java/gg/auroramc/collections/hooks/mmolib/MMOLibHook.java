package gg.auroramc.collections.hooks.mmolib;

import gg.auroramc.aurora.api.util.NamespacedId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.hooks.Hook;

public class MMOLibHook implements Hook {
    @Override
    public void hook(AuroraCollections plugin) {
        plugin.getCollectionManager().getRewardFactory()
                .registerRewardType(NamespacedId.fromDefault("mmo_stat"), MMOStatReward.class);

        plugin.getCollectionManager().getRewardAutoCorrector()
                .registerCorrector(NamespacedId.fromDefault("mmo_stat"), new MMOStatCorrector(plugin));
    }
}
