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
public class RecipeBookMenuConfig extends AuroraConfig {
    private String title = "Recipe Book";
    private Integer rows = 6;
    private List<String> appendLore = new ArrayList<>();
    private ItemConfig filler;
    private Map<String, ItemConfig> customItems;

    public RecipeBookMenuConfig(AuroraCrafting plugin) {
        super(getFile(plugin));
    }

    public static File getFile(AuroraCrafting plugin) {
        return new File(plugin.getDataFolder() + "/menus", "recipe_book.yml");
    }

    public static void saveDefault(AuroraCrafting plugin) {
        if (!getFile(plugin).exists()) {
            plugin.saveResource("menus/recipe_book.yml", false);
        }
    }
}
