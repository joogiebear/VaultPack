package gg.auroramc.crafting.api.blueprint;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.crafting.api.ItemPair;
import gg.auroramc.crafting.api.workbench.Workbench;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class BrewingBlueprint extends Blueprint {
    private TypeId input;
    private ItemStack inputItem;

    public BrewingBlueprint(Workbench workbench, String id) {
        super(workbench, id);
    }

    public static BrewingBlueprint brewingBlueprint(Workbench workbench, String id) {
        return new BrewingBlueprint(workbench, id);
    }

    public BrewingBlueprint input(TypeId input) {
        this.input = input;
        this.inputItem = AuroraAPI.getItemManager().resolveItem(input);
        return this;
    }

    public BrewingBlueprint ingredient(TypeId ingredient) {
        this.addIngredient(new ItemPair(ingredient, 1));
        return this;
    }

    public ItemStack getIngredientItem() {
        return this.ingredientItems.getFirst();
    }

    public TypeId getIngredient() {
        return this.ingredients.getFirst().getItemPair().id();
    }

    @Override
    public Blueprint addIngredient(ItemPair itemPair) {
        if (!this.ingredients.isEmpty()) {
            throw new IllegalStateException("BrewingBlueprint can only have one ingredient.");
        }
        return super.addIngredient(itemPair);
    }

    @Override
    public int getTimesCraftable(BlueprintContext context) {
        return 0;
    }

    @Override
    public ItemStack[] calcRemainingIngredientMatrix(BlueprintContext context, int timesCrafted) {
        return new ItemStack[0];
    }

    @Override
    public Blueprint complete() {
        this.mergeOptionsEnabled = false;
        return this;
    }
}
