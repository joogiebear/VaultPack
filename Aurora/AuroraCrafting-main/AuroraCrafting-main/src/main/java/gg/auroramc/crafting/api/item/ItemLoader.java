package gg.auroramc.crafting.api.item;

import gg.auroramc.crafting.AuroraCrafting;
import gg.auroramc.crafting.api.event.ItemsLoadedEvent;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class ItemLoader {
    private final Map<String, Boolean> toWaitFor = new HashMap<>();
    private boolean loaded = false;

    public ItemLoader() {
        addToWaitFor("AuroraCrafting", 5);
    }

    public void addToWaitFor(String pluginName, int maxWaitTicks) {
        toWaitFor.put(pluginName, false);
        Bukkit.getGlobalRegionScheduler().runDelayed(AuroraCrafting.getInstance(), (t) -> {
            if (loaded) return;
            setLoaded(pluginName);
        }, maxWaitTicks);
    }

    public void setLoaded(String pluginName) {
        if (toWaitFor.get(pluginName)) return;
        toWaitFor.put(pluginName, true);
        if (isAllLoaded()) {
            loaded = true;
            Bukkit.getPluginManager().callEvent(new ItemsLoadedEvent());
        }
    }

    public boolean isAllLoaded() {
        for (var entry : toWaitFor.entrySet()) {
            if (!entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}
