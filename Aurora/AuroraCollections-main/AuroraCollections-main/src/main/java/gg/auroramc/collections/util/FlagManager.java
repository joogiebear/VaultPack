package gg.auroramc.collections.util;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import gg.auroramc.collections.AuroraCollections;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FlagManager {
    public static final StateFlag XP_GAIN_FLAG = new StateFlag("aurora-collections-xp-gain", true);

    public static void registerFlags() {
        var registry = WorldGuard.getInstance().getFlagRegistry();
        registerXpGainFlag(registry);
    }

    private static void registerXpGainFlag(FlagRegistry registry) {
        try {
            registry.register(XP_GAIN_FLAG);
            AuroraCollections.logger().info("Registered WorldGuard flag aurora-collections-xp-gain");
        } catch (FlagConflictException e) {
            AuroraCollections.logger().warning("Could not register WorldGuard flag aurora-collections-xp-gain");
        }
    }

    public static boolean isBlocked(Player player) {
        return FlagUtil.isBlocked(player, XP_GAIN_FLAG);
    }

    public static boolean isBlocked(Player player, Location location) {
        return FlagUtil.isBlocked(player, location, XP_GAIN_FLAG);
    }
}
