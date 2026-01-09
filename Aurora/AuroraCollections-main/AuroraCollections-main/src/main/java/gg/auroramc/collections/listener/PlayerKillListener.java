package gg.auroramc.collections.listener;

import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Trigger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;


public class PlayerKillListener implements Listener {
    private final AuroraCollections plugin;

    public PlayerKillListener(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        var killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (e.getEntity().equals(e.getEntity().getKiller())) return;

        var manager = plugin.getCollectionManager();
        manager.progressCollections(killer, null, 1, Trigger.PLAYER_KILL);
    }
}

