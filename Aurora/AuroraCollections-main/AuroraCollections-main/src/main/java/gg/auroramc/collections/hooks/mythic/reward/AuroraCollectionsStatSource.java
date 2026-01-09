package gg.auroramc.collections.hooks.mythic.reward;

import io.lumine.mythic.core.skills.stats.StatSource;

public class AuroraCollectionsStatSource implements StatSource {
    @Override
    public boolean removeOnReload() {
        return false;
    }
}
