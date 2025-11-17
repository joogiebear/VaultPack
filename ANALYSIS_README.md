# VaultPack - Folia Compatibility Analysis

## Overview

This directory contains a comprehensive analysis of VaultPack's compatibility with PaperMC Folia.

**Status: CRITICAL ISSUES IDENTIFIED - Will not work on Folia without fixes**

---

## Key Findings

- **39 Java files** analyzed
- **17 scheduler calls** without region context (will crash)
- **30+ inventory operations** without thread safety
- **8 major issue categories** identified
- **Critical, High, Medium, and Low severity** issues

---

## Report Files

### 1. FOLIA_QUICK_REFERENCE.txt (Start Here!)
**Best for:** Quick lookup by file and line number
- All issues with exact file paths and line numbers
- Organized by severity
- Migration patterns with before/after examples
- Priority fix order

### 2. FOLIA_CRITICAL_ISSUES_SUMMARY.md
**Best for:** Understanding the 3 critical issues
- Focused on blocking issues
- Code examples showing problems
- Implementation patterns
- Testing checklist

### 3. FOLIA_COMPATIBILITY_ANALYSIS.md  
**Best for:** Complete technical analysis
- Detailed explanation of all 8 issue categories
- Folia solutions for each problem
- 3-phase action plan
- Migration checklist
- References and documentation

---

## Critical Issues Summary

### Issue 1: Scheduler Calls (17 occurrences)
- **Files:** EnderChestListener, BackpackListener, MenuClickHandler, CraftingListener, etc.
- **Impact:** IllegalStateException crashes in Folia
- **Fix:** Replace `Bukkit.getScheduler()` with `player.getScheduler()`

### Issue 2: Inventory Operations (30+ occurrences)
- **Files:** BackpackManager, EnderChestManager, all listeners and GUIs
- **Impact:** Race conditions, data corruption
- **Fix:** Wrap with `player.getScheduler().execute()`

### Issue 3: Async Data Saves (2 occurrences)
- **File:** BackpackDataManager
- **Impact:** Data corruption from concurrent modifications
- **Fix:** Add synchronization and thread-safe collections

---

## Priority Files to Fix

### CRITICAL (Blocks Folia entirely)
1. EnderChestListener.java - 5 scheduler calls
2. BackpackListener.java - 5 scheduler calls
3. MenuClickHandler.java - 3 scheduler calls
4. BackpackDataManager.java - Race conditions
5. BackpackManager.java - Inventory operations

### HIGH
6. CraftingListener.java - 1 scheduler call
7. EnderChestManager.java - Inventory operations
8. BackpackSelectorGUI.java - 1 scheduler call

### MEDIUM
9. PlayerListener.java
10. DeathProtectionListener.java

---

## Quick Fix Pattern

Most fixes follow this pattern:

```java
// BEFORE (WRONG IN FOLIA)
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    player.openInventory(inventory);
}, 1L);

// AFTER (CORRECT FOR FOLIA)
player.getScheduler().execute(plugin, () -> {
    player.openInventory(inventory);
});
```

---

## What Each Report Contains

| Report | Size | Content | Use Case |
|--------|------|---------|----------|
| FOLIA_QUICK_REFERENCE.txt | 8 KB | Line numbers, patterns, priority | Quick lookup while coding |
| FOLIA_CRITICAL_ISSUES_SUMMARY.md | 7.5 KB | 3 critical issues, code examples | Understanding the main problems |
| FOLIA_COMPATIBILITY_ANALYSIS.md | 14 KB | Complete analysis, action plan | Full understanding, planning fixes |

---

## Testing After Fixes

After implementing changes, test:
- Open backpack (no crash)
- Navigate pages (no crash)
- Close backpack (items saved correctly)
- Craft backpack (item given correctly)
- Multiple players in different regions
- Auto-backup during concurrent modifications
- Delete with items (correct drops)
- Server shutdown with open backpacks

---

## Estimated Timeline

- **Analysis time:** Already complete
- **Fix implementation:** 4-6 hours (familiar developer)
- **Testing:** 2-3 hours
- **Total:** 6-9 hours for experienced developer

---

## Key Concepts

**Folia Changes Bukkit's Threading Model:**
- Bukkit: Single-threaded main thread
- Folia: Multiple regions, each with own thread

**What This Means:**
- Can't use global scheduler for player operations
- Must use player's region scheduler
- Inventory operations must be on player's thread
- Shared state needs synchronization

---

## Next Steps

1. **Read FOLIA_QUICK_REFERENCE.txt** for a quick overview
2. **Read FOLIA_CRITICAL_ISSUES_SUMMARY.md** to understand the issues
3. **Review FOLIA_COMPATIBILITY_ANALYSIS.md** for complete details
4. **Start fixing CRITICAL files** in priority order
5. **Test thoroughly** with multiple players

---

## Questions?

Refer to the specific report for:
- **Line numbers:** FOLIA_QUICK_REFERENCE.txt
- **Code examples:** FOLIA_CRITICAL_ISSUES_SUMMARY.md
- **Technical details:** FOLIA_COMPATIBILITY_ANALYSIS.md

All files contain migration patterns and examples to help guide the fixes.

---

Generated: November 17, 2025
Analysis Scope: 39 Java files
Issues Found: 8 categories, 60+ specific issues
Severity: CRITICAL - Folia incompatible without fixes
