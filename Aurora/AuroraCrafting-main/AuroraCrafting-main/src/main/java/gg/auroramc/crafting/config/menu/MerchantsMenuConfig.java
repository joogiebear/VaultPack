package gg.auroramc.crafting.config.menu;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.crafting.AuroraCrafting;
import lombok.Getter;

import java.io.File;
import java.util.Map;

@Getter
public class MerchantsMenuConfig extends AuroraConfig {
    private String title = "Merchants";
    private Integer rows = 6;
    private ItemConfig filler;
    private Map<String, ItemConfig> customItems;

    public MerchantsMenuConfig(AuroraCrafting plugin) {
        super(getFile(plugin));
    }

    public static File getFile(AuroraCrafting plugin) {
        return new File(plugin.getDataFolder() + "/menus", "merchants.yml");
    }

    public static void saveDefault(AuroraCrafting plugin) {
        if (!getFile(plugin).exists()) {
            plugin.saveResource("menus/merchants.yml", false);
        }
    }
}
