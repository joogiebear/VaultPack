package com.vaultpack.api.expansion;

import com.vaultpack.VaultPackPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an expansion for VaultPack.
 *
 * <p>Expansions can add new features, data holders, and functionality
 * to VaultPack without modifying the core plugin.</p>
 *
 * @since 3.0.0
 */
public interface VaultPackExpansion {

    /**
     * Get the unique ID of this expansion.
     *
     * @return The expansion ID (lowercase, no spaces)
     */
    @NotNull String getId();

    /**
     * Get the display name of this expansion.
     *
     * @return The display name
     */
    @NotNull String getName();

    /**
     * Get the version of this expansion.
     *
     * @return The version string
     */
    @NotNull String getVersion();

    /**
     * Get the author(s) of this expansion.
     *
     * @return The author name(s)
     */
    @NotNull String getAuthor();

    /**
     * Called when the expansion is enabled.
     *
     * @param plugin The VaultPack plugin instance
     */
    void onEnable(@NotNull VaultPackPlugin plugin);

    /**
     * Called when the expansion is disabled.
     */
    void onDisable();

    /**
     * Check if this expansion is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Set whether this expansion is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);
}
