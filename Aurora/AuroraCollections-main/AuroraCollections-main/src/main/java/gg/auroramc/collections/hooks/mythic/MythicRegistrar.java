package gg.auroramc.collections.hooks.mythic;

import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.hooks.mythic.conditions.HasCollectionLevelCondition;
import gg.auroramc.collections.hooks.mythic.mechanics.AddToCollectionMechanic;
import gg.auroramc.collections.hooks.mythic.mechanics.ProgressCollectionMechanic;
import io.lumine.mythic.api.skills.ISkillMechanic;
import io.lumine.mythic.api.skills.conditions.ISkillCondition;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.utils.annotations.MythicCondition;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import lombok.SneakyThrows;

public class MythicRegistrar {
    private final AuroraCollections plugin;

    public MythicRegistrar(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    public void registerApplicableCondition(MythicConditionLoadEvent event) {
        registerCondition(event, HasCollectionLevelCondition.class);
    }

    public void registerApplicableMechanic(MythicMechanicLoadEvent event) {
        registerMechanic(event, AddToCollectionMechanic.class);
        registerMechanic(event, ProgressCollectionMechanic.class);
    }

    @SneakyThrows
    private void registerCondition(MythicConditionLoadEvent event, Class<? extends ISkillCondition> conditionClass) {
        var annotation = conditionClass.getAnnotation(MythicCondition.class);

        if (event.getConditionName().equalsIgnoreCase(annotation.name())) {
            event.register(conditionClass.getConstructor(AuroraCollections.class, MythicConditionLoadEvent.class)
                    .newInstance(plugin, event));
            return;
        }

        for (var alias : annotation.aliases()) {
            if (event.getConditionName().equalsIgnoreCase(alias)) {
                event.register(conditionClass.getConstructor(AuroraCollections.class, MythicConditionLoadEvent.class)
                        .newInstance(plugin, event));
                return;
            }
        }
    }

    @SneakyThrows
    private void registerMechanic(MythicMechanicLoadEvent event, Class<? extends ISkillMechanic> mechanicClass) {
        var annotation = mechanicClass.getAnnotation(MythicMechanic.class);

        if (event.getMechanicName().equalsIgnoreCase(annotation.name())) {
            event.register(mechanicClass.getConstructor(AuroraCollections.class, MythicMechanicLoadEvent.class)
                    .newInstance(plugin, event));
            return;
        }

        for (var alias : annotation.aliases()) {
            if (event.getMechanicName().equalsIgnoreCase(alias)) {
                event.register(mechanicClass.getConstructor(AuroraCollections.class, MythicMechanicLoadEvent.class)
                        .newInstance(plugin, event));
                return;
            }
        }
    }
}
