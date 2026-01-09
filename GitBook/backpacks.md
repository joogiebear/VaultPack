# Backpack Types

Complete guide to configuring backpack types in `backpacks.yml`.

## File Location

The backpack definitions file is located at:
```
plugins/VaultPack/backpacks.yml
```

After making changes, reload with:
```
/backpack reload
```

---

## Overview

VaultPack supports 5 default backpack tiers, each with increasing capacity:

| Tier | Size | Rows | Rarity |
|------|------|------|--------|
| Small | 9 slots | 1 | Uncommon |
| Medium | 18 slots | 2 | Rare |
| Large | 27 slots | 3 | Epic |
| Greater | 36 slots | 4 | Celestial |
| Jumbo | 45 slots | 5 | Legendary |

---

## Backpack Structure

Each backpack type is defined with the following structure:

```yaml
backpacks:
  tier_name:
    display-name: "&aBackpack Name"
    rarity: "&aUncommon"
    tier: tier_name
    size: 9
    rows: 1
    material: PLAYER_HEAD
    texture: "base64_texture_string"
    custom-model-data: 0
    glow: false
    recipe:
      - "material amount"
      - "material amount"
      # ... 9 items total
    upgrade-from: previous_tier
    crafting-permission: "vaultpack.craft.tier_name"
    lore:
      - "Lore line 1"
      - "Lore line 2"
```

---

## Configuration Options

### Basic Properties

| Property | Description | Type | Required |
|----------|-------------|------|----------|
| `display-name` | Display name with color codes | String | Yes |
| `rarity` | Rarity text (cosmetic) | String | Yes |
| `tier` | Internal tier identifier | String | Yes |
| `size` | Number of inventory slots | Integer | Yes |
| `rows` | Number of rows (size ÷ 9) | Integer | Yes |

**Example:**
```yaml
display-name: "&9Medium Backpack"
rarity: "&9Rare"
tier: medium
size: 18
rows: 2
```

---

### Item Appearance

| Property | Description | Type | Default |
|----------|-------------|------|---------|
| `material` | Item material type | Material | PLAYER_HEAD |
| `texture` | Base64 skull texture | String | None |
| `custom-model-data` | Custom model data value | Integer | 0 |
| `glow` | Apply enchantment glow | Boolean | false |

**Example:**
```yaml
material: PLAYER_HEAD
texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI2ZTAzMTg2MDI0YzRiZDRjYjQ0NGY4YzYwNzUxZTQ4ODgzOTNhYjE1NGY0YTgzNTcxNDQwMWRkNDM4YTM0MiJ9fX0="
custom-model-data: 0
glow: true
```

**Finding Skull Textures:**
- Visit https://minecraft-heads.com
- Copy the "Value" field from the give command
- Paste into the `texture` field

---

### Crafting Recipe

Recipes are defined as a list of 9 items in crafting table order (left-to-right, top-to-bottom).

**Format:**
```yaml
recipe:
  - "material amount"  # Top-left
  - "material amount"  # Top-center
  - "material amount"  # Top-right
  - "material amount"  # Middle-left
  - "material amount"  # Middle-center (often the upgrade item)
  - "material amount"  # Middle-right
  - "material amount"  # Bottom-left
  - "material amount"  # Bottom-center
  - "material amount"  # Bottom-right
```

**Special Formats:**
- `"material amount"` - Vanilla material with amount (e.g., `"leather 64"`)
- `"material"` - Vanilla material with amount 1 (e.g., `"diamond"`)
- `"vaultpack:tier"` - Previous backpack tier (e.g., `"vaultpack:small"`)
- `"ecoitems:item_id"` - EcoItems custom item (e.g., `"ecoitems:enchanted_leather 6"`)
- `""` - Empty slot

**Example 1: Basic Recipe**
```yaml
recipe:
  - "leather 10"
  - "leather 10"
  - "leather 10"
  - "leather 10"
  - ""
  - "leather 10"
  - "leather 10"
  - "leather 10"
  - "leather 10"
```

**Example 2: Upgrade Recipe**
```yaml
recipe:
  - "leather 64"
  - "leather 64"
  - "leather 64"
  - "leather 64"
  - "vaultpack:small"      # Previous tier in center
  - "leather 64"
  - "leather 64"
  - "leather 64"
  - "leather 64"
```

**Example 3: Custom Items (EcoItems)**
```yaml
recipe:
  - "ecoitems:enchanted_leather 6"
  - "ecoitems:enchanted_leather 6"
  - "ecoitems:enchanted_leather 6"
  - "ecoitems:enchanted_leather 6"
  - "vaultpack:medium"
  - "ecoitems:enchanted_leather 6"
  - "ecoitems:enchanted_leather 6"
  - "ecoitems:enchanted_leather 6"
  - "ecoitems:enchanted_leather 6"
```

---

### Upgrade Path

| Property | Description | Type | Required |
|----------|-------------|------|----------|
| `upgrade-from` | Previous tier name | String | No |
| `crafting-permission` | Permission to craft | String | Yes |

**Example:**
```yaml
upgrade-from: small
crafting-permission: "vaultpack.craft.medium"
```

**How Upgrades Work:**
1. Player crafts higher-tier backpack using lower-tier
2. All items from old backpack transfer to new one
3. Old backpack is consumed in crafting
4. New backpack appears with all previous contents

---

### Lore

Lore lines support placeholders and color codes.

**Available Placeholders:**
- `%tier%` - Backpack tier name
- `%size%` - Maximum capacity
- `%used%` - Currently used slots
- `%rows%` - Number of rows

**Example:**
```yaml
lore:
  - ""
  - "&7Rarity: &9Rare"
  - "&7Tier: &9%tier%"
  - "&7Capacity: &f%used%/%size% items"
  - ""
  - "&6Right-click to open"
  - "&7Drag into backpack menu to store"
```

---

## Default Backpack Tiers

### Small Backpack

Entry-level backpack with 9 slots.

```yaml
small:
  display-name: "&aSmall Backpack"
  rarity: "&aUncommon"
  tier: small
  size: 9
  rows: 1
  material: PLAYER_HEAD
  texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI2ZTAzMTg2MDI0YzRiZDRjYjQ0NGY4YzYwNzUxZTQ4ODgzOTNhYjE1NGY0YTgzNTcxNDQwMWRkNDM4YTM0MiJ9fX0="
  custom-model-data: 0
  glow: false
  recipe:
    - "leather 10"
    - "leather 10"
    - "leather 10"
    - "leather 10"
    - ""
    - "leather 10"
    - "leather 10"
    - "leather 10"
    - "leather 10"
  crafting-permission: "vaultpack.craft.small"
  lore:
    - ""
    - "&7Rarity: &aUncommon"
    - "&7Tier: &a%tier%"
    - "&7Capacity: &f%used%/%size% items"
    - ""
    - "&6Right-click to open"
```

**Crafting Cost:** 80 leather

---

### Medium Backpack

Mid-tier backpack with 18 slots.

```yaml
medium:
  display-name: "&9Medium Backpack"
  rarity: "&9Rare"
  tier: medium
  size: 18
  rows: 2
  material: PLAYER_HEAD
  glow: true
  recipe:
    - "leather 64"
    - "leather 64"
    - "leather 64"
    - "leather 64"
    - "vaultpack:small"
    - "leather 64"
    - "leather 64"
    - "leather 64"
    - "leather 64"
  upgrade-from: small
  crafting-permission: "vaultpack.craft.medium"
```

**Crafting Cost:** 512 leather + 1 small backpack

---

### Large Backpack

High-tier backpack with 27 slots (requires EcoItems).

```yaml
large:
  display-name: "&5Large Backpack"
  rarity: "&5Epic"
  tier: large
  size: 27
  rows: 3
  material: PLAYER_HEAD
  glow: true
  recipe:
    - "ecoitems:enchanted_leather 6"
    - "ecoitems:enchanted_leather 6"
    - "ecoitems:enchanted_leather 6"
    - "ecoitems:enchanted_leather 6"
    - "vaultpack:medium"
    - "ecoitems:enchanted_leather 6"
    - "ecoitems:enchanted_leather 6"
    - "ecoitems:enchanted_leather 6"
    - "ecoitems:enchanted_leather 6"
  upgrade-from: medium
  crafting-permission: "vaultpack.craft.large"
```

**Crafting Cost:** 48 enchanted leather + 1 medium backpack

---

### Greater Backpack

Premium tier with 36 slots.

```yaml
greater:
  display-name: "&b&lGreater Backpack"
  rarity: "&b&lCelestial"
  tier: greater
  size: 36
  rows: 4
  material: PLAYER_HEAD
  custom-model-data: 1
  glow: true
  recipe:
    - "ecoitems:enchanted_leather 12"
    - "ecoitems:enchanted_leather 12"
    - "ecoitems:enchanted_leather 12"
    - "ecoitems:enchanted_leather 12"
    - "vaultpack:large"
    - "ecoitems:enchanted_leather 12"
    - "ecoitems:enchanted_leather 12"
    - "ecoitems:enchanted_leather 12"
    - "ecoitems:enchanted_leather 12"
  upgrade-from: large
  crafting-permission: "vaultpack.craft.greater"
```

**Crafting Cost:** 96 enchanted leather + 1 large backpack

---

### Jumbo Backpack

Ultimate tier with 45 slots (5 rows).

```yaml
jumbo:
  display-name: "&6&lJumbo Backpack"
  rarity: "&6&lLegendary"
  tier: jumbo
  size: 45
  rows: 5
  material: PLAYER_HEAD
  custom-model-data: 2
  glow: true
  recipe:
    - ""
    - "ecoitems:jumbo_backpack_upgrade"
    - ""
    - ""
    - "vaultpack:greater"
    - ""
    - ""
    - ""
    - ""
  upgrade-from: greater
  crafting-permission: "vaultpack.craft.jumbo"
```

**Crafting Cost:** 1 upgrade token + 1 greater backpack

---

## Creating Custom Tiers

You can create additional custom backpack tiers!

### Example: "Massive" Tier (54 slots)

**Note:** Minecraft GUIs support up to 54 slots (6 rows).

```yaml
massive:
  display-name: "&c&lMassive Backpack"
  rarity: "&c&lMythic"
  tier: massive
  size: 54
  rows: 6
  material: PLAYER_HEAD
  texture: "your_custom_texture_here"
  custom-model-data: 3
  glow: true
  recipe:
    - "ecoitems:mythic_essence"
    - "ecoitems:mythic_essence"
    - "ecoitems:mythic_essence"
    - "ecoitems:mythic_essence"
    - "vaultpack:jumbo"
    - "ecoitems:mythic_essence"
    - "ecoitems:mythic_essence"
    - "ecoitems:mythic_essence"
    - "ecoitems:mythic_essence"
  upgrade-from: jumbo
  crafting-permission: "vaultpack.craft.massive"
  lore:
    - ""
    - "&7Rarity: &c&lMythic"
    - "&7Tier: &c&l%tier%"
    - "&7Capacity: &f%used%/%size% items"
    - ""
    - "&6Right-click to open"
    - "&8The peak of storage technology!"
```

**Steps to add:**
1. Add the tier to `backpacks.yml`
2. Add crafting permission to `plugin.yml`
3. Reload with `/backpack reload`

---

## Recipe Balancing Tips

### Early Game (Small/Medium)
Use common materials:
```yaml
- "leather 10"
- "iron_ingot 5"
- "string 16"
```

### Mid Game (Large)
Use harder-to-obtain vanilla items:
```yaml
- "diamond 8"
- "emerald 4"
- "netherite_scrap 2"
```

### End Game (Greater/Jumbo)
Use custom items from EcoItems:
```yaml
- "ecoitems:enchanted_leather 12"
- "ecoitems:dragon_scale 5"
- "ecoitems:upgrade_token 1"
```

### Upgrade Costs
Set economy costs in `config.yml`:
```yaml
economy:
  upgrade-costs:
    small-to-medium: 5000
    medium-to-large: 10000
    large-to-greater: 20000
    greater-to-jumbo: 40000
```

---

## Common Issues

### Recipe Not Working
- Check YAML indentation (use spaces, not tabs)
- Verify material names match Bukkit materials
- For EcoItems, ensure items exist in EcoItems config
- Check crafting permission is granted

### Backpack Not Appearing
- Run `/backpack reload` after changes
- Check console for YAML errors
- Verify tier name is unique

### Texture Not Showing
- Ensure texture string has no line breaks
- Use PLAYER_HEAD material for custom skulls
- Get textures from minecraft-heads.com

### Upgrade Not Working
- Verify `upgrade-from` matches previous tier name exactly
- Check upgrade costs in `config.yml`
- Ensure player has economy balance or bypass permission

---

## Related Documentation

- [Economy Configuration](economy.md)
- [EcoItems Integration](ecoitems.md)
- [Crafting Permissions](permissions.md)
- [Main Configuration](configuration.md)
