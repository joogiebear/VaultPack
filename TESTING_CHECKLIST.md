# VaultPack Testing Checklist

This document provides comprehensive testing procedures for each phase of development and before each release.

---

## Quick Test (After Each Change)

Run these tests after making any code changes:

- [ ] **Build succeeds**: `./gradlew clean shadowJar`
- [ ] **No compilation errors**
- [ ] **JAR file generated** in `build/libs/`
- [ ] **Plugin loads** on test server without errors

---

## Phase 0: Testing Infrastructure

**Status:** ⬜ Not Tested

### Setup Tests

- [ ] **Run-Paper works**: `./gradlew runServer`
- [ ] **Server starts successfully**
- [ ] **Dependencies download** (Vault, PlaceholderAPI, LuckPerms)
- [ ] **Plugin loads** without errors
- [ ] **Can stop server cleanly**

---

## Core Functionality Tests

Run these tests after ANY significant changes:

### Backpack System

- [ ] **Open backpack** - `/backpack` or `/bp`
- [ ] **Open specific slot** - `/backpack open 1`
- [ ] **Unlock slot** - `/backpack unlock 2`
- [ ] **Store items** in backpack
- [ ] **Retrieve items** from backpack
- [ ] **Multiple backpack types** work correctly
- [ ] **Blacklist system** prevents blacklisted items
- [ ] **Crafting integration** works with backpack items
- [ ] **Backpack persistence** (items saved after restart)

### Ender Chest Integration

- [ ] **Open ender chest** - `/enderchest` or `/ec`
- [ ] **Store items** in ender chest
- [ ] **Retrieve items** from ender chest
- [ ] **Ender chest persistence** (items saved after restart)

### Storage Menu

- [ ] **Storage menu opens** - `/storage`
- [ ] **Navigation** between backpacks and ender chest
- [ ] **Icons display** correctly
- [ ] **Click handling** works properly

### Permissions

- [ ] `vaultpack.backpack` - Access backpacks
- [ ] `vaultpack.backpack.open` - Open specific slots
- [ ] `vaultpack.backpack.unlock` - Unlock slots
- [ ] `vaultpack.enderchest` - Access ender chest
- [ ] `vaultpack.admin.*` - Admin commands work

### Admin Commands

- [ ] **Unlock slot for player** - `/backpack admin unlock <player> <slot>`
- [ ] **Open player's backpack** - `/backpack admin open <player> <slot>`
- [ ] **Reload configuration** - `/vaultpack reload`

### Data Persistence

- [ ] **YAML storage** works correctly
- [ ] **MySQL storage** works correctly (if configured)
- [ ] **Data migration** YAML → MySQL works
- [ ] **Backup system** creates backups
- [ ] **No data loss** after restart
- [ ] **Cache system** works efficiently

### Multi-Player Tests

- [ ] **Multiple players** can use backpacks simultaneously
- [ ] **No data conflicts** between players
- [ ] **Cache handles** concurrent access
- [ ] **Performance** acceptable with 10+ players

---

## Phase 1: Build & Dependencies

**Status:** ⬜ Not Tested

### After Phase 1 Completion

- [ ] **ACF dependency added** and relocated
- [ ] **Maven publishing** configured
- [ ] **Build succeeds** with new dependencies
- [ ] **JAR size** reasonable (<2MB)
- [ ] **No dependency conflicts**
- [ ] **All Phase 0 tests** still pass

---

## Phase 2: Configuration System

**Status:** ⬜ Not Tested

### After Phase 2 Completion

- [ ] **New config system** loads successfully
- [ ] **Config migration** runs automatically
- [ ] **All config values** migrate correctly
- [ ] **Config reload** works without restart
- [ ] **Backward compatibility** maintained
- [ ] **All Phase 0-1 tests** still pass

---

## Phase 3: Command Framework

**Status:** ⬜ Not Tested

### After Phase 3 Completion

- [ ] **All commands** work identically to before
- [ ] **Tab completion** improved/working
- [ ] **Help system** auto-generated
- [ ] **Permissions** still enforced
- [ ] **Error messages** clear and helpful
- [ ] **All Phase 0-2 tests** still pass

---

## Phase 4: Message & Localization

**Status:** ⬜ Not Tested

### After Phase 4 Completion

- [ ] **All messages** display correctly
- [ ] **Placeholders** work properly
- [ ] **Multi-language** switching works
- [ ] **Fallback to English** works
- [ ] **MiniMessage formatting** renders correctly
- [ ] **All Phase 0-3 tests** still pass

---

## Phase 5: Data Architecture

**Status:** ⬜ Not Tested

### After Phase 5 Completion

- [ ] **Data loads** correctly with new system
- [ ] **Data saves** correctly with new system
- [ ] **Dirty tracking** reduces I/O operations
- [ ] **No data loss** during migration
- [ ] **Performance** maintained or improved
- [ ] **Old data migrates** to new format
- [ ] **All Phase 0-4 tests** still pass

---

## Phase 6: API Refactoring

**Status:** ⬜ Not Tested

### After Phase 6 Completion

- [ ] **Public API** accessible from external plugins
- [ ] **API methods** work as documented
- [ ] **No internal dependencies** needed
- [ ] **Javadoc** comprehensive and accurate
- [ ] **All Phase 0-5 tests** still pass

---

## Phase 7: Expansion System

**Status:** ⬜ Not Tested

### After Phase 7 Completion (Optional)

- [ ] **Expansions** can be registered
- [ ] **Statistics expansion** tracks data
- [ ] **Expansions** enable/disable correctly
- [ ] **All Phase 0-6 tests** still pass

---

## Phase 8: Pre-Release Testing

**Status:** ⬜ Not Tested

### Final Checks Before v2.0.0

- [ ] **All previous phase tests** pass
- [ ] **Performance testing** completed
- [ ] **Memory leak testing** completed
- [ ] **Load testing** with 50+ players
- [ ] **Migration testing** from v1.x to v2.0
- [ ] **Documentation** complete and accurate
- [ ] **CHANGELOG** written
- [ ] **README** updated

---

## Performance Benchmarks

Track these metrics during development:

### Startup Performance

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Plugin load time | <500ms | - | - |
| Config load time | <100ms | - | - |
| Data cache initialization | <200ms | - | - |

### Runtime Performance

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Backpack open latency | <50ms | - | - |
| Item save latency | <100ms | - | - |
| Cache hit rate | >90% | - | - |
| Memory usage (100 players) | <100MB | - | - |

### Database Performance (MySQL)

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Data load time | <200ms | - | - |
| Data save time | <100ms | - | - |
| Bulk save (100 users) | <2s | - | - |
| Connection pool utilization | <50% | - | - |

---

## Test Scenarios

### Scenario 1: New Player

1. Player joins server for first time
2. No data exists for player
3. Player runs `/backpack`
4. Backpack selector opens with slot 1 unlocked
5. Player can store/retrieve items
6. Data persists after logout/login

### Scenario 2: Slot Unlock

1. Player has slot 1 unlocked
2. Player runs `/backpack unlock 2`
3. Slot 2 becomes available
4. Player can access both slots
5. Data persists after restart

### Scenario 3: Multiple Backpack Types

1. Player creates backpack type "small" in slot 1
2. Player creates backpack type "large" in slot 2
3. Both backpacks maintain separate inventories
4. Correct type displays in selector GUI
5. Data persists correctly

### Scenario 4: Server Restart

1. Players have active backpacks with items
2. Server performs clean shutdown
3. Server restarts
4. All player data loads correctly
5. No items lost
6. All backpacks accessible

### Scenario 5: Data Migration

1. Server running v1.x with YAML storage
2. Update to v2.0 with MySQL configured
3. Migration tool converts data
4. All backpacks migrate successfully
5. No data loss
6. Server runs on MySQL storage

---

## Bug Reporting Template

When a bug is found during testing:

```markdown
## Bug Report

**Phase:** [Phase number where bug was found]
**Severity:** [Critical/High/Medium/Low]
**Test:** [Which test case failed]

**Description:**
[Clear description of the bug]

**Steps to Reproduce:**
1. [First step]
2. [Second step]
3. [...]

**Expected Behavior:**
[What should happen]

**Actual Behavior:**
[What actually happens]

**Logs/Errors:**
```
[Paste relevant logs here]
```

**Environment:**
- Minecraft Version:
- Paper Version:
- Plugin Version:
- Java Version:
```

---

## Notes

- ✅ = Test passed
- ⬜ = Not tested yet
- ❌ = Test failed
- ⚠️ = Test passed with minor issues

Update this checklist as you complete each phase of the migration!
