# Announcements

Broadcasts announcements on an animated boss bar in-game and to Discord, with optional recurring schedules. Also used by the in-game `/announce` command. Nothing is posted to chat: the title and message appear on the boss bar, which shimmers and drains over its duration.

## Configuration

Keys under `features.announcements`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `default-channel` | `"announcements"` | Discord channel used for announcements. |
| `send-to-discord` | `false` | When true, announcements are posted to Discord as embeds. |
| `boss-bar.enabled` | `true` | Show each announcement on a boss bar for its duration. |
| `boss-bar.seconds` | `10` | Seconds before the boss bar fades away. |
| `boss-bar.color` | `"BLUE"` | Boss bar color: `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, `YELLOW`. |
| `boss-bar.style` | `"SOLID"` | Boss bar style: `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12`, `SEGMENTED_20`. |
| `scheduled` | `{}` | Map of recurring announcements. |

Each entry under `scheduled.<id>`:

| Key | Default | Description |
| --- | --- | --- |
| `title` | `""` | Announcement title. |
| `messages` | list | Lines broadcast to players. |
| `interval` | `300` | Seconds between repeats. |
| `enabled` | `true` | Toggle this announcement. |
| `permission` | unset | Only players with this permission receive it. |
| `minecraft` | `true` | Send in-game. |
| `discord` | `true` | Send to Discord. |
| `discord-color` | `"#FFD700"` | Embed color for Discord posts. |

A legacy `features.announcements.announcements` section is still read when `scheduled` is absent. The shipped `show-title`, `title-stay`, `title-fade-in`, `title-fade-out`, `prefix`, `sound`, and root `discord-color` keys are not read in this version; the prefix and sound are fixed in code. The `boss-bar` keys are read.

## Commands

Discord:
- `/announcement list` - show configured announcements.
- `/announcement send <message> [title]` - send a custom announcement.
- `/announcement test` - send a test announcement.
- Text equivalents: `!announcement list`, `!announcement send <message>`.

Sender must be in `adminIDs` or hold an allowed role. Allowed roles default to `admin`, `moderator`, and `staff`, and can be overridden with the root-level `announcements.allowed-roles` list (not present in the shipped config).

In-game:
- `/announce <message>` - broadcast an announcement. Permission: `xdiscord.announce`.

## Permissions

- `xdiscord.announce` - use `/announce`.

## Notes

- Schedule timers start when the module enables; entries with `enabled: false`, or an `interval` of 0 or less, are skipped.
- A welcome announcement is sent to players when they join and is always enabled in code.
- Announcements also appear on a boss bar above the hotbar; tune it with the `boss-bar` keys.
- Override the embed style with `messages.embeds.announcement`.
- Colors accept hex values such as `#FFD700`.
