package gg.auroramc.collections.hooks.auroralevels;

import gg.auroramc.aurora.api.util.NamespacedId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.hooks.Hook;

public class AuroraLevelsHook implements Hook {
    @Override
    public void hook(AuroraCollections plugin) {
        plugin.getCollectionManager().getRewardFactory()
                .registerRewardType(NamespacedId.fromDefault("levels_xp"), AuroraLevelsReward.class);

        AuroraCollections.logger().info("Hooked into AuroraLevels for xp rewards with reward type: 'levels_xp'");
    }
}
