# Getting Started

This guide will help you install and set up VaultPack on your Minecraft server.

## Requirements

### Server Requirements
- Minecraft version 1.21 or higher
- Java 17 or higher
- Spigot, Paper, or compatible fork

### Optional Dependencies
VaultPack works standalone but integrates with these plugins for enhanced features:
- **Vault** - Required for economy features
- **PlaceholderAPI** - Required for GUI placeholders and chat integration
- **EcoItems** - For custom item support in crafting recipes

## Installation

### 1. Download the Plugin
Download the latest version of VaultPack from:
- SpigotMC
- Your purchase platform
- Discord support server

### 2. Install the Plugin
1. Stop your server
2. Place `VaultPack.jar` in your server's `plugins/` folder
3. If you want economy features, install Vault and an economy plugin (e.g., EssentialsX, CMI)
4. If you want PlaceholderAPI support, install PlaceholderAPI
5. Start your server

### 3. First Launch
On first launch, VaultPack will:
- Generate default configuration files in `plugins/VaultPack/`
- Create the following files:
  - `config.yml` - Main plugin configuration
  - `backpacks.yml` - Backpack tier definitions
  - `lang.yml` - Language/messages
  - `menus/` folder - GUI menu configurations
  - `data/` folder - Player data storage

## Initial Configuration

### Basic Setup

After installation, you should configure these basic settings:

#### 1. Economy Settings
Edit `config.yml`:
```yaml
economy:
  enabled: true  # Set to false if you don't use economy

  slot-unlock:
    enabled: true
    base-cost: 1000
    cost-per-slot: 500
```

#### 2. Storage Limits
Configure maximum slots and pages:
```yaml
storage:
  max-backpack-slots: 18  # Maximum slots players can unlock
  default-unlocked-slots: 1  # Slots unlocked by default
  max-enderchest-pages: 9  # Maximum ender pages
  default-unlocked-pages: 1  # Pages unlocked by default
```

#### 3. Permissions
Decide whether to use permissions or economy:
```yaml
permissions:
  use-for-slots: true  # Allow permission-based slot unlocking
  use-for-pages: true  # Allow permission-based page unlocking
  require-both: false  # If true, requires BOTH money AND permission
```

### Default Player Setup

By default, new players will have:
- 1 backpack slot unlocked
- 1 ender chest page unlocked
- Permission to use `/backpack`, `/enderchest`, and `/storage` commands

To change this, modify:
```yaml
storage:
  default-unlocked-slots: 1  # Change this value
  default-unlocked-pages: 1  # Change this value
```

## Verifying Installation

### Check Plugin Status
Run the following command in console or in-game:
```
/plugins
```

You should see VaultPack in green, indicating it's loaded successfully.

### Check Dependencies
Look at the server startup log:
```
[VaultPack] Starting VaultPack plugin...
[VaultPack] VaultPack plugin enabled successfully!
[VaultPack] Vault: ✓
[VaultPack] PlaceholderAPI: ✓
```

- ✓ means the dependency is detected
- ✗ means the dependency is not found (features will be limited)

### Test Basic Functionality
1. Join the server as a player
2. Run `/backpack` to open the backpack menu
3. Run `/storage` to open the unified storage menu
4. Run `/enderchest` to access ender chest pages

If these commands work, VaultPack is properly installed!

## Next Steps

Now that VaultPack is installed, you can:
1. [Customize your configuration](configuration.md)
2. [Set up permissions for your players](permissions.md)
3. [Configure backpack types and recipes](backpacks.md)
4. [Customize menu appearances](menus.md)
5. [Set up PlaceholderAPI integration](placeholders.md)

## Troubleshooting

### Plugin won't load
- Check that you're running Minecraft 1.21 or higher
- Verify Java 17+ is installed
- Check server logs for specific errors

### Economy features don't work
- Install Vault plugin
- Install an economy plugin (EssentialsX, CMI, etc.)
- Restart the server

### GUI placeholders show as %placeholder%
- Install PlaceholderAPI
- Restart the server
- Run `/papi reload`

### Players can't craft backpacks
- Check that recipes are properly configured in `backpacks.yml`
- Verify players have the crafting permission (e.g., `vaultpack.craft.small`)
- Check if EcoItems is required for certain recipes

For more help, visit our [Troubleshooting Guide](troubleshooting.md) or join the Discord support server.
