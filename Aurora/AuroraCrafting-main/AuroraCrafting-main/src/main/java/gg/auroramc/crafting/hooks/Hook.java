package gg.auroramc.crafting.hooks;


import gg.auroramc.crafting.AuroraCrafting;

public interface Hook {
    void hook(AuroraCrafting plugin);

    default void hookAtStartUp(AuroraCrafting plugin) {
    }
}
