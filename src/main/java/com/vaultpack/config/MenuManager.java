package com.vaultpack.config;

import com.vaultpack.VaultPackPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages loading and caching of menu configurations
 */
public class MenuManager {

    private final VaultPackPlugin plugin;
    private final Logger logger;
    private final Map<String, MenuConfig> menus;
    private final File menusFolder;

    public MenuManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.menus = new HashMap<>();
        this.menusFolder = new File(plugin.getDataFolder(), "menus");
    }

    /**
     * Load all menus from the menus/ folder
     */
    public void loadMenus() {
        logger.info("Loading menu configurations...");

        // Create menus folder if it doesn't exist
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }

        // Copy default menu files from JAR if they don't exist
        copyDefaultMenus();

        // Load all menu files
        File[] menuFiles = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (menuFiles == null || menuFiles.length == 0) {
            logger.warning("No menu files found in menus/ folder!");
            return;
        }

        menus.clear();
        for (File file : menuFiles) {
            String menuId = file.getName().replace(".yml", "");
            try {
                MenuConfig menu = new MenuConfig(menuId, file);
                menus.put(menuId, menu);
                logger.info("Loaded menu: " + menuId);
            } catch (Exception e) {
                logger.severe("Failed to load menu: " + menuId);
                e.printStackTrace();
            }
        }

        logger.info("Loaded " + menus.size() + " menu configurations!");
    }

    /**
     * Copy default menu files from JAR
     */
    private void copyDefaultMenus() {
        String[] defaultMenus = {
            "storage.yml",
            "backpack_selector.yml",
            "enderchest.yml",
            "enderchest_page.yml"
        };

        for (String menuFile : defaultMenus) {
            File file = new File(menusFolder, menuFile);
            if (!file.exists()) {
                try (InputStream in = plugin.getResource("menus/" + menuFile)) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        logger.info("Created default menu file: " + menuFile);
                    }
                } catch (IOException e) {
                    logger.warning("Could not create default menu file: " + menuFile);
                }
            }
        }
    }

    /**
     * Get a menu by ID
     */
    public MenuConfig getMenu(String id) {
        return menus.get(id);
    }

    /**
     * Check if a menu exists
     */
    public boolean hasMenu(String id) {
        return menus.containsKey(id);
    }

    /**
     * Reload all menus
     */
    public void reloadMenus() {
        logger.info("Reloading menu configurations...");
        loadMenus();
    }

    /**
     * Get all loaded menus
     */
    public Map<String, MenuConfig> getMenus() {
        return new HashMap<>(menus);
    }
}
