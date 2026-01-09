package gg.auroramc.collections.collection;

import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.aurora.api.reward.Reward;

import java.util.List;
import java.util.Map;

public record CategoryReward(double percentage, List<Reward> rewards, Map<String, ItemConfig> items) {
}
