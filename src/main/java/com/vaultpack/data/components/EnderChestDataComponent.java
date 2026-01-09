package com.vaultpack.data.components;

import com.vaultpack.models.EnderPage;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Data component for managing player ender chest pages.
 * Handles ender chest storage, page unlocking, and serialization.
 */
public class EnderChestDataComponent extends BaseDataComponent {

    @Getter
    private int unlockedPages;

    @Getter
    private final Map<Integer, EnderPage> pages;

    public EnderChestDataComponent() {
        super("ender-chests");
        this.unlockedPages = 1; // Default 1 unlocked page
        this.pages = new HashMap<>();
    }

    /**
     * Check if a page is unlocked.
     *
     * @param page The page number
     * @return true if unlocked
     */
    public boolean isPageUnlocked(int page) {
        return page <= unlockedPages;
    }

    /**
     * Unlock a page.
     *
     * @param page The page number to unlock
     */
    public void unlockPage(int page) {
        if (page > unlockedPages) {
            unlockedPages = page;
            markDirty();
        }
    }

    /**
     * Set unlocked pages count.
     *
     * @param count The number of unlocked pages
     */
    public void setUnlockedPages(int count) {
        if (this.unlockedPages != count) {
            this.unlockedPages = count;
            markDirty();
        }
    }

    /**
     * Get an ender page.
     * Creates a new page if it doesn't exist.
     *
     * @param page The page number
     * @return The ender page
     */
    public EnderPage getPage(int page) {
        return pages.computeIfAbsent(page, p -> {
            markDirty();
            return new EnderPage(p);
        });
    }

    /**
     * Check if a page has been created.
     *
     * @param page The page number
     * @return true if page exists
     */
    public boolean hasPage(int page) {
        return pages.containsKey(page);
    }

    /**
     * Set an ender page.
     *
     * @param page      The page number
     * @param enderPage The ender page to set
     */
    public void setPage(int page, EnderPage enderPage) {
        pages.put(page, enderPage);
        markDirty();
    }

    /**
     * Remove an ender page.
     *
     * @param page The page number
     */
    public void removePage(int page) {
        if (pages.remove(page) != null) {
            markDirty();
        }
    }

    /**
     * Get total storage slots across all ender pages.
     *
     * @return Total slot count
     */
    public int getTotalStorageSlots() {
        return unlockedPages * 45; // Each page has 45 slots
    }

    /**
     * Get total used slots across all ender pages.
     *
     * @return Total used slots
     */
    public int getTotalUsedSlots() {
        return pages.values().stream()
            .mapToInt(EnderPage::getUsedSlots)
            .sum();
    }

    @Override
    public void load(ConfigurationSection section) {
        unlockedPages = section.getInt("unlocked-pages", 1);

        ConfigurationSection pagesSection = section.getConfigurationSection("pages");
        if (pagesSection != null) {
            for (String pageKey : pagesSection.getKeys(false)) {
                try {
                    int pageNum = Integer.parseInt(pageKey);
                    ConfigurationSection pageSection = pagesSection.getConfigurationSection(pageKey);

                    if (pageSection != null) {
                        EnderPage enderPage = EnderPage.deserialize(pageSection);
                        pages.put(pageNum, enderPage);
                    }
                } catch (NumberFormatException e) {
                    // Invalid page number, skip
                }
            }
        }
    }

    @Override
    public void save(ConfigurationSection section) {
        section.set("unlocked-pages", unlockedPages);

        // Clear existing pages section
        section.set("pages", null);

        if (!pages.isEmpty()) {
            ConfigurationSection pagesSection = section.createSection("pages");

            for (Map.Entry<Integer, EnderPage> entry : pages.entrySet()) {
                ConfigurationSection pageSection = pagesSection.createSection(String.valueOf(entry.getKey()));
                entry.getValue().serialize(pageSection);
            }
        }
    }

    @Override
    public void reset() {
        unlockedPages = 1;
        pages.clear();
        markDirty();
    }

    @Override
    public boolean validate() {
        // Validate unlocked pages is positive
        if (unlockedPages < 0) {
            return false;
        }

        // Validate page range (1-9)
        if (unlockedPages > 9) {
            return false;
        }

        // Validate all pages
        for (EnderPage page : pages.values()) {
            if (page == null) {
                return false;
            }
        }

        return true;
    }
}
