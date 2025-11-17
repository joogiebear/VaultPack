# VaultPack - Folia Compatibility Analysis Report

## Executive Summary
VaultPack has **several critical Folia compatibility issues** related to scheduler usage, inventory operations, and static Bukkit calls. The codebase currently assumes Bukkit's single-threaded model and will experience race conditions and thread safety issues in Folia's regional server model.

---

## 1. BUKKIT SCHEDULER USAGE - CRITICAL

### Issue 1.1: Direct Scheduler Calls Without Region Awareness

**Problem:** Multiple files use `Bukkit.getScheduler()` directly, which is not thread-safe in Folia.

**Locations Found:**

1. **EnderChestListener.java** (5 occurrences)
   - Lines 100, 111, 125, 139, 153
   ```java
   org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {...}, 1L);
   ```
   - Pattern: Schedules delayed GUI reopens after navigation clicks
   - Issue: No region context, will fail with IllegalStateException in Folia

2. **BackpackListener.java** (5 occurrences)
   - Lines 448, 459, 473, 487, 501
   ```java
   org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {...}, 1L);
   ```
   - Pattern: Schedules delayed GUI reopens for backpack navigation
   - Issue: Same as above - no region context

3. **MenuClickHandler.java** (3 occurrences)
   - Lines 105, 135, 263, 334
   ```java
   org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {...}, 1L);
   org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {...}, 3L);
   ```
   - Pattern: Reopens storage menu after unlock attempts
   - Issue: Tasks not tied to any region/entity

4. **BackpackSelectorGUI.java** (1 occurrence)
   - Line 380
   ```java
   org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {...}, 1L);
   ```

5. **CraftingListener.java** (1 occurrence)
   - Line 293
   ```java
   Bukkit.getScheduler().runTask(plugin, () -> {...});
   ```
   - Pattern: Gives crafting result to player after event
   - Issue: Runs on global scheduler, not player's region

6. **BackpackDataManager.java** (2 occurrences)
   - Lines 273, 389
   ```java
   org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveDataFile);
   org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, ..., 36000L, 36000L);
   ```
   - Pattern: Async data saving and auto-backup
   - Issue: Not tied to any entity/region; async operations may cause race conditions

### Folia Solution Required:
Replace with:
- `entity.getScheduler().execute()` for player-related tasks
- `io.papermc.paper.threadedregions.RegionizedServer.getGlobalRegionScheduler()` for non-entity tasks
- For async I/O: Use `java.util.concurrent` with proper synchronization

---

## 2. INVENTORY OPERATIONS - CRITICAL

### Issue 2.1: No Thread Safety for Inventory Access

**Problem:** Inventory operations assume single-threaded access. In Folia, inventories belonging to players in different regions can be accessed concurrently.

**Affected Operations:**

1. **Opening Inventories** (Multiple files)
   - `player.openInventory(inventory)` - BackpackManager.java, EnderChestManager.java, GUI files
   - Not protected by any synchronization
   - In Folia: Player inventory operations must occur on the player's owning region

2. **Setting Inventory Items**
   - `inventory.setItem(slot, item)` - Used throughout GUI builders
   - `inventory.setContents(contents)` - BackpackListener.java (line 290)
   - These modify inventory state without locks

3. **Reading Inventory Contents**
   - `inventory.getContents()` - BackpackManager.java (saveBackpackContents method)
   - `inventory.getItem()` - Multiple listeners
   - Concurrent reads/writes with no protection

4. **Storage of Active Inventories**
   - BackpackManager: `backpack.setActiveInventory(inventory)` 
   - EnderChestManager: `page.setActiveInventory(inventory)`
   - Stores Inventory objects in memory, accessed from different event threads

### Key Files with Inventory Issues:
- **BackpackManager.java** (lines 70, 83, 234-243)
- **EnderChestManager.java** (lines 53, 66, 98-108)
- **BackpackListener.java** (lines 50-51, 62-82)
- **CraftingListener.java** (line 290)
- **MenuClickHandler.java** (lines 325, 329)

### Folia Solution Required:
- Use `entity.getScheduler()` for all inventory operations
- Ensure inventory access happens on the player's owning region
- Add synchronization for shared inventory state
- Avoid storing references to Inventory objects; operate through the region scheduler

---

## 3. ASYNC OPERATIONS WITHOUT SYNCHRONIZATION - HIGH RISK

### Issue 3.1: Async Data Saving with Shared State

**Problem:** BackpackDataManager saves player data asynchronously while inventory contents can be modified synchronously.

**Locations:**

1. **BackpackDataManager.java** (Lines 272-274, 388-392)
   ```java
   // Line 272-274
   private void saveDataFileAsync() {
       org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveDataFile);
   }
   
   // Line 388-392
   private void scheduleAutoBackup() {
       org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
           plugin.getLogger().info("Running automatic backup...");
           createBackup();
       }, 36000L, 36000L);
   }
   ```

2. **Potential Race Condition:**
   - Player modifies inventory (sync, on region thread)
   - `savePlayerData()` called asynchronously (line 306)
   - Inventory contents accessed while being modified (lines 232-240)

3. **The Problem:**
   - Map<Integer, ItemStack> contents = backpack.getContents(); (line 231)
   - Player could be modifying the same map in a different region thread
   - No synchronization between read and write

### Folia Solution Required:
- Use region-local storage for player data
- Implement proper synchronization (ConcurrentHashMap or locks)
- Schedule saves on the player's region scheduler
- Use Folia's data APIs for concurrent data access

---

## 4. DIRECT BUKKIT STATIC CALLS - HIGH RISK

### Issue 4.1: Static Bukkit Method Calls Without Region Context

**Problem:** Multiple static Bukkit calls that bypass region scheduler.

**Affected Calls:**

1. **Bukkit.getPluginManager()** - CraftingListener.java (line 37, 516, 544, 568, 575)
   - Should be fine (global state)
   - BUT: Bukkit.getLogger() calls should use plugin.getLogger()

2. **Bukkit.getOnlinePlayers()** - BackpackManager.java (line 715)
   ```java
   for (Player player : Bukkit.getOnlinePlayers()) {
       if (isBackpackOpen(player)) {
           player.closeInventory();
       }
   }
   ```
   - Closes inventories for all players without region context
   - DANGEROUS in Folia: closing inventory must happen on player's region thread

3. **Bukkit.createInventory()** - BackpackManager.java (line 70), EnderChestManager.java (line 53)
   ```java
   Inventory inventory = Bukkit.createInventory(null, totalRows * 9, title);
   ```
   - Should be fine, but the inventory must be opened on the player's region

4. **Bukkit.createPlayerProfile()** - BackpackManager.java (line 731), EnderChestManager.java (line 313)
   ```java
   org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
   ```
   - Static call, should be fine
   - BUT: Setting textures is fine (global operation)

### Folia Solution Required:
- Wrap all player-related operations with `player.getScheduler()`
- For closeAllBackpacks(): Iterate and schedule on each player's region
- Replace global logger calls with plugin.getLogger() (already mostly done)

---

## 5. EVENT LISTENERS - MEDIUM RISK

### Issue 5.1: Event Handlers Without Region Awareness

**Problem:** Event handlers process events from different regions without proper thread safety.

**Listeners Identified:**

1. **BackpackListener** (Inventory events)
   - Lines 19-89: onInventoryClick() - Handles clicks on all inventory types
   - Issue: Accesses PlayerBackpackData and modifies inventories without sync
   - Risk: Two regions could modify same player's data simultaneously

2. **EnderChestListener** (Player interaction and inventory events)
   - Lines 31-49: onEnderChestOpen() - Intercepts vanilla ender chest
   - Lines 54-82: onInventoryClick() - Handles page navigation
   - Lines 216-233: onInventoryClose() - Saves contents
   - Issue: Multiple operations on inventory contents without locking

3. **CraftingListener** (Craft events)
   - Lines 55-90: onPrepareCraft() - Validates recipes
   - Lines 208-306: onCraftItem() - Handles crafting and gives items
   - Issue: Line 293 gives items without player's region context
   - Issue: Line 296 accesses player.getInventory() concurrently

4. **PlayerListener** (Player login/logout)
   - Lines 17-21: onPlayerJoin() - Loads player data (sync, OK)
   - Lines 23-28: onPlayerQuit() - Saves and unloads (potential race)
   - Issue: Line 26-27 closes backpack while data is being saved

5. **DeathProtectionListener** (Player death)
   - Lines 22-32: onPlayerDeath()
   - Uses MONITOR priority (waits for other listeners)
   - Calls `savePlayerDataSync()` which might conflict with async saves

6. **MenuClickHandler** (Inventory clicks)
   - Lines 25-61: onInventoryClick() - Routes clicks to handlers
   - Multiple inventory operations in event context
   - No synchronization with region threads

### Folia Solution Required:
- All player-related event operations must schedule on player's region
- Use `entity.getScheduler().execute()` for event-triggered actions
- Implement proper event ordering if multiple regions affect same player

---

## 6. PLAYER INVENTORY ACCESS - CRITICAL

### Issue 6.1: Direct Player Inventory Operations

**Problem:** Multiple locations directly modify player inventory without region context.

**Locations:**

1. **MenuClickHandler.java**
   - Line 325: `player.getInventory().addItem(backpackItem);` - Gives item to player
   - Line 329: `player.getWorld().dropItem(player.getLocation(), backpackItem);` - Drops item
   - Issue: Not on player's region thread

2. **BackpackListener.java**
   - Line 180: `org.bukkit.inventory.ItemStack cursor = player.getItemOnCursor();`
   - Line 260: `player.setItemOnCursor(cursor);`
   - Line 296: `player.getInventory().addItem(result);`
   - Multiple direct inventory accesses

3. **CraftingListener.java**
   - Lines 296, 303: `player.getInventory().addItem(result);`
   - Line 303: `player.setItemOnCursor(result);`
   - Issue: Runs on global scheduler (line 293)

4. **BackpackManager.java**
   - Line 306-309: `dropItemNaturally()` - Drops items on death
   - Issue: No region context, might fail if world is in different region

### Folia Solution Required:
- All player inventory modifications must use `player.getScheduler()`
- Example replacement:
  ```java
  player.getScheduler().execute(plugin, () -> {
      HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
  });
  ```

---

## 7. ENTITY & WORLD OPERATIONS - MEDIUM RISK

### Issue 7.1: World Modification Without Region Context

**Locations:**

1. **BackpackManager.java** (line 308)
   ```java
   player.getWorld().dropItemNaturally(player.getLocation(), item);
   ```
   - Used in removeBackpack() method
   - Must be on player's region thread

2. **CraftingListener.java** (line 299)
   ```java
   player.getWorld().dropItem(player.getLocation(), result);
   ```
   - Drops item to world
   - Must be on player's region thread

### Folia Solution Required:
- Use `player.getScheduler()` or region scheduler for world operations
- Ensure drops happen on player's owning region

---

## 8. SOUND PLAYBACK - LOW RISK

### Issue 8.1: Sound Operations

**Locations:**
- MenuClickHandler.java, BackpackSelectorGUI.java, EnderChestGUI.java, StorageMenuGUI.java
- `player.playSound()` calls

**Assessment:**
- Usually thread-safe in Paper/Folia
- Should work from any thread
- Not a critical issue but best practice: schedule on player's region

---

## SUMMARY TABLE

| Issue | Severity | Type | Files | Count |
|-------|----------|------|-------|-------|
| Scheduler without region | CRITICAL | Threading | 6 files | 17 calls |
| Inventory operations | CRITICAL | Threading | 10 files | 30+ operations |
| Async data saving | HIGH | Concurrency | 1 file | 2 calls |
| Static Bukkit calls | HIGH | Threading | 5 files | 5 critical calls |
| Event listener sync | MEDIUM | Threading | 6 files | Multiple |
| Player inventory access | CRITICAL | Threading | 4 files | 8 calls |
| World operations | MEDIUM | Threading | 2 files | 2 calls |

---

## RECOMMENDED ACTION PLAN

### Phase 1: Critical Fixes (Required for Folia)
1. Replace all `Bukkit.getScheduler()` calls with region-aware equivalents
2. Add region context to all inventory operations
3. Fix async data saving with proper synchronization
4. Protect player inventory modifications

### Phase 2: Medium Priority Fixes
1. Refactor event listeners for concurrency
2. Fix world operation threading
3. Add synchronization to shared PlayerBackpackData

### Phase 3: Polish
1. Add region-specific configuration
2. Optimize region-local caching
3. Add logging for debugging region issues

---

## MIGRATION CHECKLIST

- [ ] Replace `Bukkit.getScheduler().runTask*()` with `entity.getScheduler()`
- [ ] Add `ConcurrentHashMap` for playerDataCache
- [ ] Synchronize access to active inventories (backpackActiveInventory, enderPageActiveInventory)
- [ ] Implement region-aware event listener logic
- [ ] Test with multiple players in different regions
- [ ] Test with async data saves during inventory modifications
- [ ] Add Folia compatibility logging
- [ ] Test death/quit edge cases
- [ ] Performance test with region separation

---

## REFERENCES
- Folia Documentation: https://github.com/PaperMC/Folia
- Region Scheduler: Folia provides `entity.getScheduler()` for entity-local tasks
- Global Scheduler: `RegionizedServer.getGlobalRegionScheduler()` for non-entity tasks
