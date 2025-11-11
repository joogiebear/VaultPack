package com.vaultpack.models;

public enum BackpackTier {
    SMALL(9, "Small", "&a"),            // Uncommon (Green)
    MEDIUM(18, "Medium", "&9"),         // Rare (Blue)
    LARGE(27, "Large", "&5"),           // Epic (Purple)
    GREATER(36, "Greater", "&5"),       // Epic (Purple)
    JUMBO(45, "Jumbo", "&6");           // Legendary (Gold)

    private final int size;
    private final String displayName;
    private final String colorCode;

    BackpackTier(int size, String displayName, String colorCode) {
        this.size = size;
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public int getSize() {
        return size;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColoredDisplayName() {
        return colorCode + displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public BackpackTier getNext() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return null; // Already max tier
        }
        return values()[nextOrdinal];
    }

    public boolean isMaxTier() {
        return this == JUMBO;
    }

    public static BackpackTier fromString(String tier) {
        try {
            return valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SMALL; // Default
        }
    }

    public static BackpackTier fromSize(int size) {
        for (BackpackTier tier : values()) {
            if (tier.getSize() == size) {
                return tier;
            }
        }
        return SMALL; // Default
    }
}
