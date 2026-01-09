# VaultPack vs Aurora - Coding Standards Comparison

This document compares VaultPack's current coding practices with Aurora's enterprise-level standards to identify improvement opportunities.

---

## Executive Summary

**VaultPack Strengths:**
- Well-organized package structure
- Modern async data handling with caching
- Comprehensive Folia compatibility
- Dual message system (legacy + modern)
- Clean public API

**Key Areas for Improvement:**
- Configuration system could be more sophisticated
- Command framework could be modernized
- Data holder pattern not yet implemented
- More extensive use of design patterns
- Better separation between API and implementation

---

## 1. Project Structure & Organization

### Aurora Approach ✅
```
gg.auroramc.aurora/
├── api/                    # Public API layer (EXPLICIT SEPARATION)
│   ├── command/
│   ├── config/
│   ├── dependency/
│   └── ...
├── commands/              # Implementation (internal)
├── config/
└── expansions/            # Plugin extensions
```

**Key Principles:**
- Clear API/implementation boundary
- Feature-based packages
- Consistent naming conventions (*Manager, *Config, *Command, *Listener)

### VaultPack Current ⚠️
```
com.vaultpack/
├── api/                   # Has API package but mixed with events
├── commands/
├── config/
├── data/
├── gui/
├── listeners/
└── managers/
```

**Observations:**
- API package exists but contains implementation details
- Less consistent suffix naming
- No expansion/extension system

### Recommendation
```diff
+ Create clearer API boundary:
  com.vaultpack/
  ├── api/                 # ONLY public interfaces/abstractions
  │   ├── backpack/       # Public backpack API
  │   ├── events/         # Public events
  │   └── storage/        # Storage interface
  ├── internal/           # Implementation details (not for external use)
  │   ├── commands/
  │   ├── listeners/
  │   └── storage/
  └── expansions/         # Future: Expansion system for custom backpack types
```

---

## 2. Configuration Management

### Aurora Approach ✅

**Annotation-Based Config System:**
```java
public abstract class AuroraConfig {
    @IgnoreField
    private final File file;

    @IgnoreField
    private final YamlConfiguration rawConfiguration;

    // Automatic field serialization with camelCase → kebab-case
    // Built-in migration system
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return List.of();
    }
}
```

**Usage:**
```java
public class LevelConfig extends AuroraConfig {
    private String xpFormula;           // → xp-formula in YAML
    private Integer leaderboardCacheSize; // → leaderboard-cache-size
}
```

**Benefits:**
- Type-safe configuration
- Automatic serialization/deserialization
- Built-in migration support
- Reflection-based field mapping
- No manual getters needed

### VaultPack Current ⚠️

**Manual Config Access:**
```java
public class ConfigManager {
    private FileConfiguration config;

    public int getMaxBackpackSlots() {
        return config.getInt("storage.max-backpack-slots", 18);
    }

    public boolean isStorageTypeMySQL() {
        return config.getString("storage.type", "yaml").equalsIgnoreCase("mysql");
    }
}
```

**Issues:**
- Manual getter methods for every config value
- No type safety
- No migration system
- Scattered validation
- High maintenance cost

### Recommendation

**Option 1: Adopt Aurora-Style Config System**
```java
public class VaultPackConfig extends AuroraConfig {
    // Storage settings
    private String storageType = "yaml";
    private Integer maxBackpackSlots = 18;
    private Integer cacheSize = 200;

    // MySQL settings
    private String mysqlHost = "localhost";
    private Integer mysqlPort = 3306;
    private String mysqlDatabase = "vaultpack";

    // Feature flags
    private Boolean enableBackups = true;
    private Integer backupInterval = 30;

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return List.of(
            (yaml) -> {
                // V1: Initial version
                yaml.set("config-version", 1);
            },
            (yaml) -> {
                // V2: Add new cache-size option
                if (!yaml.contains("storage.cache-size")) {
                    yaml.set("storage.cache-size", 200);
                }
                yaml.set("config-version", 2);
            }
        );
    }
}
```

**Option 2: Premade Config Classes**

Aurora provides reusable config types:
```java
// For items in GUIs
public class ItemConfig {
    private String material;
    private String displayName;
    private List<String> lore;
    private Map<String, Integer> enchantments;
}

// For requirements
public class RequirementConfig {
    private String type;
    private String value;
}
```

**VaultPack could use these for:**
- Backpack GUI items
- Ender chest GUI items
- Storage menu items
- Unlock requirements

---

## 3. Command Framework

### Aurora Approach ✅

**ACF (Aikar's Command Framework):**
```java
@CommandAlias("aurora")
public class AuroraCommand extends BaseCommand {

    @Subcommand("reload")
    @CommandPermission("aurora.core.admin.reload")
    public void onReload(CommandSender sender) {
        // Implementation
    }

    @Subcommand("dispatch")
    @CommandCompletion("@players @commandActions @nothing")
    @CommandPermission("aurora.core.admin.dispatch")
    public void onDispatch(CommandSender sender,
                          @Flags("other") Player player,
                          String action,
                          String... args) {
        // Implementation
    }
}
```

**Benefits:**
- Annotation-driven
- Automatic permission checking
- Built-in tab completion
- Parameter validation
- Error handling
- Less boilerplate

**Command Dispatcher System:**
```yaml
# In configs:
actions:
  - "[console] give {player} diamond 1"
  - "[message] You received a reward!"
  - "[give-money] economy:vault currency:default 100"
```

### VaultPack Current ⚠️

**Bukkit Command API:**
```java
public class BackpackCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command,
                           String label, String[] args) {
        // Manual permission checks
        if (!sender.hasPermission("vaultpack.backpack")) {
            sender.sendMessage("No permission");
            return true;
        }

        // Manual argument parsing
        try {
            int slot = Integer.parseInt(args[0]);
            // ...
        } catch (NumberFormatException e) {
            // ...
        }
    }
}
```

**Issues:**
- Lots of boilerplate
- Manual permission checking
- Manual argument parsing
- Manual error handling
- Manual tab completion

### Recommendation

**Adopt ACF:**
```java
@CommandAlias("backpack|bp")
public class BackpackCommand extends BaseCommand {
    private final VaultPackPlugin plugin;

    @Default
    @CommandPermission("vaultpack.backpack")
    public void onBackpack(Player player) {
        // Open first available backpack
        plugin.getBackpackManager().openFirstAvailableBackpack(player);
    }

    @Subcommand("open")
    @CommandCompletion("@range:1-18")
    @CommandPermission("vaultpack.backpack.open")
    public void onOpen(Player player, int slot) {
        plugin.getBackpackManager().openBackpack(player, slot);
    }

    @Subcommand("unlock")
    @CommandCompletion("@range:1-18")
    @CommandPermission("vaultpack.backpack.unlock")
    public void onUnlock(Player player, int slot) {
        plugin.getBackpackManager().unlockSlot(player, slot);
    }

    @Subcommand("admin unlock")
    @CommandCompletion("@players @range:1-18")
    @CommandPermission("vaultpack.admin.unlock")
    public void onAdminUnlock(CommandSender sender,
                             @Flags("other") Player target,
                             int slot) {
        plugin.getBackpackManager().unlockSlot(target, slot);
        sender.sendMessage("Unlocked slot " + slot + " for " + target.getName());
    }
}
```

**Benefits:**
- ~50% less code
- Automatic validation
- Better error messages
- Cleaner structure
- Easier to maintain

---

## 4. Data Management Architecture

### Aurora Approach ✅

**Data Holder Pattern:**
```java
public abstract class UserDataHolder implements DataHolder {
    protected UUID uuid;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public abstract NamespacedId getId();
    public abstract void initFrom(@Nullable ConfigurationSection section);
    public abstract void serializeInto(ConfigurationSection section);

    public boolean isDirty() { return dirty.get(); }
}
```

**Composable User Data:**
```java
public class AuroraUser {
    private final Map<Class<? extends UserDataHolder>, UserDataHolder> dataHolderMap;

    public <T extends DataHolder> T getData(Class<T> holderClass) {
        return (T) dataHolderMap.get(holderClass);
    }
}
```

**Plugin-Specific Data Holders:**
```java
// In AuroraLevels:
public class LevelData extends UserDataHolder {
    private int level = 1;
    private double currentXP = 0.0;

    @Override
    public NamespacedId getId() {
        return NamespacedId.of("levels");
    }

    @Override
    public void initFrom(@Nullable ConfigurationSection section) {
        if (section == null) return;
        level = section.getInt("level", 1);
        currentXP = section.getDouble("current-xp", 0.0);
    }

    @Override
    public void serializeInto(ConfigurationSection section) {
        section.set("level", level);
        section.set("current-xp", currentXP);
    }
}
```

**Benefits:**
- Composable data components
- Plugin-specific extensions
- Dirty tracking for performance
- Centralized serialization logic
- Type-safe data access

### VaultPack Current ⚠️

**Monolithic Data Model:**
```java
public class PlayerBackpackData {
    private UUID playerId;
    private Map<Integer, ItemStack[]> backpacks;
    private Map<Integer, String> backpackTypes;
    private Set<Integer> unlockedSlots;
    private ItemStack[] enderChestContents;

    // All data in one class
}
```

**Issues:**
- Not extensible for other plugins
- Hard to add new data types
- No dirty tracking
- Tightly coupled to backpack functionality

### Recommendation

**Adopt Data Holder Pattern:**
```java
// Base class
public abstract class VaultPackDataHolder implements DataHolder {
    protected UUID uuid;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public abstract String getId();
    public abstract void loadFrom(ConfigurationSection section);
    public abstract void saveTo(ConfigurationSection section);

    protected void markDirty() {
        dirty.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void clearDirty() {
        dirty.set(false);
    }
}

// Backpack-specific data
public class BackpackData extends VaultPackDataHolder {
    private Map<Integer, ItemStack[]> backpacks = new ConcurrentHashMap<>();
    private Map<Integer, String> backpackTypes = new ConcurrentHashMap<>();
    private Set<Integer> unlockedSlots = ConcurrentHashMap.newKeySet();

    @Override
    public String getId() {
        return "backpacks";
    }

    public void setBackpackContents(int slot, ItemStack[] contents) {
        backpacks.put(slot, contents);
        markDirty();
    }
}

// Ender chest data
public class EnderChestData extends VaultPackDataHolder {
    private ItemStack[] contents;

    @Override
    public String getId() {
        return "enderchest";
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
        markDirty();
    }
}

// User container
public class VaultPackUser {
    private final UUID uuid;
    private final Map<String, VaultPackDataHolder> dataHolders = new HashMap<>();

    public <T extends VaultPackDataHolder> T getData(Class<T> holderClass) {
        // Get by ID
    }

    public void addDataHolder(VaultPackDataHolder holder) {
        dataHolders.put(holder.getId(), holder);
    }
}
```

**Benefits for VaultPack:**
- Other plugins could extend with custom data
- Easier to add features (statistics, achievements, etc.)
- Better performance (only save dirty data)
- Cleaner separation of concerns

---

## 5. Messaging & Localization

### Aurora Approach ✅

**Multi-Language Support:**
```java
public interface LanguageProvider {
    Locale getPlayerLocale(Player player);
    void setPlayerLocale(Player player, Locale locale);
    Locale getFallbackLocale();
    List<Locale> getSupportedLocales();
}
```

**Per-Player Locales:**
```
resources/
  messages_en.yml
  messages_es.yml
  messages_fr.yml
```

**Placeholder System:**
```java
public record Placeholder<T>(String key, T value) {
    public static <T> Placeholder<T> of(String key, T value) {
        return new Placeholder<>(key, value);
    }
}

// Usage:
Chat.sendMessage(player, messages.getLevelUp(),
    Placeholder.of("{level}", level),
    Placeholder.of("{xp}", xp)
);
```

**Benefits:**
- True multi-language support
- Type-safe placeholders
- Player-specific locales
- Cleaner syntax

### VaultPack Current ⚠️

**Single Language:**
```java
// lang.yml only (no multi-language)
public class MessageManager {
    private Map<String, String> messages = new HashMap<>();

    public void send(Player player, String key, String... replacements) {
        String message = messages.get(key);
        // Manual replacement logic
    }
}
```

**Issues:**
- No multi-language support
- Manual placeholder replacement
- Not type-safe

### Recommendation

**Option 1: Add Multi-Language Support**
```java
public class LocalizationManager {
    private final Map<Locale, Map<String, String>> messages = new HashMap<>();
    private final Map<UUID, Locale> playerLocales = new HashMap<>();

    public void loadLanguages() {
        // Load messages_en.yml, messages_es.yml, etc.
    }

    public String getMessage(Player player, String key, Placeholder<?>... placeholders) {
        Locale locale = playerLocales.getOrDefault(player.getUniqueId(), Locale.ENGLISH);
        String message = messages.get(locale).getOrDefault(key, key);
        return Placeholder.execute(message, placeholders);
    }
}
```

**Option 2: Adopt Aurora Placeholder System**
```java
// Current:
messageManager.send(player, "backpack-opened", "%slot%", String.valueOf(slot));

// With Placeholder:
messageManager.send(player, "backpack-opened",
    Placeholder.of("{slot}", slot)
);
```

---

## 6. Build Configuration

### Aurora Approach ✅

**Comprehensive Relocation:**
```kotlin
tasks.withType<ShadowJar> {
    relocate("com.zaxxer.hikari", "gg.auroramc.aurora.libs.hikari")
    relocate("net.objecthunter.exp4j", "gg.auroramc.aurora.libs.exp4j")
    relocate("co.aikar.commands", "gg.auroramc.aurora.libs.acf")
    relocate("org.bstats", "gg.auroramc.aurora.libs.bstats")

    // Prevents dependency conflicts with other plugins
}
```

**Maven Publishing:**
```kotlin
publishing {
    repositories {
        maven {
            url = if (version.toString().endsWith("SNAPSHOT")) {
                URI.create("https://repo.auroramc.gg/snapshots/")
            } else {
                URI.create("https://repo.auroramc.gg/releases/")
            }
        }
    }
}
```

### VaultPack Current ⚠️

**Basic Relocation:**
```kotlin
relocate("com.zaxxer.hikari", "com.vaultpack.libs.hikari")
relocate("com.mysql", "com.vaultpack.libs.mysql")
relocate("net.kyori.adventure.text.minimessage", "com.vaultpack.libs.minimessage")
```

**Missing:**
- Maven publishing configuration
- Version-specific relocation patterns
- Dependency exclusions

### Recommendation

**Add Publishing Support:**
```kotlin
plugins {
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.vaultpack"
            artifactId = "VaultPack"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
```

**Benefits:**
- Other plugins can depend on VaultPack API
- Easier distribution
- Version management

---

## 7. Code Organization & Design Patterns

### Aurora Patterns ✅

**1. Facade Pattern (AuroraAPI):**
```java
public class AuroraAPI {
    public static UserManager getUserManager() { }
    public static ItemManager getItemManager() { }
    public static AuroraEconomy getDefaultEconomy() { }
    // Unified access point
}
```

**2. Strategy Pattern (Storage):**
```java
public interface UserStorage {
    AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders);
    boolean saveUser(AuroraUser user, SaveReason reason);
}
// Implementations: MySqlStorage, YamlStorage
```

**3. Observer Pattern (Custom Events):**
```java
public class AuroraUserLoadedEvent extends Event {
    // Plugins can react to data loading
}
```

**4. Builder Pattern (Items):**
```java
ItemBuilder.of(config)
    .defaultMaterial(Material.DIAMOND)
    .setName("&eExample")
    .placeholder(Placeholder.of("{level}", level))
    .build(player);
```

### VaultPack Current ⚠️

**Patterns Used:**
- Singleton (Plugin instance)
- Factory (DataStorageFactory)
- Manager pattern
- Holder pattern (GUI holders)

**Missing:**
- Facade pattern (API access scattered)
- Builder pattern (GUI builders exist but not fluent)
- Strategy pattern (storage is factory-based but could be cleaner)

### Recommendation

**Add Facade:**
```java
public class VaultPackAPI {
    // Instead of: plugin.getBackpackManager().openBackpack(player, slot)
    public static void openBackpack(Player player, int slot) {
        getInstance().getBackpackManager().openBackpack(player, slot);
    }

    // Instead of: plugin.getBackpackManager().unlockSlot(player, slot)
    public static boolean unlockSlot(Player player, int slot) {
        return getInstance().getBackpackManager().unlockSlot(player, slot);
    }

    // Cleaner for external plugins
}
```

**Improve Builder:**
```java
// Current:
BackpackGUIBuilder builder = new BackpackGUIBuilder(plugin);
builder.buildGUI(player, backpackType, slotNumber);

// Fluent:
BackpackGUI.builder()
    .player(player)
    .type(backpackType)
    .slot(slotNumber)
    .title("&6Backpack")
    .placeholder(Placeholder.of("{slot}", slotNumber))
    .build()
    .open();
```

---

## 8. Documentation Standards

### Aurora Approach ✅

**Focused Documentation:**
- Minimal Javadoc, but all public APIs documented
- Self-documenting code (clear names)
- Inline comments for complex logic only

**Example:**
```java
/**
 * Creates a custom logger with you plugin name as prefix.
 *
 * @param plugin    the name of your plugin
 * @param debugMode the supplier for the debug parameter
 * @return the newly created logger
 */
public static AuroraLogger createLogger(String plugin, Supplier<Boolean> debugMode) { }
```

### VaultPack Current ⚠️

**Inconsistent:**
- Some classes well-documented (VaultPackAPI)
- Others sparse (internal classes)
- Phase comments useful but could be cleaner

### Recommendation

**Standard for Public API:**
```java
/**
 * Opens a backpack for the player at the specified slot.
 *
 * <p>This method will fail silently if:</p>
 * <ul>
 *   <li>The slot is not unlocked</li>
 *   <li>The slot number is invalid</li>
 *   <li>The player is offline</li>
 * </ul>
 *
 * @param player the player to open the backpack for
 * @param slot the slot number (1-18)
 * @return true if the backpack was opened successfully
 */
public boolean openBackpack(Player player, int slot) { }
```

---

## 9. Expansion System

### Aurora Approach ✅

**Built-in Expansion System:**
```java
public interface AuroraExpansion {
    void onEnable();
    void onDisable();
    String getId();
    boolean isEnabled();
}
```

**Built-in Expansions:**
- Economy expansion
- Entity expansion
- GUI expansion
- Item expansion
- Leaderboard expansion
- Region expansion

**Benefits:**
- Plugins can add features to Aurora
- Modular architecture
- Easy to enable/disable features

### VaultPack Current ⚠️

**No Expansion System:**
- All features built-in
- Hard to extend
- No third-party integration points

### Recommendation

**Add Expansion System:**
```java
public interface VaultPackExpansion {
    String getId();
    String getName();
    String getVersion();

    void onEnable(VaultPackPlugin plugin);
    void onDisable();

    boolean isEnabled();
}

// Example expansion:
public class StatisticsExpansion implements VaultPackExpansion {
    @Override
    public String getId() {
        return "statistics";
    }

    // Track opens, closes, items added/removed, etc.
}
```

**Potential Expansions:**
- Statistics (opens, closes, items stored)
- Leaderboards (most items, most valuable backpack)
- Achievements (unlock all slots, store 1000 items)
- Backpack sorting
- Auto-deposit from inventory
- Backpack search

---

## 10. Testing & Quality Assurance

### Aurora Approach ✅

**Run-Paper Plugin:**
```kotlin
plugins {
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

// Allows: ./gradlew runServer
// Automatic test server setup
```

### VaultPack Current ⚠️

**No Testing Infrastructure:**
- No run-paper plugin
- Manual testing only

### Recommendation

**Add Run-Paper:**
```kotlin
plugins {
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

tasks {
    runServer {
        minecraftVersion("1.21.4")

        downloadPlugins {
            url("https://download.luckperms.net/1542/bukkit/loader/LuckPerms-Bukkit-5.4.139.jar")
            // Download dependencies automatically
        }
    }
}
```

---

## Priority Recommendations

### High Priority 🔴

1. **Adopt ACF for Commands**
   - Immediate impact on code quality
   - Reduces boilerplate by ~50%
   - Better error handling

2. **Implement Data Holder Pattern**
   - Makes VaultPack extensible
   - Better performance (dirty tracking)
   - Cleaner architecture

3. **Improve Config System**
   - Type-safe configuration
   - Migration support
   - Reduced maintenance

### Medium Priority 🟡

4. **Add Multi-Language Support**
   - Server appeal to international users
   - Professional feature

5. **Create Expansion System**
   - Allows community extensions
   - Modular architecture

6. **Improve API Facade**
   - Easier for external plugins
   - Cleaner public interface

### Low Priority 🟢

7. **Add Run-Paper Plugin**
   - Development convenience
   - Faster testing

8. **Improve Documentation**
   - Long-term maintenance
   - Community contributions

9. **Maven Publishing**
   - Distribution
   - Dependency management

---

## Conclusion

Aurora demonstrates **enterprise-level plugin development practices** with:
- Sophisticated architecture (data holders, expansions)
- Modern tooling (ACF, reflection-based configs)
- Excellent extensibility (composable data, expansion system)
- Strong Folia compatibility
- Professional code organization

VaultPack has a **solid foundation** with:
- Good package structure
- Modern async patterns
- Folia compatibility
- Dual storage support

By adopting Aurora's practices, VaultPack can become **more maintainable, extensible, and professional** while reducing boilerplate and improving the developer experience.

---

## Next Steps

1. Review this comparison with the team
2. Prioritize which improvements to implement
3. Create implementation plan for high-priority items
4. Consider gradual adoption (start with ACF, then data holders, etc.)
5. Maintain backward compatibility during migration

The goal is not to copy Aurora, but to learn from its excellent practices and apply them where they make sense for VaultPack's specific needs.
