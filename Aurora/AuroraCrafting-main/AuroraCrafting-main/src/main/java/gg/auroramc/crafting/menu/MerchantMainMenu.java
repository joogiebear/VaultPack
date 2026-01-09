package gg.auroramc.crafting.menu;

import gg.auroramc.aurora.api.menu.AuroraMenu;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.crafting.AuroraCrafting;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class MerchantMainMenu {
    private final AuroraCrafting plugin;
    private final Player player;

    public static MerchantMainMenu merchantMainMenu(AuroraCrafting plugin, Player player) {
        return new MerchantMainMenu(plugin, player);
    }

    public void open() {
        var mc = plugin.getConfigManager().getMerchantsMenuConfig();
        var merchants = plugin.getConfigManager().getMerchantsConfig().getMerchants();

        var menu = new AuroraMenu(player, mc.getTitle(), mc.getRows() * 9, false);

        menu.addFiller(ItemBuilder.of(mc.getFiller()).toItemStack(player));

        for (var item : mc.getCustomItems().values()) {
            menu.addItem(ItemBuilder.of(item).build(player));
        }

        for (var entry : merchants.entrySet()) {
            var merchant = entry.getValue();
            if (merchant.getPermission() == null || player.hasPermission(merchant.getPermission())) {
                menu.addItem(ItemBuilder.of(merchant.getMenu().getItem())
                                .placeholder(Placeholder.of("{name}", merchant.getName()))
                                .build(player),
                        (e) -> {
                            player.getScheduler().run(plugin, (t) -> {
                                player.closeInventory();
                                MerchantMenu.merchantMenu(player, entry.getValue()).open();
                            }, null);

                        });
            } else {
                menu.addItem(ItemBuilder.of(merchant.getMenu().getItem()).placeholder(Placeholder.of("{name}", merchant.getName()))
                        .setLore(merchant.getMenu().getLockedLore())
                        .build(player));
            }
        }

        menu.open();
    }
}
