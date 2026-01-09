package gg.auroramc.collections.hooks.mythic;

import gg.auroramc.aurora.api.util.NamespacedId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.hooks.Hook;
import gg.auroramc.collections.hooks.mythic.listener.MythicMobsListener;
import gg.auroramc.collections.hooks.mythic.reward.MythicStatCorrector;
import gg.auroramc.collections.hooks.mythic.reward.MythicStatReward;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicHook implements Hook, Listener {
    private MythicRegistrar registrar;

    @Override
    public void hook(AuroraCollections plugin) {
        this.registrar = new MythicRegistrar(plugin);

        plugin.getCollectionManager().getRewardFactory()
                .registerRewardType(NamespacedId.fromDefault("mythic_stat"), MythicStatReward.class);

        plugin.getCollectionManager().getRewardAutoCorrector()
                        .registerCorrector(NamespacedId.fromDefault("mythic_stat"), new MythicStatCorrector(plugin));

        Bukkit.getPluginManager().registerEvents(new MythicMobsListener(plugin), plugin);

        AuroraCollections.logger().info("Hooked into MythicMobs for entity_kill and entity_loot collections with namespace 'mythicmobs'");
        AuroraCollections.logger()
                .info("Hooked into MythicMobs for custom mechanics (addToCollection, progressCollection), conditions (hasCollectionLevel) and \"mythic_stat\" reward.");
    }

    @EventHandler
    public void onMechanicLoad(MythicMechanicLoadEvent event) {
        registrar.registerApplicableMechanic(event);
    }

    @EventHandler
    public void onConditionLoad(MythicConditionLoadEvent event) {
        registrar.registerApplicableCondition(event);
    }
}
