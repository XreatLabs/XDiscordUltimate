# Player Stats

Tracks per-player statistics, shows them via `/stats` and PlaceholderAPI, and powers leaderboard and rank placeholders.

## Tracked stats

In display order: `kills`, `deaths`, `kd-ratio`, `mob-kills`, `playtime`, `blocks-broken`, `blocks-placed`, `damage-dealt`, `damage-taken`, `distance-walked`, `jumps`, `fish-caught`, `items-crafted`, `advancements`.

Data is recorded in the plugin database as players join, chat, die, and complete advancements.

## Configuration

Keys under `features.player-stats`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `use-placeholderapi` | `true` | Parse PlaceholderAPI placeholders in stats text. |
| `thousands-separator` | `true` | Format large numbers with separators. |
| `kd-decimals` | `2` | Decimals for the K/D ratio (clamped to 0-6). |
| `playtime-format` | `"auto"` | `auto`, `clock`, `hours`, or `minutes`. |
| `show-rank` | `true` | Append the player rank to values. |
| `rank-format` | `"%value% (#%rank%)"` | Format used when `show-rank` is true. |
| `top-cache-minutes` | `10` | Minutes between leaderboard cache refreshes. |
| `snapshots.auto-reset` | `true` | Reset weekly and monthly snapshots after 7 or 30 days. |
| `embed.*` | see config.yml | Discord embed title, color, footer, thumbnail, inline-fields, timestamp. |
| `lines.<stat>.enabled` | `true` | Toggle a stat line. |
| `lines.<stat>.label` | label with emoji | Label used in-game and as the Discord field name. |
| `tab.enabled` | `false` | Opt-in tab list header and footer (not shipped in config.yml). |
| `tab.header`, `tab.footer` | `""` | Tab text. Supports `%player%`, stat placeholders, and `%rank%`. |
| `tab.update-seconds` | `30` | Tab refresh interval (minimum 5). |

A `lines` entry may also be a plain boolean instead of a section.

## Commands

- `/stats [player]` - view stats. Aliases: `/playerstats`, `/pstats`.
- `/xdiscord stats snapshot <weekly|monthly>` - force a snapshot update.
- `/xdiscord stats reset <weekly|monthly>` - reset snapshot values.

Discord: `/stats` slash command.

## Placeholders

With PlaceholderAPI (`%xdiscord_...%`):
- `stats_<stat>` - a single stat value.
- `stats_top_<stat>_<n>` - player name at position n; append `_name` for the name only.
- `stats_weekly_<stat>`, `stats_monthly_<stat>` - change since the last snapshot.
- Also: `verified`, `linked`, `discord_name`, `discord_id`, `discord_tag`, `discord_avatar`, `has_nitro`, `is_booster`, `bot_status`, `guild_name`, `guild_members`, `guild_online`.

## Notes

- Stat ids use hyphens in placeholders, for example `%xdiscord_stats_kd-ratio%`.
- Top placeholders are served from a cache refreshed every `top-cache-minutes`.
- Weekly and monthly values show change since the last snapshot and reset automatically when `snapshots.auto-reset` is true.
- Disabling a line hides it from stats output but does not stop data collection.
