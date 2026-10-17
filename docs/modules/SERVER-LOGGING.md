# Server Logging

Posts server events and command usage to a Discord logs channel. Console output streaming is not active in this version.

## Configuration

Keys under `features.server-logging`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `logs-channel-id` | `""` | Channel ID or name for event embeds. Falls back to `discord.channels.logs`. |
| `log-server-events` | `true` | Gates server event embeds such as startup and shutdown. |
| `log-commands` | `true` | Logs console commands and player command usage. |
| `log-advancements` | `true` | Posts significant advancements to the logs channel. |
| `ignored-commands` | `afk`, `balancetop`, `seen` | Player commands excluded from command logging. |
| `command-message` | `` ```[%timestamp%] %player% used command: %command%``` `` | Format used for command logs. |
| `advancement-message` | `":trophy: **%player%** has made the advancement **[%advancement%]**"` | Format used for advancements. |

## What gets logged

- Server Started and Server Stopping embeds when the plugin enables or disables.
- Console Command entries for commands run from the server console. Commands containing `password`, `token`, or `key`, plus `op` and `deop` commands, are redacted.
- Player command usage, excluding anything listed in `ignored-commands`.
- Significant advancements; recipe advancements are skipped.

Join, leave, and death messages are sent by the Chat Bridge module to its events channel, not by this module. The shipped `log-join-leave` and `log-deaths` keys are not read here.

## Commands

None.

## Permissions

None.

## Notes

- `logs-channel-id` accepts a numeric channel ID or a channel name.
- Console output streaming is disabled in this version; use the Bot Console module if you want live console output in Discord.
- Advancements are only processed when both `enabled` and `log-advancements` are true.
- Keep the logs channel private; command logs can reveal server operations.
