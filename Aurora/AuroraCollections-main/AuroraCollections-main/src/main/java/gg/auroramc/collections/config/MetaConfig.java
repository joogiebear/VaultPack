package gg.auroramc.collections.config;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.collections.AuroraCollections;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class MetaConfig extends AuroraConfig {
    private boolean customFishingCollectionsSaved = false;

    public MetaConfig(AuroraCollections plugin) {
        super(getFile(plugin));
    }

    public static File getFile(AuroraCollections plugin) {
        return new File(plugin.getDataFolder(), "meta.yml");
    }

    public static void saveDefault(AuroraCollections plugin) {
        if (!getFile(plugin).exists()) {
            plugin.saveResource("meta.yml", false);
        }
    }
}
