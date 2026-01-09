package gg.auroramc.crafting.config;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.aurora.api.config.decorators.IgnoreField;
import gg.auroramc.crafting.AuroraCrafting;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private String merchantOpened = "&aOpened merchant: &2{id} &afor player: &2{player}&a!";
    private String merchantFailedToOpen = "&cFailed to open merchant: &4{id} &cfor player: &4{player}&c!";
    private String workbenchNotFound = "&cWorkbench not found!";
    private String forceOpened = "&aWorkbench opened!";

    public MessageConfig(AuroraCrafting plugin, String language) {
        super(getFile(plugin, language));
    }

    private static File getFile(AuroraCrafting plugin, String language) {
        return new File(plugin.getDataFolder(), "messages_" + language + ".yml");
    }

    public static void saveDefault(AuroraCrafting plugin, String language) {
        if (!getFile(plugin, language).exists()) {
            try {
                plugin.saveResource("messages_" + language + ".yml", false);
            } catch (Exception e) {
                AuroraCrafting.logger().warning("Internal message file for language: " + language + " not found! Creating a new one from english...");

                var file = getFile(plugin, language);


                try (InputStream in = plugin.getResource("messages_en.yml")) {
                    Files.copy(in, file.toPath());
                } catch (IOException ex) {
                    AuroraCrafting.logger().severe("Failed to create message file for language: " + language);
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        var resource = AuroraCrafting.getInstance().getResource("messages_" + AuroraCrafting.getInstance().getConfigManager().getConfig().getLanguage() + ".yml");

        if (resource == null) {
            resource = AuroraCrafting.getInstance().getResource("messages_en.yml");
        }

        InputStream finalResource = resource;

        return List.of(
                (yaml) -> {
                    try (var in = finalResource) {
                        var original = YamlConfiguration.loadConfiguration(new InputStreamReader(in));

                        for (var key : original.getKeys(false)) {
                            if (yaml.contains(key)) continue;
                            yaml.set(key, original.get(key));
                        }
                    } catch (Exception e) {
                        AuroraCrafting.logger().severe("Failed to run migrations on the message file.");
                        e.printStackTrace();
                    }
                }
        );
    }
}
