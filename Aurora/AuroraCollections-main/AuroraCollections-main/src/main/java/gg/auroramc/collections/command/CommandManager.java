package gg.auroramc.collections.command;

import co.aikar.commands.MessageKeys;
import co.aikar.commands.MinecraftMessageKeys;
import co.aikar.commands.PaperCommandManager;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Collection;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandManager {
    private final AuroraCollections plugin;
    private final PaperCommandManager commandManager;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand().toBuilder()
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private boolean hasSetup = false;

    public CommandManager(AuroraCollections plugin) {
        this.commandManager = new PaperCommandManager(plugin);
        this.plugin = plugin;
    }

    private void setupCommands() {
        if (!this.hasSetup) {
            commandManager.getLocales().setDefaultLocale(Locale.ENGLISH);
            commandManager.usePerIssuerLocale(false);

            var aliases = plugin.getConfigManager().getConfig().getCommandAliases();

            commandManager.getCommandCompletions().registerCompletion("categories",
                    c -> plugin.getConfigManager().getCategoriesConfig().getCategories().keySet());

            commandManager.getCommandCompletions().registerCompletion("collections", c -> {
                var manager = plugin.getCollectionManager();
                var category = c.getContextValue(String.class);
                if (!manager.hasCategory(category)) return List.of();
                return manager.getCollectionsByCategory(category).stream().map(Collection::getId).toList();
            });

            commandManager.getCommandReplacements().addReplacement("collectionsAlias", a(aliases.getCollections()));
            commandManager.getCommandReplacements().addReplacement("progressionAlias", a(aliases.getProgression()));
        }

        var msg = plugin.getConfigManager().getMessageConfig();
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.NO_PLAYER_FOUND, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.NO_PLAYER_FOUND_OFFLINE, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.NO_PLAYER_FOUND_SERVER, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.IS_NOT_A_VALID_NAME, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.COULD_NOT_FIND_PLAYER, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED, m(msg.getNoPermission()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED_PARAMETER, m(msg.getNoPermission()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.INVALID_SYNTAX, m(msg.getInvalidSyntax()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.MUST_BE_A_NUMBER, m(msg.getMustBeNumber()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.ERROR_PERFORMING_COMMAND, m(msg.getCommandError()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.ERROR_GENERIC_LOGGED, m(msg.getCommandError()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.NOT_ALLOWED_ON_CONSOLE, m(msg.getPlayerOnlyCommand()));

        if (!this.hasSetup) {
            this.commandManager.registerCommand(new CollectionsCommand(plugin));
            this.hasSetup = true;
        }
    }

    public void reload() {
        this.setupCommands();
    }

    private String a(List<String> aliases) {
        return String.join("|", aliases);
    }

    private String m(String msg) {
        return serializer.serialize(Text.component(Chat.translateToMM(msg)));
    }

    public void unregisterCommands() {
        this.commandManager.unregisterCommands();
    }
}
