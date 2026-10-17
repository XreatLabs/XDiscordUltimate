# Admin Alerts

Sends security and performance alerts as Discord embeds so staff can react to suspicious activity and resource problems.

## Configuration

Keys under `features.admin-alerts`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `alert-channel` | `"admin-alerts"` | Channel name or ID where alerts are posted. |
| `tps-threshold` | `15.0` | Alert when the 1-minute TPS drops below this value. |
| `ram-threshold` | `90` | Alert when memory usage exceeds this percentage. |
| `dm-alerts` | `true` | Gates the recurring TPS and memory monitoring task. |
| `check-interval` | `30` | Present in config.yml, but resource checks run on a fixed 5 minute interval. |

## Alerts sent

- A player with OP joins the server.
- A player joins from a different IP than their previous session.
- Multiple accounts join from the same IP.
- Suspicious commands run by players or the console: `op`, `deop`, `stop`, `reload`, `restart`, `ban`, `ban-ip`, `pardon`, `whitelist`, `gamemode`, `gamerule`, `difficulty`.
- Monitored blocks placed or broken: TNT, bedrock, command block, barrier.
- TPS below `tps-threshold` or memory above `ram-threshold`, checked every 5 minutes.

## Commands

None. Alerts are pushed automatically.

## Permissions

None. Visibility is controlled by who can see `alert-channel`.

## Notes

- The alert channel can be a name or a numeric ID; alerts are skipped when the channel cannot be resolved.
- `dm-alerts` is the only alert toggle read from config. Block, OP, IP, and command alerts are always enabled in code.
- Resource alerts repeat on every check while the condition persists, so a dedicated channel is recommended.
- If TPS cannot be read reflectively, the module assumes 20.0 and no low-TPS alerts fire.
