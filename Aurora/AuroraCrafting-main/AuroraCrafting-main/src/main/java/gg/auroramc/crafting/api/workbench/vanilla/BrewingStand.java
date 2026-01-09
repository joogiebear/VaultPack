package gg.auroramc.crafting.api.workbench.vanilla;

import gg.auroramc.crafting.api.blueprint.Blueprint;
import gg.auroramc.crafting.api.blueprint.BlueprintAdapter;
import gg.auroramc.crafting.api.blueprint.BrewingBlueprint;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BrewingStand extends VanillaWorkbench<BrewingBlueprint> {
    public BrewingStand() {
        super("vanilla-brewing-stand", -1, List.of(), VanillaType.BREWING_STAND);
    }

    @Override
    protected @Nullable NamespacedKey registerVanillaRecipe(Blueprint blueprint) {
        if (blueprint instanceof BrewingBlueprint brewingBlueprint) {
            var potionMix = BlueprintAdapter.adapt(brewingBlueprint);
            Bukkit.getPotionBrewer().addPotionMix(potionMix);
            return potionMix.getKey();
        }
        return null;
    }

    @Override
    protected void removeVanillaRecipe(NamespacedKey key) {
        Bukkit.getPotionBrewer().removePotionMix(key);
    }
}
