package gg.auroramc.crafting.config;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.crafting.AuroraCrafting;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class MerchantsConfig extends AuroraConfig {
    private Map<String, MerchantConfig> merchants;

    @Getter
    public static final class MerchantConfig {
        private String name = "";
        private String permission;
        private List<MerchantOfferConfig> offers = List.of();
        private MenuConfig menu;
    }

    @Getter
    public static final class MerchantOfferConfig {
        private List<String> ingredients = List.of();
        private String result = "";
        private String permission;
    }

    @Getter
    public static final class MenuConfig {
        private String title = "";
        private ItemConfig item;
        private List<String> lockedLore = new ArrayList<>();
    }


    public MerchantsConfig(AuroraCrafting plugin) {
        super(getFile(plugin));
    }

    public static File getFile(AuroraCrafting plugin) {
        return new File(plugin.getDataFolder(), "merchants.yml");
    }

    public static void saveDefault(AuroraCrafting plugin) {
        if (!getFile(plugin).exists()) {
            plugin.saveResource("merchants.yml", false);
        }
    }
}
