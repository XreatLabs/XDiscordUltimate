# Module Manager

Every major feature is a module. The module manager registers each module under a config key, enables it when `features.<key>.enabled` is true, and skips modules whose declared dependencies are disabled.

## Module keys

Enable or disable a module with `features.<key>.enabled` in config.yml:

`chat-bridge`, `server-logging`, `verification`, `server-control`, `admin-alerts`, `tickets`, `moderation`, `bot-console`, `announcements`, `player-stats`, `server-status`, `booster-perks`, `welcome-dm`, `leaderboard`, `activity-roles`, `voice-channels`, `economy-bridge`, `server-ip`, `whitelist`, `role-menus`

A module's config path is its display name lowercased with spaces replaced by hyphens; for example "Server Control" maps to `features.server-control`.

## Lifecycle

1. `loadModules()` registers all modules.
2. A module whose `features.<key>.enabled` is false is logged as disabled and never enabled.
3. `@DependsOn` is checked next: when a listed dependency is disabled, the module is skipped with a warning. Tickets declares `@DependsOn("verification")`.
4. Enabled modules run `onEnable()`, then `onDiscordReady()` once JDA connects.
5. Enabled modules can contribute Discord slash commands through `getSlashCommands`, dispatched to `handleSlashCommand`.

## Commands

- `/xdiscord modules` - list every module with enabled/disabled status and description.
- `/xdiscord reload` - reload config.yml, messages, module configs, admin IDs, and bot activity.
- `/xdiscord reload <module>` - restart one module (disable, then enable). Accepts the config key or the display name; unknown names return an error.
- `/xdiscord status` - show enabled module count, Discord and database state, memory, and players.

All `/xdiscord` subcommands require `xdiscord.admin`.

## Embed styling

Keys under `messages.embeds` override module embed styling (color, footer, thumbnail, and so on). Available ids: `stats`, `ticket-panel`, `ticket-welcome`, `ticket-closed`, `announcement`, `server-status`, `leaderboard`, `event`.

## Notes

- `reloadModule` restarts a module, so Discord resources such as the ticket panel are recreated.
- A plain `/xdiscord reload` calls each enabled module's `reloadConfig` without restarting it.
- Dependencies are checked only at startup; disabling a dependency later does not stop an already enabled module.
- The console logs "Module disabled in config: <name>" for each module that stays off, which helps when debugging a feature.
