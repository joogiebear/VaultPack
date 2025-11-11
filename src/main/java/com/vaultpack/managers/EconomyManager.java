package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final VaultPackPlugin plugin;

    public EconomyManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasMoney(Player player, double amount) {
        if (!plugin.isVaultEnabled()) {
            return true; // No economy = free
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return true;
        }

        return economy.has(player, amount);
    }

    public void takeMoney(Player player, double amount) {
        if (!plugin.isVaultEnabled()) {
            return;
        }

        Economy economy = plugin.getEconomy();
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public void giveMoney(Player player, double amount) {
        if (!plugin.isVaultEnabled()) {
            return;
        }

        Economy economy = plugin.getEconomy();
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    public double getBalance(Player player) {
        if (!plugin.isVaultEnabled()) {
            return 0;
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return 0;
        }

        return economy.getBalance(player);
    }

    public String format(double amount) {
        if (!plugin.isVaultEnabled()) {
            return String.valueOf(amount);
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return String.valueOf(amount);
        }

        return economy.format(amount);
    }
}
