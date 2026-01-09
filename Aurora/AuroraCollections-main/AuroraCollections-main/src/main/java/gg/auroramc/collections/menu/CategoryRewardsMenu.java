package gg.auroramc.collections.menu;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.menu.AuroraMenu;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.util.NamespacedId;
import gg.auroramc.collections.AuroraCollections;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CategoryRewardsMenu {
    @Getter
    private final static NamespacedId menuId = NamespacedId.fromDefault("collections_category_rewards_menu");

    private final Player player;
    private final AuroraCollections plugin;
    private final String category;

    public CategoryRewardsMenu(Player player, AuroraCollections plugin, String category) {
        this.player = player;
        this.plugin = plugin;
        this.category = category;
    }

    public void open() {
        createMenu().open();
    }

    private AuroraMenu createMenu() {
        var config = plugin.getConfigManager().getCategoryRewardsMenuConfig();
        var cf = plugin.getConfigManager().getCategoriesConfig().getCategories().get(category);

        var menu = new AuroraMenu(player, config.getTitle(), config.getRows() * 9, false, menuId,
                Placeholder.of("{category_name}", cf.getName())
        );

        if (config.getItems().getFiller().getEnabled()) {
            menu.addFiller(ItemBuilder.of(config.getItems().getFiller().getItem()).toItemStack(player));
        } else {
            menu.addFiller(ItemBuilder.filler(Material.AIR));
        }

        for (var customItem : config.getCustomItems().values()) {
            menu.addItem(ItemBuilder.of(customItem).build(player));
        }

        var items = config.getItems();

        menu.addItem(ItemBuilder.back(items.getBack()).build(player), (e) -> {
            new CategoryMenu(player, plugin).open();
        });


        var category = plugin.getCollectionManager().getCategory(this.category);
        var rewards = category.getRewards();

        if (!category.isLevelingEnabled()) return menu;

        var currentPercentage = plugin.getCollectionManager().getCategoryCompletionPercent(this.category, player) * 100;

        boolean previousCompleted = true;
        for (int i = 0; i < config.getDisplayArea().size(); i++) {
            var slot = config.getDisplayArea().get(i);
            if (rewards.size() == i) {
                break;
            }
            var reward = rewards.get(i);

            var completed = currentPercentage >= reward.percentage();
            var inProgress = !completed && previousCompleted;
            previousCompleted = completed;

            var itemConfig = inProgress ? config.getItems().getNextLevel() : completed ? config.getItems().getCompletedLevel() : config.getItems().getLockedLevel();

            itemConfig = itemConfig.merge(reward.items().get(inProgress ? "next-level" : completed ? "completed-level" : "locked-level"));

            List<Placeholder<?>> placeholders = List.of(
                    Placeholder.of("{player}", player.getName()),
                    Placeholder.of("{category_name}", cf.getName()),
                    Placeholder.of("{category_id}", this.category),
                    Placeholder.of("{percent}", AuroraAPI.formatNumber(reward.percentage())),
                    Placeholder.of("{progress_percent}", AuroraAPI.formatNumber(currentPercentage))
            );

            var lore = new ArrayList<String>();
            var levelRewards = reward.rewards();

            for (var line : itemConfig.getLore()) {
                if (line.equals("component:rewards")) {
                    var display = config.getDisplayComponents().get("rewards");

                    if (!levelRewards.isEmpty()) {
                        lore.add(display.getTitle());
                    }
                    for (var r : levelRewards) {
                        lore.add(display.getLine().replace("{reward}", r.getDisplay(player, placeholders)));
                    }
                } else {
                    lore.add(line);
                }
            }

            var builder = ItemBuilder.of(itemConfig).slot(slot)
                    .loreCompute(() -> lore.stream().map(l -> Text.component(player, l, placeholders)).toList())
                    .placeholder(placeholders);


            menu.addItem(builder.build(player));
        }

        return menu;
    }
}
