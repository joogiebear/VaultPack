package com.vaultpack.utils;

/**
 * Constants used throughout the VaultPack plugin
 * Replaces magic numbers with named constants for better maintainability
 */
public final class Constants {

    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    // ========== BACKPACK LIMITS ==========

    /**
     * Minimum backpack slot number
     */
    public static final int MIN_BACKPACK_SLOT = 1;

    /**
     * Maximum backpack slot number (18 total slots)
     */
    public static final int MAX_BACKPACK_SLOT = 18;

    /**
     * Minimum ender chest page number
     */
    public static final int MIN_ENDER_PAGE = 1;

    /**
     * Maximum ender chest page number (9 total pages)
     */
    public static final int MAX_ENDER_PAGE = 9;

    /**
     * Number of slots per ender chest page
     */
    public static final int ENDER_PAGE_SIZE = 45;

    // ========== NAVIGATION HEADER SLOTS ==========

    /**
     * Navigation header - Close button slot
     */
    public static final int NAV_SLOT_CLOSE = 0;

    /**
     * Navigation header - Back to menu button slot
     */
    public static final int NAV_SLOT_BACK = 1;

    /**
     * Navigation header - Filler slots (start)
     */
    public static final int NAV_SLOT_FILLER_START = 2;

    /**
     * Navigation header - Filler slots (end)
     */
    public static final int NAV_SLOT_FILLER_END = 4;

    /**
     * Navigation header - First backpack button slot
     */
    public static final int NAV_SLOT_FIRST = 5;

    /**
     * Navigation header - Previous backpack button slot
     */
    public static final int NAV_SLOT_PREVIOUS = 6;

    /**
     * Navigation header - Next backpack button slot
     */
    public static final int NAV_SLOT_NEXT = 7;

    /**
     * Navigation header - Last backpack button slot
     */
    public static final int NAV_SLOT_LAST = 8;

    /**
     * Navigation header - Total slots (0-8 = 9 slots)
     */
    public static final int NAV_HEADER_SIZE = 9;

    // ========== BACKPACK TIER SIZES ==========

    /**
     * Small backpack size (1 row)
     */
    public static final int TIER_SMALL_SIZE = 9;

    /**
     * Medium backpack size (2 rows)
     */
    public static final int TIER_MEDIUM_SIZE = 18;

    /**
     * Large backpack size (3 rows)
     */
    public static final int TIER_LARGE_SIZE = 27;

    /**
     * Greater backpack size (4 rows)
     */
    public static final int TIER_GREATER_SIZE = 36;

    /**
     * Jumbo backpack size (5 rows)
     */
    public static final int TIER_JUMBO_SIZE = 45;

    // ========== DATA MANAGEMENT ==========

    /**
     * Number of backups to keep
     */
    public static final int BACKUP_RETENTION_COUNT = 10;

    /**
     * Auto-save interval in ticks (300 seconds = 6000 ticks)
     */
    public static final long AUTO_SAVE_INTERVAL_TICKS = 6000L;

    /**
     * Backup interval in ticks (30 minutes = 36000 ticks)
     */
    public static final long BACKUP_INTERVAL_TICKS = 36000L;

    /**
     * Removal confirmation timeout in milliseconds
     */
    public static final long REMOVAL_CONFIRMATION_TIMEOUT_MS = 10000L;

    // ========== GUI REFRESH DELAYS ==========

    /**
     * Delay in ticks before refreshing GUI (1 tick)
     */
    public static final long GUI_REFRESH_DELAY_TICKS = 1L;

    /**
     * Delay in ticks before showing updated state after unlock (3 ticks)
     */
    public static final long UNLOCK_REFRESH_DELAY_TICKS = 3L;
}
