package gg.auroramc.collections.config;

import com.google.common.collect.Maps;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.config.menu.CategoriesMenuConfig;
import gg.auroramc.collections.config.menu.CategoryRewardsMenuConfig;
import gg.auroramc.collections.config.menu.CollectionListMenuConfig;
import gg.auroramc.collections.config.menu.CollectionMenuConfig;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;


@Getter
public class ConfigManager {
    private final AuroraCollections plugin;
    private Config config;
    private MetaConfig metaConfig;
    private MessageConfig messageConfig;
    private CategoriesConfig categoriesConfig;
    private CategoriesMenuConfig categoriesMenuConfig;
    private CollectionListMenuConfig collectionListMenuConfig;
    private CollectionMenuConfig collectionMenuConfig;
    private CategoryRewardsMenuConfig categoryRewardsMenuConfig;
    private final Map<String, Map<String, CollectionConfig>> collections = Maps.newConcurrentMap();

    public ConfigManager(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    @SneakyThrows
    public void reload() {
        boolean saveDefaultCollections = !Config.getFile(plugin).exists();

        Config.saveDefault(plugin);
        config = new Config(plugin);
        config.load();

        MetaConfig.saveDefault(plugin);
        metaConfig = new MetaConfig(plugin);
        metaConfig.load();

        MessageConfig.saveDefault(plugin, config.getLanguage());
        messageConfig = new MessageConfig(plugin, config.getLanguage());
        messageConfig.load();

        CategoriesConfig.saveDefault(plugin);
        categoriesConfig = new CategoriesConfig(plugin);
        categoriesConfig.load();

        CategoriesMenuConfig.saveDefault(plugin);
        categoriesMenuConfig = new CategoriesMenuConfig(plugin);
        categoriesMenuConfig.load();

        CollectionListMenuConfig.saveDefault(plugin);
        collectionListMenuConfig = new CollectionListMenuConfig(plugin);
        collectionListMenuConfig.load();

        CollectionMenuConfig.saveDefault(plugin);
        collectionMenuConfig = new CollectionMenuConfig(plugin);
        collectionMenuConfig.load();

        CategoryRewardsMenuConfig.saveDefault(plugin);
        categoryRewardsMenuConfig = new CategoryRewardsMenuConfig(plugin);
        categoryRewardsMenuConfig.load();

        if (saveDefaultCollections) {
            this.saveDefaultCollections();
        }

        reloadCollections();
    }

    private void reloadCollections() {
        collections.clear();
        Path collectionsDir = plugin.getDataFolder().toPath().resolve("collections");

        if (!Files.exists(collectionsDir) || !Files.isDirectory(collectionsDir)) {
            return;
        }

        for (var dir : collectionsDir.toFile().listFiles()) {
            if (!dir.isDirectory()) {
                AuroraCollections.logger().warning("File " + dir.getName() + " is in the collections directory, but should be in a category subdirectory");
            } else if (!categoriesConfig.getCategories().keySet().contains(dir.getName())) {
                AuroraCollections.logger().warning("Category " + dir.getName() + " does not exist in categories.yml");
            }
        }

        var validIdPattern = Pattern.compile("^[a-zA-Z0-9_-]+$");

        for (var dir : categoriesConfig.getCategories().keySet()) {
            Path categoryDir = collectionsDir.resolve(dir);
            if (!Files.exists(categoryDir)) {
                continue;
            }

            if (!validIdPattern.matcher(dir).matches()) {
                AuroraCollections.logger().severe("Category ID: '" + dir + "' doesn't match the required format ^[a-zA-Z0-9_-]+$. " +
                        "Use only english alphabet, numbers, underscores and hyphens. " +
                        "Category will be loaded, but you may experience issues.");
            }

            try (Stream<Path> paths = Files.list(categoryDir)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".yml"))
                        .forEach(path -> {
                            var fileName = path.getFileName().toString().replace(".yml", "");

                            if (fileName.contains(".")) {
                                AuroraCollections.logger().severe("ID: '" + fileName + "' contains a dot/period (.), which is not allowed. Skipping.");
                                return;
                            }

                            if (!validIdPattern.matcher(fileName).matches()) {
                                AuroraCollections.logger().warning("ID: '" + fileName + "' doesn't match the required format ^[a-zA-Z0-9_-]+$. " +
                                        "Use only english alphabet, numbers, underscores and hyphens. " +
                                        "Collection will be loaded, but you may experience issues.");
                            }

                            CollectionConfig collectionConfig = new CollectionConfig(path.toFile());
                            collectionConfig.load();
                            collections.computeIfAbsent(dir, k -> Maps.newConcurrentMap()).computeIfAbsent(fileName, k -> collectionConfig);
                        });
            } catch (IOException e) {
                AuroraCollections.logger().warning("Failed to load collections for category: " + dir + " error: " + e.getMessage());
            }
        }
    }

    @SneakyThrows
    private void saveDefaultCollections() {
        Path jarPath = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        Path dataFolder = plugin.getDataFolder().toPath();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.stream()
                    .filter(entry -> entry.getName().startsWith("collections/") && entry.getName().endsWith(".yml"))

                    .forEach(entry -> {
                        Path outFile = dataFolder.resolve(entry.getName());
                        Path parentDir = outFile.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            try {
                                Files.createDirectories(parentDir);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        try (InputStream is = jar.getInputStream(entry)) {
                            Files.copy(is, outFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
