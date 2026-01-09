# Developer API

Complete guide to using the VaultPack API in your plugins.

## Getting Started

VaultPack provides a comprehensive API for developers to integrate backpack functionality into their own plugins.

### Adding VaultPack as a Dependency

#### Maven

Add JitPack repository:
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

Add VaultPack dependency:
```xml
<dependency>
    <groupId>com.github.yourusername</groupId>
    <artifactId>VaultPack</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.yourusername:VaultPack:2.0.0'
}
```

#### plugin.yml

Add VaultPack as a dependency:
```yaml
depend: [VaultPack]
# or as soft dependency
softdepend: [VaultPack]
```

---

## API Instance

### Getting the API

```java
import com.vaultpack.api.VaultPackAPI;

VaultPackAPI api = VaultPackAPI.getInstance();

if (api != null) {
    // VaultPack is loaded and ready
}
```

**Note:** Always check if the API instance is not null before using it, especially if you're using soft dependencies.

---

## Backpack Management

### Check if Player has Backpack

```java
Player player = ...;
int slotNumber = 1;

if (api.hasBackpack(player, slotNumber)) {
    // Player has a backpack in slot 1
}

// Alternative with UUID
UUID uuid = player.getUniqueId();
if (api.hasBackpack(uuid, slotNumber)) {
    // Player has a backpack
}
```

---

### Open Backpack

```java
Player player = ...;
int slotNumber = 1;

// Open specific backpack slot
api.openBackpack(player, slotNumber);

// Open backpack selector menu
api.openBackpackMenu(player);
```

---

### Create Backpack

```java
import com.vaultpack.models.BackpackTier;

Player player = ...;
int slotNumber = 1;

// Get tier from tier manager
VaultPackPlugin plugin = api.getPlugin();
BackpackTier tier = plugin.getBackpackTypeManager().getBackpackTier("small");

// Create backpack
api.createBackpack(player, slotNumber, tier);
```

---

### Get Backpack Contents

```java
Player player = ...;
int slotNumber = 1;

Map<Integer, ItemStack> contents = api.getBackpackContents(player, slotNumber);

if (contents != null) {
    for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
        int slot = entry.getKey();
        ItemStack item = entry.getValue();
        // Process backpack contents
    }
}
```

---

### Get Backpack Information

```java
Player player = ...;
int slotNumber = 1;

// Get size
int size = api.getBackpackSize(player, slotNumber);
// Returns: 9, 18, 27, 36, 45, or 0 if no backpack

// Get tier
BackpackTier tier = api.getBackpackTier(player, slotNumber);
if (tier != null) {
    String tierName = tier.getName();      // "small", "medium", etc.
    String displayName = tier.getDisplayName(); // "&aSmall Backpack"
}
```

---

## Slot Management

### Check if Slot is Unlocked

```java
Player player = ...;
int slotNumber = 5;

if (api.isSlotUnlocked(player, slotNumber)) {
    // Slot 5 is unlocked for this player
}
```

---

### Unlock Slot Programmatically

```java
Player player = ...;
int slotNumber = 5;

// Unlock slot (bypasses economy and permissions)
api.unlockSlot(player, slotNumber);
```

**Warning:** This bypasses all checks! Use with caution.

---

### Get Unlocked Slot Count

```java
Player player = ...;

int unlockedSlots = api.getUnlockedSlots(player);
// Returns: 1-18
```

---

## Player Statistics

### Get Active Backpack Count

```java
Player player = ...;

int activeBackpacks = api.getActiveBackpackCount(player);
// Returns the number of slots with backpacks in them
```

---

### Get Total Storage Capacity

```java
Player player = ...;

// Total slots across all backpacks
int totalSlots = api.getTotalStorageSlots(player);

// Total used slots across all backpacks
int usedSlots = api.getTotalUsedSlots(player);

// Calculate free space
int freeSpace = totalSlots - usedSlots;
```

---

## Ender Chest Management

### Check if Ender Page is Unlocked

```java
Player player = ...;
int pageNumber = 2;

if (api.isEnderPageUnlocked(player, pageNumber)) {
    // Page 2 is unlocked
}

// Alternative with UUID
UUID uuid = player.getUniqueId();
if (api.isEnderPageUnlocked(uuid, pageNumber)) {
    // Page is unlocked
}
```

---

### Unlock Ender Page

```java
Player player = ...;
int pageNumber = 2;

// Unlock page (bypasses economy and permissions)
api.unlockEnderPage(player, pageNumber);
```

---

### Open Ender Page

```java
Player player = ...;
int pageNumber = 2;

api.openEnderPage(player, pageNumber);
```

---

### Get Ender Page Contents

```java
Player player = ...;
int pageNumber = 1;

Map<Integer, ItemStack> contents = api.getEnderPageContents(player, pageNumber);

if (contents != null) {
    for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
        int slot = entry.getKey();
        ItemStack item = entry.getValue();
        // Process ender page contents
    }
}
```

---

### Get Ender Storage Statistics

```java
Player player = ...;

// Total ender chest slots
int totalEnderSlots = api.getTotalEnderStorageSlots(player);

// Total used ender slots
int usedEnderSlots = api.getTotalUsedEnderSlots(player);

// Get unlocked page count
int unlockedPages = api.getUnlockedEnderPages(player);
```

---

## Unified Storage

### Open Unified Storage GUI

```java
Player player = ...;

// Opens the unified storage menu (backpacks + ender chest)
api.openUnifiedStorageGUI(player);
```

---

## Configuration

### Check Blacklisted Items

```java
import org.bukkit.Material;

Material material = Material.BEDROCK;

if (api.isBlacklisted(material)) {
    // This material cannot be stored in backpacks
}
```

---

### Get Configuration Values

```java
// Get maximum backpack slots
int maxSlots = api.getMaxBackpackSlots();
// Default: 18
```

---

## Events

VaultPack fires several events you can listen to:

### BackpackOpenEvent

Called when a player opens a backpack (cancellable).

```java
import com.vaultpack.api.events.BackpackOpenEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {

    @EventHandler
    public void onBackpackOpen(BackpackOpenEvent event) {
        Player player = event.getPlayer();
        int slotNumber = event.getSlotNumber();

        // Cancel if player is in combat
        if (isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage("You cannot open backpacks in combat!");
        }
    }
}
```

---

### BackpackCloseEvent

Called when a player closes a backpack (cancellable).

```java
import com.vaultpack.api.events.BackpackCloseEvent;

@EventHandler
public void onBackpackClose(BackpackCloseEvent event) {
    Player player = event.getPlayer();
    int slotNumber = event.getSlotNumber();

    // Log when backpacks are closed
    getLogger().info(player.getName() + " closed backpack #" + slotNumber);
}
```

---

### BackpackCreateEvent

Called when a backpack is created (cancellable).

```java
import com.vaultpack.api.events.BackpackCreateEvent;
import com.vaultpack.models.BackpackTier;

@EventHandler
public void onBackpackCreate(BackpackCreateEvent event) {
    Player player = event.getPlayer();
    int slotNumber = event.getSlotNumber();
    BackpackTier tier = event.getTier();

    // Prevent creating jumbo backpacks in certain worlds
    if (tier.getName().equals("jumbo") && isRestrictedWorld(player.getWorld())) {
        event.setCancelled(true);
        player.sendMessage("Jumbo backpacks are not allowed in this world!");
    }
}
```

---

### SlotUnlockEvent

Called when a player unlocks a backpack slot (cancellable).

```java
import com.vaultpack.api.events.SlotUnlockEvent;
import com.vaultpack.api.events.SlotUnlockEvent.UnlockMethod;

@EventHandler
public void onSlotUnlock(SlotUnlockEvent event) {
    Player player = event.getPlayer();
    int slotNumber = event.getSlotNumber();
    UnlockMethod method = event.getMethod();

    switch (method) {
        case ECONOMY:
            // Player paid money to unlock
            break;
        case PERMISSION:
            // Player had permission
            break;
        case ADMIN_COMMAND:
            // Admin gave them the slot
            break;
        case API:
            // Another plugin unlocked it
            break;
    }

    // Send congratulations message
    player.sendMessage("Congratulations! You unlocked slot #" + slotNumber);
}
```

---

## Complete Integration Examples

### Example 1: Reward System

Give players backpack slots as quest rewards:

```java
import com.vaultpack.api.VaultPackAPI;

public class QuestReward {

    public void giveBackpackSlotReward(Player player, int slotNumber) {
        VaultPackAPI api = VaultPackAPI.getInstance();

        if (api == null) {
            // VaultPack not loaded
            return;
        }

        if (!api.isSlotUnlocked(player, slotNumber)) {
            api.unlockSlot(player, slotNumber);
            player.sendMessage("§aQuest Complete! You unlocked backpack slot #" + slotNumber);
        } else {
            // Already unlocked, give alternative reward
            player.sendMessage("§7You already have this slot unlocked!");
        }
    }
}
```

---

### Example 2: Storage Checker

Check if player has enough free storage:

```java
import com.vaultpack.api.VaultPackAPI;

public class StorageChecker {

    public boolean hasEnoughSpace(Player player, int requiredSlots) {
        VaultPackAPI api = VaultPackAPI.getInstance();

        if (api == null) return false;

        int totalSlots = api.getTotalStorageSlots(player);
        int usedSlots = api.getTotalUsedSlots(player);
        int freeSlots = totalSlots - usedSlots;

        return freeSlots >= requiredSlots;
    }

    public void checkAndNotify(Player player) {
        VaultPackAPI api = VaultPackAPI.getInstance();

        if (api == null) return;

        int total = api.getTotalStorageSlots(player);
        int used = api.getTotalUsedSlots(player);
        double percent = (used / (double) total) * 100;

        if (percent > 90) {
            player.sendMessage("§cWarning: Your backpacks are " +
                String.format("%.1f%%", percent) + " full!");
        }
    }
}
```

---

### Example 3: Combat Restriction

Prevent opening backpacks during combat:

```java
import com.vaultpack.api.events.BackpackOpenEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CombatRestriction implements Listener {

    private Set<UUID> playersInCombat = new HashSet<>();

    @EventHandler
    public void onBackpackOpen(BackpackOpenEvent event) {
        Player player = event.getPlayer();

        if (playersInCombat.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot open backpacks while in combat!");
        }
    }

    // Your combat tagging system would add/remove from playersInCombat
}
```

---

### Example 4: Statistics Display

Create a command to show storage stats:

```java
import com.vaultpack.api.VaultPackAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StorageStatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;
        VaultPackAPI api = VaultPackAPI.getInstance();

        if (api == null) {
            player.sendMessage("§cVaultPack is not loaded!");
            return true;
        }

        // Backpack stats
        int unlockedSlots = api.getUnlockedSlots(player);
        int activeBackpacks = api.getActiveBackpackCount(player);
        int totalStorage = api.getTotalStorageSlots(player);
        int usedStorage = api.getTotalUsedSlots(player);

        // Ender stats
        int unlockedPages = api.getUnlockedEnderPages(player);
        int totalEnder = api.getTotalEnderStorageSlots(player);
        int usedEnder = api.getTotalUsedEnderSlots(player);

        player.sendMessage("§6§l=== Storage Statistics ===");
        player.sendMessage("§7Backpack Slots: §f" + unlockedSlots + " unlocked");
        player.sendMessage("§7Active Backpacks: §f" + activeBackpacks);
        player.sendMessage("§7Backpack Storage: §f" + usedStorage + "/" + totalStorage);
        player.sendMessage("§7Ender Pages: §f" + unlockedPages + " unlocked");
        player.sendMessage("§7Ender Storage: §f" + usedEnder + "/" + totalEnder);

        return true;
    }
}
```

---

## Best Practices

### 1. Always Check for Null

```java
VaultPackAPI api = VaultPackAPI.getInstance();
if (api == null) {
    // VaultPack not loaded
    return;
}
```

### 2. Use Soft Dependencies

If VaultPack is optional for your plugin:
```yaml
# plugin.yml
softdepend: [VaultPack]
```

### 3. Listen to Events

Don't poll for changes - use events:
```java
@EventHandler
public void onBackpackOpen(BackpackOpenEvent event) {
    // React to backpack opens
}
```

### 4. Respect Cancellation

If you cancel an event, provide feedback:
```java
event.setCancelled(true);
player.sendMessage("§cYou cannot do that right now!");
```

### 5. Save After Modifications

If you unlock slots/pages programmatically, data is saved automatically. However, if you're doing bulk operations, consider batch saving.

---

## API Methods Reference

### Backpack Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `hasBackpack(Player, int)` | boolean | Check if slot has backpack |
| `hasBackpack(UUID, int)` | boolean | Check by UUID |
| `openBackpack(Player, int)` | void | Open specific backpack |
| `openBackpackMenu(Player)` | void | Open selector menu |
| `createBackpack(Player, int, BackpackTier)` | void | Create new backpack |
| `getBackpackContents(Player, int)` | Map<Integer, ItemStack> | Get backpack items |
| `getBackpackSize(Player, int)` | int | Get backpack size |
| `getBackpackTier(Player, int)` | BackpackTier | Get backpack tier |

### Slot Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `isSlotUnlocked(Player, int)` | boolean | Check if slot unlocked |
| `unlockSlot(Player, int)` | void | Unlock slot |
| `getUnlockedSlots(Player)` | int | Get unlocked count |

### Statistics Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getActiveBackpackCount(Player)` | int | Active backpack count |
| `getTotalStorageSlots(Player)` | int | Total backpack storage |
| `getTotalUsedSlots(Player)` | int | Used backpack slots |

### Ender Chest Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `isEnderPageUnlocked(Player, int)` | boolean | Check page unlocked |
| `unlockEnderPage(Player, int)` | void | Unlock ender page |
| `openEnderPage(Player, int)` | void | Open ender page |
| `getEnderPageContents(Player, int)` | Map<Integer, ItemStack> | Get page contents |
| `getUnlockedEnderPages(Player)` | int | Get unlocked count |
| `getTotalEnderStorageSlots(Player)` | int | Total ender storage |
| `getTotalUsedEnderSlots(Player)` | int | Used ender slots |

---

## Support

If you need help with the API:
1. Check this documentation
2. Review the example code above
3. Join the Discord: https://discord.gg/joogiebear
4. Check the source code on GitHub

---

## Version Information

```java
VaultPackAPI api = VaultPackAPI.getInstance();
String version = api.getVersion();
// Returns: "2.0.0"
```

---

## Advanced: Direct Plugin Access

For advanced features not in the API:

```java
VaultPackAPI api = VaultPackAPI.getInstance();
VaultPackPlugin plugin = api.getPlugin();

// Access managers directly (use with caution!)
BackpackManager backpackManager = plugin.getBackpackManager();
ConfigManager configManager = plugin.getConfigManager();
```

**Warning:** Direct manager access is not part of the stable API and may change between versions!

---

## Next Steps

- [View Event Examples](events.md)
- [See PlaceholderAPI Integration](placeholders.md)
- [Check Configuration Options](configuration.md)
