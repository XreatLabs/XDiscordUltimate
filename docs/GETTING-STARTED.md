# Getting Started

Step-by-step setup for XDiscordUltimate v1.3.0.

## Requirements

- Spigot or Paper server, Minecraft 1.16.5 - 26.x
- Java 17 or newer (Java 21+ for Minecraft 1.20.5+ and 26.x servers)
- A Discord server where you have Manage Server permission

## 1. Create the Discord bot

1. Open the Discord Developer Portal at https://discord.com/developers/applications
2. Click New Application and give it a name.
3. Open the Bot tab, reset/create the token, and copy it. Treat the token like a password.
4. On the Bot tab, enable the privileged intents:
   - Presence Intent
   - Server Members Intent
   - Message Content Intent

XDiscordUltimate requests these gateway intents on login: guild members, guild presences, guild messages, message content, direct messages, message reactions, voice states, and emojis/stickers. The three privileged intents above must be enabled in the portal or the login fails.

## 2. Invite the bot

1. Open OAuth2 > URL Generator.
2. Scopes: `bot` and `applications.commands`.
3. Permissions: View Channels, Send Messages, Embed Links, Read Message History, Attach Files, Manage Messages, Manage Channels (tickets and dynamic voice), Manage Roles (verification and role menus), Add Reactions.
4. Open the generated URL and add the bot to your server.

To copy IDs: Discord Settings > Advanced > Developer Mode, then right-click a server, channel, role, or user and choose Copy ID.

## 3. Install the plugin

1. Put the XDiscordUltimate jar in `plugins/`.
2. Start the server once. This generates `plugins/XDiscordUltimate/config.yml` and downloads runtime libraries.
3. Stop the server.

## 4. Configure the essentials

Edit `plugins/XDiscordUltimate/config.yml`:

```yaml
discord:
  bot-token: "YOUR_BOT_TOKEN"
  guild-id: "YOUR_GUILD_ID"
```

The setup wizard can do this for you: run `/xdiscord setup` in-game or from the console. It asks for the bot token, guild ID, and chat channel ID, and writes `discord.bot-token`, `discord.guild-id`, `discord.channels.chat`, and `features.chat-bridge.chat-channel-id`.

Recommended keys to review:

| Key | Purpose |
|-----|---------|
| `discord.invite-url` | Invite link shown by `/discord` |
| `discord.guild-name` | Server name shown by `/discord` |
| `features.chat-bridge.chat-channel-id` | Chat bridge channel (name or ID) |
| `features.server-logging.logs-channel-id` | Log channel (name or ID) |
| `adminIDs` | Discord user IDs allowed to run admin/console commands |

## 5. Start and verify

Start the server and check the console for:

- `XDiscordUltimate v1.3.0 has been enabled!`
- `Discord bot connected successfully!`
- `Successfully connected to guild: <name>`
- `Registered N slash commands to <guild>`

Then confirm in game and Discord:

1. `/xdiscord status` shows Discord as Connected.
2. `/xdiscord modules` lists enabled modules.
3. `/help` in Discord lists the bot commands.
4. Run `/verify` in Discord to get a code, then `/verify <code>` in-game to test linking.

## First run checklist

- Bot token and guild ID set (config or wizard).
- No `Discord bot token is not configured` or `Invalid Discord bot token format` errors.
- Bot shows online in Discord.
- Intents enabled in the Developer Portal.
- At least one channel configured per feature you want to use.
- Config validation warnings printed on startup are fixed where relevant. These warnings never block startup; they only list suspicious values.
- For token or guild ID changes, restart the server. `/xdiscord reload` reloads configuration but does not restart the bot connection.
