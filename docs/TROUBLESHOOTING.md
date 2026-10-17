# Troubleshooting

Enable `general.debug: true` and use `/xdiscord status` for a quick state summary.

## Bot is offline or login fails

Check the startup log for one of these messages:

| Message | Cause and fix |
|---------|---------------|
| `Discord bot token is not configured!` | `discord.bot-token` is empty. Set it or run `/xdiscord setup` |
| `Invalid Discord bot token format!` | The value is not a bot token. Use the token from the Bot tab, not the application client secret. Tokens look like `BASE64.BASE64.BASE64` and are 59+ characters |
| `Failed to initialize Discord bot` | Login was rejected. Usually wrong token, disabled intents, or account issues |
| `Failed to initialize Discord bot! Check your bot token and guild ID.` | Plugin continues without Discord. Fix credentials and restart |
| `Bot is not connected to any guilds!` | Invite the bot to your server |
| `Could not find guild with ID: ...` | Wrong `discord.guild-id`. The log lists the guilds the bot is in; if the bot is in exactly one guild it is auto-selected |

Token or guild changes require a server restart. `/xdiscord reload` only reloads configuration.

## Missing intents

In the Discord Developer Portal, Bot tab, enable Presence Intent, Server Members Intent, and Message Content Intent. XDiscordUltimate requests guild members, presences, messages, message content, direct messages, reactions, voice states, and emojis/stickers. If the privileged intents are not enabled in the portal, the bot fails to connect. Symptoms of missing intents while connected:

- Chat bridge receives no Discord messages: Message Content Intent.
- Booster detection, nickname sync, or member lookups fail: Server Members Intent and Presence Intent.

## Slash commands not showing

1. Confirm the log line `Registered N slash commands to <guild>`. Nothing is registered if the main guild could not be resolved.
2. Re-invite the bot with the `applications.commands` scope (OAuth2 URL Generator, scopes `bot` and `applications.commands`).
3. Commands are registered per guild, not globally. Wrong-guild bots will not see them.
4. A `Duplicate slash command '<name>' skipped during consolidation.` warning means two modules registered the same name; disable one of them.
5. Discord clients cache command lists. Restart the client or wait a few minutes after registration.
6. `/xdiscord reload` does not re-register commands. Restart the server after changing modules.

## Verification not working

1. Generate a code with `/verify` in Discord (the code is sent only to that user).
2. Use `/verify <code>` in-game. Codes are case-insensitive and expire after `features.verification.code-expiry-minutes`.
3. The `Account Already Linked` embed means `one-time-only` is on and the Discord user already has a link.
4. Repeated wrong codes trigger the failed-attempt cooldown and then a lockout for `verify-lockout-seconds`.
5. The `verified-role` name must match an existing Discord role. The user must be a member of the main guild for the role to be granted.
6. `/support` and `/report` refuse unlinked players when verification is required. Link the account first.
7. Check `xdiscord.bypass.verification` if a player is expected to skip verification.

## Database errors

- `Failed to initialize database!` disables the plugin. Fix the underlying connection issue first.
- SQLite (default): verify the plugin folder is writable and `data.db` (or `database.sqlite.file`) is not locked by another process.
- MySQL: check `database.mysql.host`, `port`, `database`, `username`, `password`, and `ssl`. The database must already exist.
- PostgreSQL: set `database.type: "postgresql"` and add a `database.postgresql` section with `host`, `port`, `database`, `username`, and `password`. The default config.yml does not include it.
- `Database not initialized!` from an operation means initialization failed early; fix the connection and restart.
- Schema versioning is tracked in the `schema_versions` table; migrations run automatically and are logged.

## Optional plugins not detected

| Log line | Fix |
|----------|-----|
| `LuckPerms not found. Some features will be disabled.` | Install LuckPerms and restart. Needed for group rewards and the role sync integration |
| `PlaceholderAPI not found. Some features will be disabled.` | Install PlaceholderAPI and restart. The `xdiscord` expansion is registered automatically when present |
| `Failed to register PlaceholderAPI expansion` | Usually a version mismatch; update PlaceholderAPI |
| Vault warnings or economy commands reply that Vault is missing | Install Vault plus an economy provider. Required by `economy-bridge` and `integrations.vault-reward` |

## Config validation warnings

These warnings are informational and never block startup. Each one names the key to fix:

- `discord.guild-id is missing or not a valid Discord server ID`
- `discord.bot-token is not set - Discord features will stay offline`
- `features.tickets is enabled but ticket-panel-channel is empty`
- `features.tickets.max-open-tickets must be at least 1`
- `features.tickets.auto-close-hours cannot be negative`
- `features.chat-bridge is enabled but chat-channel-id is empty`
- `features.verification is enabled but verified-role is empty`
- `language file lang/messages_<lang>.yml is missing - falling back to en_US`
- `advanced.metrics.bstats-id is still the placeholder 12345 - replace it with your bStats plugin ID`
- `messages.embed-color does not look like a valid color: <value>`

## Channels not found

Channel keys accept an ID or a channel name depending on the module. If a module reports a missing channel, confirm the channel ID is correct and that the bot can view and send in that channel. Channels the bot cannot access behave as missing.

## Tickets not working

- Verify `ticket-panel-channel` exists and the bot has Manage Channels.
- Players need a linked account and `xdiscord.support`.
- Limits: `max-open-tickets`, optional `max-open-tickets-per-type`, and `creation-cooldown-seconds`.
- Ticket panel persistence is stored in `tickets.yml`; if it looks corrupted, the plugin keeps timestamped backups in `backups/`.

## Chat filter issues

`filter-words` accepts plain words and `regex:` entries. Players with `xdiscord.bypass.filter` are exempt. Messages blocked in Minecraft are not relayed.
