package gg.auroramc.crafting.hooks.mythicmobs;

import gg.auroramc.crafting.AuroraCrafting;
import gg.auroramc.crafting.hooks.Hook;

public class MythicHook implements Hook {

    @Override
    public void hook(AuroraCrafting plugin) {
        // MythicMobs is loading enchants 40 ticks after the server starts
        plugin.getItemLoader().addToWaitFor("MythicMobs", 40);
    }
}
