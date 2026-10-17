# Configuration

Main file: `plugins/XDiscordUltimate/config.yml`. It is created from the jar on first start. Missing keys fall back to built-in defaults, so you can delete a key to restore default behavior.

## Top-level sections

| Section | Purpose |
|---------|---------|
| `adminIDs` | Discord user IDs treated as trusted admins for console, server control, and announcements |
| `general` | Debug mode, default language, update checks |
| `features` | One block per module, each gated by `enabled` |
| `discord` | Bot credentials, activity, command settings, channel map |
| `database` | Storage backend and connection settings |
| `messages` | Chat formats, global embed color, per-embed overrides |
| `integrations` | Vault reward on verify, LuckPerms role sync |
| `advanced` | Thread pool, caches, rate limits, bStats |
| `discord-console` | Log4j console streaming to a Discord channel |

## general

| Key | Default | Description |
|-----|---------|-------------|
| `debug` | false | Verbose console logging |
| `default-language` | en_US | Locale loaded by the message system |
| `language` | en_US | Secondary language key read by the config manager and validator |
| `check-updates` | true | Update check on startup (GitHub, then Modrinth, then Spigot) |
| `spigot-resource-id` | 127923 | Spigot resource ID for update checks (0 = disabled) |
| `modrinth-project` | xdiscordultimate | Modrinth project slug or ID for update checks ("" = disabled) |

## features

Every module has a `features.<name>.enabled` toggle. Registered modules:

| Config key | Module |
|------------|--------|
| `chat-bridge` | Minecraft/Discord chat bridge |
| `server-logging` | Join/leave, death, advancement, command logging |
| `verification` | Code-based account linking |
| `server-control` | Allowed console commands from Discord |
| `admin-alerts` | TPS/RAM monitoring alerts |
| `tickets` | Support ticket system |
| `moderation` | Ban/kick/mute sync, filter, reports |
| `bot-console` | Remote console and server management commands |
| `announcements` | Broadcasts and scheduled announcements |
| `player-stats` | Stat tracking, embed, snapshots, top cache |
| `server-status` | Auto-updating status embed |
| `booster-perks` | Rewards for Discord boosters |
| `welcome-dm` | Welcome DM to linked players |
| `leaderboard` | Auto-updating leaderboard message |
| `activity-roles` | Playtime tier roles |
| `voice-channels` | Status channel and dynamic voice rooms |
| `economy-bridge` | Vault economy Discord commands |
| `server-ip` | Server address image via `/ip` |
| `whitelist` | Whitelist requests from Discord |
| `role-menus` | Self-assignable role button panels |

Notable module keys:

| Key | Description |
|-----|-------------|
| `chat-bridge.chat-channel-id` | Channel ID or name; falls back to `chat-bridge.channel` |
| `chat-bridge.minecraft-to-discord` / `discord-to-minecraft` | Direction toggles |
| `verification.verified-role` | Discord role granted on successful verify |
| `verification.verified-group` | LuckPerms group granted on verify |
| `verification.code-length` / `code-expiry-minutes` | Code generation settings |
| `verification.failed-attempt-cooldown-seconds` / `max-verify-attempts` / `verify-lockout-seconds` | Brute-force protection |
| `verification.one-time-only` / `kick-after-minutes` / `whitelist-mode` | Verification behavior |
| `tickets.ticket-panel-channel` / `ticket-category` / `support-role` | Where the panel lives and who handles tickets |
| `tickets.max-open-tickets` / `creation-cooldown-seconds` / `auto-close-hours` | Limits and lifecycle |
| `moderation.auto-moderate` / `filter-words` / `report-cooldown-minutes` | Filter and reports |
| `moderation.sync-bans` / `sync-kicks` / `sync-mutes` / `muted-role` | Discord/Minecraft moderation sync |
| `server-control.allowed-commands` | Per-command allow flags (`restart`, `stop`, `kick`, `tps`, `list`) |
| `server-control.require-role` / `control-role` | Discord role gate for server control |
| `admin-alerts.tps-threshold` / `ram-threshold` / `check-interval` | Alert thresholds |
| `player-stats.lines` | Enable/rename each stat line by id (kills, playtime, and so on) |
| `player-stats.snapshots.auto-reset` / `top-cache-minutes` | Snapshot resets and leaderboard cache refresh |
| `server-status.channel-id` / `update-interval` | Status embed target and cadence |
| `leaderboard.channel-id` / `top-players` / `default-type` | Leaderboard message settings |
| `voice-channels.status-channel-id` / `status-channel-format` | Player count voice channel |
| `voice-channels.enable-dynamic-channels` / `category-id` / `max-dynamic-channels` | Dynamic rooms |
| `role-menus.panels` | Panel id, channel, title, and per-option label/emoji/style/role |

## discord

| Key | Description |
|-----|-------------|
| `bot-token` | Bot token from the Developer Portal |
| `guild-id` | Main Discord server ID |
| `guild-name` | Display name used by `/discord` |
| `invite-url` | Invite link used by `/discord` |
| `text-command-prefix` | Prefix for legacy text commands (default `!`); this is the key the bot reads |
| `commands.use-slash-commands` / `commands.legacy-prefix` | Command settings declared in the shipped config |
| `activity.type` / `activity.text` / `activity.status` | Bot presence (text supports `%players%` and `%max%`) |
| `channels.*` | Named channel map: welcome, logs, chat, console, events, tickets, moderation |

## database

| Key | Default | Description |
|-----|---------|-------------|
| `type` | sqlite | `sqlite`, `mysql`, or `postgresql` |
| `sqlite.file` | data.db | File name inside the plugin folder |
| `mysql.host` / `port` / `database` / `username` / `password` / `ssl` | localhost / 3306 / xdiscord / root / password / false | MySQL connection |

For PostgreSQL, set `type: "postgresql"` and add a `database.postgresql` section with `host`, `port` (default 5432), `database`, `username`, and `password`. It is read by the plugin but is not present in the default config file.

## messages

| Key | Description |
|-----|-------------|
| `embed-color` | Default accent color for embeds; empty uses the built-in palette |
| `minecraft-to-discord` / `discord-to-minecraft` | Fallback chat formats |
| `join-message` / `leave-message` / `death-message` | Fallback event messages |
| `embeds.<id>` | Per-embed overrides (color, footer, thumbnail) for ids such as `stats`, `ticket-panel`, `ticket-welcome`, `ticket-closed`, `announcement`, `server-status`, `leaderboard`, `event` |

Player-facing text lives in `lang/messages_<locale>.yml`; see [MESSAGES.md](MESSAGES.md).

## integrations

| Key | Description |
|-----|-------------|
| `vault-reward.enabled` / `amount` | Vault economy reward on successful verification |
| `luckperms-role-sync.enabled` / `mappings` | Mirror LuckPerms groups onto Discord role names |

## advanced

| Key | Default | Description |
|-----|---------|-------------|
| `thread-pool-size` | 4 | Present in the shipped config; not read by this build |
| `cache.user-ttl` / `cache.max-size` | 60 / 1000 | Present in the shipped config; not read by this build |
| `rate-limit.commands-per-minute` | 10 | Present in the shipped config; not read by this build |
| `metrics.enabled` / `metrics.bstats-id` | true / 0 | bStats submission; 0 disables it |

## discord-console

| Key | Description |
|-----|-------------|
| `enabled` | Stream server console output to Discord |
| `channel-id` | Target channel for console output |
| `allowed-roles` | Discord role names allowed to read/use the console |
