# Server Control

Runs a whitelist of server actions from chat messages in a Discord control channel.

## Configuration

Keys under `features.server-control`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `allowed-commands.restart` | `true` | Allow `restart`. |
| `allowed-commands.stop` | `true` | Allow `stop`. |
| `allowed-commands.kick` | `true` | Allow `kick <player> [reason]`. |
| `allowed-commands.tps` | `true` | Allow `tps`. |
| `allowed-commands.list` | `true` | Allow `list`. |
| `require-role` | `true` | Require the control role in addition to admin IDs. |
| `control-role` | `"Server Admin"` | Role name checked when `require-role` is true. |

Control channel resolution: `features.server-control.channel`, then `features.server-control.server-control-channel`, then the hardcoded default `bot-console`. The shipped config does not set either key, so the default channel name applies.

## Usage

Send a message starting with `!` or `/` in the control channel:

- `!restart` - announces a restart and dispatches `/restart` after 30 seconds.
- `!stop` - announces shutdown and stops the server after 5 seconds.
- `!kick <player> [reason]` - kicks an online player (default reason when omitted).
- `!tps` - prints the current 1-minute TPS.
- `!list` - lists online players.

## Authorization

The author must be listed in `adminIDs` or hold the `control-role` role when `require-role` is true. With `require-role: false`, any Discord user who can post in the control channel can run allowed commands, so keep the channel private.

## Commands

No slash commands. Text messages only.

## Permissions

None in plugin.yml; access is enforced through `adminIDs` and the control role.

## Notes

- The default control channel is shared with the Bot Console module (`bot-console`); set a dedicated channel if you want them separate.
- Disallowed commands receive an error reply in the channel.
- Each action is checked against its own `allowed-commands` toggle, so individual commands can be disabled.
- The server console runs the resulting commands, so the bot needs the same rights the console has.
