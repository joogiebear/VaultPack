# VaultPack → Aurora Standards Migration Guide

This document outlines a phased approach to refactor VaultPack to follow Aurora's enterprise-level coding standards. Each phase is designed to be completed independently while maintaining backward compatibility and plugin functionality.

---

## Migration Overview

**Goal:** Transform VaultPack into an Aurora-style enterprise plugin with modern architecture, extensibility, and maintainability.

**Approach:** Incremental refactoring with backward compatibility maintained throughout each phase.

**Estimated Duration:** 8-12 weeks (depending on available development time)

---

## Phase Status Tracker

| Phase | Status | Completion Date | Notes |
|-------|--------|----------------|-------|
| Phase 0: Preparation | ✅ Completed | 2026-01-08 | Testing infrastructure set up successfully |
| Phase 1: Build & Dependencies | ⬜ Not Started | - | - |
| Phase 2: Configuration System | ⬜ Not Started | - | - |
| Phase 3: Command Framework | ⬜ Not Started | - | - |
| Phase 4: Message & Localization | ⬜ Not Started | - | - |
| Phase 5: Data Architecture | ⬜ Not Started | - | - |
| Phase 6: API Refactoring | ⬜ Not Started | - | - |
| Phase 7: Expansion System | ⬜ Not Started | - | - |
| Phase 8: Polish & Documentation | ⬜ Not Started | - | - |

**Legend:**
- ⬜ Not Started
- 🔄 In Progress
- ✅ Completed
- ⚠️ Blocked
- 🔴 Issues Found

---

## Phase 0: Preparation & Setup

**Duration:** 1-2 days
**Status:** ✅ Completed
**Completion Date:** 2026-01-08

### Objectives

1. Set up testing infrastructure
2. Create backup/rollback strategy
3. Establish coding standards
4. Create feature branch for migration

### Tasks

- [ ] **0.1 - Create Migration Branch**
  ```bash
  git checkout -b feature/aurora-migration
  git push -u origin feature/aurora-migration
  ```

- [ ] **0.2 - Add Run-Paper Plugin**
  ```kotlin
  // build.gradle.kts
  plugins {
      id("xyz.jpenilla.run-paper") version "2.3.0"
  }

  tasks {
      runServer {
          minecraftVersion("1.21.4")

          downloadPlugins {
              url("https://download.luckperms.net/1542/bukkit/loader/LuckPerms-Bukkit-5.4.139.jar")
              github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
              hangar("PlaceholderAPI", "2.11.6")
          }
      }
  }
  ```

- [ ] **0.3 - Create Testing Checklist**
  - Document all current features to test
  - Create test scenarios for backpacks
  - Create test scenarios for ender chests
  - Create test scenarios for data persistence
  - Create test scenarios for permissions

- [ ] **0.4 - Set Up Aurora Reference**
  - Clone Aurora as reference: Keep at `C:\Users\e85sr\Documents\GitHub\VaultPack\Aurora`
  - Document key Aurora patterns to reference
  - Create code snippet library

- [ ] **0.5 - Establish Version Strategy**
  - Current version: `1.x.x`
  - Migration versions: `2.0.0-alpha.X` (during migration)
  - Release version: `2.0.0` (after completion)

### Deliverables

- ✅ Feature branch created (`feature/enterprise-refactoring`)
- ✅ Testing infrastructure set up (Run-Paper plugin added)
- ✅ Rollback strategy documented
- ✅ Reference materials organized locally
- ✅ Testing checklist created (`TESTING_CHECKLIST.md`)
- ✅ Local-only folders removed from repository (.claude, Aurora, GitBook)

### Success Criteria

- Can run test server with `./gradlew runServer`
- All current features documented
- Version strategy established

---

## Phase 1: Build & Dependencies

**Duration:** 2-3 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Add ACF (Aikar's Command Framework)
2. Improve dependency management
3. Add Maven publishing support
4. Update relocation patterns

### Tasks

- [ ] **1.1 - Add ACF Dependency**
  ```kotlin
  repositories {
      maven("https://repo.aikar.co/content/groups/aikar/")
  }

  dependencies {
      implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
  }
  ```

- [ ] **1.2 - Add Config Library (Optional)**
  ```kotlin
  // For Aurora-style config system
  dependencies {
      implementation("org.spongepowered:configurate-yaml:4.1.2")
  }
  ```

- [ ] **1.3 - Update Shadow Configuration**
  ```kotlin
  tasks.withType<ShadowJar> {
      relocate("co.aikar.commands", "com.vaultpack.libs.acf")
      relocate("co.aikar.locales", "com.vaultpack.libs.locales")
      relocate("org.spongepowered.configurate", "com.vaultpack.libs.configurate")

      minimize() // Reduce JAR size
  }
  ```

- [ ] **1.4 - Add Maven Publishing**
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

              pom {
                  name.set("VaultPack")
                  description.set("Advanced backpack and storage system for Paper")
                  url.set("https://github.com/yourusername/VaultPack")

                  licenses {
                      license {
                          name.set("MIT License")
                          url.set("https://opensource.org/licenses/MIT")
                      }
                  }

                  developers {
                      developer {
                          id.set("yourid")
                          name.set("Your Name")
                      }
                  }
              }
          }
      }

      repositories {
          maven {
              name = "GitHubPackages"
              url = uri("https://maven.pkg.github.com/yourusername/VaultPack")
              credentials {
                  username = System.getenv("GITHUB_ACTOR")
                  password = System.getenv("GITHUB_TOKEN")
              }
          }
      }
  }
  ```

- [ ] **1.5 - Update Lombok Configuration**
  ```kotlin
  dependencies {
      compileOnly("org.projectlombok:lombok:1.18.30")
      annotationProcessor("org.projectlombok:lombok:1.18.30")
  }
  ```

- [ ] **1.6 - Add bStats (Optional)**
  ```kotlin
  dependencies {
      implementation("org.bstats:bstats-bukkit:3.0.2")
  }

  // In shadowJar:
  relocate("org.bstats", "com.vaultpack.libs.bstats")
  ```

- [ ] **1.7 - Build & Test**
  ```bash
  ./gradlew clean shadowJar
  # Test that JAR builds successfully
  # Verify relocations worked
  ```

### Deliverables

- ✅ ACF dependency added and relocated
- ✅ Maven publishing configured
- ✅ Build produces clean JAR
- ✅ All dependencies properly relocated

### Success Criteria

- `./gradlew clean shadowJar` succeeds
- JAR size reasonable (<2MB)
- No dependency conflicts in test server

### Notes

**Dependencies Added:**
- ACF: For command framework
- Configurate (optional): For advanced config system
- bStats (optional): For usage statistics

**Breaking Changes:** None (backward compatible)

---

## Phase 2: Configuration System

**Duration:** 4-5 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Create Aurora-style base config classes
2. Migrate existing configs to new system
3. Implement migration system
4. Add config validation

### Tasks

#### **2.1 - Create Base Config Classes**

- [ ] **Create `AuroraConfig` base class**

  File: `src/main/java/com/vaultpack/config/base/AuroraConfig.java`

  ```java
  package com.vaultpack.config.base;

  import lombok.Getter;
  import org.bukkit.configuration.file.YamlConfiguration;
  import org.jetbrains.annotations.Nullable;

  import java.io.File;
  import java.io.IOException;
  import java.lang.reflect.Field;
  import java.util.*;
  import java.util.function.Consumer;

  public abstract class AuroraConfig {
      @IgnoreField
      @Getter
      private final File file;

      @IgnoreField
      private YamlConfiguration yaml;

      public AuroraConfig(File file) {
          this.file = file;
          this.yaml = YamlConfiguration.loadConfiguration(file);
      }

      /**
       * Load config values from YAML into this object
       */
      public void load() {
          migrate();
          loadFields();
      }

      /**
       * Save config values from this object to YAML
       */
      public void save() {
          saveFields();
          try {
              yaml.save(file);
          } catch (IOException e) {
              e.printStackTrace();
          }
      }

      /**
       * Override to provide migration steps
       */
      protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
          return Collections.emptyList();
      }

      /**
       * Execute migrations
       */
      private void migrate() {
          int currentVersion = yaml.getInt("config-version", 0);
          List<Consumer<YamlConfiguration>> migrations = getMigrationSteps();

          if (currentVersion < migrations.size()) {
              for (int i = currentVersion; i < migrations.size(); i++) {
                  migrations.get(i).accept(yaml);
              }
              try {
                  yaml.save(file);
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }
      }

      /**
       * Load fields from YAML using reflection
       */
      private void loadFields() {
          for (Field field : getClass().getDeclaredFields()) {
              if (field.isAnnotationPresent(IgnoreField.class)) continue;

              field.setAccessible(true);
              String key = serializeKey(field.getName());

              try {
                  Object value = yaml.get(key);
                  if (value != null) {
                      field.set(this, value);
                  }
              } catch (IllegalAccessException e) {
                  e.printStackTrace();
              }
          }
      }

      /**
       * Save fields to YAML using reflection
       */
      private void saveFields() {
          for (Field field : getClass().getDeclaredFields()) {
              if (field.isAnnotationPresent(IgnoreField.class)) continue;

              field.setAccessible(true);
              String key = serializeKey(field.getName());

              try {
                  Object value = field.get(this);
                  yaml.set(key, value);
              } catch (IllegalAccessException e) {
                  e.printStackTrace();
              }
          }
      }

      /**
       * Convert camelCase to kebab-case
       */
      private String serializeKey(String fieldName) {
          return fieldName.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase(Locale.ROOT);
      }

      /**
       * Reload config from disk
       */
      public void reload() {
          this.yaml = YamlConfiguration.loadConfiguration(file);
          load();
      }
  }
  ```

- [ ] **Create `@IgnoreField` annotation**

  File: `src/main/java/com/vaultpack/config/base/IgnoreField.java`

  ```java
  package com.vaultpack.config.base;

  import java.lang.annotation.ElementType;
  import java.lang.annotation.Retention;
  import java.lang.annotation.RetentionPolicy;
  import java.lang.annotation.Target;

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface IgnoreField {
  }
  ```

#### **2.2 - Create Main Config Class**

- [ ] **Create `MainConfig` class**

  File: `src/main/java/com/vaultpack/config/MainConfig.java`

  ```java
  package com.vaultpack.config;

  import com.vaultpack.config.base.AuroraConfig;
  import com.vaultpack.config.base.IgnoreField;
  import lombok.Getter;
  import org.bukkit.configuration.file.YamlConfiguration;

  import java.io.File;
  import java.util.List;
  import java.util.function.Consumer;

  @Getter
  public class MainConfig extends AuroraConfig {

      // Storage settings
      private String storageType = "yaml";
      private Integer maxBackpackSlots = 18;
      private Integer cacheSize = 200;
      private Integer cacheDuration = 30;

      // MySQL settings
      private String mysqlHost = "localhost";
      private Integer mysqlPort = 3306;
      private String mysqlDatabase = "vaultpack";
      private String mysqlUsername = "root";
      private String mysqlPassword = "password";
      private Integer mysqlPoolSize = 10;

      // Backup settings
      private Boolean enableBackups = true;
      private Integer backupInterval = 30;
      private Integer maxBackups = 10;

      // Performance settings
      private Boolean enableAsyncSaving = true;
      private Integer saveInterval = 5;

      // Feature flags
      private Boolean enableEnderChestIntegration = true;
      private Boolean enableBlacklistSystem = true;
      private Boolean enableCraftingIntegration = true;

      // Debug settings
      private Boolean debugMode = false;
      private Boolean enableMetrics = true;

      public MainConfig(File file) {
          super(file);
      }

      @Override
      protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
          return List.of(
              // Version 1: Initial migration
              (yaml) -> {
                  if (!yaml.contains("config-version")) {
                      // Migrate old values if they exist
                      if (yaml.contains("storage.type")) {
                          yaml.set("storage-type", yaml.getString("storage.type"));
                      }
                      if (yaml.contains("storage.max-backpack-slots")) {
                          yaml.set("max-backpack-slots", yaml.getInt("storage.max-backpack-slots"));
                      }
                      yaml.set("config-version", 1);
                  }
              },

              // Version 2: Add cache settings
              (yaml) -> {
                  if (yaml.getInt("config-version") < 2) {
                      yaml.set("cache-size", 200);
                      yaml.set("cache-duration", 30);
                      yaml.set("config-version", 2);
                  }
              },

              // Version 3: Add feature flags
              (yaml) -> {
                  if (yaml.getInt("config-version") < 3) {
                      yaml.set("enable-ender-chest-integration", true);
                      yaml.set("enable-blacklist-system", true);
                      yaml.set("enable-crafting-integration", true);
                      yaml.set("config-version", 3);
                  }
              }
          );
      }
  }
  ```

#### **2.3 - Create Backpack Type Config**

- [ ] **Create `BackpackTypeConfig` class**

  File: `src/main/java/com/vaultpack/config/BackpackTypeConfig.java`

  ```java
  package com.vaultpack.config;

  import com.vaultpack.config.base.AuroraConfig;
  import lombok.Getter;
  import org.bukkit.Material;
  import org.bukkit.configuration.ConfigurationSection;

  import java.io.File;
  import java.util.*;

  @Getter
  public class BackpackTypeConfig extends AuroraConfig {

      private Map<String, BackpackType> backpackTypes = new HashMap<>();

      public BackpackTypeConfig(File file) {
          super(file);
      }

      @Override
      public void load() {
          super.load();
          loadBackpackTypes();
      }

      private void loadBackpackTypes() {
          ConfigurationSection typesSection = getYaml().getConfigurationSection("backpack-types");
          if (typesSection == null) return;

          for (String key : typesSection.getKeys(false)) {
              ConfigurationSection typeSection = typesSection.getConfigurationSection(key);
              if (typeSection == null) continue;

              BackpackType type = new BackpackType();
              type.setId(key);
              type.setName(typeSection.getString("name", key));
              type.setMaterial(Material.valueOf(typeSection.getString("material", "CHEST")));
              type.setRows(typeSection.getInt("rows", 3));
              type.setCustomModelData(typeSection.getInt("custom-model-data", 0));
              type.setLore(typeSection.getStringList("lore"));

              backpackTypes.put(key, type);
          }
      }

      @Getter
      @Setter
      public static class BackpackType {
          private String id;
          private String name;
          private Material material;
          private Integer rows;
          private Integer customModelData;
          private List<String> lore;
      }
  }
  ```

#### **2.4 - Update ConfigManager**

- [ ] **Refactor ConfigManager to use new system**

  File: `src/main/java/com/vaultpack/managers/ConfigManager.java`

  ```java
  package com.vaultpack.managers;

  import com.vaultpack.VaultPackPlugin;
  import com.vaultpack.config.MainConfig;
  import com.vaultpack.config.BackpackTypeConfig;
  import lombok.Getter;

  import java.io.File;

  public class ConfigManager {
      private final VaultPackPlugin plugin;

      @Getter
      private MainConfig mainConfig;

      @Getter
      private BackpackTypeConfig backpackTypeConfig;

      public ConfigManager(VaultPackPlugin plugin) {
          this.plugin = plugin;
          loadConfigs();
      }

      public void loadConfigs() {
          // Ensure files exist
          plugin.saveDefaultConfig();
          if (!new File(plugin.getDataFolder(), "backpacks.yml").exists()) {
              plugin.saveResource("backpacks.yml", false);
          }

          // Load configs
          mainConfig = new MainConfig(new File(plugin.getDataFolder(), "config.yml"));
          mainConfig.load();

          backpackTypeConfig = new BackpackTypeConfig(new File(plugin.getDataFolder(), "backpacks.yml"));
          backpackTypeConfig.load();
      }

      public void reloadConfigs() {
          mainConfig.reload();
          backpackTypeConfig.reload();
      }

      // Backward compatibility methods (delegate to new config)
      public int getMaxBackpackSlots() {
          return mainConfig.getMaxBackpackSlots();
      }

      public boolean isStorageTypeMySQL() {
          return "mysql".equalsIgnoreCase(mainConfig.getStorageType());
      }

      // ... other backward compatibility methods
  }
  ```

#### **2.5 - Testing**

- [ ] **Test config migration**
  - Backup current config.yml
  - Start server with new system
  - Verify all values migrated correctly
  - Verify config-version added
  - Test reload command

- [ ] **Test backward compatibility**
  - Ensure existing code still works
  - Verify no breaking changes
  - Test all config getters

### Deliverables

- ✅ AuroraConfig base class created
- ✅ MainConfig migrated to new system
- ✅ BackpackTypeConfig created
- ✅ Migration system working
- ✅ Backward compatibility maintained

### Success Criteria

- All configs load without errors
- Migration runs automatically
- Old config values preserved
- New fields added with defaults
- Existing code still works

### Breaking Changes

**None** - Full backward compatibility maintained via ConfigManager delegation methods.

---

## Phase 3: Command Framework Migration

**Duration:** 3-4 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Integrate ACF (Aikar's Command Framework)
2. Migrate all commands to ACF
3. Improve tab completion
4. Add command contexts and completions

### Tasks

#### **3.1 - Setup ACF**

- [ ] **Initialize ACF in main plugin class**

  File: `src/main/java/com/vaultpack/VaultPackPlugin.java`

  ```java
  import co.aikar.commands.PaperCommandManager;

  public class VaultPackPlugin extends JavaPlugin {
      @Getter
      private PaperCommandManager commandManager;

      @Override
      public void onEnable() {
          // Initialize ACF
          commandManager = new PaperCommandManager(this);

          // Register custom completions
          setupCommandCompletions();

          // Register custom contexts
          setupCommandContexts();

          // Register commands
          registerCommands();
      }

      private void setupCommandCompletions() {
          commandManager.getCommandCompletions().registerCompletion("backpackSlots", c -> {
              Player player = c.getPlayer();
              if (player == null) return List.of();

              return backpackManager.getUnlockedSlots(player.getUniqueId())
                  .stream()
                  .map(String::valueOf)
                  .toList();
          });

          commandManager.getCommandCompletions().registerCompletion("backpackTypes", c -> {
              return configManager.getBackpackTypeConfig()
                  .getBackpackTypes()
                  .keySet()
                  .stream()
                  .toList();
          });
      }

      private void setupCommandContexts() {
          // Add custom parameter resolvers if needed
      }

      private void registerCommands() {
          commandManager.registerCommand(new BackpackCommand(this));
          commandManager.registerCommand(new EnderChestCommand(this));
          commandManager.registerCommand(new StorageCommand(this));
          commandManager.registerCommand(new VaultPackCommand(this));
      }
  }
  ```

#### **3.2 - Migrate BackpackCommand**

- [ ] **Refactor BackpackCommand to use ACF**

  File: `src/main/java/com/vaultpack/commands/BackpackCommand.java`

  ```java
  package com.vaultpack.commands;

  import co.aikar.commands.BaseCommand;
  import co.aikar.commands.annotation.*;
  import com.vaultpack.VaultPackPlugin;
  import org.bukkit.command.CommandSender;
  import org.bukkit.entity.Player;

  @CommandAlias("backpack|bp")
  @Description("Manage your backpacks")
  public class BackpackCommand extends BaseCommand {
      private final VaultPackPlugin plugin;

      public BackpackCommand(VaultPackPlugin plugin) {
          this.plugin = plugin;
      }

      @Default
      @CommandPermission("vaultpack.backpack")
      @Description("Open your first available backpack")
      public void onDefault(Player player) {
          plugin.getBackpackManager().openFirstAvailableBackpack(player);
      }

      @Subcommand("open")
      @CommandPermission("vaultpack.backpack.open")
      @CommandCompletion("@backpackSlots")
      @Description("Open a specific backpack slot")
      @Syntax("<slot>")
      public void onOpen(Player player, int slot) {
          if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
              plugin.getMessageManager().send(player, "invalid-slot");
              return;
          }

          if (!plugin.getBackpackManager().hasSlotUnlocked(player.getUniqueId(), slot)) {
              plugin.getMessageManager().send(player, "slot-locked");
              return;
          }

          plugin.getBackpackManager().openBackpack(player, slot);
      }

      @Subcommand("unlock")
      @CommandPermission("vaultpack.backpack.unlock")
      @CommandCompletion("@range:1-18")
      @Description("Unlock a backpack slot")
      @Syntax("<slot>")
      public void onUnlock(Player player, int slot) {
          if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
              plugin.getMessageManager().send(player, "invalid-slot");
              return;
          }

          if (plugin.getBackpackManager().hasSlotUnlocked(player.getUniqueId(), slot)) {
              plugin.getMessageManager().send(player, "slot-already-unlocked");
              return;
          }

          plugin.getBackpackManager().unlockSlot(player, slot);
      }

      @Subcommand("list")
      @CommandPermission("vaultpack.backpack.list")
      @Description("List all your backpack slots")
      public void onList(Player player) {
          plugin.getBackpackManager().showBackpackList(player);
      }

      @Subcommand("info")
      @CommandPermission("vaultpack.backpack.info")
      @CommandCompletion("@backpackSlots")
      @Description("Get info about a backpack")
      @Syntax("[slot]")
      public void onInfo(Player player, @Optional Integer slot) {
          if (slot == null) {
              plugin.getBackpackManager().showBackpackInfo(player);
          } else {
              plugin.getBackpackManager().showBackpackInfo(player, slot);
          }
      }

      // Admin commands
      @Subcommand("admin unlock")
      @CommandPermission("vaultpack.admin.unlock")
      @CommandCompletion("@players @range:1-18")
      @Description("Unlock a slot for a player")
      @Syntax("<player> <slot>")
      public void onAdminUnlock(CommandSender sender, @Flags("other") Player target, int slot) {
          if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
              sender.sendMessage("Invalid slot number!");
              return;
          }

          plugin.getBackpackManager().unlockSlot(target, slot);
          sender.sendMessage("Unlocked slot " + slot + " for " + target.getName());
      }

      @Subcommand("admin open")
      @CommandPermission("vaultpack.admin.open")
      @CommandCompletion("@players @range:1-18")
      @Description("Open another player's backpack")
      @Syntax("<player> <slot>")
      public void onAdminOpen(Player player, @Flags("other") Player target, int slot) {
          if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
              player.sendMessage("Invalid slot number!");
              return;
          }

          plugin.getBackpackManager().openBackpackAsAdmin(player, target, slot);
      }

      @Subcommand("admin reset")
      @CommandPermission("vaultpack.admin.reset")
      @CommandCompletion("@players")
      @Description("Reset a player's backpack data")
      @Syntax("<player>")
      public void onAdminReset(CommandSender sender, @Flags("other") Player target) {
          sender.sendMessage("Are you sure? This will delete all backpack data for " + target.getName());
          sender.sendMessage("Run /backpack admin confirmreset " + target.getName() + " to confirm");
      }
  }
  ```

#### **3.3 - Migrate Other Commands**

- [ ] **Migrate EnderChestCommand**
- [ ] **Migrate StorageCommand**
- [ ] **Migrate VaultPackCommand**

Each following the same ACF pattern with:
- `@CommandAlias` for command name
- `@Subcommand` for subcommands
- `@CommandPermission` for permissions
- `@CommandCompletion` for tab completion
- `@Description` for help text

#### **3.4 - Remove Old Command Files**

- [ ] **Update plugin.yml**

  Remove manual command registrations:
  ```yaml
  # DELETE these sections:
  # commands:
  #   backpack:
  #     description: ...
  #     usage: ...
  ```

- [ ] **Remove old registration code**

  Remove from VaultPackPlugin.java:
  ```java
  // DELETE:
  // getCommand("backpack").setExecutor(new BackpackCommand(this));
  // getCommand("backpack").setTabCompleter(new BackpackCommand(this));
  ```

#### **3.5 - Testing**

- [ ] **Test all commands**
  - Test default command (no args)
  - Test all subcommands
  - Test tab completion
  - Test permissions
  - Test with/without arguments
  - Test admin commands

- [ ] **Verify help system**
  ```
  /backpack help
  /backpack ?
  ```

### Deliverables

- ✅ ACF integrated and configured
- ✅ All commands migrated to ACF
- ✅ Tab completion working
- ✅ Help system functional
- ✅ Old command code removed

### Success Criteria

- All commands work identically to before
- Tab completion improved
- Code reduced by ~40-50%
- Help system auto-generated
- No permission issues

### Breaking Changes

**None** - Commands work identically from player perspective.

### Code Reduction Estimate

**Before:** ~500 lines across all commands
**After:** ~250 lines
**Savings:** ~50% reduction + better maintainability

---

## Phase 4: Message & Localization System

**Duration:** 3-4 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Implement Aurora-style placeholder system
2. Add multi-language support
3. Refactor message handling
4. Maintain backward compatibility

### Tasks

#### **4.1 - Create Placeholder System**

- [ ] **Create Placeholder record**

  File: `src/main/java/com/vaultpack/api/Placeholder.java`

  ```java
  package com.vaultpack.api;

  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.ArrayList;
  import java.util.List;

  /**
   * Type-safe placeholder for message formatting.
   *
   * @param key   The placeholder key (e.g., "{player}")
   * @param value The value to replace with
   * @param <T>   The type of the value
   */
  public record Placeholder<T>(@NotNull String key, @Nullable T value) {

      /**
       * Create a new placeholder.
       *
       * @param key   The placeholder key
       * @param value The value
       * @return A new Placeholder instance
       */
      public static <T> Placeholder<T> of(@NotNull String key, @Nullable T value) {
          return new Placeholder<>(key, value);
      }

      /**
       * Execute all placeholders on a string.
       *
       * @param text         The text to process
       * @param placeholders The placeholders to apply
       * @return The processed text
       */
      public static String execute(@NotNull String text, @NotNull Placeholder<?>... placeholders) {
          for (Placeholder<?> placeholder : placeholders) {
              text = text.replace(
                  placeholder.key(),
                  placeholder.value() != null ? String.valueOf(placeholder.value()) : ""
              );
          }
          return text;
      }

      /**
       * Execute all placeholders on a list of strings.
       *
       * @param lines        The lines to process
       * @param placeholders The placeholders to apply
       * @return The processed lines
       */
      public static List<String> executeList(@NotNull List<String> lines, @NotNull Placeholder<?>... placeholders) {
          List<String> result = new ArrayList<>();
          for (String line : lines) {
              result.add(execute(line, placeholders));
          }
          return result;
      }
  }
  ```

#### **4.2 - Create Language Provider**

- [ ] **Create LanguageProvider interface**

  File: `src/main/java/com/vaultpack/api/localization/LanguageProvider.java`

  ```java
  package com.vaultpack.api.localization;

  import org.bukkit.entity.Player;
  import org.jetbrains.annotations.NotNull;

  import java.util.List;
  import java.util.Locale;

  /**
   * Provides language/locale support for players.
   */
  public interface LanguageProvider {

      /**
       * Get the locale for a player.
       *
       * @param player The player
       * @return The player's locale
       */
      @NotNull Locale getPlayerLocale(@NotNull Player player);

      /**
       * Set the locale for a player.
       *
       * @param player The player
       * @param locale The locale
       */
      void setPlayerLocale(@NotNull Player player, @NotNull Locale locale);

      /**
       * Get the fallback locale.
       *
       * @return The fallback locale (usually English)
       */
      @NotNull Locale getFallbackLocale();

      /**
       * Get all supported locales.
       *
       * @return List of supported locales
       */
      @NotNull List<Locale> getSupportedLocales();
  }
  ```

- [ ] **Create default implementation**

  File: `src/main/java/com/vaultpack/localization/VaultPackLanguageProvider.java`

  ```java
  package com.vaultpack.localization;

  import com.vaultpack.api.localization.LanguageProvider;
  import org.bukkit.entity.Player;
  import org.jetbrains.annotations.NotNull;

  import java.util.*;
  import java.util.concurrent.ConcurrentHashMap;

  public class VaultPackLanguageProvider implements LanguageProvider {
      private final Map<UUID, Locale> playerLocales = new ConcurrentHashMap<>();
      private final Locale fallbackLocale = Locale.ENGLISH;
      private final List<Locale> supportedLocales;

      public VaultPackLanguageProvider(List<Locale> supportedLocales) {
          this.supportedLocales = new ArrayList<>(supportedLocales);
          if (!this.supportedLocales.contains(fallbackLocale)) {
              this.supportedLocales.add(fallbackLocale);
          }
      }

      @Override
      public @NotNull Locale getPlayerLocale(@NotNull Player player) {
          return playerLocales.getOrDefault(player.getUniqueId(), fallbackLocale);
      }

      @Override
      public void setPlayerLocale(@NotNull Player player, @NotNull Locale locale) {
          if (supportedLocales.contains(locale)) {
              playerLocales.put(player.getUniqueId(), locale);
          }
      }

      @Override
      public @NotNull Locale getFallbackLocale() {
          return fallbackLocale;
      }

      @Override
      public @NotNull List<Locale> getSupportedLocales() {
          return new ArrayList<>(supportedLocales);
      }

      public void clearPlayerLocale(UUID uuid) {
          playerLocales.remove(uuid);
      }
  }
  ```

#### **4.3 - Refactor Message Manager**

- [ ] **Update MessageManager for multi-language**

  File: `src/main/java/com/vaultpack/managers/MessageManager.java`

  ```java
  package com.vaultpack.managers;

  import com.vaultpack.VaultPackPlugin;
  import com.vaultpack.api.Placeholder;
  import com.vaultpack.api.localization.LanguageProvider;
  import net.kyori.adventure.text.Component;
  import net.kyori.adventure.text.minimessage.MiniMessage;
  import org.bukkit.command.CommandSender;
  import org.bukkit.configuration.file.YamlConfiguration;
  import org.bukkit.entity.Player;
  import org.jetbrains.annotations.NotNull;

  import java.io.File;
  import java.util.*;
  import java.util.concurrent.ConcurrentHashMap;

  public class MessageManager {
      private final VaultPackPlugin plugin;
      private final LanguageProvider languageProvider;
      private final MiniMessage miniMessage;

      // Locale -> (Key -> Message)
      private final Map<Locale, Map<String, String>> messages = new ConcurrentHashMap<>();

      public MessageManager(VaultPackPlugin plugin, LanguageProvider languageProvider) {
          this.plugin = plugin;
          this.languageProvider = languageProvider;
          this.miniMessage = MiniMessage.miniMessage();

          loadMessages();
      }

      /**
       * Load all language files.
       */
      public void loadMessages() {
          messages.clear();

          // Load each supported locale
          for (Locale locale : languageProvider.getSupportedLocales()) {
              loadMessagesForLocale(locale);
          }

          // Ensure fallback locale is loaded
          if (!messages.containsKey(languageProvider.getFallbackLocale())) {
              loadMessagesForLocale(languageProvider.getFallbackLocale());
          }
      }

      /**
       * Load messages for a specific locale.
       */
      private void loadMessagesForLocale(Locale locale) {
          String fileName = "lang_" + locale.getLanguage() + ".yml";
          File file = new File(plugin.getDataFolder(), fileName);

          // Save default if doesn't exist
          if (!file.exists()) {
              plugin.saveResource(fileName, false);
          }

          YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
          Map<String, String> localeMessages = new HashMap<>();

          // Recursively load all keys
          loadKeysRecursively(yaml, "", localeMessages);

          messages.put(locale, localeMessages);
      }

      /**
       * Recursively load all keys from config.
       */
      private void loadKeysRecursively(YamlConfiguration yaml, String prefix, Map<String, String> map) {
          for (String key : yaml.getKeys(false)) {
              String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

              if (yaml.isConfigurationSection(key)) {
                  loadKeysRecursively(yaml.getConfigurationSection(key), fullKey, map);
              } else {
                  map.put(fullKey, yaml.getString(key, ""));
              }
          }
      }

      /**
       * Get a message for a player with placeholders.
       *
       * @param player       The player (determines locale)
       * @param key          The message key
       * @param placeholders The placeholders to apply
       * @return The formatted message
       */
      public String getMessage(@NotNull Player player, @NotNull String key, @NotNull Placeholder<?>... placeholders) {
          Locale locale = languageProvider.getPlayerLocale(player);
          return getMessage(locale, key, placeholders);
      }

      /**
       * Get a message for a specific locale.
       */
      public String getMessage(@NotNull Locale locale, @NotNull String key, @NotNull Placeholder<?>... placeholders) {
          Map<String, String> localeMessages = messages.get(locale);

          // Fallback to default locale if not found
          if (localeMessages == null) {
              localeMessages = messages.get(languageProvider.getFallbackLocale());
          }

          String message = localeMessages.getOrDefault(key, key);

          // Apply placeholders
          message = Placeholder.execute(message, placeholders);

          return message;
      }

      /**
       * Send a message to a player.
       */
      public void send(@NotNull Player player, @NotNull String key, @NotNull Placeholder<?>... placeholders) {
          String message = getMessage(player, key, placeholders);
          Component component = miniMessage.deserialize(translateLegacyColors(message));
          player.sendMessage(component);
      }

      /**
       * Send a message to a command sender.
       */
      public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Placeholder<?>... placeholders) {
          if (sender instanceof Player player) {
              send(player, key, placeholders);
          } else {
              // Console uses fallback locale
              String message = getMessage(languageProvider.getFallbackLocale(), key, placeholders);
              Component component = miniMessage.deserialize(translateLegacyColors(message));
              sender.sendMessage(component);
          }
      }

      /**
       * Get message component for player.
       */
      public Component getComponent(@NotNull Player player, @NotNull String key, @NotNull Placeholder<?>... placeholders) {
          String message = getMessage(player, key, placeholders);
          return miniMessage.deserialize(translateLegacyColors(message));
      }

      /**
       * Translate legacy color codes to MiniMessage format.
       */
      private String translateLegacyColors(String text) {
          // &c -> <red>, &l -> <bold>, etc.
          return text
              .replace("&0", "<black>")
              .replace("&1", "<dark_blue>")
              .replace("&2", "<dark_green>")
              .replace("&3", "<dark_aqua>")
              .replace("&4", "<dark_red>")
              .replace("&5", "<dark_purple>")
              .replace("&6", "<gold>")
              .replace("&7", "<gray>")
              .replace("&8", "<dark_gray>")
              .replace("&9", "<blue>")
              .replace("&a", "<green>")
              .replace("&b", "<aqua>")
              .replace("&c", "<red>")
              .replace("&d", "<light_purple>")
              .replace("&e", "<yellow>")
              .replace("&f", "<white>")
              .replace("&l", "<bold>")
              .replace("&m", "<strikethrough>")
              .replace("&n", "<underlined>")
              .replace("&o", "<italic>")
              .replace("&r", "<reset>");
      }

      /**
       * Reload all messages.
       */
      public void reload() {
          loadMessages();
      }
  }
  ```

#### **4.4 - Create Language Files**

- [ ] **Create lang_en.yml (English)**

  File: `src/main/resources/lang_en.yml`

  ```yaml
  prefix: "<gradient:#FFD700:#FFA500>[VaultPack]</gradient> "

  # Backpack messages
  backpack:
    opened: "<green>Opened backpack slot {slot}"
    closed: "<gray>Backpack closed"
    slot-locked: "<red>Slot {slot} is locked! Unlock it first."
    slot-unlocked: "<green>Unlocked slot {slot}!"
    slot-already-unlocked: "<yellow>Slot {slot} is already unlocked."
    invalid-slot: "<red>Invalid slot number! Must be between 1 and {max}."
    no-backpacks: "<red>You don't have any backpacks!"
    item-blacklisted: "<red>This item cannot be stored in backpacks!"

  # Ender chest messages
  enderchest:
    opened: "<green>Opened ender chest"
    locked: "<red>Ender chest is locked!"

  # Error messages
  error:
    no-permission: "<red>You don't have permission to do that!"
    player-not-found: "<red>Player {player} not found!"
    invalid-command: "<red>Invalid command! Use /backpack help"
    data-load-failed: "<red>Failed to load your data. Please try again."

  # Admin messages
  admin:
    unlocked-for-player: "<green>Unlocked slot {slot} for {player}"
    reset-confirm: "<yellow>Are you sure? Run /backpack admin confirmreset {player} to confirm."
    reset-complete: "<green>Reset all data for {player}"
  ```

- [ ] **Create lang_es.yml (Spanish) - Optional**

  File: `src/main/resources/lang_es.yml`

  ```yaml
  prefix: "<gradient:#FFD700:#FFA500>[VaultPack]</gradient> "

  backpack:
    opened: "<green>Mochila ranura {slot} abierta"
    closed: "<gray>Mochila cerrada"
    slot-locked: "<red>¡La ranura {slot} está bloqueada! Desbloquéala primero."
    # ... etc
  ```

#### **4.5 - Update All Message Calls**

- [ ] **Update managers to use new system**

  Example changes:
  ```java
  // OLD:
  plugin.getMessageManager().send(player, "slot-locked", "%slot%", String.valueOf(slot));

  // NEW:
  plugin.getMessageManager().send(player, "backpack.slot-locked",
      Placeholder.of("{slot}", slot)
  );
  ```

- [ ] **Update all files using messages**
  - BackpackManager
  - EnderChestManager
  - All commands
  - All listeners
  - GUI builders

#### **4.6 - Testing**

- [ ] **Test message system**
  - All messages display correctly
  - Placeholders work
  - Multi-language switching works
  - Fallback to English works
  - MiniMessage formatting works

### Deliverables

- ✅ Placeholder system created
- ✅ Multi-language support added
- ✅ MessageManager refactored
- ✅ All language files created
- ✅ All code updated to use new system

### Success Criteria

- Messages display correctly
- Type-safe placeholders work
- Multi-language functional
- MiniMessage formatting supported
- Backward compatibility maintained

### Breaking Changes

**API Changes:**
- Old `send(player, key, String... replacements)` deprecated
- New `send(player, key, Placeholder... placeholders)` recommended

Migration is straightforward - update call sites to use `Placeholder.of()`.

---

## Phase 5: Data Architecture Refactoring

**Duration:** 5-7 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Implement Data Holder pattern
2. Refactor PlayerBackpackData to composable holders
3. Add dirty tracking
4. Make system extensible for other plugins

### Tasks

#### **5.1 - Create Base Data Holder Classes**

- [ ] **Create DataHolder interface**

  File: `src/main/java/com/vaultpack/api/data/DataHolder.java`

  ```java
  package com.vaultpack.api.data;

  import org.bukkit.configuration.ConfigurationSection;
  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.UUID;

  /**
   * Represents a piece of player data that can be loaded and saved.
   */
  public interface DataHolder {

      /**
       * Get the unique identifier for this data holder.
       *
       * @return The identifier (e.g., "backpacks", "enderchest")
       */
      @NotNull String getId();

      /**
       * Get the player UUID this data belongs to.
       *
       * @return The player UUID
       */
      @NotNull UUID getUuid();

      /**
       * Load data from a configuration section.
       *
       * @param section The configuration section (null if no data exists)
       */
      void loadFrom(@Nullable ConfigurationSection section);

      /**
       * Save data to a configuration section.
       *
       * @param section The configuration section to save to
       */
      void saveTo(@NotNull ConfigurationSection section);

      /**
       * Check if this data holder has unsaved changes.
       *
       * @return true if dirty (needs saving)
       */
      boolean isDirty();

      /**
       * Mark this data holder as clean (no unsaved changes).
       */
      void clearDirty();
  }
  ```

- [ ] **Create abstract UserDataHolder**

  File: `src/main/java/com/vaultpack/data/holder/UserDataHolder.java`

  ```java
  package com.vaultpack.data.holder;

  import com.vaultpack.api.data.DataHolder;
  import lombok.Getter;
  import org.jetbrains.annotations.NotNull;

  import java.util.UUID;
  import java.util.concurrent.atomic.AtomicBoolean;

  /**
   * Base class for all user data holders.
   */
  public abstract class UserDataHolder implements DataHolder {

      @Getter
      protected final UUID uuid;

      private final AtomicBoolean dirty = new AtomicBoolean(false);

      protected UserDataHolder(@NotNull UUID uuid) {
          this.uuid = uuid;
      }

      /**
       * Mark this data as modified (needs saving).
       */
      protected void markDirty() {
          dirty.set(true);
      }

      @Override
      public boolean isDirty() {
          return dirty.get();
      }

      @Override
      public void clearDirty() {
          dirty.set(false);
      }
  }
  ```

#### **5.2 - Create Specific Data Holders**

- [ ] **Create BackpackDataHolder**

  File: `src/main/java/com/vaultpack/data/holder/BackpackDataHolder.java`

  ```java
  package com.vaultpack.data.holder;

  import org.bukkit.configuration.ConfigurationSection;
  import org.bukkit.inventory.ItemStack;
  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.Map;
  import java.util.Set;
  import java.util.UUID;
  import java.util.concurrent.ConcurrentHashMap;

  /**
   * Holds backpack data for a player.
   */
  public class BackpackDataHolder extends UserDataHolder {

      private final Map<Integer, ItemStack[]> backpacks = new ConcurrentHashMap<>();
      private final Map<Integer, String> backpackTypes = new ConcurrentHashMap<>();
      private final Set<Integer> unlockedSlots = ConcurrentHashMap.newKeySet();

      public BackpackDataHolder(@NotNull UUID uuid) {
          super(uuid);
      }

      @Override
      public @NotNull String getId() {
          return "backpacks";
      }

      @Override
      public void loadFrom(@Nullable ConfigurationSection section) {
          if (section == null) return;

          // Load unlocked slots
          if (section.contains("unlocked-slots")) {
              unlockedSlots.addAll(section.getIntegerList("unlocked-slots"));
          }

          // Load backpack types
          ConfigurationSection typesSection = section.getConfigurationSection("types");
          if (typesSection != null) {
              for (String key : typesSection.getKeys(false)) {
                  int slot = Integer.parseInt(key);
                  String type = typesSection.getString(key);
                  backpackTypes.put(slot, type);
              }
          }

          // Load backpack contents
          ConfigurationSection contentsSection = section.getConfigurationSection("contents");
          if (contentsSection != null) {
              for (String key : contentsSection.getKeys(false)) {
                  int slot = Integer.parseInt(key);
                  @SuppressWarnings("unchecked")
                  List<ItemStack> items = (List<ItemStack>) contentsSection.getList(key);
                  if (items != null) {
                      backpacks.put(slot, items.toArray(new ItemStack[0]));
                  }
              }
          }

          clearDirty();
      }

      @Override
      public void saveTo(@NotNull ConfigurationSection section) {
          // Save unlocked slots
          section.set("unlocked-slots", new ArrayList<>(unlockedSlots));

          // Save backpack types
          ConfigurationSection typesSection = section.createSection("types");
          for (Map.Entry<Integer, String> entry : backpackTypes.entrySet()) {
              typesSection.set(String.valueOf(entry.getKey()), entry.getValue());
          }

          // Save backpack contents
          ConfigurationSection contentsSection = section.createSection("contents");
          for (Map.Entry<Integer, ItemStack[]> entry : backpacks.entrySet()) {
              contentsSection.set(String.valueOf(entry.getKey()), Arrays.asList(entry.getValue()));
          }

          clearDirty();
      }

      // Getters and setters with dirty marking

      public boolean hasSlotUnlocked(int slot) {
          return unlockedSlots.contains(slot);
      }

      public void unlockSlot(int slot) {
          if (unlockedSlots.add(slot)) {
              markDirty();
          }
      }

      public Set<Integer> getUnlockedSlots() {
          return Set.copyOf(unlockedSlots);
      }

      @Nullable
      public ItemStack[] getBackpackContents(int slot) {
          return backpacks.get(slot);
      }

      public void setBackpackContents(int slot, @NotNull ItemStack[] contents) {
          backpacks.put(slot, contents);
          markDirty();
      }

      @Nullable
      public String getBackpackType(int slot) {
          return backpackTypes.get(slot);
      }

      public void setBackpackType(int slot, @NotNull String type) {
          backpackTypes.put(slot, type);
          markDirty();
      }

      public boolean hasBackpack(int slot) {
          return backpacks.containsKey(slot);
      }

      public void removeBackpack(int slot) {
          if (backpacks.remove(slot) != null || backpackTypes.remove(slot) != null) {
              markDirty();
          }
      }

      public void clearAll() {
          backpacks.clear();
          backpackTypes.clear();
          unlockedSlots.clear();
          markDirty();
      }
  }
  ```

- [ ] **Create EnderChestDataHolder**

  File: `src/main/java/com/vaultpack/data/holder/EnderChestDataHolder.java`

  ```java
  package com.vaultpack.data.holder;

  import org.bukkit.configuration.ConfigurationSection;
  import org.bukkit.inventory.ItemStack;
  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.Arrays;
  import java.util.List;
  import java.util.UUID;

  /**
   * Holds ender chest data for a player.
   */
  public class EnderChestDataHolder extends UserDataHolder {

      private ItemStack[] contents;
      private boolean unlocked = false;

      public EnderChestDataHolder(@NotNull UUID uuid) {
          super(uuid);
      }

      @Override
      public @NotNull String getId() {
          return "enderchest";
      }

      @Override
      public void loadFrom(@Nullable ConfigurationSection section) {
          if (section == null) return;

          unlocked = section.getBoolean("unlocked", false);

          @SuppressWarnings("unchecked")
          List<ItemStack> items = (List<ItemStack>) section.getList("contents");
          if (items != null) {
              contents = items.toArray(new ItemStack[0]);
          }

          clearDirty();
      }

      @Override
      public void saveTo(@NotNull ConfigurationSection section) {
          section.set("unlocked", unlocked);
          if (contents != null) {
              section.set("contents", Arrays.asList(contents));
          }

          clearDirty();
      }

      @Nullable
      public ItemStack[] getContents() {
          return contents;
      }

      public void setContents(@NotNull ItemStack[] contents) {
          this.contents = contents;
          markDirty();
      }

      public boolean isUnlocked() {
          return unlocked;
      }

      public void setUnlocked(boolean unlocked) {
          if (this.unlocked != unlocked) {
              this.unlocked = unlocked;
              markDirty();
          }
      }
  }
  ```

- [ ] **Create StatisticsDataHolder (Optional)**

  File: `src/main/java/com/vaultpack/data/holder/StatisticsDataHolder.java`

  ```java
  package com.vaultpack.data.holder;

  import org.bukkit.configuration.ConfigurationSection;
  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.UUID;

  /**
   * Holds statistics for a player.
   */
  public class StatisticsDataHolder extends UserDataHolder {

      private long totalBackpackOpens = 0;
      private long totalItemsStored = 0;
      private long totalItemsRetrieved = 0;

      public StatisticsDataHolder(@NotNull UUID uuid) {
          super(uuid);
      }

      @Override
      public @NotNull String getId() {
          return "statistics";
      }

      @Override
      public void loadFrom(@Nullable ConfigurationSection section) {
          if (section == null) return;

          totalBackpackOpens = section.getLong("backpack-opens", 0);
          totalItemsStored = section.getLong("items-stored", 0);
          totalItemsRetrieved = section.getLong("items-retrieved", 0);

          clearDirty();
      }

      @Override
      public void saveTo(@NotNull ConfigurationSection section) {
          section.set("backpack-opens", totalBackpackOpens);
          section.set("items-stored", totalItemsStored);
          section.set("items-retrieved", totalItemsRetrieved);

          clearDirty();
      }

      public void incrementBackpackOpens() {
          totalBackpackOpens++;
          markDirty();
      }

      public void incrementItemsStored(int count) {
          totalItemsStored += count;
          markDirty();
      }

      public void incrementItemsRetrieved(int count) {
          totalItemsRetrieved += count;
          markDirty();
      }

      // Getters
      public long getTotalBackpackOpens() { return totalBackpackOpens; }
      public long getTotalItemsStored() { return totalItemsStored; }
      public long getTotalItemsRetrieved() { return totalItemsRetrieved; }
  }
  ```

#### **5.3 - Create VaultPackUser**

- [ ] **Create VaultPackUser container class**

  File: `src/main/java/com/vaultpack/data/VaultPackUser.java`

  ```java
  package com.vaultpack.data;

  import com.vaultpack.api.data.DataHolder;
  import com.vaultpack.data.holder.UserDataHolder;
  import lombok.Getter;
  import org.bukkit.configuration.ConfigurationSection;
  import org.bukkit.configuration.file.YamlConfiguration;
  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;
  import java.util.concurrent.atomic.AtomicBoolean;

  /**
   * Represents all data for a player.
   */
  public class VaultPackUser {

      @Getter
      private final UUID uuid;

      private final Map<String, UserDataHolder> dataHolders = new HashMap<>();

      @Getter
      private final AtomicBoolean loaded = new AtomicBoolean(false);

      public VaultPackUser(@NotNull UUID uuid) {
          this.uuid = uuid;
      }

      /**
       * Register a data holder.
       *
       * @param holder The data holder to register
       */
      public void addDataHolder(@NotNull UserDataHolder holder) {
          dataHolders.put(holder.getId(), holder);
      }

      /**
       * Get a data holder by its class.
       *
       * @param holderClass The holder class
       * @return The data holder, or null if not found
       */
      @Nullable
      @SuppressWarnings("unchecked")
      public <T extends UserDataHolder> T getData(@NotNull Class<T> holderClass) {
          for (UserDataHolder holder : dataHolders.values()) {
              if (holderClass.isInstance(holder)) {
                  return (T) holder;
              }
          }
          return null;
      }

      /**
       * Get a data holder by its ID.
       *
       * @param id The holder ID
       * @return The data holder, or null if not found
       */
      @Nullable
      public UserDataHolder getData(@NotNull String id) {
          return dataHolders.get(id);
      }

      /**
       * Load all data from a configuration.
       *
       * @param yaml The configuration to load from
       */
      public void loadFrom(@NotNull YamlConfiguration yaml) {
          for (UserDataHolder holder : dataHolders.values()) {
              ConfigurationSection section = yaml.getConfigurationSection(holder.getId());
              holder.loadFrom(section);
          }
          loaded.set(true);
      }

      /**
       * Save all data to a configuration.
       *
       * @return The configuration with saved data
       */
      public YamlConfiguration saveTo() {
          YamlConfiguration yaml = new YamlConfiguration();

          for (UserDataHolder holder : dataHolders.values()) {
              ConfigurationSection section = yaml.createSection(holder.getId());
              holder.saveTo(section);
          }

          return yaml;
      }

      /**
       * Check if any data holder is dirty.
       *
       * @return true if any holder needs saving
       */
      public boolean isDirty() {
          return dataHolders.values().stream().anyMatch(DataHolder::isDirty);
      }

      /**
       * Clear dirty flag on all holders.
       */
      public void clearDirty() {
          dataHolders.values().forEach(DataHolder::clearDirty);
      }
  }
  ```

#### **5.4 - Refactor Data Managers**

- [ ] **Update BackpackDataManager**

  Major changes to support VaultPackUser:
  - Change cache from `PlayerBackpackData` to `VaultPackUser`
  - Update all methods to work with data holders
  - Maintain backward compatibility through wrapper methods

- [ ] **Create UserManager**

  File: `src/main/java/com/vaultpack/managers/UserManager.java`

  ```java
  package com.vaultpack.managers;

  import com.github.benmanes.caffeine.cache.Cache;
  import com.github.benmanes.caffeine.cache.Caffeine;
  import com.vaultpack.VaultPackPlugin;
  import com.vaultpack.data.VaultPackUser;
  import com.vaultpack.data.holder.*;
  import lombok.Getter;
  import org.bukkit.Bukkit;
  import org.bukkit.entity.Player;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.Listener;
  import org.bukkit.event.player.PlayerJoinEvent;
  import org.bukkit.event.player.PlayerQuitEvent;

  import java.util.UUID;
  import java.util.concurrent.CompletableFuture;
  import java.util.concurrent.TimeUnit;

  /**
   * Manages VaultPackUser instances.
   */
  public class UserManager implements Listener {

      private final VaultPackPlugin plugin;

      private final Cache<UUID, VaultPackUser> cache;

      @Getter
      private final BackpackDataManager dataManager;

      public UserManager(VaultPackPlugin plugin) {
          this.plugin = plugin;
          this.dataManager = plugin.getBackpackDataManager();

          // Create cache
          this.cache = Caffeine.newBuilder()
              .maximumSize(plugin.getConfigManager().getMainConfig().getCacheSize())
              .expireAfterAccess(plugin.getConfigManager().getMainConfig().getCacheDuration(), TimeUnit.MINUTES)
              .build();

          // Register listener
          Bukkit.getPluginManager().registerEvents(this, plugin);
      }

      /**
       * Get or create a user.
       *
       * @param uuid The player UUID
       * @return The user
       */
      @NotNull
      public VaultPackUser getUser(@NotNull UUID uuid) {
          return cache.get(uuid, this::createUser);
      }

      /**
       * Create a new user with all data holders.
       */
      private VaultPackUser createUser(UUID uuid) {
          VaultPackUser user = new VaultPackUser(uuid);

          // Register all data holders
          user.addDataHolder(new BackpackDataHolder(uuid));
          user.addDataHolder(new EnderChestDataHolder(uuid));
          user.addDataHolder(new StatisticsDataHolder(uuid));

          return user;
      }

      /**
       * Load a user's data asynchronously.
       */
      public CompletableFuture<VaultPackUser> loadUserAsync(@NotNull UUID uuid) {
          return CompletableFuture.supplyAsync(() -> {
              VaultPackUser user = getUser(uuid);

              // Load from storage
              YamlConfiguration yaml = dataManager.loadRawData(uuid);
              if (yaml != null) {
                  user.loadFrom(yaml);
              }

              return user;
          });
      }

      /**
       * Save a user's data asynchronously.
       */
      public CompletableFuture<Boolean> saveUserAsync(@NotNull VaultPackUser user) {
          if (!user.isDirty()) {
              return CompletableFuture.completedFuture(true);
          }

          return CompletableFuture.supplyAsync(() -> {
              YamlConfiguration yaml = user.saveTo();
              return dataManager.saveRawData(user.getUuid(), yaml);
          });
      }

      @EventHandler
      public void onJoin(PlayerJoinEvent event) {
          loadUserAsync(event.getPlayer().getUniqueId());
      }

      @EventHandler
      public void onQuit(PlayerQuitEvent event) {
          UUID uuid = event.getPlayer().getUniqueId();
          VaultPackUser user = cache.getIfPresent(uuid);

          if (user != null && user.isDirty()) {
              saveUserAsync(user).join(); // Block until saved
          }

          cache.invalidate(uuid);
      }
  }
  ```

#### **5.5 - Update Managers to Use Data Holders**

- [ ] **Update BackpackManager**

  Change all methods to work with data holders:
  ```java
  public void openBackpack(Player player, int slot) {
      VaultPackUser user = plugin.getUserManager().getUser(player.getUniqueId());
      BackpackDataHolder data = user.getData(BackpackDataHolder.class);

      if (!data.hasSlotUnlocked(slot)) {
          // Handle locked
          return;
      }

      ItemStack[] contents = data.getBackpackContents(slot);
      // ... open GUI
  }
  ```

- [ ] **Update EnderChestManager similarly**

#### **5.6 - Testing**

- [ ] **Test data loading/saving**
  - Create backpacks
  - Restart server
  - Verify data persists
  - Check dirty tracking works

- [ ] **Test backward compatibility**
  - Old data migrates correctly
  - No data loss
  - Performance acceptable

### Deliverables

- ✅ Data holder pattern implemented
- ✅ All data types converted to holders
- ✅ VaultPackUser created
- ✅ Dirty tracking functional
- ✅ All managers updated

### Success Criteria

- Data loads and saves correctly
- Dirty tracking reduces I/O
- System extensible for other plugins
- No data loss during migration
- Performance maintained or improved

### Breaking Changes

**Internal API:**
- `PlayerBackpackData` deprecated (keep for compatibility)
- New `VaultPackUser` is primary data container

**External API:**
- Public API methods maintain signatures
- Data accessors work identically

---

## Phase 6: API Refactoring & Facade Pattern

**Duration:** 2-3 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Create clear API/implementation boundary
2. Implement facade pattern
3. Improve API documentation
4. Package reorganization

### Tasks

#### **6.1 - Package Reorganization**

- [ ] **Move internal classes**

  Create new package structure:
  ```
  com.vaultpack/
  ├── api/                      # PUBLIC API ONLY
  │   ├── VaultPackAPI.java
  │   ├── data/
  │   │   └── DataHolder.java
  │   ├── events/
  │   │   ├── BackpackOpenEvent.java
  │   │   └── ...
  │   ├── localization/
  │   │   └── LanguageProvider.java
  │   └── Placeholder.java
  │
  ├── internal/                 # IMPLEMENTATION (not for external use)
  │   ├── commands/
  │   ├── config/
  │   ├── data/
  │   ├── gui/
  │   ├── listeners/
  │   └── managers/
  │
  └── VaultPackPlugin.java      # Main class
  ```

- [ ] **Add package-info.java**

  File: `src/main/java/com/vaultpack/api/package-info.java`

  ```java
  /**
   * VaultPack Public API
   *
   * <p>This package contains the public API for VaultPack. All classes in this
   * package are safe to use in external plugins and will maintain backward
   * compatibility across minor versions.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   * VaultPackAPI api = VaultPackAPI.getInstance();
   * api.openBackpack(player, 1);
   * }</pre>
   *
   * @since 2.0.0
   */
  package com.vaultpack.api;
  ```

#### **6.2 - Enhance VaultPackAPI**

- [ ] **Expand facade methods**

  File: `src/main/java/com/vaultpack/api/VaultPackAPI.java`

  ```java
  package com.vaultpack.api;

  import com.vaultpack.VaultPackPlugin;
  import com.vaultpack.api.data.DataHolder;
  import com.vaultpack.data.VaultPackUser;
  import org.bukkit.entity.Player;
  import org.jetbrains.annotations.NotNull;
  import org.jetbrains.annotations.Nullable;

  import java.util.Set;
  import java.util.UUID;

  /**
   * Main API class for VaultPack.
   *
   * <p>This class provides a simplified interface for interacting with VaultPack
   * from external plugins. All methods are thread-safe and Folia-compatible.</p>
   *
   * @since 1.0.0
   */
  public class VaultPackAPI {

      private static VaultPackAPI instance;
      private final VaultPackPlugin plugin;

      private VaultPackAPI(VaultPackPlugin plugin) {
          this.plugin = plugin;
      }

      /**
       * Initialize the API.
       *
       * @param plugin The plugin instance
       */
      public static void init(VaultPackPlugin plugin) {
          instance = new VaultPackAPI(plugin);
      }

      /**
       * Get the API instance.
       *
       * @return The API instance
       * @throws IllegalStateException if API not initialized
       */
      public static VaultPackAPI getInstance() {
          if (instance == null) {
              throw new IllegalStateException("VaultPackAPI not initialized!");
          }
          return instance;
      }

      // ========== Backpack Methods ==========

      /**
       * Open a backpack for a player.
       *
       * @param player The player
       * @param slot   The slot number (1-18)
       * @return true if opened successfully
       */
      public boolean openBackpack(@NotNull Player player, int slot) {
          return plugin.getBackpackManager().openBackpack(player, slot);
      }

      /**
       * Check if a player has a slot unlocked.
       *
       * @param uuid The player UUID
       * @param slot The slot number
       * @return true if unlocked
       */
      public boolean hasSlotUnlocked(@NotNull UUID uuid, int slot) {
          return plugin.getBackpackManager().hasSlotUnlocked(uuid, slot);
      }

      /**
       * Unlock a slot for a player.
       *
       * @param player The player
       * @param slot   The slot number
       * @return true if unlocked successfully
       */
      public boolean unlockSlot(@NotNull Player player, int slot) {
          return plugin.getBackpackManager().unlockSlot(player, slot);
      }

      /**
       * Get all unlocked slots for a player.
       *
       * @param uuid The player UUID
       * @return Set of unlocked slot numbers
       */
      @NotNull
      public Set<Integer> getUnlockedSlots(@NotNull UUID uuid) {
          return plugin.getBackpackManager().getUnlockedSlots(uuid);
      }

      /**
       * Get the type of a backpack in a slot.
       *
       * @param uuid The player UUID
       * @param slot The slot number
       * @return The backpack type ID, or null if none
       */
      @Nullable
      public String getBackpackType(@NotNull UUID uuid, int slot) {
          return plugin.getBackpackManager().getBackpackType(uuid, slot);
      }

      /**
       * Check if a player has a backpack in a slot.
       *
       * @param uuid The player UUID
       * @param slot The slot number
       * @return true if backpack exists
       */
      public boolean hasBackpack(@NotNull UUID uuid, int slot) {
          return plugin.getBackpackManager().hasBackpack(uuid, slot);
      }

      // ========== Data Access ==========

      /**
       * Get a player's VaultPack user data.
       *
       * @param uuid The player UUID
       * @return The user data
       */
      @NotNull
      public VaultPackUser getUser(@NotNull UUID uuid) {
          return plugin.getUserManager().getUser(uuid);
      }

      /**
       * Get a specific data holder for a player.
       *
       * @param uuid        The player UUID
       * @param holderClass The data holder class
       * @return The data holder, or null if not found
       */
      @Nullable
      public <T extends DataHolder> T getUserData(@NotNull UUID uuid, @NotNull Class<T> holderClass) {
          VaultPackUser user = getUser(uuid);
          return user.getData(holderClass);
      }

      // ========== Configuration ==========

      /**
       * Get the maximum number of backpack slots.
       *
       * @return The max slots
       */
      public int getMaxBackpackSlots() {
          return plugin.getConfigManager().getMaxBackpackSlots();
      }

      /**
       * Reload all configurations.
       */
      public void reloadConfigs() {
          plugin.getConfigManager().reloadConfigs();
      }

      // ========== Utility Methods ==========

      /**
       * Check if an item is blacklisted.
       *
       * @param item The item to check
       * @return true if blacklisted
       */
      public boolean isItemBlacklisted(@NotNull org.bukkit.inventory.ItemStack item) {
          return plugin.getBackpackManager().isItemBlacklisted(item);
      }

      /**
       * Get the plugin version.
       *
       * @return The version string
       */
      @NotNull
      public String getVersion() {
          return plugin.getDescription().getVersion();
      }
  }
  ```

#### **6.3 - Documentation**

- [ ] **Add comprehensive Javadoc**
  - All public API methods documented
  - Include examples
  - Document exceptions
  - Add @since tags

- [ ] **Create API usage guide**

  File: `API_GUIDE.md`

  ```markdown
  # VaultPack API Guide

  ## Getting Started

  Add VaultPack as a dependency in your `build.gradle.kts`:

  ```kotlin
  repositories {
      maven("https://jitpack.io")
  }

  dependencies {
      compileOnly("com.github.yourusername:VaultPack:2.0.0")
  }
  ```

  ## Basic Usage

  ### Opening a Backpack

  ```java
  import com.vaultpack.api.VaultPackAPI;

  public class MyPlugin {
      public void openPlayerBackpack(Player player) {
          VaultPackAPI api = VaultPackAPI.getInstance();
          api.openBackpack(player, 1);
      }
  }
  ```

  ### Checking Unlocked Slots

  ```java
  Set<Integer> slots = api.getUnlockedSlots(player.getUniqueId());
  player.sendMessage("You have " + slots.size() + " unlocked slots!");
  ```

  ### Listening to Events

  ```java
  @EventHandler
  public void onBackpackOpen(BackpackOpenEvent event) {
      Player player = event.getPlayer();
      int slot = event.getSlot();
      // Do something
  }
  ```

  ## Advanced Usage

  ### Custom Data Holders

  You can extend VaultPack's data system:

  ```java
  public class MyCustomDataHolder extends UserDataHolder {
      private int customValue;

      @Override
      public String getId() {
          return "myplugin_custom";
      }

      @Override
      public void loadFrom(ConfigurationSection section) {
          customValue = section.getInt("value", 0);
      }

      @Override
      public void saveTo(ConfigurationSection section) {
          section.set("value", customValue);
      }
  }

  // Register it:
  VaultPackUser user = api.getUser(player.getUniqueId());
  user.addDataHolder(new MyCustomDataHolder(player.getUniqueId()));
  ```
  ```

#### **6.4 - Testing**

- [ ] **Test API from external plugin**
  - Create test plugin
  - Use all public API methods
  - Verify no internal dependencies needed

### Deliverables

- ✅ Clean API/implementation boundary
- ✅ Enhanced VaultPackAPI facade
- ✅ Comprehensive Javadoc
- ✅ API usage guide created

### Success Criteria

- External plugins can use API without accessing internals
- All public methods documented
- API guide complete and accurate

---

## Phase 7: Expansion System (Optional)

**Duration:** 3-4 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Create expansion framework
2. Build sample expansions
3. Document expansion API

### Tasks

#### **7.1 - Create Expansion Framework**

- [ ] **Create Expansion interface**

  File: `src/main/java/com/vaultpack/api/expansion/VaultPackExpansion.java`

  ```java
  package com.vaultpack.api.expansion;

  import com.vaultpack.VaultPackPlugin;
  import org.jetbrains.annotations.NotNull;

  /**
   * Represents an expansion for VaultPack.
   *
   * <p>Expansions can add new features, data holders, and functionality
   * to VaultPack without modifying the core plugin.</p>
   */
  public interface VaultPackExpansion {

      /**
       * Get the unique ID of this expansion.
       *
       * @return The expansion ID (lowercase, no spaces)
       */
      @NotNull String getId();

      /**
       * Get the display name of this expansion.
       *
       * @return The display name
       */
      @NotNull String getName();

      /**
       * Get the version of this expansion.
       *
       * @return The version string
       */
      @NotNull String getVersion();

      /**
       * Get the author(s) of this expansion.
       *
       * @return The author name(s)
       */
      @NotNull String getAuthor();

      /**
       * Called when the expansion is enabled.
       *
       * @param plugin The VaultPack plugin instance
       */
      void onEnable(@NotNull VaultPackPlugin plugin);

      /**
       * Called when the expansion is disabled.
       */
      void onDisable();

      /**
       * Check if this expansion is enabled.
       *
       * @return true if enabled
       */
      boolean isEnabled();

      /**
       * Set whether this expansion is enabled.
       *
       * @param enabled true to enable, false to disable
       */
      void setEnabled(boolean enabled);
  }
  ```

- [ ] **Create ExpansionManager**

  File: `src/main/java/com/vaultpack/internal/managers/ExpansionManager.java`

  ```java
  package com.vaultpack.internal.managers;

  import com.vaultpack.VaultPackPlugin;
  import com.vaultpack.api.expansion.VaultPackExpansion;
  import lombok.Getter;

  import java.util.HashMap;
  import java.util.Map;
  import java.util.Optional;

  public class ExpansionManager {
      private final VaultPackPlugin plugin;

      @Getter
      private final Map<String, VaultPackExpansion> expansions = new HashMap<>();

      public ExpansionManager(VaultPackPlugin plugin) {
          this.plugin = plugin;
      }

      /**
       * Register an expansion.
       */
      public void registerExpansion(VaultPackExpansion expansion) {
          if (expansions.containsKey(expansion.getId())) {
              plugin.getLogger().warning("Expansion " + expansion.getId() + " already registered!");
              return;
          }

          expansions.put(expansion.getId(), expansion);
          expansion.onEnable(plugin);

          plugin.getLogger().info("Registered expansion: " + expansion.getName() + " v" + expansion.getVersion());
      }

      /**
       * Get an expansion by ID.
       */
      public Optional<VaultPackExpansion> getExpansion(String id) {
          return Optional.ofNullable(expansions.get(id));
      }

      /**
       * Disable all expansions.
       */
      public void disableAll() {
          for (VaultPackExpansion expansion : expansions.values()) {
              expansion.onDisable();
          }
          expansions.clear();
      }
  }
  ```

#### **7.2 - Create Sample Expansion (Statistics)**

- [ ] **Create StatisticsExpansion**

  File: `src/main/java/com/vaultpack/internal/expansions/StatisticsExpansion.java`

  ```java
  package com.vaultpack.internal.expansions;

  import com.vaultpack.VaultPackPlugin;
  import com.vaultpack.api.expansion.VaultPackExpansion;
  import com.vaultpack.api.events.BackpackOpenEvent;
  import org.bukkit.Bukkit;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.Listener;
  import org.jetbrains.annotations.NotNull;

  /**
   * Built-in expansion for tracking player statistics.
   */
  public class StatisticsExpansion implements VaultPackExpansion, Listener {

      private VaultPackPlugin plugin;
      private boolean enabled = false;

      @Override
      public @NotNull String getId() {
          return "statistics";
      }

      @Override
      public @NotNull String getName() {
          return "Statistics";
      }

      @Override
      public @NotNull String getVersion() {
          return "1.0.0";
      }

      @Override
      public @NotNull String getAuthor() {
          return "VaultPack Team";
      }

      @Override
      public void onEnable(@NotNull VaultPackPlugin plugin) {
          this.plugin = plugin;
          this.enabled = true;

          // Register listener
          Bukkit.getPluginManager().registerEvents(this, plugin);
      }

      @Override
      public void onDisable() {
          this.enabled = false;
      }

      @Override
      public boolean isEnabled() {
          return enabled;
      }

      @Override
      public void setEnabled(boolean enabled) {
          this.enabled = enabled;
      }

      @EventHandler
      public void onBackpackOpen(BackpackOpenEvent event) {
          if (!enabled) return;

          // Increment statistics
          VaultPackUser user = plugin.getUserManager().getUser(event.getPlayer().getUniqueId());
          StatisticsDataHolder stats = user.getData(StatisticsDataHolder.class);

          if (stats != null) {
              stats.incrementBackpackOpens();
          }
      }
  }
  ```

#### **7.3 - Register Built-in Expansions**

- [ ] **Update VaultPackPlugin to register expansions**

  ```java
  @Override
  public void onEnable() {
      // ... existing code

      // Initialize expansion manager
      expansionManager = new ExpansionManager(this);

      // Register built-in expansions
      if (configManager.getMainConfig().isEnableStatistics()) {
          expansionManager.registerExpansion(new StatisticsExpansion());
      }
  }
  ```

### Deliverables

- ✅ Expansion framework created
- ✅ ExpansionManager functional
- ✅ Sample expansion (Statistics) working
- ✅ Expansion documentation

### Success Criteria

- Expansions can be registered and enabled
- Statistics expansion tracks data
- Other plugins can create custom expansions

---

## Phase 8: Polish, Documentation & Release

**Duration:** 2-3 days
**Status:** ⬜ Not Started
**Completion Date:** -

### Objectives

1. Final testing and bug fixes
2. Complete documentation
3. Performance optimization
4. Prepare release

### Tasks

- [ ] **8.1 - Comprehensive Testing**
  - Test all features end-to-end
  - Test with multiple players
  - Test data migration from v1
  - Performance testing
  - Memory leak testing

- [ ] **8.2 - Update README**
  - Features list
  - Installation instructions
  - Configuration guide
  - API documentation link

- [ ] **8.3 - Create CHANGELOG**
  - Document all changes from v1 to v2
  - Migration guide
  - Breaking changes (if any)

- [ ] **8.4 - Performance Optimization**
  - Profile with JProfiler/YourKit
  - Optimize hot paths
  - Review cache sizes
  - Optimize database queries

- [ ] **8.5 - Code Cleanup**
  - Remove dead code
  - Remove debug logging
  - Remove Phase comments
  - Clean up TODOs

- [ ] **8.6 - Final Build**
  - Update version to 2.0.0
  - Build final JAR
  - Test on clean server
  - Verify all features work

- [ ] **8.7 - Release Preparation**
  - Create GitHub release
  - Upload to distribution platforms
  - Announce release

### Deliverables

- ✅ All bugs fixed
- ✅ Documentation complete
- ✅ Performance optimized
- ✅ v2.0.0 released

---

## Migration Best Practices

### During Each Phase

1. **Create feature branch** for the phase
2. **Commit frequently** with descriptive messages
3. **Test thoroughly** before moving to next phase
4. **Maintain backward compatibility** unless explicitly breaking
5. **Document changes** in code comments
6. **Update this guide** with completion dates and notes

### Testing Strategy

1. **Unit testing** individual components
2. **Integration testing** between phases
3. **Manual testing** on test server
4. **Performance testing** with realistic data
5. **Regression testing** to ensure no breaks

### Rollback Strategy

1. Each phase in separate branch
2. Tag before starting each phase
3. Keep old code commented until phase complete
4. Maintain v1 branch for emergency rollback

---

## Post-Migration Goals

After completing the migration:

1. **Community Feedback**
   - Gather feedback from users
   - Address issues quickly
   - Iterate on pain points

2. **Additional Features**
   - Implement user-requested features
   - Expand expansion system
   - Add more data holders

3. **Performance Monitoring**
   - Track performance metrics
   - Optimize based on real usage
   - Monitor error rates

4. **Maintenance**
   - Keep dependencies updated
   - Stay compatible with latest Paper
   - Fix bugs promptly

---

## Notes Section

### Phase Completion Notes

Add notes here as each phase is completed:

**Phase 0:**
- Completed: 2026-01-08
- Notes:
  - Successfully set up Run-Paper testing infrastructure
  - Git configured with user credentials (joogiebear)
  - Created feature branch: `feature/enterprise-refactoring`
  - Removed reference folders from repository (.claude, Aurora, GitBook) - kept locally for reference
  - Created comprehensive testing checklist for all phases
  - Ready to begin Phase 1 (Build & Dependencies)

**Phase 1:**
- Completed: [Date]
- Notes: [Any issues or learnings]

[etc...]

---

## Resources

- Aurora Repository: `C:\Users\e85sr\Documents\GitHub\VaultPack\Aurora`
- Aurora Analysis: `AURORA_COMPARISON.md`
- ACF Documentation: https://github.com/aikar/commands/wiki
- Paper API Docs: https://jd.papermc.io/paper/1.21/
- MiniMessage Docs: https://docs.advntr.dev/minimessage/

---

**This is a living document. Update it as you progress through each phase!**
