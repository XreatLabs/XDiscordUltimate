# Bot Console

Streams the server console to Discord and runs server and game commands from Discord. Access is limited to trusted administrators.

## Configuration

Keys under `features.bot-console`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `console-channel` | `"bot-console"` | Channel used for console streaming and text commands. |
| `allow-dm` | `false` | Inverted in code: console input is enabled when this is `false`. |
| `command-prefix` | `"!"` | Shipped key; text dispatch uses `discord.text-command-prefix` (`!`) instead. |

Console output is captured with a Log4j appender and streamed to the console channel.

## Discord commands

- `/console <command>` - run a console command.
- `/server logs|tps|memory|players|plugins|restart|stop|save|reload`
- `/server op|deop|kick|pardon <player> [reason]`
- `/server ban <player> [reason]`, `/server whitelist <action> [player]`
- `/game gamemode|difficulty|time|weather|give|tp`
- `/session start|stop` - capture console output and get a transcript when stopping.

Text equivalents in the console channel: `!console`, `!server`, `!game`, `!session`. Only users listed in `adminIDs` can use them.

## In-game command

- `/discordconsole <command>` - execute a command as the server console and echo console output back. Alias: `/dconsole`. Permission: `xdiscord.console` (default false).

## Permissions

- `xdiscord.console` - use `/discordconsole`.

## Notes

- Keep the console channel private; it exposes raw console output.
- Admin status is re-checked on every text command, so removing an ID from `adminIDs` takes effect immediately.
- `/session stop` returns a transcript of console activity captured during the session.
- The legacy `discord-console` section in config.yml is separate from this module and governs the console output channel used elsewhere.
