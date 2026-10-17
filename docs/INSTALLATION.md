# Installation

## Requirements

| Requirement | Details |
|-------------|---------|
| Server | Spigot or Paper, Minecraft 1.16.5 - 26.x |
| Java | 17 or newer (Java 21+ required by Minecraft 1.20.5+ and 26.x servers) |
| Discord bot | Created in the Discord Developer Portal with privileged intents enabled |
| Disk | Space for the plugin, runtime libraries, and the database |

The plugin declares `folia-supported: false`. All scheduling is routed through an internal `SchedulerCompat` layer that falls back to the Bukkit scheduler, but Folia is not officially supported by this build.

## Optional dependencies

These plugins are declared as `softdepend` and are detected at startup:

| Plugin | Used for |
|--------|----------|
| LuckPerms | Reading primary groups; the role sync integration mirrors groups onto Discord roles |
| PlaceholderAPI | Registers the `xdiscord` expansion and lets stats text parse external placeholders |
| Vault | Economy features: economy bridge commands and the verify reward integration |

Missing optional plugins never prevent startup. The console prints a warning such as `LuckPerms not found. Some features will be disabled.`

## Install steps

1. Download the XDiscordUltimate jar.
2. Place the jar in your server's `plugins/` folder.
3. Start the server once so the plugin can generate its configuration and download its runtime libraries.
4. Stop the server.
5. Edit `plugins/XDiscordUltimate/config.yml` and set at least `discord.bot-token` and `discord.guild-id`.
6. Start the server again and watch the console for `Discord bot connected successfully!`.

Runtime dependencies (JDA, HikariCP, database drivers, and their transitive libraries) are downloaded on first start using Libby. The server needs network access to Maven Central the first time. If dependency loading fails, the plugin disables itself with `Failed to load dependencies! Plugin will be disabled.`

## Files generated in plugins/XDiscordUltimate/

| Path | Contents |
|------|----------|
| `config.yml` | Main configuration (saved from the jar on first start) |
| `lang/messages_<locale>.yml` | Language files; all bundled locales are extracted on startup |
| `player_languages.yml` | Per-player language choices set with `/language` |
| `tickets.yml` | Ticket panel persistence; backed up to `backups/tickets-<timestamp>.yml` when replaced |
| `stats-snapshots.yml` | Weekly/monthly stat baselines used by snapshot deltas |
| `data.db` | SQLite database (when `database.type` is `sqlite`, the default) |
| `backups/` | Timestamped ticket data backups |
| `exports/` | JSON exports written by `/xdiscord export` |
| `lib/` | Runtime libraries downloaded by Libby |

## Updating

Replace the old jar with the new one and restart. Existing configuration files are not overwritten. New configuration keys are read with built-in defaults, so they take effect even if your config.yml does not list them. Delete a key only if you want the default behavior.

## Uninstalling

Stop the server, delete the jar, and delete `plugins/XDiscordUltimate/` if you also want to remove the database, language files, and backups.
