# Commands

Complete reference for all VaultPack commands.

## Player Commands

### /backpack (Aliases: /bp, /bag, /bags)
Main backpack command for players.

**Permission:** `vaultpack.use`

#### Usage

| Command | Description |
|---------|-------------|
| `/backpack` | Opens the backpack selector menu |
| `/backpack <slot>` | Opens a specific backpack slot (1-18) |
| `/backpack help` | Shows help information |

#### Examples
```
/backpack        # Opens backpack menu
/backpack 1      # Opens backpack in slot 1
/bp 3            # Opens backpack in slot 3 (using alias)
```

---

### /enderchest (Aliases: /ec, /echest)
Access ender chest pages.

**Permission:** `vaultpack.use`

#### Usage

| Command | Description |
|---------|-------------|
| `/enderchest` | Opens the first ender chest page |
| `/enderchest <page>` | Opens a specific ender chest page (1-9) |

#### Examples
```
/enderchest      # Opens page 1
/enderchest 2    # Opens page 2
/ec 5            # Opens page 5 (using alias)
```

---

### /storage (Aliases: /vault, /st)
Opens the unified storage menu.

**Permission:** `vaultpack.use`

#### Usage

| Command | Description |
|---------|-------------|
| `/storage` | Opens the unified storage menu with all backpacks and ender chest access |

#### Examples
```
/storage         # Opens storage menu
/vault           # Opens storage menu (using alias)
```

---

## Admin Commands

### /backpack give
Give a player access to a backpack slot.

**Permission:** `vaultpack.give`

#### Usage
```
/backpack give <player> <slot>
```

#### Examples
```
/backpack give Notch 1        # Gives Notch access to slot 1
/backpack give Steve 5        # Gives Steve access to slot 5
```

---

### /backpack remove
Remove a player's access to a backpack slot.

**Permission:** `vaultpack.remove`

#### Usage
```
/backpack remove <player> <slot>
```

**Warning:** This will clear all items in that backpack slot!

#### Examples
```
/backpack remove Notch 1      # Removes Notch's access to slot 1
/backpack remove Steve 5      # Removes Steve's access to slot 5
```

---

### /backpack clear
Clear all items from a player's backpack slot.

**Permission:** `vaultpack.clear`

#### Usage
```
/backpack clear <player> <slot>
```

**Warning:** This action is permanent and cannot be undone!

#### Examples
```
/backpack clear Notch 1       # Clears all items in Notch's slot 1
/backpack clear Steve all     # Clears all of Steve's backpack slots
```

---

### /backpack reload
Reload plugin configuration.

**Permission:** `vaultpack.reload`

#### Usage
```
/backpack reload
```

This will reload:
- `config.yml`
- `backpacks.yml`
- `lang.yml`
- All menu files in `menus/` folder
- Player data from storage

**Note:** Players currently viewing backpacks may need to reopen them to see changes.

#### Examples
```
/backpack reload              # Reloads all configuration
```

---

## Internal Commands

### /vaultpack
Internal command for GUI interactions and programmatic operations.

**Permission:** `vaultpack.use`

**Warning:** This command is primarily used internally by the plugin. Direct use is not recommended for players.

#### Subcommands

| Command | Description |
|---------|-------------|
| `/vaultpack open <slot>` | Opens a backpack slot |
| `/vaultpack remove <slot>` | Removes a backpack from a slot |
| `/vaultpack upgrade <slot>` | Upgrades a backpack in a slot |
| `/vaultpack unlock <slot>` | Unlocks a backpack slot |

These commands are typically executed through GUI clicks and should not be run manually.

---

## Command Permissions Summary

| Command | Permission | Default |
|---------|------------|---------|
| `/backpack` | `vaultpack.use` | Everyone |
| `/enderchest` | `vaultpack.use` | Everyone |
| `/storage` | `vaultpack.use` | Everyone |
| `/backpack give` | `vaultpack.give` | Operators |
| `/backpack remove` | `vaultpack.remove` | Operators |
| `/backpack clear` | `vaultpack.clear` | Operators |
| `/backpack reload` | `vaultpack.reload` | Operators |
| `/vaultpack` | `vaultpack.use` | Everyone |

---

## Tab Completion

All commands support tab completion:

### /backpack
- `help` - Show help
- `give` - Give slot access
- `remove` - Remove slot access
- `clear` - Clear backpack
- `reload` - Reload config
- `<slot>` - Slot numbers (1-18)

### /enderchest
- `<page>` - Page numbers (1-9)

### /backpack give/remove/clear
- `<player>` - Online player names
- `<slot>` - Slot numbers or "all"

---

## Command Tips

### Using Aliases
All main commands have shorter aliases for convenience:
- `/backpack` → `/bp` or `/bag` or `/bags`
- `/enderchest` → `/ec` or `/echest`
- `/storage` → `/vault` or `/st`

### Quick Access
For fastest access to specific backpacks:
```
/bp 1     # Quickest way to open slot 1
/ec 2     # Quickest way to open ender page 2
```

### Admin Operations
When managing player backpacks:
1. Use `/backpack give` to grant access
2. Players can then unlock with economy/permissions
3. Use `/backpack remove` only when necessary (destructive!)
4. Use `/backpack clear` to reset contents without removing access

---

## Next Steps

- [View all permissions](permissions.md)
- [Configure command settings](configuration.md)
- [Customize command messages](lang.md)
