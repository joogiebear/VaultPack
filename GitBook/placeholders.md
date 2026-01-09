# PlaceholderAPI Integration

Complete guide to using VaultPack placeholders with PlaceholderAPI.

## Requirements

To use VaultPack placeholders, you need:
1. **PlaceholderAPI** plugin installed
2. **VaultPack** plugin installed
3. Both plugins loaded successfully

Verify installation with:
```
/papi info vaultpack
```

---

## Global Placeholders

These placeholders return general information about a player's backpack system.

### Total Slots
```
%vaultpack_total_slots%
```
Returns the number of backpack slots the player has unlocked.

**Example Output:** `5`

---

### Active Backpack Count
```
%vaultpack_active_count%
```
Returns the number of backpack slots that currently have a backpack stored.

**Example Output:** `3`

---

### Total Storage Capacity
```
%vaultpack_total_storage%
```
Returns the total number of inventory slots available across all active backpacks.

**Example Output:** `72` (e.g., 2 medium backpacks = 36 slots)

---

## Slot-Specific Placeholders

These placeholders provide information about a specific backpack slot. Replace `X` with the slot number (1-18).

### Slot Item Material
```
%vaultpack_slot_X_item%
```
Returns the material name representing the slot's status. Useful for GUI building.

**Possible Returns:**
- `gray_dye` - Slot is locked
- `lime_dye` - Slot is unlocked but empty
- `chest` - Slot has a backpack

**Example:**
```
%vaultpack_slot_1_item%  → chest
%vaultpack_slot_2_item%  → lime_dye
%vaultpack_slot_3_item%  → gray_dye
```

---

### Slot Display Name
```
%vaultpack_slot_X_name%
```
Returns a formatted display name for the slot.

**Possible Returns:**
- `Backpack Slot #X [LOCKED]` (gray) - Slot is locked
- `Backpack Slot #X [EMPTY]` (green) - Slot is empty
- `Backpack #X` (gold) - Slot has a backpack

**Example:**
```
%vaultpack_slot_1_name%  → Backpack #1
```

---

### Slot Unlocked Status
```
%vaultpack_slot_X_unlocked%
```
Returns whether the slot is unlocked.

**Returns:** `true` or `false`

**Example:**
```
%vaultpack_slot_1_unlocked%  → true
%vaultpack_slot_5_unlocked%  → false
```

---

### Slot Has Backpack
```
%vaultpack_slot_X_has_backpack%
```
Returns whether the slot currently contains a backpack.

**Returns:** `true` or `false`

**Example:**
```
%vaultpack_slot_1_has_backpack%  → true
%vaultpack_slot_2_has_backpack%  → false
```

---

### Backpack Tier Name
```
%vaultpack_slot_X_tier%
```
Returns the tier/type of the backpack in the slot.

**Returns:** Tier display name or `None`

**Example:**
```
%vaultpack_slot_1_tier%  → Medium Backpack
%vaultpack_slot_2_tier%  → None
```

---

### Backpack Size
```
%vaultpack_slot_X_size%
```
Returns the total number of slots in the backpack.

**Returns:** Number of slots (0 if empty)

**Example:**
```
%vaultpack_slot_1_size%  → 18
%vaultpack_slot_2_size%  → 0
```

---

### Backpack Used Slots
```
%vaultpack_slot_X_used%
```
Returns the number of slots currently occupied in the backpack.

**Returns:** Number of used slots (0 if empty)

**Example:**
```
%vaultpack_slot_1_used%  → 12
```

---

### Backpack Fullness Percentage
```
%vaultpack_slot_X_fullness%
```
Returns the percentage of the backpack that is full.

**Returns:** Percentage with one decimal place (e.g., `66.7%`)

**Example:**
```
%vaultpack_slot_1_fullness%  → 66.7%
```

---

### Backpack Fullness Bar
```
%vaultpack_slot_X_fullness_bar%
```
Returns a visual progress bar showing backpack fullness.

**Returns:** Colored progress bar

**Example:**
```
%vaultpack_slot_1_fullness_bar%  → [████████--]
```

---

## Usage Examples

### In Chat Messages

**Display player stats:**
```yaml
format: "&7[&6%vaultpack_total_slots%&7] &r%player_name%: %message%"
```

**Show backpack info:**
```
You have %vaultpack_active_count% backpacks with a total of %vaultpack_total_storage% slots!
```

---

### In Scoreboards

**Using FeatherBoard:**
```yaml
scoreboard:
  lines:
    - "&6&lBackpacks"
    - "&7Unlocked: &f%vaultpack_total_slots%"
    - "&7Active: &f%vaultpack_active_count%"
    - "&7Storage: &f%vaultpack_total_storage%"
```

---

### In Custom GUIs

**Using DeluxeMenus:**
```yaml
items:
  slot_1:
    material: "%vaultpack_slot_1_item%"
    display_name: "%vaultpack_slot_1_name%"
    lore:
      - "&7Tier: %vaultpack_slot_1_tier%"
      - "&7Capacity: %vaultpack_slot_1_used%/%vaultpack_slot_1_size%"
      - "&7Fullness: %vaultpack_slot_1_fullness%"
      - ""
      - "%vaultpack_slot_1_fullness_bar%"
```

---

### In Hologram Plugins

**Using DecentHolograms:**
```yaml
lines:
  - "&6&lYour Storage"
  - "&7Backpacks: &f%vaultpack_active_count%/%vaultpack_total_slots%"
  - "&7Total Space: &f%vaultpack_total_storage% slots"
```

---

### In Tab List

**Using TAB plugin:**
```yaml
tabprefix: "&7[BP:%vaultpack_total_slots%] "
```

---

## Conditional Formatting

Use placeholders with conditional plugins to create dynamic displays.

### With PlaceholderAPI's Conditional Placeholder

**Check if slot is unlocked:**
```
%placeholder_vaultpack_slot_1_unlocked_yes_&aUnlocked_no_&cLocked%
```

**Check if slot has backpack:**
```
%placeholder_vaultpack_slot_1_has_backpack_yes_&6Active_no_&7Empty%
```

---

### With DeluxeMenus Conditionals

```yaml
items:
  backpack_slot:
    material: "%vaultpack_slot_1_item%"
    display_name: "%vaultpack_slot_1_name%"
    requirements:
      slot_unlocked:
        type: placeholder
        placeholder: "%vaultpack_slot_1_unlocked%"
        value: "true"
        deny_commands:
          - "[message] &cThis slot is locked!"
```

---

## Complete Backpack Selector Example

Create a custom backpack selector GUI using placeholders:

```yaml
# DeluxeMenus Example
menu_title: "&8Backpacks - %vaultpack_active_count%/%vaultpack_total_slots%"
size: 54

items:
  # Slot 1
  slot_1:
    slot: 10
    material: "%vaultpack_slot_1_item%"
    display_name: "%vaultpack_slot_1_name%"
    lore:
      - "&7Tier: %vaultpack_slot_1_tier%"
      - "&7Space: %vaultpack_slot_1_used%/%vaultpack_slot_1_size%"
      - "%vaultpack_slot_1_fullness_bar%"
    left_click_commands:
      - "[player] backpack 1"

  # Slot 2
  slot_2:
    slot: 11
    material: "%vaultpack_slot_2_item%"
    display_name: "%vaultpack_slot_2_name%"
    lore:
      - "&7Tier: %vaultpack_slot_2_tier%"
      - "&7Space: %vaultpack_slot_2_used%/%vaultpack_slot_2_size%"
      - "%vaultpack_slot_2_fullness_bar%"
    left_click_commands:
      - "[player] backpack 2"

  # Continue for all 18 slots...

  # Info item
  info:
    slot: 49
    material: BOOK
    display_name: "&6&lStorage Statistics"
    lore:
      - "&7Total Backpacks: &f%vaultpack_active_count%"
      - "&7Unlocked Slots: &f%vaultpack_total_slots%"
      - "&7Total Storage: &f%vaultpack_total_storage% slots"
```

---

## Placeholder Testing

### Test in Chat
Use PlaceholderAPI's parse command:
```
/papi parse me %vaultpack_total_slots%
```

### Test All Slot Placeholders
```
/papi parse me %vaultpack_slot_1_tier%
/papi parse me %vaultpack_slot_1_size%
/papi parse me %vaultpack_slot_1_used%
/papi parse me %vaultpack_slot_1_fullness%
```

### Debug Mode
Enable debug in PlaceholderAPI config to see placeholder resolution:
```yaml
debug: true
```

---

## Common Issues

### Placeholders Show as %placeholder%

**Causes:**
- PlaceholderAPI is not installed
- VaultPack's PlaceholderAPI hook is disabled
- PlaceholderAPI hasn't loaded VaultPack's expansion

**Solutions:**
1. Install PlaceholderAPI
2. Enable in `config.yml`:
   ```yaml
   hooks:
     placeholderapi:
       enabled: true
   ```
3. Restart the server
4. Verify with `/papi info vaultpack`

---

### Placeholders Return Empty

**Causes:**
- Player has never joined the server
- Player data hasn't loaded yet

**Solutions:**
- Ensure the player has joined at least once
- Use placeholders only for online/valid players

---

### Slot Placeholders Always Return 0

**Causes:**
- Slot number is invalid (must be 1-18)
- Player doesn't have that slot unlocked
- No backpack is stored in that slot

**Solutions:**
- Verify slot number is correct
- Check if slot is unlocked with `%vaultpack_slot_X_unlocked%`
- Check if backpack exists with `%vaultpack_slot_X_has_backpack%`

---

## Placeholder Performance

VaultPack placeholders are optimized for performance:
- Data is cached in memory
- No database queries per placeholder
- Minimal CPU overhead

**Best Practices:**
- Use placeholders freely in GUIs and scoreboards
- Avoid excessive placeholder updates (>20/second per player)
- Consider using static displays where possible

---

## Related Documentation

- [GUI Menu Configuration](menus.md)
- [PlaceholderAPI Official Docs](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki)
- [DeluxeMenus Integration](https://wiki.helpch.at/clips-plugins/deluxemenus)
- [Commands Reference](commands.md)
