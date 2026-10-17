# XDiscordUltimate

The most comprehensive Discord integration plugin for Minecraft servers. Features modern slash commands, chat bridge, verification system, tickets, moderation, and much more.

![Version](https://img.shields.io/badge/version-1.3.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.16.5+-green)
![Java](https://img.shields.io/badge/java-17+-orange)
![Discord](https://img.shields.io/badge/discord-JDA_5.0-7289da)
![License](https://img.shields.io/badge/license-MIT-purple)

## What's New in 1.3.0

- **Ticket upgrades** - per-type open limits, creation cooldown, close reasons, 1-5 star feedback DMs, ticket participants, and HTML transcripts.
- **Stats upgrades** - stat ranks, cached top lists, weekly/monthly snapshots, `/stats compare`, and matching PlaceholderAPI placeholders.
- **Discord role menus** - config-driven panels with buttons that toggle member roles.
- **Two-way moderation sync** - Discord timeouts and mute roles now mute Minecraft chat, mutes persist across restarts, and auto-moderation actions are logged to Discord.
- **Minecraft-side polish** - `/support` opens a ticket GUI, ticket links are clickable in chat, and player stats can be shown in the tab list.
- **Integrations** - LuckPerms group to Discord role sync, Vault rewards for verifying, and a configurable Spigot update-checker ID.
- **Folia-ready scheduler bridge** - all scheduling is routed through a `SchedulerCompat` layer that falls back to the Bukkit scheduler and can be adapted for Folia.
- **Ops quality of life** - `/xdiscord reload <module>`, `/xdiscord stats snapshot|reset`, startup config validation, tickets.yml backups, and moderation history persistence.

## What's New in 1.2.0

- **`/unlink` command** — players can unlink their Discord account; admins can unlink anyone via `/xdiscord unlink <player>` (removes Discord role, LuckPerms group, and DB link).
- **Hardened verification codes** — `SecureRandom` generation, configurable failed-attempt cooldown + max-attempts lockout, and consistent code expiry (the `code-expiry-minutes` config is now honored exactly).
- **Fixed config/code default mismatches** — `one-time-only`, `kick-after-minutes`, and `verify-cooldown-seconds` now align with `config.yml`.
- **Fixed a `ConcurrentModificationException`** in the message/localization system that could throw on a player's first localized message.
- **Discord nickname re-sync** — linked members' stored Discord name stays fresh when they change their nickname.

## Features

- **Chat Bridge** - Seamless chat sync between Minecraft and Discord
- **Verification System** - Secure account linking with verification codes, self/admin unlink, brute-force lockout, and Discord nickname sync
- **Support Tickets** - Full ticket system with Discord integration
- **Player Stats** - Track and display player statistics
- **Server Status** - Auto-updating status embed in Discord
- **Moderation** - Ban/kick/mute sync, chat filtering, and report system
- **Role Menus** - Button panels that let members pick their own roles
- **Admin Console** - Execute server commands from Discord
- **Announcements** - Broadcast to both Discord and Minecraft
- **Booster Perks** - Reward Discord server boosters
- **Admin Alerts** - Server health monitoring
- **Server Logging** - Log all events to Discord

## Quick Start

1. Download the latest release
2. Place in your `plugins` folder
3. Start server to generate config
4. Configure `config.yml` with your bot token and guild ID
5. Restart server

```yaml
discord:
  bot-token: "YOUR_BOT_TOKEN"
  guild-id: "YOUR_GUILD_ID"
  channels:
    chat: "CHANNEL_ID"
    logs: "CHANNEL_ID"
```

## Building from source

```bash
gradle build            # release jar:  build/libs/XDiscordUltimate-1.3.0.jar
gradle shadowJar -Pdev  # development jar: build/libs/XDiscordUltimate-1.3.0-DEV.jar
```

The `-Pdev` flag appends a `-DEV` suffix to the plugin version so development builds are easy to tell apart from releases. Requires JDK 17 or newer.

## Documentation

### Getting Started

**New to XDiscordUltimate?** Start here:

| Guide | Description |
|-------|-------------|
| [**Getting Started Guide**](docs/GETTING-STARTED.md) | Step-by-step setup from scratch |
| [Installation Guide](docs/INSTALLATION.md) | Quick installation reference |

### Core Documentation

| Document | Description |
|----------|-------------|
| [Installation Guide](docs/INSTALLATION.md) | Complete setup instructions |
| [Configuration Guide](docs/CONFIGURATION.md) | All configuration options |
| [Commands Reference](docs/COMMANDS.md) | All commands and permissions |
| [Features Guide](docs/FEATURES.md) | Detailed feature explanations |
| [Permissions Reference](docs/PERMISSIONS.md) | All permission nodes |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [Messages Configuration](docs/MESSAGES.md) | Customize all messages |
| [Developer API](docs/API.md) | API for developers |
| [Database Reference](docs/DATABASE.md) | Database schema and configuration |

### Module Documentation

| Module | Description |
|--------|-------------|
| [Chat Bridge](docs/modules/CHAT-BRIDGE.md) | Minecraft-Discord chat sync |
| [Verification](docs/modules/VERIFICATION.md) | Account linking system |
| [Server Logging](docs/modules/SERVER-LOGGING.md) | Event logging to Discord |
| [Admin Alerts](docs/modules/ADMIN-ALERTS.md) | Server health monitoring |
| [Tickets](docs/modules/TICKETS.md) | Support ticket system |
| [Moderation](docs/modules/MODERATION.md) | Ban/kick/mute sync, filter, reports |
| [Server Control](docs/modules/SERVER-CONTROL.md) | Remote server management |
| [Player Stats](docs/modules/PLAYER-STATS.md) | Statistics tracking |
| [Server Status](docs/modules/SERVER-STATUS.md) | Auto-updating status embed |
| [Booster Perks](docs/modules/BOOSTER-PERKS.md) | Discord booster rewards |
| [Bot Console](docs/modules/BOT-CONSOLE.md) | Discord console access |
| [Announcements](docs/modules/ANNOUNCEMENTS.md) | Cross-platform broadcasts |
| [Module Manager](docs/modules/MODULE-MANAGER.md) | Managing modules |

## Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/verify <code>` | Link your Discord account |
| `/unlink` | Unlink your Discord account |
| `/support [message]` | Open the ticket menu or create a ticket |
| `/report <player> <reason>` | Report a player |
| `/stats [player]` | View player statistics |
| `/help` | Open help menu |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/xdiscord reload [module]` | Reload configuration or a single module |
| `/xdiscord stats <snapshot\|reset> <weekly\|monthly>` | Manage stats snapshots |
| `/xdiscord status` | Show plugin status |
| `/xdiscord modules` | List all modules |
| `/xdiscord unlink <player>` | Unlink a player from Discord |
| `/announce <message>` | Make an announcement |

### Discord Slash Commands
| Command | Description |
|---------|-------------|
| `/verify` | Get verification code |
| `/status` | Show server status |
| `/players` | List online players |
| `/stats [player]` | View player statistics |
| `/console <command>` | Execute server command |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `xdiscord.verify` | Use verification | true |
| `xdiscord.unlink` | Unlink own account | true |
| `xdiscord.support` | Create tickets | true |
| `xdiscord.report` | Report players | true |
| `xdiscord.admin` | Admin commands | op |
| `xdiscord.console` | Console access | false |
| `xdiscord.announce` | Make announcements | op |
| `xdiscord.bypass.verification` | Bypass verification | false |
| `xdiscord.bypass.filter` | Bypass chat filter | false |

## Requirements

- **Minecraft Server**: Spigot/Paper 1.16.5 – 26.x (latest)
- **Java**: 17 or higher (Java 21+ required by Minecraft 1.20.5+ and 26.x servers)
- **Discord Bot**: With MESSAGE_CONTENT intent enabled

### Optional Dependencies
- LuckPerms - For permission groups
- PlaceholderAPI - For placeholders
- Vault - Economy integration (balance commands and verify rewards)

## Configuration Example

```yaml
# Admin Discord IDs
adminIDs:
  - "123456789012345678"

# Enable features
features:
  chat-bridge:
    enabled: true
    chat-channel-id: "1234567890"
    minecraft-to-discord: true
    discord-to-minecraft: true
  
  verification:
    enabled: true
    verified-role: "Verified"
    code-expiry-minutes: 5
  
  server-logging:
    enabled: true
    logs-channel-id: "1234567890"
    log-join-leave: true
    log-commands: true
  
  tickets:
    enabled: true
    auto-close-hours: 48
    save-transcript: true
  
  player-stats:
    enabled: true
  
  server-status:
    enabled: true
    channel-id: "1234567890"
    update-interval: 60
  
  moderation:
    enabled: true
    sync-bans: true
    auto-moderate: true
  
  admin-alerts:
    enabled: true
    tps-threshold: 15.0
    ram-threshold: 90
  
  booster-perks:
    enabled: true
    permission-group: "booster"

# Discord settings
discord:
  bot-token: "YOUR_TOKEN"
  guild-id: "YOUR_GUILD_ID"
  channels:
    chat: ""
    logs: ""
    tickets: ""
    moderation: ""

# Database
database:
  type: "sqlite"
```

## Support

- **Issues**: [GitHub Issues](https://github.com/xreatlabs/XDiscordUltimate/issues)
- **Discord**: [Join our Discord](https://discord.gg/xreatlabs)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Made with ❤️ by XreatLabs
