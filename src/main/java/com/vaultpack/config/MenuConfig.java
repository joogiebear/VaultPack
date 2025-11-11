package com.vaultpack.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Represents a menu configuration loaded from menus/ folder
 * Supports EcoMenus-style slot-based layouts
 */
public class MenuConfig {

    private final String id;
    private final FileConfiguration config;

    // Basic properties
    private String title;
    private int rows;

    // Mask pattern
    private Map<Character, String> maskItems;
    private List<String> maskPattern;

    // Custom slots (buttons, decorations, etc.)
    private Map<Integer, SlotConfig> customSlots;

    // Sounds
    private Map<String, String> sounds;

    public MenuConfig(String id, File file) {
        this.id = id;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.customSlots = new HashMap<>();
        this.maskItems = new HashMap<>();
        this.maskPattern = new ArrayList<>();
        this.sounds = new HashMap<>();

        load();
    }

    private void load() {
        // Load basic properties
        this.title = config.getString("title", "&8Menu");
        this.rows = config.getInt("rows", 6);

        // Load mask pattern
        loadMask();

        // Load custom slots
        loadSlots();

        // Load sounds
        loadSounds();
    }

    private void loadMask() {
        ConfigurationSection maskSection = config.getConfigurationSection("mask");
        if (maskSection == null) return;

        // Load mask items (1 = material, 2 = material, etc.)
        List<String> items = maskSection.getStringList("items");
        for (int i = 0; i < items.size(); i++) {
            char key = String.valueOf(i + 1).charAt(0);
            maskItems.put(key, items.get(i));
        }

        // Load pattern
        maskPattern = maskSection.getStringList("pattern");
    }

    private void loadSlots() {
        ConfigurationSection slotsSection = config.getConfigurationSection("slots");
        if (slotsSection == null) return;

        for (String key : slotsSection.getKeys(false)) {
            ConfigurationSection slotSection = slotsSection.getConfigurationSection(key);
            if (slotSection == null) continue;

            int slot = slotSection.getInt("slot", -1);
            if (slot == -1) continue;

            SlotConfig slotConfig = new SlotConfig();
            slotConfig.key = key;
            slotConfig.slot = slot;
            slotConfig.material = slotSection.getString("item", "STONE");
            slotConfig.name = slotSection.getString("name", "");
            slotConfig.lore = slotSection.getStringList("lore");
            slotConfig.glow = slotSection.getBoolean("glow", false);
            slotConfig.head = slotSection.getString("head", null);
            slotConfig.clickAction = slotSection.getString("click-action", "none");
            slotConfig.leftClickAction = slotSection.getString("left-click-action", null);
            slotConfig.rightClickAction = slotSection.getString("right-click-action", null);
            slotConfig.menu = slotSection.getString("menu", null);

            customSlots.put(slot, slotConfig);
        }
    }

    private void loadSounds() {
        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection == null) return;

        for (String key : soundsSection.getKeys(false)) {
            sounds.put(key, soundsSection.getString(key));
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public Map<Integer, SlotConfig> getCustomSlots() {
        return customSlots;
    }

    public SlotConfig getSlot(int slot) {
        return customSlots.get(slot);
    }

    public String getSound(String key) {
        return sounds.getOrDefault(key, null);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Get the mask material for a specific slot
     */
    public String getMaskMaterial(int slot) {
        if (maskPattern.isEmpty()) return null;

        int row = slot / 9;
        int col = slot % 9;

        if (row >= maskPattern.size()) return null;

        String patternRow = maskPattern.get(row);
        if (col >= patternRow.length()) return null;

        char patternChar = patternRow.charAt(col);
        if (patternChar == '0') return null; // 0 = empty

        return maskItems.get(patternChar);
    }

    /**
     * Check if a slot is masked (has a mask item)
     */
    public boolean isMasked(int slot) {
        return getMaskMaterial(slot) != null;
    }

    /**
     * Represents a custom slot configuration
     */
    public static class SlotConfig {
        public String key;
        public int slot;
        public String material;
        public String name;
        public List<String> lore;
        public boolean glow;
        public String head;
        public String clickAction;
        public String leftClickAction;
        public String rightClickAction;
        public String menu;

        public SlotConfig() {
            this.lore = new ArrayList<>();
        }
    }
}
