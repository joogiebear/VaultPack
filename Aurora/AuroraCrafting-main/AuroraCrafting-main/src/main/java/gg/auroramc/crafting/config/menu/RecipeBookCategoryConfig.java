package gg.auroramc.crafting.config.menu;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.crafting.AuroraCrafting;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class RecipeBookCategoryConfig extends AuroraConfig {
    private CategoryIcon categoryIcon;
    private Integer rows = 6;
    private List<String> appendLore = new ArrayList<>();
    private List<Integer> displayArea = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    );
    private SecretDisplay secretRecipeDisplay;
    private ItemConfig filler;
    private Map<String, ItemConfig> items;
    private Map<String, ItemConfig> customItems;

    @Getter
    public static final class CategoryIcon {
        private Boolean enabled = true;
        private ItemConfig item;
    }

    @Getter
    public static final class SecretDisplay {
        private Boolean enabled = true;
        private ItemConfig item;
    }

    public RecipeBookCategoryConfig(AuroraCrafting plugin) {
        super(getFile(plugin));
    }

    public static File getFile(AuroraCrafting plugin) {
        return new File(plugin.getDataFolder() + "/menus", "recipe_book_category.yml");
    }

    public static void saveDefault(AuroraCrafting plugin) {
        if (!getFile(plugin).exists()) {
            plugin.saveResource("menus/recipe_book_category.yml", false);
        }
    }
}
