package gg.auroramc.collections.hooks.oraxen;

import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.hooks.Hook;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class OraxenHook implements Hook, Listener {
    private AuroraCollections plugin;

    @Override
    public void hook(AuroraCollections plugin) {
        this.plugin = plugin;

        if(Bukkit.getPluginManager().getPlugin("Oraxen").getDescription().getVersion().startsWith("2")) {
            new Oraxen2Hook().hook(plugin);
        }
    }
}
