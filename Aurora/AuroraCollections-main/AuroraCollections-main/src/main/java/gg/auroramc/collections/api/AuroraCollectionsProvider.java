package gg.auroramc.collections.api;

import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.CollectionManager;

public class AuroraCollectionsProvider {
    private static AuroraCollections plugin;

    private AuroraCollectionsProvider() {
    }

    public static CollectionManager getCollectionManager() {
        return plugin.getCollectionManager();
    }
}
