# Commands

## In-game commands

| Command | Aliases | Permission | Description |
|---------|---------|------------|-------------|
| `/verify <code>` | `link`, `discord` | `xdiscord.verify` | Link your Minecraft account using a code from the Discord `/verify` command |
| `/unlink` | - | `xdiscord.unlink` | Unlink your own Discord account |
| `/support [message]` | `ticket` | `xdiscord.support` | Open the ticket GUI, or create a ticket with a message. Requires a linked account |
| `/report <player> <reason>` | `reportplayer` | `xdiscord.report` | Report an online player to Discord moderators. Requires a linked account |
| `/stats [player]` | `playerstats`, `pstats` | - | Show stats for a player, or `/stats compare <p1> <p2>` |
| `/help [category]` | `xhelp`, `xdhelp` | - | Open the help GUI |
| `/discord` | `disc`, `invite` | - | Show the Discord invite from `discord.invite-url` |
| `/language [locale]` | `lang`, `locale` | `xdiscord.language` | Set your language; console sets the server default |
| `/announce <message>` | - | `xdiscord.announce` | Broadcast to Discord and in-game |
| `/embed <channel> <title> <description>` | `dembed` | `xdiscord.embed` | Send a custom embed to a Discord channel |
| `/discordconsole <command>` | `dconsole` | `xdiscord.console` | Run a command as the server console |
| `/welcomedm <test\|reload\|help>` | - | `xdiscord.admin` | Test or reload the welcome DM module |
| `/ip [set <address>]` | - | `xdiscord.ip` (`xdiscord.ip.admin` for set) | Show or set the server address image |
| `/whitelist <username>` | - | `xdiscord.whitelist` | Submit a whitelist request |
| `/xdiscord ...` | `xdu`, `xdiscordultimate` | `xdiscord.admin` | Main admin command, see below |

Note that plugin.yml declares `discord` as an alias of `/verify` while a separate `/discord` invite command also exists, so that label is ambiguous. Use `/verify <code>` or `/link <code>` to enter a code.

## /xdiscord subcommands

| Subcommand | Description |
|------------|-------------|
| `/xdiscord setup` | Interactive wizard: bot token, guild ID, chat channel ID |
| `/xdiscord reload [module]` | Reload all configuration, or a single module by config key or name |
| `/xdiscord status` (alias `info`) | Show version, Discord/database state, modules, memory, players |
| `/xdiscord modules` | List every module with enabled state and description |
| `/xdiscord stats <snapshot\|reset> <weekly\|monthly>` | Manage stat snapshot baselines |
| `/xdiscord debug <on\|off>` | Toggle debug logging |
| `/xdiscord admin <add\|remove> <discord-id>` | Add or remove a trusted admin ID |
| `/xdiscord export [all\|verified\|stats\|tickets]` | Export data to JSON in `exports/` |
| `/xdiscord unlink <player>` | Unlink a player from Discord |
| `/xdiscord help [command]` | In-game help |

## Discord slash commands

Core commands are registered by the plugin; module commands only appear when their module is enabled.

| Command | Options / Subcommands | Module | Notes |
|---------|-----------------------|--------|-------|
| `/help` | - | core | List bot commands |
| `/status` | - | core | Server status with TPS and player count |
| `/players` | - | core | Online players with their worlds |
| `/ping` | - | core | Bot latency |
| `/tps` | - | core | TPS and MSPT |
| `/stats` | `[player]` | core | Stats embed; uses your linked account when player is omitted |
| `/announce` | `<message> [title]` | core | Announcement to all players |
| `/online` | - | core | Quick online list |
| `/info` | - | core | Plugin info and version |
| `/verify` | - | verification | Reply with a verification code (expires per `code-expiry-minutes`) |
| `/ticket` | `create [subject] [priority]`, `close [reason]`, `adduser <user>`, `removeuser <user>`, `info`, `priority <level>` | tickets | Ticket channel management |
| `/ticket edit` | `title <title>`, `description <text>`, `priority <level>`, `addbutton <label> <style> [target]`, `removebutton <label>` | tickets | Edit the current ticket |
| `/leaderboard` | `kills`, `deaths`, `kd`, `playtime`, `mobs`, `blocks-mined`, `blocks-placed`, `distance` | leaderboard | Top lists |
| `/announcement` | `list`, `send <message> [title]`, `test` | announcements | Scheduled/custom announcements |
| `/rolemenu` | `post <panel>`, `list` | role-menus | Post requires Manage Roles |
| `/voicechannels` | `status`, `list`, `setup`, `help` | voice-channels | Voice channel info |
| `/whitelist` | `<username>` | whitelist | Submit a whitelist request |
| `/ip` | - | server-ip | Show server address image |
| `/ipset` | `<address>` | server-ip | Set the address; requires Manage Channel |
| `/balance` | `[player]` | economy-bridge | Vault balance |
| `/baltop` | - | economy-bridge | Richest players |
| `/pay` | `<player> <amount>` | economy-bridge | Only when `allow-transfers` is true |
| `/economy` | `<give\|take\|set> <player> <amount>` | economy-bridge | Trusted admins only |
| `/console` | `<command>` | bot-console | Trusted admins only |
| `/server` | `logs`, `tps`, `memory`, `players`, `plugins`, `restart`, `stop`, `save`, `op <player>`, `deop <player>`, `ban <player> [reason]`, `pardon <player>`, `kick <player> [reason]`, `whitelist <action> [player]`, `reload` | bot-console | Trusted admins only |
| `/game` | `gamemode <mode> [player]`, `difficulty <level>`, `time <set\|add> <value>`, `weather <type> [duration]`, `give <player> <item> [amount]`, `tp <player> <target>` | bot-console | Trusted admins only |
| `/session` | `start`, `stop` | bot-console | Console log session |

"Trusted admins" are the Discord user IDs listed under `adminIDs` in config.yml.

## Discord legacy text commands

When a message starts with `discord.text-command-prefix` (default `!`), these core commands are handled without a slash: `help`, `status`, `players` (aliases `online`, `list`), `ping`, `tps`, `stats [player]`, `announce <message>`, `info`, and `server`. Modules also accept their command names as text commands, leading with the same prefix: `verify`, `ticket`, `leaderboard`, `announcement`, `whitelist`, `ip`, `ipset`, `voicechannels`, and `console`/`server`/`game`/`session`.
