package com.vaultpack.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.EnderChestGUI;
import org.bukkit.entity.Player;

/**
 * ACF-based ender chest command.
 * Opens ender chest page selector or specific page.
 */
@CommandAlias("enderchest|ec")
@Description("Manage your ender chest pages")
public class EnderChestCommand extends BaseCommand {

    private final VaultPackPlugin plugin;
    private final EnderChestGUI enderChestGUI;

    public EnderChestCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.enderChestGUI = new EnderChestGUI(plugin);
    }

    /**
     * Default command - opens ender chest page selector.
     * Usage: /enderchest
     */
    @Default
    @CommandPermission("vaultpack.use")
    @Description("Open your ender chest page selector")
    public void onDefault(Player player) {
        enderChestGUI.open(player);
    }

    /**
     * Open a specific ender chest page.
     * Usage: /enderchest <page>
     */
    @Subcommand("open")
    @CommandAlias("enderchest")
    @CommandPermission("vaultpack.use")
    @Description("Open a specific ender chest page")
    @CommandCompletion("@enderPages")
    @Syntax("<page> - Page number (1-9)")
    public void onOpenPage(Player player, int pageNumber) {
        // Validate page number range
        if (pageNumber < 1 || pageNumber > 9) {
            plugin.getMessageManager().send(player, "enderchest-invalid-page");
            return;
        }

        // Check if page is unlocked
        if (!plugin.getDataManager().getPlayerData(player.getUniqueId()).isEnderPageUnlocked(pageNumber)) {
            plugin.getMessageManager().send(player, "enderchest-page-locked",
                "%page%", String.valueOf(pageNumber));
            plugin.getMessageManager().send(player, "enderchest-view-pages");
            return;
        }

        // Open the ender page directly
        plugin.getEnderChestManager().openEnderPage(player, pageNumber);
    }

    /**
     * Show ender chest command help.
     * Usage: /enderchest help
     */
    @Subcommand("help")
    @Description("Show ender chest command help")
    @HelpCommand
    public void onHelp(Player player) {
        player.sendMessage("§8§m                                               ");
        player.sendMessage("§6§lVaultPack Ender Chests");
        player.sendMessage("");
        player.sendMessage("§e/enderchest §7- Open ender chest page selector");
        player.sendMessage("§e/enderchest <page> §7- Open specific ender page");
        player.sendMessage("§e/enderchest help §7- Show this help message");
        player.sendMessage("");
        player.sendMessage("§7Unlock more pages to expand your ender storage!");
        player.sendMessage("§8§m                                               ");
    }
}
