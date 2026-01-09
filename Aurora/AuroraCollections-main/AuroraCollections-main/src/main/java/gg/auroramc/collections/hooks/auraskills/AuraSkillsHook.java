package gg.auroramc.collections.hooks.auraskills;

import gg.auroramc.aurora.api.util.NamespacedId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.hooks.Hook;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class AuraSkillsHook implements Hook {

    private AuraSkillsCorrector corrector;

    @Override
    public void hook(AuroraCollections plugin) {
        this.corrector = new AuraSkillsCorrector(plugin);

        plugin.getCollectionManager().getRewardFactory()
                .registerRewardType(NamespacedId.fromDefault("auraskills_stat"), AuraSkillsStatReward.class);
        plugin.getCollectionManager().getRewardFactory()
                .registerRewardType(NamespacedId.fromDefault("auraskills_xp"), AuraSkillsXpReward.class);

        plugin.getCollectionManager().getRewardAutoCorrector()
                .registerCorrector(NamespacedId.fromDefault("auraskills_stat"), corrector);

        Bukkit.getPluginManager().registerEvents(new AuraSkillsListener(plugin, this), plugin);

        AuroraCollections.logger().info("Hooked into AuraSkills for handling extra loot drops");
        AuroraCollections.logger().info("Hooked into AuraSkills for stat rewards with reward type: 'auraskills_stat' and 'auraskills_xp'. Auto reward corrector for stats is registered.");
    }
}
