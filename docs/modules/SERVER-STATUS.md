# Server Status

Keeps a single status embed up to date in a Discord channel: player count, TPS, memory, uptime, version, health, and an online player list.

## Configuration

Keys under `features.server-status`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `channel-id` | `""` | Target Discord channel ID. Required. |
| `update-interval` | `60` | Seconds between embed updates. |
| `message-id` | `""` | ID of the status message. Written back automatically. |

## Behavior

- Starts when the Discord bot becomes ready.
- Runs the first update about one second after startup, then every `update-interval` seconds.
- Edits the existing message when `message-id` resolves; otherwise it posts a new message and saves the new ID to config.
- The embed color follows TPS: green at 18 or above, yellow at 15 or above, red below 15.

Embed fields: Players, TPS, Memory, Uptime, Version, Status, and Online Players (up to 20 names, then "+N more").

## Commands

None. The module only maintains the embed.

## Permissions

None.

## Notes

- `channel-id` must be a channel ID; channel name lookup is not supported.
- To replace the status message, clear `message-id` and reload; the module posts a fresh message.
- TPS is taken from the 1-minute average and capped at 20.0 for display.
- Updates run asynchronously, so keep the interval reasonable on busy servers.
