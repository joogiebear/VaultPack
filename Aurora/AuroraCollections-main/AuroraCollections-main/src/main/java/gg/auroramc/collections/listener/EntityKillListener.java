package gg.auroramc.collections.listener;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Trigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;


public class EntityKillListener implements Listener {
    private final AuroraCollections plugin;

    public EntityKillListener(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        var killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (e.getEntity() instanceof Player) return;
        var drops = e.getDrops();

        var manager = plugin.getCollectionManager();

        var mobId = AuroraAPI.getEntityManager().resolveId(e.getEntity());
        if (mobId.namespace().equals("mythicmobs")) return;

        manager.progressCollections(killer, mobId, 1, Trigger.ENTITY_KILL);

        for (var drop : drops) {
            manager.progressCollections(killer, plugin.getItemManager().resolveId(drop), drop.getAmount(), Trigger.ENTITY_LOOT);
        }
    }
}
