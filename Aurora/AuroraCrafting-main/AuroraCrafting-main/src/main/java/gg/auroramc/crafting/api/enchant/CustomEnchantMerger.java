package gg.auroramc.crafting.api.enchant;

import gg.auroramc.crafting.AuroraCrafting;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class CustomEnchantMerger {
    private static final Map<String, BiFunction<ItemStack, ItemStack, ItemStack>> mergers = new ConcurrentHashMap<>();

    public static boolean hasAnyMerger() {
        return !mergers.isEmpty();
    }

    public static ItemStack merge(ItemStack from, ItemStack to) {
        ItemStack result = null;

        for (var entry : mergers.entrySet()) {
            try {
                result = entry.getValue().apply(from, to);
            } catch (Throwable e) {
                AuroraCrafting.logger().severe("Failed to merge enchantments for " + entry.getKey() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return result;
    }

    public static void registerMerger(String id, BiFunction<ItemStack, ItemStack, ItemStack> merger) {
        mergers.put(id, merger);
    }
}
