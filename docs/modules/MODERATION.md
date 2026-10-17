# Moderation

Bridges moderation between Minecraft and Discord: logs punishments, mirrors actions between platforms, and provides the chat filter used by auto-moderation.

## Configuration

Keys under `features.moderation`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `sync-bans` | `true` | Minecraft bans also ban the linked Discord user; Discord bans are applied to linked Minecraft players. |
| `sync-kicks` | `false` | Post Minecraft kicks to Discord. |
| `sync-mutes` | `false` | Sync mutes: Discord timeouts and the muted role mute the player in Minecraft chat. |
| `muted-role` | `"Muted"` | Discord role used for permanent mute sync. |
| `log-channel` | `"mod-logs"` | Channel for moderation logs. |
| `auto-moderate` | `true` | Enables the chat filter and Minecraft command logging. |
| `report-cooldown-minutes` | `5` | Cooldown between `/report` uses. |
| `require-verification-for-reports` | `true` | Only verified players can report. |
| `filter-words` | list | Blocked words. Entries starting with `regex:` are treated as regular expressions. |

## Chat filter behavior (auto-moderate)

A message is cancelled when it contains a filter word, six or more repeated letters (for example `aaaaaa`), or is longer than 5 characters and more than 70 percent uppercase. Filtered messages are logged to console, the database, and the Discord moderation channel.

Spam detection runs independently of `auto-moderate`: a repeated message, or more than 3 messages within 1.5 seconds, is cancelled with a warning.

## Command logging

With `auto-moderate` enabled, `/ban`, `/tempban`, `/ban-ip`, `/kick`, `/mute`, `/tempmute`, and `/warn` are detected after they run and posted to the log channel. Tempban and tempmute durations are parsed from the command arguments.

## Commands

- `/report <player> <reason>` - report a player. Alias: `/reportplayer`. Permission: `xdiscord.report` (default true).

## Permissions

- `xdiscord.report` - send reports.

## Notes

- The filter applies to every player; there is no permission bypass for chat filtering in this version.
- Moderation history and muted players are persisted and reloaded when the module is enabled.
- Discord bans are applied as Minecraft name bans with the reason "Discord Ban Sync".
- Invalid or empty filter-word entries and duplicate punishment handlers are the usual cause of a noisy log channel; verify your list first.
