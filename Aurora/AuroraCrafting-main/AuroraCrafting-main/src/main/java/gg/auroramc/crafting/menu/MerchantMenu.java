package gg.auroramc.crafting.menu;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.crafting.AuroraCrafting;
import gg.auroramc.crafting.config.MerchantsConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;

public class MerchantMenu {
    private final Player player;
    private final MerchantsConfig.MerchantConfig config;

    public static MerchantMenu merchantMenu(Player player, MerchantsConfig.MerchantConfig config) {
        return new MerchantMenu(player, config);
    }

    public MerchantMenu(Player player, MerchantsConfig.MerchantConfig config) {
        this.player = player;
        this.config = config;
    }

    public void open() {
        var merchant = Bukkit.createMerchant(Text.component(player, config.getMenu().getTitle(), Placeholder.of("{name}", config.getName())));

        var trades = new ArrayList<MerchantRecipe>(config.getOffers().size());

        for (var offerConfig : config.getOffers()) {
            if (offerConfig.getPermission() != null) {
                if (!player.hasPermission(offerConfig.getPermission())) {
                    continue;
                }
            }
            var recipe = new MerchantRecipe(getItemStack(offerConfig.getResult()), Integer.MAX_VALUE);
            recipe.setIgnoreDiscounts(true);

            for (var ingredient : offerConfig.getIngredients()) {
                var item = getItemStack(ingredient);
                if (item == null) continue;
                recipe.addIngredient(item);
            }

            trades.add(recipe);
        }

        merchant.setRecipes(trades);

        if (Bukkit.isOwnedByCurrentRegion(player)) {
            player.openMerchant(merchant, true);
        } else {
            player.getScheduler().run(AuroraCrafting.getInstance(),
                    (t) -> player.openMerchant(merchant, false), null);
        }
    }

    private ItemStack getItemStack(String id) {
        if (id.equals("air")) {
            return null;
        }
        var split = id.split("/");
        var item = AuroraAPI.getItemManager().resolveItem(TypeId.fromDefault(split[0]));
        var amount = Math.min(Integer.parseInt(split[1]), item.getMaxStackSize());
        item.setAmount(amount);
        return item;
    }
}
