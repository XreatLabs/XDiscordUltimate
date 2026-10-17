# Verification

Links Discord accounts to Minecraft accounts with one-time codes, then applies the verified role, optional LuckPerms group, and nickname sync.

## Flow

1. In Discord, run `/verify` (slash) or `!verify` (text). The bot replies with a code that expires after `code-expiry-minutes`.
2. In-game, run `/verify <code>` while online to claim the code.
3. On success the bot grants `verified-role`, adds the LuckPerms group from `verified-group`, and syncs the nickname when enabled.

## Configuration

Keys under `features.verification`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `one-time-only` | `false` | Refuse a new link when the Discord account is already linked. |
| `kick-after-minutes` | `0` | Kick unverified players after N minutes. `0` disables the kick. |
| `whitelist-mode` | `false` | Treat the server as verify-first; unverified players are placed on the kick timer. |
| `verified-role` | `"Verified"` | Discord role granted on success. |
| `verified-group` | `"verified"` | LuckPerms group granted on success. |
| `sync-nickname` | `true` | Sync the Discord nickname with Minecraft. |
| `code-length` | `6` | Number of digits in a verification code. |
| `code-expiry-minutes` | `5` | Code lifetime. |
| `verify-cooldown-seconds` | `30` | Cooldown applied after a successful verification. |
| `failed-attempt-cooldown-seconds` | `5` | Cooldown applied after each failed attempt. |
| `max-verify-attempts` | `5` | Consecutive failed attempts before a lockout. |
| `verify-lockout-seconds` | `300` | Lockout duration once the failed attempt limit is reached. |

## Commands

In-game:
- `/verify <code>` - link the player. Aliases: `/link`, `/discord`. Permission: `xdiscord.verify` (default true).
- `/unlink` - remove the link. Permission: `xdiscord.unlink` (default true).

Discord:
- `/verify` slash command and `!verify` text command.

## Permissions

- `xdiscord.verify` - redeem a verification code.
- `xdiscord.unlink` - unlink your own account.
- `xdiscord.bypass.verification` (default false) - skip the verification requirement.

## Notes

- Codes are single use and are cleaned up when they expire or the module disables.
- The player must be online when redeeming a code obtained in Discord.
- The Tickets module declares `@DependsOn("verification")`, so it is skipped when verification is disabled.
- With `whitelist-mode` and a non-zero `kick-after-minutes`, unverified players are reminded and then kicked.
