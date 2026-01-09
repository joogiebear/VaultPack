package gg.auroramc.crafting.listener;

import gg.auroramc.crafting.api.event.BlueprintCraftEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BlueprintListener implements Listener {
    @EventHandler
    public void onCraft(BlueprintCraftEvent event) {
        event.getBlueprint().executeCraftActions(event.getPlayer(), event.getItem(), event.getAmount());
    }
}
