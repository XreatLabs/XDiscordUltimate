# Booster Perks

Gives Discord server boosters in-game perks: a LuckPerms group plus optional reward and removal commands.

## Configuration

Keys under `features.booster-perks`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `permission-group` | `"booster"` | LuckPerms group added while boosting and removed afterwards. |
| `sync-interval` | `300` | Seconds between booster scans. |
| `use-native-detection` | `true` | Treat native Discord boosters (`isBoosting`) as boosting. |
| `booster-role-id` | `""` | Custom Discord role ID that also counts as a booster. |
| `additional-booster-roles` | `[]` | Extra Discord role IDs that count as boosters. |
| `reward-commands` | list | Console commands run when a linked player starts boosting. Supports `%player%` and `%uuid%`. |
| `remove-commands` | `[]` | Console commands run when boosting stops. |

## Behavior

- Scans the guild asynchronously every `sync-interval` seconds.
- Only linked Minecraft accounts receive perks; unlinked boosters are ignored.
- On start: adds `lp user <uuid> parent add <group>` when LuckPerms is present, runs `reward-commands` when the player is online, then sends a thank-you message.
- On removal: removes the group, runs `remove-commands` when the player is online, then sends an expiry message.

A member counts as a booster when native detection is enabled and they boost, or when they hold any configured booster role ID.

## Commands

None.

## Permissions

None. Boosters are detected from Discord; no Minecraft permission checks apply.

## Notes

- Role IDs are Discord role IDs from Developer Mode, not role names.
- The LuckPerms group is only managed when LuckPerms is installed; on other servers only the command lists run.
- Reward and removal commands are dispatched from the console; test them before use.
- Detection waits for the next sync cycle, so perks can take up to `sync-interval` seconds to appear after boosting.
