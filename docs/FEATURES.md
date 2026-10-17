# Features

XDiscordUltimate is built from modules. Each module is toggled under `features.<name>.enabled` in config.yml and can be reloaded individually with `/xdiscord reload <name>`.

| Module | Config key | Description |
|--------|------------|-------------|
| Chat Bridge | `chat-bridge` | Bridges chat between Minecraft and Discord |
| Server Logging | `server-logging` | Logs server events, console output, and commands to Discord |
| Verification | `verification` | Code-based Discord account verification system |
| Server Control | `server-control` | Control the server from Discord with admin protection |
| Admin Alerts | `admin-alerts` | TPS/RAM monitoring alerts to administrators |
| Tickets | `tickets` | Support ticket system with Discord integration |
| Moderation | `moderation` | Moderation logging and sync between Minecraft and Discord |
| Bot Console | `bot-console` | Remote console access and log streaming through Discord |
| Announcements | `announcements` | Announcement broadcasting with scheduling |
| Player Stats | `player-stats` | Track and display player statistics |
| Server Status | `server-status` | Auto-updating status embed in Discord |
| Booster Perks | `booster-perks` | Give perks to Discord server boosters |
| Welcome DM | `welcome-dm` | Sends a welcome DM to new linked players |
| Leaderboard | `leaderboard` | Auto-updating Discord leaderboard with buttons |
| Activity Roles | `activity-roles` | Auto-assign Discord roles based on playtime tiers |
| Voice Channels | `voice-channels` | Dynamic voice channels and player count status channel |
| Economy Bridge | `economy-bridge` | Discord commands for Vault economy |
| Server IP | `server-ip` | Server address images via `/ip` |
| Whitelist | `whitelist` | Whitelist requests from Discord |
| Role Menus | `role-menus` | Self-assignable roles through button panels |

## Chat Bridge

Relays chat in both directions with `minecraft-to-discord` and `discord-to-minecraft` toggles. Supports webhook relay (`use-webhook`, `webhook-url`, `auto-create-webhook`), attachments (`relay-attachments`, `show-attachment-links`), mentions, emoji conversion, and per-direction formats set under `features.chat-bridge` or overridden in the `messages` section.

## Server Logging

Posts join/leave, deaths, advancements, commands, and server events to `logs-channel-id`. Individual toggles exist for each event type, plus `ignored-commands` and per-event message templates.

## Verification

`/verify` in Discord returns a code; `/verify <code>` in-game links the accounts. Supports `one-time-only`, `verified-role` and `verified-group` rewards, `sync-nickname`, code length/expiry, failed-attempt cooldowns, and lockout. `/xdiscord unlink <player>` and `/unlink` remove links and fire the unlink event.

## Server Control

Lets trusted Discord users run a fixed command set in the control channel: `restart`, `stop`, `kick`, `tps`, `list`. Each command has an allow flag under `allowed-commands`, and access is limited by `adminIDs` or `control-role`.

## Admin Alerts

Periodically checks TPS and RAM usage against `tps-threshold` and `ram-threshold` and sends alerts to the configured channel, with optional DMs.

## Tickets

Ticket panel posted in `ticket-panel-channel`, category and support role configurable, optional ticket `types` with their own buttons and categories. Supports per-player open limits, creation cooldown, auto-close with warning, player close, 1-5 star feedback DMs, participants (`/ticket adduser`, `/ticket removeuser`), and text or HTML transcripts. In-game `/support` opens a ticket GUI.

## Moderation

Syncs bans, kicks, and mutes with Discord (`sync-bans`, `sync-kicks`, `sync-mutes`, `muted-role`), applies Discord timeouts/mutes to Minecraft chat, persists mutes across restarts, and logs moderation actions. Includes word/regex chat filter (`filter-words`, bypass with `xdiscord.bypass.filter`) and `/report` with cooldown and verification requirement.

## Bot Console

`/console`, `/server`, `/game`, and `/session` Discord commands for console access, server management (restart, stop, save, whitelist, ban, kick, op/deop, reload), game settings, and log sessions. Access is limited to trusted admins.

## Announcements

`/announce` broadcasts to both platforms and `/announcement` in Discord lists, sends, and tests announcements. Supports titles, sounds, prefix, and a `scheduled` map for timed broadcasts.

## Player Stats

Tracks 14 stats (kills, deaths, kd-ratio, mob-kills, playtime, blocks broken/placed, damage dealt/taken, distance walked, jumps, fish caught, items crafted, advancements). Configurable embed, per-line toggles and labels, ranks, weekly/monthly snapshots, cached top lists, `/stats compare`, and PlaceholderAPI placeholders.

## Server Status

Maintains a status embed in `server-status.channel-id`, updated every `update-interval` seconds, with TPS, player count, and server details.

## Booster Perks

Detects Discord boosters (native detection or configured booster roles), maps them to a permission group, and runs `reward-commands` on boost and `remove-commands` when the boost ends.

## Welcome DM

DMs linked players on join with an embed or plain message. Options for `first-join-only` and `verified-only`. Test with `/welcomedm test <player>`.

## Leaderboard

Posts an auto-updating leaderboard message with buttons for each stat category (`kills`, `deaths`, `kd`, `playtime`, `mobs`, `blocks-mined`, `blocks-placed`, `distance`).

## Activity Roles

Assigns Discord roles based on playtime tiers defined in `activity-roles.tiers` (`min-hours` and `role-id` per tier). Optional promotion announcements and removal of old tier roles.

## Voice Channels

Maintains a voice channel whose name shows the player count (`status-channel-format`) and can create temporary dynamic voice rooms on demand under `category-id`, up to `max-dynamic-channels`.

## Economy Bridge

Discord `/balance`, `/baltop`, and `/pay` commands backed by Vault. Transfers are gated by `allow-transfers` with min/max amounts and an optional tax. `/economy` admin actions require a trusted admin. Requires Vault and an economy plugin.

## Server IP

`/ip` and the Discord `/ip` command show a generated server address image. Set the address with `/ip set <address>` (permission `xdiscord.ip.admin`) or `/ipset` in Discord (Manage Channel).

## Whitelist

Players request whitelist access with `/whitelist <username>` in-game or the Discord `/whitelist` command. Requests are posted to `whitelist-channel` for staff to accept or decline, with optional DMs on either outcome.

## Role Menus

Config-driven button panels under `features.role-menus.panels`. Each option toggles a configured role on or off. Post a panel in its channel with `/rolemenu post <panel>`.

## Integrations

- `integrations.vault-reward` pays a Vault amount when a player verifies.
- `integrations.luckperms-role-sync` mirrors LuckPerms primary groups onto Discord roles using `mappings`.
- LuckPerms, PlaceholderAPI, and Vault are optional; only the features that need them are affected when they are missing.
