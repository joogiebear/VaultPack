package gg.auroramc.collections.hooks.customfishing.listener;

import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Trigger;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.mechanic.loot.LootType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class CustomFishingListener implements Listener {
    private final AuroraCollections plugin;

    public CustomFishingListener(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFishLootSpawn(FishingLootSpawnEvent e) {
        if (e.getLoot().type() != LootType.ITEM) return;
        var isVanilla = e.getLoot().id().equals("vanilla");

        if (e.getEntity() instanceof Item item) {
            var itemId = isVanilla ? TypeId.from(item.getItemStack().getType()) : new TypeId("customfishing", e.getLoot().id());
            int quantity = item.getItemStack().getAmount();

            var manager = plugin.getCollectionManager();
            manager.progressCollections(e.getPlayer(), itemId, quantity, Trigger.FISH);
        } else {
            var itemId = new TypeId("customfishing", e.getLoot().id());
            var manager = plugin.getCollectionManager();
            manager.progressCollections(e.getPlayer(), itemId, 1, Trigger.FISH);
        }
    }
}
