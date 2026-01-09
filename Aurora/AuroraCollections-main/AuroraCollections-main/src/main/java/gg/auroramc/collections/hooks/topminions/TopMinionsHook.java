package gg.auroramc.collections.hooks.topminions;

import com.sarry20.topminion.event.minion.MinionTakeInventoryItemsEvent;
import com.sarry20.topminion.event.chest.ChestTakeItemsEvent;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Trigger;
import gg.auroramc.collections.hooks.Hook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TopMinionsHook implements Hook, Listener {
    private AuroraCollections plugin;

    @Override
    public void hook(AuroraCollections plugin) {
        this.plugin = plugin;
        AuroraCollections.logger().info("Hooked into TopMinions with trigger: minion_loot");
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTakeItems(MinionTakeInventoryItemsEvent event) {
        for (var entry : event.getItems().entrySet()) {
            var item = entry.getKey();
            var amount = entry.getValue();
            plugin.getCollectionManager().progressCollections(event.getPlayer(), TypeId.fromDefault(item), amount, Trigger.MINION_LOOT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTakeItems(ChestTakeItemsEvent event) {
        for (var drop : event.getChest().getInventory().getContents()) {
            if (drop == null) continue;
            plugin.getCollectionManager()
                    .progressCollections(event.getPlayer(), AuroraAPI.getItemManager().resolveId(drop), drop.getAmount(), Trigger.MINION_LOOT);
        }
    }
}
