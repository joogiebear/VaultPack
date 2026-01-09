package gg.auroramc.collections.listener;

import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Trigger;
import io.papermc.paper.event.block.PlayerShearBlockEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;


public class ShearListener implements Listener {
    private final AuroraCollections plugin;

    public ShearListener(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearBlockEvent event) {
        var player = event.getPlayer();
        var drops = event.getDrops();

        var manager = plugin.getCollectionManager();
        for (var drop : drops) {
            manager.progressCollections(player, TypeId.from(drop.getType()), drop.getAmount(), Trigger.BLOCK_SHEAR_LOOT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        var player = event.getPlayer();
        var drops = event.getDrops();

        var manager = plugin.getCollectionManager();
        for (var drop : drops) {
            manager.progressCollections(player, TypeId.from(drop.getType()), drop.getAmount(), Trigger.SHEAR_LOOT);
        }
    }
}
