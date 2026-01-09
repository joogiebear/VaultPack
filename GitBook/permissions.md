# Permissions

Complete reference for all VaultPack permission nodes.

## Basic Permissions

### vaultpack.use
**Default:** `true` (everyone)

Allows players to use the basic VaultPack commands and features.

**Grants access to:**
- `/backpack` command
- `/enderchest` command
- `/storage` command
- Opening and managing own backpacks

---

## Admin Permissions

### vaultpack.admin
**Default:** `op`

Master admin permission that grants all administrative privileges.

**Includes:**
- `vaultpack.use`
- `vaultpack.reload`
- `vaultpack.give`
- `vaultpack.remove`
- `vaultpack.clear`

---

### vaultpack.reload
**Default:** `op`

Permission to reload plugin configuration.

**Grants access to:**
- `/backpack reload` command

---

### vaultpack.give
**Default:** `op`

Permission to give players backpack slot access.

**Grants access to:**
- `/backpack give <player> <slot>` command

---

### vaultpack.remove
**Default:** `op`

Permission to remove players' backpack slot access.

**Grants access to:**
- `/backpack remove <player> <slot>` command

---

### vaultpack.clear
**Default:** `op`

Permission to clear player backpack contents.

**Grants access to:**
- `/backpack clear <player> <slot>` command

---

## Economy Bypass

### vaultpack.bypass.cost
**Default:** `op`

Bypasses all economy costs for:
- Unlocking backpack slots
- Unlocking ender chest pages
- Upgrading backpacks

Players with this permission can perform all actions for free.

---

## Backpack Slot Permissions

### vaultpack.slots.*
**Default:** `false`

Grants access to all 18 backpack slots.

**Individual slot permissions:**
```
vaultpack.slots.1    # Access to slot 1
vaultpack.slots.2    # Access to slot 2
vaultpack.slots.3    # Access to slot 3
...
vaultpack.slots.18   # Access to slot 18
```

### How Slot Permissions Work

Players can unlock slots in two ways:
1. **Economy** - Pay money to unlock (if economy is enabled)
2. **Permission** - Have the specific permission node

Configuration in `config.yml`:
```yaml
permissions:
  use-for-slots: true     # Enable permission-based unlocking
  require-both: false     # If true, needs BOTH money AND permission
```

### Examples

**Grant single slot:**
```
/lp user Notch permission set vaultpack.slots.1
```

**Grant multiple slots:**
```
/lp user Notch permission set vaultpack.slots.1
/lp user Notch permission set vaultpack.slots.2
/lp user Notch permission set vaultpack.slots.3
```

**Grant all slots:**
```
/lp user Notch permission set vaultpack.slots.*
```

**Grant slots to a rank:**
```
/lp group vip permission set vaultpack.slots.1
/lp group vip permission set vaultpack.slots.2
/lp group vip permission set vaultpack.slots.3
```

---

## Ender Chest Page Permissions

### vaultpack.enderchest.*
**Default:** `false`

Grants access to all 9 ender chest pages.

**Individual page permissions:**
```
vaultpack.enderchest.page.1    # Access to page 1
vaultpack.enderchest.page.2    # Access to page 2
vaultpack.enderchest.page.3    # Access to page 3
...
vaultpack.enderchest.page.9    # Access to page 9
```

### How Page Permissions Work

Similar to slot permissions:
1. **Economy** - Pay money to unlock pages
2. **Permission** - Have the specific permission node

Configuration in `config.yml`:
```yaml
permissions:
  use-for-pages: true     # Enable permission-based unlocking
  require-both: false     # If true, needs BOTH money AND permission
```

### Examples

**Grant single page:**
```
/lp user Steve permission set vaultpack.enderchest.page.1
```

**Grant all pages:**
```
/lp user Steve permission set vaultpack.enderchest.*
```

---

## Crafting Permissions

### vaultpack.craft.*
**Default:** `true`

Allows crafting all backpack types.

**Individual crafting permissions:**
```
vaultpack.craft.small      # Craft small backpacks
vaultpack.craft.medium     # Craft medium backpacks
vaultpack.craft.large      # Craft large backpacks
vaultpack.craft.greater    # Craft greater backpacks
vaultpack.craft.jumbo      # Craft jumbo backpacks
```

### How Crafting Permissions Work

Players need the appropriate permission to craft each backpack tier. This allows you to gate higher-tier backpacks behind ranks or achievements.

### Examples

**Allow only small backpacks:**
```
/lp group default permission set vaultpack.craft.small
```

**VIP rank gets medium backpacks:**
```
/lp group vip permission set vaultpack.craft.small
/lp group vip permission set vaultpack.craft.medium
```

**MVP rank gets all backpacks:**
```
/lp group mvp permission set vaultpack.craft.*
```

---

## Permission Setup Examples

### Scenario 1: Free-to-Play Server

Default players get 1 slot, paid ranks get more:

**Default Rank:**
```yaml
permissions:
  - vaultpack.use
  - vaultpack.slots.1
  - vaultpack.enderchest.page.1
  - vaultpack.craft.small
```

**VIP Rank ($5):**
```yaml
permissions:
  - vaultpack.use
  - vaultpack.slots.1
  - vaultpack.slots.2
  - vaultpack.slots.3
  - vaultpack.enderchest.page.1
  - vaultpack.enderchest.page.2
  - vaultpack.craft.*
```

**MVP Rank ($15):**
```yaml
permissions:
  - vaultpack.use
  - vaultpack.slots.*        # All slots
  - vaultpack.enderchest.*   # All pages
  - vaultpack.craft.*        # All crafting
  - vaultpack.bypass.cost    # Free upgrades
```

---

### Scenario 2: Pure Economy Server

Everyone can unlock everything with money:

**Config:**
```yaml
economy:
  enabled: true

permissions:
  use-for-slots: false    # Disable permission requirement
  use-for-pages: false    # Disable permission requirement
```

**All Players Get:**
```yaml
permissions:
  - vaultpack.use
  - vaultpack.craft.*
```

Players earn money in-game and unlock slots/pages by paying.

---

### Scenario 3: Hybrid System

Use both permissions AND economy:

**Config:**
```yaml
economy:
  enabled: true

permissions:
  use-for-slots: true
  use-for-pages: true
  require-both: false    # Can unlock with EITHER money OR permission
```

**Free Players:**
- Must pay money to unlock slots

**VIP Players:**
- Get slots 1-5 for free via permissions
- Can pay for additional slots

**MVP Players:**
- Get all slots for free via `vaultpack.slots.*`
- Bypass all costs with `vaultpack.bypass.cost`

---

## Quick Reference

| Permission | Description | Default |
|------------|-------------|---------|
| `vaultpack.use` | Use basic commands | Everyone |
| `vaultpack.admin` | All admin permissions | Op |
| `vaultpack.reload` | Reload configuration | Op |
| `vaultpack.give` | Give slot access | Op |
| `vaultpack.remove` | Remove slot access | Op |
| `vaultpack.clear` | Clear backpacks | Op |
| `vaultpack.bypass.cost` | Bypass economy costs | Op |
| `vaultpack.slots.*` | All backpack slots | False |
| `vaultpack.slots.<1-18>` | Specific slot access | False |
| `vaultpack.enderchest.*` | All ender pages | False |
| `vaultpack.enderchest.page.<1-9>` | Specific page access | False |
| `vaultpack.craft.*` | Craft all backpacks | True |
| `vaultpack.craft.<tier>` | Craft specific tier | True |

---

## Permission Plugin Compatibility

VaultPack is compatible with all major permission plugins:
- LuckPerms (recommended)
- PermissionsEx
- GroupManager
- bPermissions

### LuckPerms Commands

**User permissions:**
```
/lp user <player> permission set <permission>
```

**Group permissions:**
```
/lp group <group> permission set <permission>
```

**Check permissions:**
```
/lp user <player> permission info
```

---

## Next Steps

- [Configure permission settings](configuration.md)
- [Set up economy integration](economy.md)
- [View available commands](commands.md)
