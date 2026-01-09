package gg.auroramc.collections.config;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.collections.AuroraCollections;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class MessageConfig extends AuroraConfig {

    private String reloaded = "&aReloaded configuration!";
    private String dataNotLoadedYet = "&cData for this player hasn't loaded yet, try again later!";
    private String dataNotLoadedYetSelf = "&cYour data isn't loaded yet, please try again later!";
    private String playerOnlyCommand = "&cThis command can only be executed by a player!";
    private String noPermission = "&cYou don't have permission to execute this command!";
    private String invalidSyntax = "&cInvalid command syntax!";
    private String mustBeNumber = "&cArgument must be a number!";
    private String playerNotFound = "&cPlayer not found!";
    private String commandError = "&cAn error occurred while executing this command!";
    private String menuOpened = "&aOpened collection menu for {player}";
    private String categoryNotFound = "&cCategory {category} not found!";
    private String collectionNotFound = "&cCollection {collection} not found!";
    private String addSuccess = "&aAdded &2{number} &ato &2{collection} &afor &2{player}!";
    private String setSuccess = "&aSet &2{collection} &ato &2{number} &afor &2{player}!";
    private String removeSuccess = "&aRemoved &2{number} &afrom &2{collection} &afor &2{player}!";
    private String resetSuccess = "&aReset &2{collection} &afor &2{player}!";

    public MessageConfig(AuroraCollections plugin, String language) {
        super(getFile(plugin, language));
    }

    private static File getFile(AuroraCollections plugin, String language) {
        return new File(plugin.getDataFolder(), "messages_" + language + ".yml");
    }

    public static void saveDefault(AuroraCollections plugin, String language) {
        if (!getFile(plugin, language).exists()) {
            try {
                plugin.saveResource("messages_" + language + ".yml", false);
            } catch (Exception e) {
                AuroraCollections.logger().warning("Internal message file for language: " + language + " not found! Creating a new one from english...");

                var file = getFile(plugin, language);


                try (InputStream in = plugin.getResource("messages_en.yml")) {
                    Files.copy(in, file.toPath());
                } catch (IOException ex) {
                    AuroraCollections.logger().severe("Failed to create message file for language: " + language);
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return List.of(
                (yaml) -> {
                    yaml.set("config-version", null);
                    yaml.set("category-not-found", "&cCategory {category} not found!");
                    yaml.set("collection-not-found", "&cCollection {collection} not found!");
                    yaml.set("add-success", "&aAdded &2{number} &ato &2{collection} &afor &2{player}!");
                    yaml.set("set-success", "&aSet &2{collection} &ato &2{number} &afor &2{player}!");
                    yaml.set("remove-success", "&aRemoved &2{number} &afrom &2{collection} &afor &2{player}!");
                    yaml.set("reset-success", "&aReset &2{collection} &afor &2{player}!");
                    yaml.set("config-version", 1);
                }
        );
    }
}
