package gg.auroramc.collections.hooks.mythic.mechanics;

import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.collections.AuroraCollections;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import org.bukkit.entity.Player;

@MythicMechanic(
        author = "erik_sz",
        name = "progressCollection",
        description = "Progress collections matched by the trigger by a certain amount just like the plugin would do it"
)
public class ProgressCollectionMechanic implements ITargetedEntitySkill {
    private final AuroraCollections plugin;
    private final String trigger;
    private final TypeId typeId;
    private final PlaceholderInt amount;

    public ProgressCollectionMechanic(AuroraCollections plugin, MythicMechanicLoadEvent loader) {
        this.plugin = plugin;
        this.amount = loader.getConfig().getPlaceholderInteger(new String[]{"amount", "a",}, 0);
        this.trigger = loader.getConfig().getString(new String[]{"trigger", "t", }).toUpperCase();
        this.typeId = TypeId.fromDefault(loader.getConfig().getString(new String[]{"typeId", "type", "id"}));
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity target) {
        if (!target.isPlayer()) return SkillResult.INVALID_TARGET;
        Player player = BukkitAdapter.adapt(target.asPlayer());

        plugin.getCollectionManager().progressCollections(player, typeId, amount.get(skillMetadata), trigger);

        return SkillResult.SUCCESS;
    }
}
