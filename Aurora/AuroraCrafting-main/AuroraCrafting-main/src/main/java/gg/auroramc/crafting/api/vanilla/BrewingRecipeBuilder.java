package gg.auroramc.crafting.api.vanilla;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import io.papermc.paper.potion.PotionMix;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public class BrewingRecipeBuilder {
    private final NamespacedKey key;
    protected ItemStack result;
    protected TypeId input;
    protected TypeId ingredient;

    public BrewingRecipeBuilder(String id) {
        this.key = new NamespacedKey("aurora", id);
    }

    public static BrewingRecipeBuilder brewingRecipe(String id) {
        return new BrewingRecipeBuilder(id);
    }

    public BrewingRecipeBuilder result(ItemStack result) {
        this.result = result;
        return this;
    }

    public BrewingRecipeBuilder input(ItemStack input) {
        this.input = AuroraAPI.getItemManager().resolveId(input);
        return this;
    }

    public BrewingRecipeBuilder ingredient(ItemStack ingredient) {
        this.ingredient = AuroraAPI.getItemManager().resolveId(ingredient);
        return this;
    }

    public PotionMix build() {
        return new PotionMix(
                key,
                result,
                PotionMix.createPredicateChoice(item -> AuroraAPI.getItemManager().resolveId(item).equals(input)),
                PotionMix.createPredicateChoice(item -> AuroraAPI.getItemManager().resolveId(item).equals(ingredient))
        );
    }
}
