# VaultPack

VaultPack is a Paper/Folia-ready Minecraft storage plugin that adds configurable backpacks, virtual ender chest pages, economy unlocks, and optional integrations for Vault, PlaceholderAPI, and EcoItems.

## Requirements

- Java 21
- Paper-compatible server using API version 1.21+
- Vault or VaultUnlocked for economy-backed unlocks
- An economy provider if economy unlocks are enabled

Optional integrations:

- PlaceholderAPI for placeholders
- EcoItems for custom item crafting ingredients

## Build

Use the committed Gradle wrapper from the repository root:

```bash
./gradlew clean build
```

The installable plugin jar is generated at:

```text
build/libs/VaultPack-3.0.0.jar
```

Do not install Gradle cache files or any `original-*` jar if one is produced by a future build setup. Use the shaded `VaultPack-<version>.jar` from `build/libs`.

## Installation

1. Stop the server.
2. Copy `build/libs/VaultPack-3.0.0.jar` into the server `plugins/` folder.
3. Install Vault/VaultUnlocked and an economy provider if economy features are enabled.
4. Start the server once to generate configuration files.
5. Review:
   - `plugins/VaultPack/config.yml`
   - `plugins/VaultPack/backpacks.yml`
   - `plugins/VaultPack/lang.yml`
   - `plugins/VaultPack/menus/`
6. Restart after major configuration changes.

## Main commands

- `/backpack` or `/bp` - Open the backpack selector.
- `/backpack open <slot>` - Open a specific backpack slot.
- `/enderchest` or `/ec` - Open ender chest page selector.
- `/enderchest open <page>` - Open a specific ender chest page.
- `/storage` or `/vault` - Open the unified storage menu.
- `/vaultpack reload` - Reload plugin configuration.

## Important permissions

- `vaultpack.use` - Allows normal plugin usage.
- `vaultpack.admin` - Grants admin command access.
- `vaultpack.bypass.cost` - Bypass economy costs.
- `vaultpack.slots.<number>` - Grants a backpack slot.
- `vaultpack.slots.*` - Grants all backpack slots.
- `vaultpack.enderchest.page.<number>` - Grants an ender chest page.
- `vaultpack.enderchest.*` - Grants all ender chest pages.
- `vaultpack.craft.<type>` - Allows crafting a backpack type.
- `vaultpack.craft.*` - Allows crafting all backpack types.

## Configuration notes

### Storage

YAML storage is the default and works out of the box:

```yaml
data:
  storage-type: yaml
```

MySQL storage is available for larger networks:

```yaml
data:
  storage-type: mysql
```

When using MySQL, configure the `database:` section in `config.yml` before switching storage type.

### bStats metrics

Metrics are disabled until a real bStats plugin id is configured:

```yaml
advanced:
  metrics: true
  bstats-plugin-id: 0
```

After registering VaultPack on bStats, replace `0` with the assigned plugin id. Keeping the value at `0` leaves metrics off.

### Nested storage safety

VaultPack blocks nested backpacks by default through `advanced.allow-nested-backpacks: false`. For stricter production servers, consider also blocking shulker boxes in backpack storage:

```yaml
item-blacklist:
  prevent-shulker-boxes: true
  prevent-all-shulker-variants: true
```

## EcoItems recipes

Backpack recipes can include custom item identifiers such as `ecoitems:<id>` in `backpacks.yml`. EcoItems is optional; recipes using EcoItems will only validate when EcoItems is installed and compatible.

## Development

Run a local build before pushing:

```bash
./gradlew clean build
```

GitHub Actions runs the same build on pull requests and pushes to `main`.
