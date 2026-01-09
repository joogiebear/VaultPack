package com.vaultpack.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Phase 3: Utility class for creating items and buttons
 * Extracted from BackpackManager to reduce class size
 */
public class ItemBuilderUtil {

    /**
     * Create a button with material, name, and lore
     */
    public static ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a button with material, name, and lore list
     */
    public static ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a player head button with custom texture
     */
    public static ItemStack createButtonWithTexture(String texture, String name, String... lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        if (skullMeta != null) {
            // Apply custom texture
            applyTexture(skullMeta, texture);

            // Set name and lore
            skullMeta.setDisplayName(name);

            if (lore.length > 0) {
                skullMeta.setLore(Arrays.asList(lore));
            }

            skull.setItemMeta(skullMeta);
        }

        return skull;
    }

    /**
     * Create a player head button with custom texture and lore list
     */
    public static ItemStack createButtonWithTexture(String texture, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        if (skullMeta != null) {
            // Apply custom texture
            applyTexture(skullMeta, texture);

            // Set name and lore
            skullMeta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                skullMeta.setLore(lore);
            }

            skull.setItemMeta(skullMeta);
        }

        return skull;
    }

    /**
     * Apply a base64 texture to a skull meta
     */
    public static void applyTexture(SkullMeta skullMeta, String texture) {
        if (texture == null || texture.isEmpty()) {
            return;
        }

        try {
            // Create a player profile with the texture
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
            profile.getProperties().add(new ProfileProperty("textures", texture));
            skullMeta.setPlayerProfile(profile);
        } catch (Exception e) {
            // Texture application failed, but don't crash - just use default head
            Bukkit.getLogger().warning("Failed to apply texture: " + e.getMessage());
        }
    }

    /**
     * Replace placeholders in a string
     */
    public static String replacePlaceholders(String text, String... replacements) {
        if (text == null) {
            return "";
        }

        String result = text;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }

        return result;
    }

    /**
     * Replace placeholders in a list of strings
     */
    public static List<String> replacePlaceholders(List<String> lines, String... replacements) {
        if (lines == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(replacePlaceholders(line, replacements));
        }

        return result;
    }

    /**
     * Create a filler/decoration item (commonly used in GUIs)
     */
    public static ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Builder pattern for creating items
     */
    public static class Builder {
        private Material material = Material.STONE;
        private String name = "";
        private List<String> lore = new ArrayList<>();
        private int amount = 1;
        private String texture = null;

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lore(String... lore) {
            this.lore = Arrays.asList(lore);
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder texture(String texture) {
            this.texture = texture;
            this.material = Material.PLAYER_HEAD;
            return this;
        }

        public ItemStack build() {
            ItemStack item;

            if (texture != null) {
                item = createButtonWithTexture(texture, name, lore);
            } else {
                item = createButton(material, name, lore);
            }

            item.setAmount(amount);
            return item;
        }
    }
}
