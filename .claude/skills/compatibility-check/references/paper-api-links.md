# Paper API Documentation Links

Quick reference for fetching specific Paper documentation when needed.

## Primary Documentation

- **Main docs**: https://docs.papermc.io/
- **Paper development guide**: https://docs.papermc.io/paper/dev/
- **Javadocs**: https://jd.papermc.io/

## Getting Started

- **Project setup**: https://docs.papermc.io/paper/dev/project-setup/
- **Paper plugins (paper-plugin.yml)**: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/
- **How plugins work**: https://docs.papermc.io/paper/dev/how-do-plugins-work/
- **plugin.yml reference**: https://docs.papermc.io/paper/dev/plugin-yml/

## Core APIs

- **Data Components (1.20.5+)**: https://docs.papermc.io/paper/dev/api/data-components/
- **Persistent Data Container (PDC)**: https://docs.papermc.io/paper/dev/api/pdc/
- **Scheduling**: https://docs.papermc.io/paper/dev/api/scheduling/
- **Plugin Configuration**: https://docs.papermc.io/paper/dev/plugin-configurations/
- **Registries**: https://docs.papermc.io/paper/dev/api/registries/
- **Plugin Messaging**: https://docs.papermc.io/paper/dev/api/plugin-messaging/
- **Recipes**: https://docs.papermc.io/paper/dev/api/recipes/
- **Particles**: https://docs.papermc.io/paper/dev/api/particles/
- **Dialog API (1.21.7+)**: https://docs.papermc.io/paper/dev/api/dialog/

## Folia Support

- **Supporting Paper and Folia**: https://docs.papermc.io/paper/dev/folia-support/
- **Folia overview**: https://docs.papermc.io/folia/reference/overview/
- **Folia region logic**: https://docs.papermc.io/folia/reference/region-logic/
- **Folia FAQ**: https://docs.papermc.io/folia/faq/

## Advanced Topics

- **paperweight-userdev (internal code access)**: https://docs.papermc.io/paper/dev/getting-started/paperweight/
- **Minecraft internals**: https://docs.papermc.io/paper/dev/misc/internals/
- **Using databases**: https://docs.papermc.io/paper/dev/misc/databases/
- **Debugging plugins**: https://docs.papermc.io/paper/dev/misc/debugging/
- **Reading stacktraces**: https://docs.papermc.io/paper/dev/misc/stacktraces/

## When to Fetch These

### During Plugin Setup
Fetch project-setup, plugin.yml, and paper-plugins documentation when:
- Creating a new plugin project
- Setting up build.gradle.kts
- Configuring paper-plugin.yml vs plugin.yml

### For Specific Features
Fetch API-specific docs when implementing:
- **Data Components**: Working with ItemStack data (1.20.5+)
- **PDC**: Storing custom data on entities/items/blocks
- **Scheduling**: Any timing/scheduling tasks (critical for Folia)
- **Registries**: Custom items, enchantments, or registry modifications
- **Particles**: Visual effects
- **Recipes**: Custom crafting recipes

### For Folia Compatibility
Fetch Folia docs when:
- Making plugin Folia-compatible
- Debugging scheduler issues
- Understanding region-based logic
- Thread-safety concerns

### For Debugging
Fetch debugging/internals docs when:
- Working with NMS (net.minecraft.server) code
- Using paperweight-userdev
- Debugging complex issues
- Reading crash reports

## Version-Specific Features

### 1.20.5+
- Data Components API (replaced NBT for most use cases)
- Mojang mappings as default

### 1.21+
- Various registry improvements
- New APIs for modern Minecraft features

### 1.21.7+
- Dialog API for custom menus/dialogs

## Usage Pattern

When encountering an API or feature you need to implement:

1. Check if it's covered in the references included in this skill
2. If not, use web_fetch to get the relevant documentation link above
3. Parse the documentation for specific implementation details
4. Apply the learned patterns with Folia-compatible approaches

## Example: Fetching Docs in Practice

```
User: "How do I use the Data Components API?"

Claude should:
1. Recognize this is a 1.20.5+ feature
2. Fetch: https://docs.papermc.io/paper/dev/api/data-components/
3. Provide implementation guidance based on docs
4. Ensure Folia-compatible patterns if modifying items during gameplay
```
