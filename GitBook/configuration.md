# Configuration

Complete guide to configuring VaultPack through `config.yml`.

## File Location

The configuration file is located at:
```
plugins/VaultPack/config.yml
```

After making changes, reload the configuration with:
```
/backpack reload
```

---

## Storage Limits

Configure maximum storage capacity and defaults for players.

```yaml
storage:
  # Maximum backpack slots players can unlock
  max-backpack-slots: 18

  # Default unlocked slots for new players
  default-unlocked-slots: 1

  # Maximum ender chest pages
  max-enderchest-pages: 9

  # Default unlocked ender pages for new players
  default-unlocked-pages: 1
```

### Options

| Option | Description | Default | Range |
|--------|-------------|---------|-------|
| `max-backpack-slots` | Maximum backpack slots available | 18 | 1-18 |
| `default-unlocked-slots` | Slots unlocked for new players | 1 | 0-18 |
| `max-enderchest-pages` | Maximum ender chest pages | 9 | 1-9 |
| `default-unlocked-pages` | Pages unlocked for new players | 1 | 0-9 |

---

## Economy Settings

Configure Vault economy integration and costs.

```yaml
economy:
  enabled: true

  # Backpack slot unlock costs
  slot-unlock:
    enabled: true
    # Formula: base-cost + (slot-number * cost-per-slot)
    base-cost: 1000
    cost-per-slot: 500

  # Ender chest page unlock costs
  page-unlock:
    enabled: true
    # Formula: base-cost + (page-number * cost-per-page)
    base-cost: 2000
    cost-per-page: 1000

  # Backpack upgrade costs
  upgrade-costs:
    small-to-medium: 5000
    medium-to-large: 10000
    large-to-huge: 20000
    huge-to-massive: 40000
    massive-to-colossal: 80000
    colossal-to-greater: 160000
    greater-to-jumbo: 320000
```

### Economy Options

| Option | Description | Default |
|--------|-------------|---------|
| `enabled` | Enable economy features | true |
| `slot-unlock.enabled` | Enable slot unlock costs | true |
| `slot-unlock.base-cost` | Base cost for slot unlocking | 1000 |
| `slot-unlock.cost-per-slot` | Additional cost per slot number | 500 |
| `page-unlock.enabled` | Enable page unlock costs | true |
| `page-unlock.base-cost` | Base cost for page unlocking | 2000 |
| `page-unlock.cost-per-page` | Additional cost per page number | 1000 |

### Cost Formula Examples

**Slot Unlocking:**
- Slot 1: 1000 + (1 × 500) = 1500
- Slot 2: 1000 + (2 × 500) = 2000
- Slot 3: 1000 + (3 × 500) = 2500
- Slot 10: 1000 + (10 × 500) = 6000

**Page Unlocking:**
- Page 1: 2000 + (1 × 1000) = 3000
- Page 2: 2000 + (2 × 1000) = 4000
- Page 3: 2000 + (3 × 1000) = 5000

**Tip:** Increase `cost-per-slot` to make higher slots more expensive.

---

## Permission Settings

Configure permission-based unlocking as an alternative to economy.

```yaml
permissions:
  # Use permissions for slot unlocking
  use-for-slots: true
  slot-format: "vaultpack.slots.%slot%"

  # Use permissions for page unlocking
  use-for-pages: true
  page-format: "vaultpack.enderchest.page.%page%"

  # If both economy and permissions are enabled:
  # false = Player can unlock with EITHER money OR permission
  # true = Player needs BOTH money AND permission
  require-both: false
```

### Permission Options

| Option | Description | Default |
|--------|-------------|---------|
| `use-for-slots` | Enable permission-based slot access | true |
| `slot-format` | Permission format for slots | See above |
| `use-for-pages` | Enable permission-based page access | true |
| `page-format` | Permission format for pages | See above |
| `require-both` | Require both money AND permission | false |

### Configuration Scenarios

**Scenario 1: Economy Only**
```yaml
permissions:
  use-for-slots: false
  use-for-pages: false
```
Players must pay money to unlock.

**Scenario 2: Permissions Only**
```yaml
economy:
  enabled: false
permissions:
  use-for-slots: true
  use-for-pages: true
```
Players need permissions to access slots/pages.

**Scenario 3: Either/Or (Recommended)**
```yaml
economy:
  enabled: true
permissions:
  use-for-slots: true
  use-for-pages: true
  require-both: false
```
Players can unlock with either money or permissions.

---

## Item Restrictions

Control which items can be stored in backpacks.

```yaml
item-blacklist:
  enabled: true

  # Prevent these items from being stored
  materials:
    - BEDROCK
    - BARRIER
    - COMMAND_BLOCK
    - CHAIN_COMMAND_BLOCK
    - REPEATING_COMMAND_BLOCK
    - COMMAND_BLOCK_MINECART
    - STRUCTURE_BLOCK
    - STRUCTURE_VOID
    - JIGSAW

  # Prevent shulker boxes (prevents nested storage)
  prevent-shulker-boxes: false

  # Also prevent custom shulker box variants
  prevent-all-shulker-variants: false

  # Message when blacklisted item is attempted
  message: "&cThis item cannot be stored in backpacks!"
```

### Blacklist Options

| Option | Description | Default |
|--------|-------------|---------|
| `enabled` | Enable item restrictions | true |
| `materials` | List of blocked materials | See above |
| `prevent-shulker-boxes` | Block all shulker boxes | false |
| `prevent-all-shulker-variants` | Block custom shulkers | false |
| `message` | Error message to display | See above |

**Common materials to blacklist:**
- Admin items (BEDROCK, BARRIER)
- Command blocks
- Structure blocks
- Shulker boxes (to prevent nested storage exploits)

---

## Death Protection

Configure item protection on player death.

```yaml
death-protection:
  # Keep backpack items on death
  keep-backpack-items: true

  # Keep ender chest items on death
  keep-ender-items: true

  # Respect keepInventory gamerule
  respect-keep-inventory-gamerule: true
```

### Death Protection Options

| Option | Description | Default |
|--------|-------------|---------|
| `keep-backpack-items` | Protect backpack items on death | true |
| `keep-ender-items` | Protect ender chest items | true |
| `respect-keep-inventory-gamerule` | Follow server gamerule | true |

### How It Works

**When `keep-backpack-items: true`:**
- Backpack contents are never dropped on death
- Items remain in backpacks after respawn

**When `respect-keep-inventory-gamerule: true`:**
- If `/gamerule keepInventory true` → backpacks are kept
- If `/gamerule keepInventory false` → follows plugin setting

---

## Plugin Integration

Configure integration with other plugins.

```yaml
hooks:
  # Vault - Economy support
  vault:
    enabled: true

  # PlaceholderAPI - Placeholder support
  placeholderapi:
    enabled: true

  # EcoItems - Custom item support
  ecoitems:
    enabled: true
```

### Integration Options

| Plugin | Required For | Default |
|--------|--------------|---------|
| Vault | Economy features | true |
| PlaceholderAPI | GUI placeholders | true |
| EcoItems | Custom recipe items | true |

**Note:** These settings only matter if the plugins are installed. If a plugin is not detected, its features are automatically disabled.

---

## Data Storage

Configure how player data is saved.

```yaml
data:
  # Auto-save interval (in seconds)
  auto-save-interval: 300

  # Save data when player logs out
  save-on-logout: true

  # Save data when plugin disables
  save-on-disable: true

  # Storage format (json or yaml)
  format: json

  # Compress data to save space
  compress: true
```

### Data Options

| Option | Description | Default |
|--------|-------------|---------|
| `auto-save-interval` | Seconds between auto-saves | 300 |
| `save-on-logout` | Save on player disconnect | true |
| `save-on-disable` | Save on server shutdown | true |
| `format` | Data format (json/yaml) | json |
| `compress` | Enable data compression | true |

**Recommendations:**
- Keep `auto-save-interval` at 300 (5 minutes) for safety
- Always enable `save-on-logout` and `save-on-disable`
- Use `json` format for better performance
- Enable `compress` to reduce file sizes

---

## Advanced Settings

Additional configuration options.

```yaml
advanced:
  # Debug mode (verbose console logging)
  debug: false

  # Check for updates on startup
  check-updates: true

  # Metrics (bStats) - Help us improve!
  metrics: true

  # Max backpack title length
  max-title-length: 32

  # Allow backpacks in backpacks (not recommended!)
  allow-nested-backpacks: false

  # Enable shift-click to move items
  shift-click-to-backpack: true
```

### Advanced Options

| Option | Description | Default |
|--------|-------------|---------|
| `debug` | Enable debug logging | false |
| `check-updates` | Check for new versions | true |
| `metrics` | Send anonymous usage stats | true |
| `max-title-length` | Maximum GUI title length | 32 |
| `allow-nested-backpacks` | Allow backpacks in backpacks | false |
| `shift-click-to-backpack` | Enable shift-click transfers | true |

**Warning:** Enabling `allow-nested-backpacks` can cause duplication exploits and is not recommended!

---

## Sound Configuration

Configure sounds played during various actions.

```yaml
sounds:
  enabled: true
  volume: 1.0
  pitch: 1.0

  # Specific sounds
  backpack-open: BLOCK_CHEST_OPEN
  backpack-close: BLOCK_CHEST_CLOSE
  enderchest-open: BLOCK_ENDER_CHEST_OPEN
  enderchest-close: BLOCK_ENDER_CHEST_CLOSE
  slot-unlock: ENTITY_PLAYER_LEVELUP
  backpack-upgrade: BLOCK_ANVIL_USE
  backpack-place: ENTITY_ITEM_PICKUP
  backpack-remove: ENTITY_ITEM_PICKUP
  error: ENTITY_VILLAGER_NO
```

### Sound Options

| Option | Description | Default | Range |
|--------|-------------|---------|-------|
| `enabled` | Enable all sounds | true | - |
| `volume` | Sound volume | 1.0 | 0.0-1.0 |
| `pitch` | Sound pitch | 1.0 | 0.5-2.0 |

### Custom Sounds

You can change any sound to a different Minecraft sound effect. Find available sounds at:
https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html

**Examples:**
```yaml
backpack-open: BLOCK_CHEST_OPEN
backpack-close: BLOCK_CHEST_CLOSE
slot-unlock: ENTITY_PLAYER_LEVELUP
error: ENTITY_VILLAGER_NO
```

---

## Configuration Tips

### 1. Balance Economy Costs
Start with low costs and increase based on your economy:
```yaml
slot-unlock:
  base-cost: 500      # For early slots
  cost-per-slot: 250  # Gradual increase
```

### 2. Limit Free Access
Prevent players from getting everything for free:
```yaml
storage:
  default-unlocked-slots: 1  # Only 1 free slot
  default-unlocked-pages: 1  # Only 1 free page
```

### 3. Use Hybrid System
Give basic access via permissions, advanced via economy:
```yaml
permissions:
  use-for-slots: true
  require-both: false  # Can use either method
```

### 4. Prevent Exploits
Always blacklist problematic items:
```yaml
item-blacklist:
  enabled: true
  prevent-shulker-boxes: true  # Prevent nested storage
  allow-nested-backpacks: false
```

---

## Related Documentation

- [Backpack Types Configuration](backpacks.md)
- [Menu Customization](menus.md)
- [Language Configuration](lang.md)
- [Permissions Setup](permissions.md)

---

## Troubleshooting

**Changes not applying?**
- Run `/backpack reload` after editing config
- Check for YAML syntax errors
- Restart server if reload doesn't work

**Economy not working?**
- Ensure Vault is installed
- Ensure an economy plugin (EssentialsX, CMI) is installed
- Check `economy.enabled: true` in config

**Permissions not working?**
- Check `permissions.use-for-slots: true`
- Verify permission plugin is properly configured
- Test with `/lp user <player> permission check vaultpack.slots.1`
