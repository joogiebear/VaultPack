# VaultPack - Folia Critical Issues Summary

## Quick Overview
VaultPack has **3 CRITICAL issues** that will cause crashes or data corruption in Folia:

1. **Scheduler calls without region context** (17 occurrences)
2. **Inventory operations without thread safety** (30+ occurrences)  
3. **Async data saves conflicting with sync modifications** (2 occurrences)

---

## Critical Issue #1: Scheduler Calls (WILL CRASH IN FOLIA)

### The Problem
Code uses `Bukkit.getScheduler()` which doesn't understand Folia's region model. Tasks fail with `IllegalStateException`.

### Example Issues Found

**File: EnderChestListener.java (5 occurrences)**
```java
// Line 100 - WRONG
org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
    if (player.isOnline()) {
        new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
    }
}, 1L);

// Should be:
player.getScheduler().execute(plugin, () -> {
    new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
});
```

**File: BackpackListener.java (5 occurrences)**
```java
// Line 448 - Same pattern as above
// Line 459, 473, 487, 501 - Same pattern
```

**File: MenuClickHandler.java (3 occurrences)**
```java
// Line 105 - Same issue
```

**File: CraftingListener.java (1 occurrence)**
```java
// Line 293 - WRONG
Bukkit.getScheduler().runTask(plugin, () -> {
    if (event.isShiftClick()) {
        java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(result);
    }
});

// Should use:
player.getScheduler().execute(plugin, () -> { ... });
```

**File: BackpackDataManager.java (2 occurrences)**
```java
// Line 273 - WRONG (async save)
org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveDataFile);

// Line 389 - WRONG (auto-backup timer)
org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
    createBackup();
}, 36000L, 36000L);
```

---

## Critical Issue #2: Inventory Operations Without Thread Safety

### The Problem
Multiple operations access inventories simultaneously from different threads with no locks.

### Example Issues Found

**File: BackpackManager.java**
```java
// Line 70-90 - Opening inventory
Inventory inventory = Bukkit.createInventory(null, totalRows * 9, title);
// ... add items ...
player.openInventory(inventory);  // NOT THREAD-SAFE
backpack.setActiveInventory(inventory);  // Stores ref without sync
openBackpacks.put(player.getUniqueId(), slotNumber);

// Line 234-243 - Saving inventory contents (NOT THREAD-SAFE)
Map<Integer, ItemStack> contents = new HashMap<>();
for (int i = 0; i < backpack.getSize(); i++) {
    ItemStack item = inventory.getItem(i + 9);  // RACE CONDITION
    if (item != null && item.getType() != Material.AIR) {
        contents.put(i, item.clone());
    }
}
```

**File: BackpackListener.java**
```java
// Line 290 - WRONG
inventory.setMatrix(matrix);  // NOT THREAD-SAFE

// Line 296 - WRONG  
HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(result);  // NOT THREAD-SAFE
```

**File: EnderChestManager.java**
```java
// Line 53-72 - Opening inventory without sync
Inventory inventory = Bukkit.createInventory(null, 54, title);
// ... add items ...
player.openInventory(inventory);  // NOT ON PLAYER'S REGION THREAD
page.setActiveInventory(inventory);  // Stored without synchronization
```

**File: MenuClickHandler.java**
```java
// Line 325 - WRONG
HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(backpackItem);

// Line 329 - WRONG
player.getWorld().dropItem(player.getLocation(), backpackItem);
```

---

## Critical Issue #3: Async Data Saves with Shared Mutable State

### The Problem
Player data saved asynchronously while inventory contents modified synchronously = **data corruption**.

### Example Issues Found

**File: BackpackDataManager.java**

```java
// Line 231-240 - Read inventory while it might be modified
Map<Integer, ItemStack> contents = backpack.getContents();  // NOT THREAD-SAFE
for (Map.Entry<Integer, ItemStack> contentEntry : contents.entrySet()) {
    int index = contentEntry.getKey();
    ItemStack item = contentEntry.getValue();
    // ...
}

// Line 306 - Called asynchronously
private void saveDataFileAsync() {
    org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveDataFile);
}

// SCENARIO:
// 1. Player clicks to place item in backpack (Region A thread)
// 2. Backpack contents updated in real-time
// 3. Meanwhile, another player's data save starts (ASYNC)
// 4. Save reads playerDataCache.get(uuid) - WRONG uuid from async context!
// 5. Data corruption or wrong player's data saved
```

---

## Implementation Pattern for Fixes

### Pattern 1: Player-Related Operations
```java
// BEFORE (WRONG):
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    player.openInventory(inventory);
}, 1L);

// AFTER (CORRECT):
player.getScheduler().execute(plugin, () -> {
    player.openInventory(inventory);
});
```

### Pattern 2: Inventory Modifications
```java
// BEFORE (WRONG):
player.getInventory().addItem(item);

// AFTER (CORRECT):
player.getScheduler().execute(plugin, () -> {
    player.getInventory().addItem(item);
});
```

### Pattern 3: World Operations
```java
// BEFORE (WRONG):
player.getWorld().dropItem(player.getLocation(), item);

// AFTER (CORRECT):
player.getScheduler().execute(plugin, () -> {
    player.getWorld().dropItem(player.getLocation(), item);
});
```

### Pattern 4: Data Synchronization
```java
// BEFORE (WRONG):
private Map<UUID, PlayerBackpackData> playerDataCache = new HashMap<>();

// AFTER (CORRECT):
private Map<UUID, PlayerBackpackData> playerDataCache = 
    new ConcurrentHashMap<>();
```

---

## Files Requiring Changes

**CRITICAL (Must Fix):**
- [ ] EnderChestListener.java - 5 scheduler calls
- [ ] BackpackListener.java - 5 scheduler calls + inventory ops
- [ ] MenuClickHandler.java - 3 scheduler calls + inventory ops
- [ ] CraftingListener.java - 1 scheduler call + inventory ops
- [ ] BackpackDataManager.java - 2 async calls + data race conditions
- [ ] BackpackManager.java - Inventory operations + closeAllBackpacks()

**HIGH PRIORITY (Should Fix):**
- [ ] BackpackSelectorGUI.java - 1 scheduler call
- [ ] EnderChestManager.java - Inventory operations

**MEDIUM PRIORITY (Nice to Have):**
- [ ] All GUI files - inventory operations
- [ ] PlayerListener.java - Player join/quit race conditions
- [ ] DeathProtectionListener.java - Sync issues with async saves

---

## Testing Checklist for Folia

After fixing, test:

- [ ] Open backpack - no crash
- [ ] Navigate backpack pages - no crash
- [ ] Close backpack and data saves - correct items
- [ ] Craft backpack - item given correctly
- [ ] Player with backpack open disconnects - no data loss
- [ ] Multiple players opening backpacks in different regions
- [ ] Close all backpacks on server shutdown - no crashes
- [ ] Auto-backup runs while players modify inventories
- [ ] Delete backpack with items - items drop correctly
- [ ] Upgrade backpack from another region - works correctly

---

## Quick Fix Priority

**Do These First (blocks Folia entirely):**
1. Replace all `Bukkit.getScheduler()` with `player.getScheduler()`
2. Wrap all `player.getInventory()` operations with `player.getScheduler().execute()`
3. Change playerDataCache to ConcurrentHashMap
4. Test basic backpack open/close

**Then (prevent data corruption):**
5. Fix async data save synchronization
6. Protect all read/write to playerDataCache
7. Test with multiple concurrent players

**Polish (optimization):**
8. Add region-aware logging
9. Performance test with region separation
10. Cleanup global references

